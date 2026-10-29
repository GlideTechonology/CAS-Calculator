package com.magic.calcucator


import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.magic.calcucator.core.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun DailyToolsScreen(textColor: Color, initialToolName: String? = null, onBack: () -> Unit) {
    val toolsList = listOf(
        DailyTool(Translator.tr("Currency Converter"), Icons.Filled.AttachMoney),
        DailyTool(Translator.tr("Discount"), Icons.Filled.LocalOffer),
        DailyTool(Translator.tr("Sales Tax / VAT"), Icons.Filled.AccountBalance),
        DailyTool(Translator.tr("Tip & Split"), Icons.Filled.Restaurant),
        DailyTool(Translator.tr("BMI"), Icons.Filled.Accessibility),
        DailyTool(Translator.tr("Loan"), Icons.Filled.MonetizationOn),
        DailyTool(Translator.tr("GPA / Exam"), Icons.Filled.School),
        DailyTool(Translator.tr("Shapes & Area"), Icons.Filled.CropSquare),
        DailyTool(Translator.tr("Date Calculator"), Icons.Filled.DateRange),
        DailyTool(Translator.tr("Custom Equations"), Icons.Filled.Edit)
    )
    var selectedTool by remember { mutableStateOf<DailyTool?>(toolsList.find { it.name == initialToolName }) }
    
    androidx.activity.compose.BackHandler(enabled = selectedTool != null) {
        selectedTool = null
    }

    AnimatedContent(
        targetState = selectedTool,
        transitionSpec = {
            if (targetState != null) {
                // Entering a tool: slide in from right, fade in
                slideInHorizontally(tween(300)) { width -> width } + fadeIn(tween(300)) togetherWith
                slideOutHorizontally(tween(300)) { width -> -width } + fadeOut(tween(300))
            } else {
                // Going back to grid: slide in from left, fade in
                slideInHorizontally(tween(300)) { width -> -width } + fadeIn(tween(300)) togetherWith
                slideOutHorizontally(tween(300)) { width -> width } + fadeOut(tween(300))
            }
        },
        label = "daily_tools_animation"
    ) { tool ->
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp)) {
            if (tool == null) {
                ToolTopBar(title = Translator.tr("Daily Tools"), textColor = textColor, onBack = onBack)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(toolsList) { t ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTool = t },
                            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(t.icon, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(t.name, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            } else {
                ToolTopBar(title = tool.name, textColor = textColor, onBack = { selectedTool = null })
                Spacer(modifier = Modifier.height(16.dp))
                when (tool.name) {
                    Translator.tr("Currency Converter") -> CurrencyTool(textColor)
                    Translator.tr("Discount") -> DiscountTool(textColor)
                    Translator.tr("Sales Tax / VAT") -> VatTool(textColor)
                    Translator.tr("Tip & Split") -> TipTool(textColor)
                    Translator.tr("BMI") -> BmiTool(textColor)
                    Translator.tr("Loan") -> LoanTool(textColor)
                    Translator.tr("GPA / Exam") -> GpaTool(textColor)
                    Translator.tr("Shapes & Area") -> AreaTool(textColor)
                    Translator.tr("Date Calculator") -> DateCalculatorUI(textColor)
                    Translator.tr("Custom Equations") -> CustomEquationsTool(textColor)
                }
            }
        }
    }
}

data class DailyTool(val name: String, val icon: ImageVector)

@Composable
fun DiscountTool(textColor: Color) {
    var price by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text(Translator.tr("Original Price")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = discount,
            onValueChange = { discount = it },
            label = { Text(Translator.tr("Discount Oranı (%)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val p = price.toDoubleOrNull() ?: 0.0
        val d = discount.toDoubleOrNull() ?: 0.0
        val saved = p * (d / 100)
        val finalPrice = p - saved
        
        Text("${Translator.tr("Discount Miktarı:")} ${String.format(Locale.US, "%.2f", saved)}", color = AccentOrange, fontSize = 20.sp)
        Text("${Translator.tr("Final Price:")} ${String.format(Locale.US, "%.2f", finalPrice)}", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VatTool(textColor: Color) {
    var price by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("20") }
    var isVatIncluded by remember { mutableStateOf(false) }
    
    Column {
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text(Translator.tr("Amount")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = vat,
            onValueChange = { vat = it },
            label = { Text(Translator.tr("Tax Rate (%)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = isVatIncluded, onCheckedChange = { isVatIncluded = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isVatIncluded) Translator.tr("Calculate from Tax Inclusive") else Translator.tr("Calculate from Tax Exclusive"), color = textColor)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val p = price.toDoubleOrNull() ?: 0.0
        val v = vat.toDoubleOrNull() ?: 0.0
        
        val vatAmount = if (isVatIncluded) p - (p / (1 + v / 100)) else p * (v / 100)
        val finalPrice = if (isVatIncluded) p else p + vatAmount
        val basePrice = if (isVatIncluded) p - vatAmount else p
        
        Text("${Translator.tr("Tax Amount:")} ${String.format(Locale.US, "%.2f", vatAmount)}", color = AccentOrange, fontSize = 20.sp)
        Text("${Translator.tr("Price without Tax:")} ${String.format(Locale.US, "%.2f", basePrice)}", color = textColor, fontSize = 18.sp)
        Text("${Translator.tr("Total Price:")} ${String.format(Locale.US, "%.2f", finalPrice)}", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TipTool(textColor: Color) {
    var bill by remember { mutableStateOf("") }
    var tipPercent by remember { mutableStateOf("10") }
    var people by remember { mutableStateOf("1") }
    
    Column {
        OutlinedTextField(
            value = bill,
            onValueChange = { bill = it },
            label = { Text(Translator.tr("Hesap Amountı")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val numPeople = people.trim().toIntOrNull()
        val isZeroOrNegative = people.isNotBlank() && (numPeople == null || numPeople <= 0)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tipPercent,
                onValueChange = { tipPercent = it },
                label = { Text(Translator.tr("Tip & Split (%)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
            )

            OutlinedTextField(
                value = people,
                onValueChange = { people = it },
                label = { Text(Translator.tr("Number of People")) },
                isError = isZeroOrNegative,
                supportingText = if (isZeroOrNegative) {
                    { Text(Translator.tr("En az 1 kişi olmalıdır!"), color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val b = bill.toDoubleOrNull() ?: 0.0
        val t = tipPercent.toDoubleOrNull() ?: 0.0
        val p = numPeople?.toDouble() ?: 1.0
        
        val tipAmount = b * (t / 100)
        val total = b + tipAmount
        
        Text("${Translator.tr("Toplam Tip & Split:")} ${String.format(Locale.US, "%.2f", tipAmount)}", color = AccentOrange, fontSize = 20.sp)
        Text("${Translator.tr("Total Bill:")} ${String.format(Locale.US, "%.2f", total)}", color = textColor, fontSize = 20.sp)
        
        if (isZeroOrNegative) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "⚠️ ${Translator.tr("Kişi sayısı 0 olamaz (Sıfıra bölme hatası)! En az 1 kişi giriniz.")}",
                color = MaterialTheme.colorScheme.error,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text("${Translator.tr("Per Person:")} —", color = textColor.copy(alpha = 0.5f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        } else {
            val perPerson = total / p
            Text("${Translator.tr("Per Person:")} ${String.format(Locale.US, "%.2f", perPerson)}", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BmiTool(textColor: Color) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text(Translator.tr("Weight (kg)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text(Translator.tr("Height (cm)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val w = weight.toDoubleOrNull() ?: 0.0
        val h = (height.toDoubleOrNull() ?: 0.0) / 100.0
        
        if (w > 0 && h > 0) {
            val bmi = w / (h * h)
            val status = when {
                bmi < 18.5 -> "Underweight"
                bmi < 25 -> "Normal"
                bmi < 30 -> "Overweight"
                else -> "Obese"
            }
            Text("${Translator.tr("Body Mass Index:")} ${String.format(Locale.US, "%.1f", bmi)}", color = AccentOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${Translator.tr("Status:")} $status", color = textColor, fontSize = 20.sp)
        }
    }
}

@Composable
fun CurrencyTool(textColor: Color) {
    var amount by remember { mutableStateOf("1") }
    var baseCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf("TRY") }
    var resultText by remember { mutableStateOf(Translator.tr("Calculating...")) }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    val currencies = listOf("USD", "EUR", "TRY", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "RUB")
    
    LaunchedEffect(Unit) {
        try {
            val jsonStr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL("https://api.frankfurter.app/latest?from=USD").readText()
            }
            val json = org.json.JSONObject(jsonStr)
            val ratesJson = json.getJSONObject("rates")
            val newRates = mutableMapOf<String, Double>()
            newRates["USD"] = 1.0 // Base
            for (key in ratesJson.keys()) {
                newRates[key] = ratesJson.getDouble(key)
            }
            rates = newRates
        } catch (e: Exception) {
            resultText = Translator.tr("No internet connection or API error.")
        }
    }
    
    LaunchedEffect(amount, baseCurrency, targetCurrency, rates) {
        if (rates.isEmpty()) return@LaunchedEffect
        val a = amount.toDoubleOrNull()
        if (a == null) {
            resultText = Translator.tr("Enter a valid amount")
            return@LaunchedEffect
        }
        
        val baseRate = rates[baseCurrency] ?: 1.0
        val targetRate = rates[targetCurrency] ?: 1.0
        
        val amountInUsd = a / baseRate
        val converted = amountInUsd * targetRate
        
        resultText = "${String.format(java.util.Locale.US, "%.2f", converted)} $targetCurrency"
    }

    Column {
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text(Translator.tr("Amount")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(Translator.tr("From"), color = textColor.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(200.dp)) {
                    items(currencies) { curr ->
                        Button(
                            onClick = { baseCurrency = curr },
                            colors = ButtonDefaults.buttonColors(containerColor = if (curr == baseCurrency) AccentOrange else textColor.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(curr, fontSize = 12.sp, color = if (curr == baseCurrency) Color.White else textColor)
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(Translator.tr("To"), color = textColor.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(200.dp)) {
                    items(currencies) { curr ->
                        Button(
                            onClick = { targetCurrency = curr },
                            colors = ButtonDefaults.buttonColors(containerColor = if (curr == targetCurrency) AccentOrange else textColor.copy(alpha = 0.1f)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(curr, fontSize = 12.sp, color = if (curr == targetCurrency) Color.White else textColor)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(resultText, color = textColor, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LoanTool(textColor: Color) {
    var principal by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var isMonthlyRate by remember { mutableStateOf(true) }
    
    Column {
        OutlinedTextField(
            value = principal, onValueChange = { principal = it },
            label = { Text(Translator.tr("Loan Amount")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(selected = isMonthlyRate, onClick = { isMonthlyRate = true }, label = { Text(Translator.tr("Aylık Faiz")) })
            FilterChip(selected = !isMonthlyRate, onClick = { isMonthlyRate = false }, label = { Text(Translator.tr("Yıllık Faiz")) })
        }
        OutlinedTextField(
            value = interestRate, onValueChange = { interestRate = it },
            label = { Text(if (isMonthlyRate) Translator.tr("Monthly Interest Rate (%)") else Translator.tr("Annual Interest Rate (%)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = months, onValueChange = { months = it },
            label = { Text(Translator.tr("Loan Term (Months)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val p = principal.toDoubleOrNull() ?: 0.0
        val enteredRate = interestRate.toDoubleOrNull() ?: 0.0
        val r = if (isMonthlyRate) enteredRate / 100 else enteredRate / 100 / 12
        val n = months.toDoubleOrNull() ?: 0.0
        
        if (p > 0 && r >= 0 && n > 0) {
            val emi = if (r > 0) (p * r * java.lang.Math.pow(1 + r, n)) / (java.lang.Math.pow(1 + r, n) - 1) else p / n
            val totalPayment = emi * n
            val totalInterest = totalPayment - p
            
            Text("${Translator.tr("Monthly Payment:")} ${String.format(java.util.Locale.US, "%.2f", emi)}", color = AccentOrange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${Translator.tr("Total Interest:")} ${String.format(java.util.Locale.US, "%.2f", totalInterest)}", color = textColor, fontSize = 20.sp)
            Text("${Translator.tr("Total Payment:")} ${String.format(java.util.Locale.US, "%.2f", totalPayment)}", color = textColor, fontSize = 20.sp)
        }
    }
}

@Composable
fun GpaTool(textColor: Color) {
    var score by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = score, onValueChange = { score = it },
            label = { Text(Translator.tr("Exam Score (0-100)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        val s = score.toDoubleOrNull() ?: -1.0
        if (s in 0.0..100.0) {
            val gpa = when {
                s >= 90 -> "AA (4.0)"
                s >= 85 -> "BA (3.5)"
                s >= 80 -> "BB (3.0)"
                s >= 75 -> "CB (2.5)"
                s >= 70 -> "CC (2.0)"
                s >= 65 -> "DC (1.5)"
                s >= 60 -> "DD (1.0)"
                s >= 50 -> "FD (0.5)"
                else -> "FF (0.0)"
            }
            Text("${Translator.tr("Letter Grade:")} $gpa", color = AccentOrange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AreaTool(textColor: Color) {
    var selectedShape by remember { mutableStateOf("Square") }
    val shapes = listOf("Square", "Rectangle", "Circle", "Triangle")
    
    var val1 by remember { mutableStateOf("") }
    var val2 by remember { mutableStateOf("") }
    
    Column {
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(100.dp)) {
            items(shapes) { shape ->
                Button(
                    onClick = { 
                        selectedShape = shape
                        val1 = ""
                        val2 = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (shape == selectedShape) AccentOrange else textColor.copy(alpha = 0.1f)),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(Translator.tr(shape), color = if (shape == selectedShape) Color.White else textColor)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (selectedShape) {
            "Square" -> {
                OutlinedTextField(
                    value = val1, onValueChange = { val1 = it },
                    label = { Text(Translator.tr("Side Length (a)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            "Rectangle" -> {
                OutlinedTextField(
                    value = val1, onValueChange = { val1 = it },
                    label = { Text(Translator.tr("Width (a)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = val2, onValueChange = { val2 = it },
                    label = { Text(Translator.tr("Height (b)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            "Circle" -> {
                OutlinedTextField(
                    value = val1, onValueChange = { val1 = it },
                    label = { Text(Translator.tr("Radius (r)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
            "Triangle" -> {
                OutlinedTextField(
                    value = val1, onValueChange = { val1 = it },
                    label = { Text(Translator.tr("Base (b)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = val2, onValueChange = { val2 = it },
                    label = { Text(Translator.tr("Height (h)")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val v1 = val1.toDoubleOrNull() ?: 0.0
        val v2 = val2.toDoubleOrNull() ?: 0.0
        
        var area = 0.0
        var perimeter = 0.0
        var showPerimeter = true
        
        val isReady = when (selectedShape) {
            "Square", "Circle" -> v1 > 0
            "Rectangle", "Triangle" -> v1 > 0 && v2 > 0
            else -> false
        }
        
        if (isReady) {
            when (selectedShape) {
                "Square" -> { area = v1 * v1; perimeter = 4 * v1 }
                "Rectangle" -> { area = v1 * v2; perimeter = 2 * (v1 + v2) }
                "Circle" -> { area = kotlin.math.PI * v1 * v1; perimeter = 2 * kotlin.math.PI * v1 }
                "Triangle" -> { area = (v1 * v2) / 2; showPerimeter = false }
            }
            
            Text("${Translator.tr("Area:")} ${String.format(java.util.Locale.US, "%.2f", area)}", color = AccentOrange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (showPerimeter) {
                Text("${Translator.tr("Perimeter:")} ${String.format(java.util.Locale.US, "%.2f", perimeter)}", color = textColor, fontSize = 20.sp)
            }
        }
    }
}


@Composable
fun DateCalculatorUI(textColor: Color) {
    var mode by remember { mutableStateOf(0) } // 0: Fark, 1: Ekle/Cikar
    
    val cal = java.util.Calendar.getInstance()
    var day1 by remember { mutableStateOf(cal.get(java.util.Calendar.DAY_OF_MONTH).toString()) }
    var month1 by remember { mutableStateOf((cal.get(java.util.Calendar.MONTH) + 1).toString()) }
    var year1 by remember { mutableStateOf(cal.get(java.util.Calendar.YEAR).toString()) }
    
    var day2 by remember { mutableStateOf("") }
    var month2 by remember { mutableStateOf("") }
    var year2 by remember { mutableStateOf("") }
    
    var addDays by remember { mutableStateOf("") }
    var addMonths by remember { mutableStateOf("") }
    var addYears by remember { mutableStateOf("") }
    var isAdd by remember { mutableStateOf(true) }
    
    var result by remember { mutableStateOf("") }
    var totalDays by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(Translator.tr("Tarih Hesaplayıcı"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0; result = ""; totalDays = "" },
                label = { Text(Translator.tr("Fark Bul")) },
                modifier = Modifier.padding(end = 8.dp)
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1; result = ""; totalDays = "" },
                label = { Text(Translator.tr("Tarih Ekle/Çıkar")) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (mode == 0) Translator.tr("Başlangıç Tarihi") else Translator.tr("Hedef Tarih"), color = textColor, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = day1, onValueChange = { day1 = it }, label = { Text(Translator.tr("Gün")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = month1, onValueChange = { month1 = it }, label = { Text(Translator.tr("Ay")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = year1, onValueChange = { year1 = it }, label = { Text(Translator.tr("Yıl")) }, modifier = Modifier.weight(1.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (mode == 0) {
            Text(Translator.tr("Bitiş Tarihi"), color = textColor, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = day2, onValueChange = { day2 = it }, label = { Text(Translator.tr("Gün")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = month2, onValueChange = { month2 = it }, label = { Text(Translator.tr("Ay")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = year2, onValueChange = { year2 = it }, label = { Text(Translator.tr("Yıl")) }, modifier = Modifier.weight(1.5f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Translator.tr("İşlem:"), color = textColor, fontWeight = FontWeight.Bold)
                FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text(Translator.tr("Ekle (+)")) })
                FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text(Translator.tr("Çıkar (-)")) })
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = addDays, onValueChange = { addDays = it }, label = { Text(Translator.tr("Gün")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = addMonths, onValueChange = { addMonths = it }, label = { Text(Translator.tr("Ay")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = addYears, onValueChange = { addYears = it }, label = { Text(Translator.tr("Yıl")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                try {
                    val c1 = java.util.Calendar.getInstance()
                    c1.set(year1.toInt(), month1.toInt() - 1, day1.toInt())
                    
                    if (mode == 0) {
                        val c2 = java.util.Calendar.getInstance()
                        c2.set(year2.toInt(), month2.toInt() - 1, day2.toInt())
                        
                        val diffMillis = kotlin.math.abs(c2.timeInMillis - c1.timeInMillis)
                        val totalD = diffMillis / (1000 * 60 * 60 * 24)
                        totalDays = "${Translator.tr("Toplam Gün:")} $totalD"
                        
                        val start = if (c1.timeInMillis < c2.timeInMillis) c1 else c2
                        val end = if (c1.timeInMillis < c2.timeInMillis) c2 else c1
                        
                        var y = end.get(java.util.Calendar.YEAR) - start.get(java.util.Calendar.YEAR)
                        var m = end.get(java.util.Calendar.MONTH) - start.get(java.util.Calendar.MONTH)
                        var d = end.get(java.util.Calendar.DAY_OF_MONTH) - start.get(java.util.Calendar.DAY_OF_MONTH)
                        
                        if (d < 0) {
                            val temp = start.clone() as java.util.Calendar
                            temp.add(java.util.Calendar.MONTH, y * 12 + m)
                            val maxDays = temp.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            d += maxDays
                            m--
                        }
                        if (m < 0) {
                            m += 12
                            y--
                        }
                        
                        val parts = mutableListOf<String>()
                        if (y > 0) parts.add("$y ${Translator.tr("Yıl")}")
                        if (m > 0) parts.add("$m ${Translator.tr("Ay")}")
                        if (d > 0) parts.add("$d ${Translator.tr("Gün")}")
                        if (parts.isEmpty()) parts.add("0 ${Translator.tr("Gün")}")
                        
                        result = parts.joinToString(", ")
                    } else {
                        val mult = if (isAdd) 1 else -1
                        val y = addYears.toIntOrNull() ?: 0
                        val m = addMonths.toIntOrNull() ?: 0
                        val d = addDays.toIntOrNull() ?: 0
                        
                        c1.add(java.util.Calendar.YEAR, y * mult)
                        c1.add(java.util.Calendar.MONTH, m * mult)
                        c1.add(java.util.Calendar.DAY_OF_MONTH, d * mult)
                        
                        val outD = c1.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                        val outM = (c1.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
                        val outY = c1.get(java.util.Calendar.YEAR)
                        
                        result = "$outD / $outM / $outY"
                        totalDays = ""
                    }
                } catch (e: Exception) {
                    result = Translator.tr("Geçersiz değerler!")
                    totalDays = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text(Translator.tr("Hesapla"), color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (result.isNotEmpty()) {
            Text("${Translator.tr("Sonuç: ")} $result", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            if (totalDays.isNotEmpty()) {
                Text(totalDays, fontSize = 16.sp, color = textColor.copy(alpha = 0.7f))
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun CustomEquationsTool(textColor: Color) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("custom_equations", Context.MODE_PRIVATE)
    
    var savedStr by remember { mutableStateOf(prefs.getString("eqs", "") ?: "") }
    
    val eqs = savedStr.split(";;").filter { it.isNotEmpty() }.map { 
        val p = it.split("|")
        Triple(p[0], p.getOrElse(1) { "" }, p.getOrElse(2) { "1" } == "1")
    }
    val activeEqs = eqs.filter { it.third }
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var formula by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(Translator.tr("Yeni Denklem Ekle")) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(Translator.tr("İsim (örn: Kinetik Enerji)")) })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = formula, onValueChange = { formula = it }, label = { Text(Translator.tr("Formül (örn: 0.5 * x * x)")) })
                    Text(Translator.tr("Değişken olarak 'x' kullanabilirsiniz."), fontSize = 12.sp, color = textColor.copy(alpha=0.7f))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && formula.isNotBlank()) {
                        val newEq = "${name.replace("|","").replace(";;","")}|${formula.replace("|","").replace(";;","")}"
                        val newStr = if (savedStr.isEmpty()) newEq else "$savedStr;;$newEq"
                        prefs.edit().putString("eqs", newStr).apply()
                        savedStr = newStr
                        showAddDialog = false
                    }
                }) { Text(Translator.tr("Kaydet")) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(Translator.tr("İptal")) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Button(
            onClick = { showAddDialog = true }, 
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(Translator.tr("+ Yeni Denklem Ekle"), color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        AnimatedVisibility(
            visible = eqs.isEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(Translator.tr("Henüz kayıtlı bir denkleminiz yok."), color = textColor.copy(alpha = 0.5f))
        }
        
        AnimatedVisibility(
            visible = activeEqs.size > 1,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            var pipelineInitial by remember { mutableStateOf("") }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(Translator.tr("Pipeline (Zincirleme Çözüm)"), fontWeight = FontWeight.Bold, color = AccentOrange, fontSize = 18.sp)
                    Text(Translator.tr("Aşağıdaki tüm denklemleri sırasıyla birbirine bağlar. (Çıktı -> Girdi)"), fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pipelineInitial, 
                        onValueChange = { pipelineInitial = it }, 
                        label = { Text(Translator.tr("Başlangıç x değeri")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    AnimatedVisibility(
                        visible = pipelineInitial.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                        var currentVal = pipelineInitial.toDoubleOrNull()
                        var err = false
                        val steps = mutableListOf<String>()
                        var finalDisplay = ""
                        
                        activeEqs.forEach { eq ->
                            if (!err && currentVal != null) {
                                try {
                                    val resStr = safeEvaluate(eq.second, currentVal)
                                    if (resStr.startsWith("Err") || resStr.isEmpty()) {
                                        err = true
                                        steps.add("${eq.first} -> Hata")
                                    } else {
                                        steps.add("${eq.first} -> $resStr")
                                        finalDisplay = resStr
                                        currentVal = resStr.replace(",", ".").toDoubleOrNull()
                                    }
                                } catch (e: Exception) {
                                    err = true
                                    steps.add("${eq.first} -> Hata")
                                }
                            }
                        }
                        
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            steps.forEachIndexed { i, step ->
                                Text("${i+1}. $step", fontSize = 14.sp, color = textColor)
                            }
                            if (!err && finalDisplay.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${Translator.tr("Final Sonuç:")} $finalDisplay", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                        }
                        }
                    }
                }
            }
        }
        
        eqs.forEach { eq ->
            var xVal by remember { mutableStateOf("") }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(eq.first, fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Translator.tr("Zincirde"), fontSize = 12.sp, color = if (eq.third) AccentOrange else textColor.copy(alpha = 0.5f))
                            Switch(
                                checked = eq.third,
                                onCheckedChange = { isChecked ->
                                    val newStr = eqs.joinToString(";;") {
                                        if (it == eq) "${it.first}|${it.second}|${if (isChecked) "1" else "0"}"
                                        else "${it.first}|${it.second}|${if (it.third) "1" else "0"}"
                                    }
                                    prefs.edit().putString("eqs", newStr).apply()
                                    savedStr = newStr
                                },
                                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                            )
                            IconButton(onClick = {
                                val newStr = eqs.filter { it != eq }.joinToString(";;") { "${it.first}|${it.second}|${if (it.third) "1" else "0"}" }
                                prefs.edit().putString("eqs", newStr).apply()
                                savedStr = newStr
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("f(x) = ${eq.second}", fontSize = 14.sp, color = AccentOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = xVal, 
                        onValueChange = { xVal = it }, 
                        label = { Text(Translator.tr("x değeri")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val result = try {
                        if (xVal.isNotEmpty()) safeEvaluate(eq.second, xVal.toDoubleOrNull()) else ""
                    } catch (e: Exception) { "Err" }
                    
                    AnimatedVisibility(
                        visible = result.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text("${Translator.tr("Sonuç: ")} $result", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}
