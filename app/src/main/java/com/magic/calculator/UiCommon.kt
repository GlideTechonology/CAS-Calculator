package com.magic.calculator

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import kotlin.math.abs

val AccentOrange = Color(0xFFFF9500)

/** Common top app bar for utility screens (History, Unit Converter, Ruler). */
@Composable
fun ToolTopBar(
    title: String,
    textColor: Color,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
        }
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

/** Formats number for readability: up to 12 significant digits, trailing zeros stripped. */
fun formatNumber(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    if (value == 0.0) return "0"
    val absV = abs(value)
    return if (absV >= 1e15 || absV < 1e-9) {
        String.format(Locale.US, "%.6e", value)
    } else {
        BigDecimal.valueOf(value).round(MathContext(12)).stripTrailingZeros().toPlainString()
    }
}
