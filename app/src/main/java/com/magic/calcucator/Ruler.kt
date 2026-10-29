package com.magic.calcucator

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor

/**
 * On-screen ruler. Point 0 is aligned with the top physical edge of the screen;
 * align the object with the phone's top bezel and drag the marker line to the tip.
 * Left edge: centimeters / millimeters, Right edge: inches (1/16 divisions).
 */
@Composable
fun RulerScreen(settings: AppSettings, textColor: Color, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("CalcSettings", Context.MODE_PRIVATE) }
    val metrics = context.resources.displayMetrics

    var calibration by remember { mutableFloatStateOf(prefs.getFloat("rulerCalibration", 1f)) }
    var markerY by remember { mutableFloatStateOf(-1f) }
    var lastHapticMm by remember { mutableFloatStateOf(-1f) }

    val pxPerMm = metrics.ydpi / 25.4f * calibration
    val pxPerInch = metrics.ydpi * calibration

    val tickColor = textColor
    val labelPaint = remember(textColor) {
        Paint().apply {
            isAntiAlias = true
            color = textColor.toArgb()
            textSize = 18f * (context.resources.configuration.fontScale * metrics.density)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    fun onMarkerMoved(y: Float) {
        markerY = y.coerceAtLeast(0f)
        // Light haptic "tick" on every half centimeter instead of every millimeter
        val halfCm = floor(markerY / (pxPerMm * 5f))
        if (settings.vibrationEnabled && halfCm != lastHapticMm) {
            lastHapticMm = halfCm
            vibrateWithStrength(context, (settings.vibrationStrength * 0.4f).coerceAtLeast(0.1f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pxPerMm) { detectTapGestures { onMarkerMoved(it.y) } }
                .pointerInput(pxPerMm) {
                    detectDragGestures { change, _ -> onMarkerMoved(change.position.y) }
                }
        ) {
            val h = size.height
            val w = size.width

            // --- Left: millimeters / centimeters ---
            var mm = 0
            while (mm * pxPerMm <= h) {
                val y = mm * pxPerMm
                val len = when {
                    mm % 10 == 0 -> 48.dp.toPx()
                    mm % 5 == 0 -> 32.dp.toPx()
                    else -> 20.dp.toPx()
                }
                drawLine(
                    color = tickColor,
                    start = Offset(0f, y),
                    end = Offset(len, y),
                    strokeWidth = if (mm % 10 == 0) 3.dp.toPx() else 1.5f.dp.toPx()
                )
                if (mm % 10 == 0 && mm > 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        (mm / 10).toString(), len + 10.dp.toPx(), y + labelPaint.textSize / 2.5f, labelPaint
                    )
                }
                mm++
            }

            // --- Right: inches (1/16 divisions) ---
            var s = 0
            val pxPerSixteenth = pxPerInch / 16f
            while (s * pxPerSixteenth <= h) {
                val y = s * pxPerSixteenth
                val len = when {
                    s % 16 == 0 -> 48.dp.toPx()
                    s % 8 == 0 -> 36.dp.toPx()
                    s % 4 == 0 -> 26.dp.toPx()
                    s % 2 == 0 -> 18.dp.toPx()
                    else -> 12.dp.toPx()
                }
                drawLine(
                    color = tickColor,
                    start = Offset(w - len, y),
                    end = Offset(w, y),
                    strokeWidth = if (s % 16 == 0) 3.dp.toPx() else 1.5f.dp.toPx()
                )
                if (s % 16 == 0 && s > 0) {
                    val label = (s / 16).toString()
                    val tw = labelPaint.measureText(label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label, w - len - 10.dp.toPx() - tw, y + labelPaint.textSize / 2.5f, labelPaint
                    )
                }
                s++
            }

            // --- Measurement indicator line ---
            if (markerY >= 0f) {
                drawLine(AccentOrange.copy(alpha = 0.25f), Offset(0f, 0f), Offset(0f, markerY), strokeWidth = 8.dp.toPx())
                drawLine(AccentOrange, Offset(0f, markerY), Offset(w, markerY), strokeWidth = 2.dp.toPx())
            }
        }

        // --- Orta panel: okuma + kalibrasyon ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp)
                .padding(top = 40.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = onBack) { Text(Translator.tr("← Calculator"), color = textColor) }
                Text(Translator.tr("Ruler"), color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (markerY >= 0f) {
                    val cm = markerY / pxPerMm / 10f
                    val inch = markerY / pxPerInch
                    Text(String.format(Locale.US, "%.1f cm", cm), color = AccentOrange, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "%.2f %s", inch, Translator.tr("inç")), color = textColor.copy(alpha = 0.7f), fontSize = 20.sp)
                } else {
                    Text(
                        Translator.tr("Align object to top edge,\nthen tap or drag to its end"),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                     "${Translator.tr("İnce Ayar (Kalibrasyon): %")}${String.format(Locale.US, "%.1f", calibration * 100)}",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Slider(
                    value = calibration,
                    onValueChange = { calibration = it },
                    onValueChangeFinished = { prefs.edit().putFloat("rulerCalibration", calibration).apply() },
                    valueRange = 0.9f..1.1f,
                    colors = SliderDefaults.colors(thumbColor = AccentOrange, activeTrackColor = AccentOrange)
                )
                Text(
                    Translator.tr("Not: Üreticiler bazen ekran ydpi değerini yanlış raporlar.\nEğer cetvel hatalıysa gerçek bir cetvel koyarak kaydırıcı ile çizgileri eşitleyin."),
                    color = textColor.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = {
                    calibration = 1f
                    prefs.edit().putFloat("rulerCalibration", 1f).apply()
                }) { Text(Translator.tr("Sıfırla"), color = AccentOrange) }
            }
        }
    }
}
