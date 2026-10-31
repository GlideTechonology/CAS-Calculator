package com.magic.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A unit definition: converts values to and from the category base unit. */
class UnitDef(
    val name: String,
    val symbol: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

class UnitCategory(val name: String, val units: List<UnitDef>)

/** Linear unit: 1 [symbol] = [factor] base unit. */
private fun lin(name: String, symbol: String, factor: Double) =
    UnitDef(name, symbol, { it * factor }, { it / factor })

val unitCategories: List<UnitCategory> = listOf(
    UnitCategory(
        "Length", listOf( // Base: meter
            lin("Milimetre", "mm", 0.001),
            lin("Santimetre", "cm", 0.01),
            lin("Metre", "m", 1.0),
            lin("Kilometre", "km", 1000.0),
            lin("İnç", "in", 0.0254),
            lin("Fit", "ft", 0.3048),
            lin("Yarda", "yd", 0.9144),
            lin("Mil", "mi", 1609.344),
            lin("Deniz mili", "nmi", 1852.0)
        )
    ),
    UnitCategory(
        "Weight", listOf( // temel: kilogram
            lin("Miligram", "mg", 1e-6),
            lin("Gram", "g", 1e-3),
            lin("Kilogram", "kg", 1.0),
            lin("Ton", "t", 1000.0),
            lin("Ons", "oz", 0.028349523125),
            lin("Pound", "lb", 0.45359237),
            lin("Karat", "ct", 0.0002)
        )
    ),
    UnitCategory(
        "Temperature", listOf( // temel: Celsius
            UnitDef("Celsius", "°C", { it }, { it }),
            UnitDef("Fahrenheit", "°F", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
            UnitDef("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
        )
    ),
    UnitCategory(
        "Area", listOf( // temel: m²
            lin("Santimetrekare", "cm²", 1e-4),
            lin("Metrekare", "m²", 1.0),
            lin("Dönüm", "dönüm", 1000.0),
            lin("Hektar", "ha", 10_000.0),
            lin("Kilometrekare", "km²", 1e6),
            lin("Fitkare", "ft²", 0.09290304),
            lin("Akre", "ac", 4046.8564224)
        )
    ),
    UnitCategory(
        "Volume", listOf( // temel: litre
            lin("Mililitre", "mL", 0.001),
            lin("Litre", "L", 1.0),
            lin("Metreküp", "m³", 1000.0),
            lin("Çay kaşığı", "tsp", 0.00492892159375),
            lin("Yemek kaşığı", "tbsp", 0.01478676478125),
            lin("Su bardağı (200 mL)", "bardak", 0.2),
            lin("Galon (ABD)", "gal", 3.785411784),
            lin("Sıvı ons (ABD)", "fl oz", 0.0295735295625)
        )
    ),
    UnitCategory(
        "Speed", listOf( // temel: m/s
            lin("Metre/saniye", "m/s", 1.0),
            lin("Kilometre/saat", "km/sa", 1.0 / 3.6),
            lin("Mil/saat", "mph", 0.44704),
            lin("Knot", "kn", 1852.0 / 3600.0)
        )
    ),
    UnitCategory(
        "Time", listOf( // temel: saniye
            lin("Milisaniye", "ms", 0.001),
            lin("Saniye", "sn", 1.0),
            lin("Dakika", "dk", 60.0),
            lin("Saat", "sa", 3600.0),
            lin("Gün", "gün", 86_400.0),
            lin("Hafta", "hafta", 604_800.0),
            lin("Yıl (365 gün)", "yıl", 31_536_000.0)
        )
    ),
    UnitCategory(
        "Data", listOf( // temel: bayt (1 KB = 1024 B)
            lin("Bit", "bit", 0.125),
            lin("Bayt", "B", 1.0),
            lin("Kilobayt", "KB", 1024.0),
            lin("Megabayt", "MB", 1024.0 * 1024),
            lin("Gigabayt", "GB", 1024.0 * 1024 * 1024),
            lin("Terabayt", "TB", 1024.0 * 1024 * 1024 * 1024)
        )
    ),
    UnitCategory(
        "Angle", listOf( // temel: derece
            lin("Derece", "°", 1.0),
            lin("Radyan", "rad", 180.0 / Math.PI),
            lin("Grad", "grad", 0.9)
        )
    )
)

@Composable
fun UnitConverterScreen(textColor: Color, onBack: () -> Unit) {
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var fromIndex by rememberSaveable { mutableIntStateOf(2) }
    var input by rememberSaveable { mutableStateOf("1") }

    val category = unitCategories[categoryIndex]
    val from = category.units[fromIndex.coerceIn(0, category.units.lastIndex)]
    val inputValue = input.replace(',', '.').toDoubleOrNull()
    val baseValue = inputValue?.let { from.toBase(it) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp)) {
        ToolTopBar(title = Translator.tr("Unit Converter"), textColor = textColor, onBack = onBack)
        Spacer(modifier = Modifier.height(12.dp))

        // Category selection
        ChipRow(
            labels = unitCategories.map { Translator.tr(it.name) },
            selected = categoryIndex,
            textColor = textColor,
            onSelect = {
                if (it != categoryIndex) {
                    categoryIndex = it
                    fromIndex = 0
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))

        val isTemperature = category.name == "Temperature"
        OutlinedTextField(
            value = input,
            onValueChange = { new ->
                input = new.filterIndexed { idx, c ->
                    c.isDigit() || c == '.' || c == ',' || (c == '-' && (idx == 0 || isTemperature))
                }
            },
            label = { Text("${Translator.tr("Değer")} (${from.symbol})") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (isTemperature) KeyboardType.Text else KeyboardType.Decimal),
            trailingIcon = if (isTemperature) {
                {
                    IconButton(onClick = {
                        input = if (input.startsWith("-")) input.removePrefix("-") else if (input.isNotEmpty()) "-$input" else "-"
                    }) {
                        Text("±", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
            } else null,
            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = textColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                cursorColor = accentColor,
                focusedLabelColor = accentColor,
                unfocusedLabelColor = textColor.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(Translator.tr("Kaynak birim"), color = textColor.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        ChipRow(
            labels = category.units.map { it.symbol },
            selected = fromIndex,
            textColor = textColor,
            onSelect = { fromIndex = it }
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(category.units.size) { i ->
                val unit = category.units[i]
                val isSource = i == fromIndex
                val valueText = baseValue?.let { formatNumber(unit.fromBase(it)) } ?: "—"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSource) accentColor.copy(alpha = 0.18f) else textColor.copy(alpha = 0.05f))
                        .clickable {
                            // Tap row → make this unit the source, keep the numerical value
                            if (baseValue != null && !isSource) {
                                input = formatNumber(unit.fromBase(baseValue))
                                fromIndex = i
                            } else if (baseValue != null) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText(Translator.tr("Birim"), valueText))
                                Toast.makeText(context, Translator.tr("Kopyalandı"), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(Translator.tr(unit.name), color = textColor.copy(alpha = 0.7f), fontSize = 13.sp)
                        Text(
                            valueText,
                            color = textColor,
                            fontSize = 22.sp,
                            fontWeight = if (isSource) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(unit.symbol, color = if (isSource) accentColor else textColor.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, textColor: Color, onSelect: (Int) -> Unit) {
    val accentColor = LocalAccentColor.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { i, label ->
            FilterChip(
                selected = i == selected,
                onClick = { onSelect(i) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = textColor,
                    selectedContainerColor = accentColor,
                    selectedLabelColor = accentColor.contrastTextColor()
                )
            )
        }
    }
}
