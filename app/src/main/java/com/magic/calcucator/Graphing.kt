package com.magic.calcucator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magic.calcucator.core.*
import java.util.Locale
import kotlin.math.*

data class GraphEq(val id: Int, var expr: String, val color: Color, val isVisible: Boolean = true)

val GRAPH_PALETTE: List<Color> = listOf(
    AccentOrange,           // 0: Vivid Orange
    Color(0xFF00E5FF),      // 1: Electric Cyan
    Color(0xFFFF1744),      // 2: Crimson Red
    Color(0xFF00E676),      // 3: Emerald Spring Green
    Color(0xFFAA00FF),      // 4: Vivid Violet
    Color(0xFFFFD600),      // 5: Sunny Yellow
    Color(0xFF2979FF),      // 6: Dodger Blue
    Color(0xFFFF4081),      // 7: Hot Pink
    Color(0xFFAEEA00),      // 8: Electric Lime
    Color(0xFF00BFA5),      // 9: Deep Teal
    Color(0xFFFF9100),      // 10: Bright Amber
    Color(0xFF651FFF),      // 11: Deep Indigo
    Color(0xFFFF6E40),      // 12: Warm Coral
    Color(0xFF1DE9B6),      // 13: Mint Aquamarine
    Color(0xFFE040FB),      // 14: Orchid Magenta
    Color(0xFF00B0FF),      // 15: Sky Blue
    Color(0xFF76FF03),      // 16: Neon Acid Green
    Color(0xFFF50057),      // 17: Rose Red
    Color(0xFF7C4DFF),      // 18: Electric Lavender
    Color(0xFFFF3D00),      // 19: Tangerine Orange
    Color(0xFF00C853),      // 20: Kelly Grass Green
    Color(0xFF3D5AFE),      // 21: Cobalt Blue
    Color(0xFFFFC400),      // 22: Golden Yellow
    Color(0xFFD500F9)       // 23: Vivid Berry Fuchsia
)

fun hsvToColor(hue: Float, saturation: Float = 0.88f, value: Float = 0.95f): Color {
    val h = (hue % 360f + 360f) % 360f
    val c = value * saturation
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = value - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

fun pickNextGraphColor(existingEquations: List<GraphEq>, seedId: Int): Color {
    val usedColors = existingEquations.map { it.color.toArgb() }.toSet()
    val available = GRAPH_PALETTE.firstOrNull { it.toArgb() !in usedColors }
    if (available != null) return available

    var step = seedId
    while (step < seedId + 360) {
        val candidate = hsvToColor((step * 137.507764f) % 360f, saturation = 0.88f, value = 0.95f)
        if (candidate.toArgb() !in usedColors) return candidate
        step++
    }
    return hsvToColor((seedId * 137.507764f) % 360f, saturation = 0.88f, value = 0.95f)
}

enum class SpecialPointType(val labelTr: String) {
    ROOT("Kök (y=0)"),
    MAX("Yerel Maks"),
    MIN("Yerel Min"),
    INTERSECTION("Kesişim")
}

data class GraphSpecialPoint(
    val mathX: Double,
    val mathY: Double,
    val type: SpecialPointType,
    val color: Color,
    val eqExpr: String
)

private fun niceGridStep(scale: Float): Double {
    val targetPixels = 75.0
    val rawStep = targetPixels / scale.toDouble()
    val mag = 10.0.pow(floor(log10(rawStep)))
    val residual = rawStep / mag
    return when {
        residual > 5.0 -> 10.0 * mag
        residual > 2.0 -> 5.0 * mag
        residual > 1.0 -> 2.0 * mag
        else -> mag
    }
}

private fun formatGridLabel(value: Double, step: Double): String {
    if (!value.isFinite() || abs(value) < 1e-9) return "0"
    return try {
        val s = when {
            step >= 1.0 && abs(value - value.roundToLong()) < 1e-6 -> value.roundToLong().toString()
            step >= 0.1 -> String.format(Locale.US, "%.1f", value).trimEnd('0').trimEnd('.')
            step >= 0.01 -> String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            step >= 0.001 -> String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
            else -> {
                val decimals = max(0, min(6, -floor(log10(step)).toInt()))
                String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
            }
        }
        if (s.isEmpty() || s == "-0" || s == "-") "0" else s
    } catch (_: Exception) {
        "0"
    }
}

private fun bisectRoot(f: (Double) -> Double, aInput: Double, bInput: Double, iterations: Int = 24): Double? {
    var a = aInput
    var b = bInput
    var fa = f(a)
    var fb = f(b)
    if (fa.isNaN() || fb.isNaN() || fa.isInfinite() || fb.isInfinite()) return null
    if (fa == 0.0) return a
    if (fb == 0.0) return b
    if (fa * fb > 0.0) return null

    for (i in 0 until iterations) {
        val mid = 0.5 * (a + b)
        val fm = f(mid)
        if (fm.isNaN() || fm.isInfinite()) return null
        if (abs(fm) < 1e-10) return mid
        if (fa * fm <= 0.0) {
            b = mid
            fb = fm
        } else {
            a = mid
            fa = fm
        }
    }
    val root = 0.5 * (a + b)
    val fRoot = f(root)
    // Filter out vertical asymptotes (e.g. 1/x or tan(x)) where values jump wildly instead of crossing 0 smoothly
    return if (!fRoot.isNaN() && abs(fRoot) < 0.05) root else null
}

/**
 * High-performance, zero-allocation native Double expression compiler for 120 FPS graphing.
 * Compiles a mathematical string into a direct (Double) -> Double functional closure once per frame.
 */
class FastCompiler(rawStr: String, private val useRadians: Boolean = true) {
    private val str: String
    private var pos = -1
    private var ch = 0

    init {
        var s = rawStr.trim()
        if (s.startsWith("y=", ignoreCase = true) || s.startsWith("y =", ignoreCase = true)) {
            s = s.substringAfter('=').trim()
        } else if (s.startsWith("f(x)=", ignoreCase = true) || s.startsWith("f(x) =", ignoreCase = true)) {
            s = s.substringAfter('=').trim()
        }
        // Connect CAS Engine: resolve derivatives (d/dx, diff), integrals, and symbolic operators
        s = GraphCasCache.resolve(s)

        // Convert any remaining Leibniz notation d/dx(expr) to diff(expr) for FastCompiler native numerical differentiation fallback
        s = s.replace(Regex("""(?:\(?d\s*/\s*d[a-zA-Z]\)?|\\frac\{d\}\{d[a-zA-Z]\})\s*(?:\*\s*)?\(([^()]+(?:\([^()]*\)[^()]*)*)\)""", RegexOption.IGNORE_CASE)) {
            "diff(${it.groupValues[1]})"
        }

        s = s.replace('×', '*').replace('÷', '/')
        s = s.replace('{', '(').replace('}', ')')
        var guard = 0
        while (s.contains('|') && guard++ < 10) {
            val replaced = s.replace(Regex("""\|([^|]+)\|"""), "abs($1)")
            if (replaced == s) break
            s = replaced
        }
        this.str = s
        nextChar()
    }

    private fun nextChar() {
        pos++
        ch = if (pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Char): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat.code) {
            nextChar()
            return true
        }
        return false
    }

    fun compile(): (Double) -> Double {
        return try {
            val expr = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < str.length) {
                { Double.NaN }
            } else {
                expr
            }
        } catch (_: Exception) {
            { Double.NaN }
        }
    }

    private fun parseExpression(): (Double) -> Double {
        var node = parseTerm()
        while (true) {
            if (eat('+')) {
                val r = parseTerm()
                val l = node
                node = { x -> l(x) + r(x) }
            } else if (eat('-')) {
                val r = parseTerm()
                val l = node
                node = { x -> l(x) - r(x) }
            } else {
                return node
            }
        }
    }

    private fun parseTerm(): (Double) -> Double {
        var node = parseFactor()
        while (true) {
            if (eat('*')) {
                val r = parseFactor()
                val l = node
                node = { x -> l(x) * r(x) }
            } else if (eat('/')) {
                val r = parseFactor()
                val l = node
                node = { x ->
                    val d = r(x)
                    if (d != 0.0) l(x) / d else Double.NaN
                }
            } else if (eat('%')) {
                val r = parseFactor()
                val l = node
                node = { x ->
                    val d = r(x)
                    if (d != 0.0) l(x) % d else Double.NaN
                }
            } else if (ch == '('.code || (ch in '0'.code..'9'.code) || (ch in 'a'.code..'z'.code) || (ch in 'A'.code..'Z'.code) || ch == 'π'.code) {
                val r = parseFactor()
                val l = node
                node = { x -> l(x) * r(x) }
            } else {
                return node
            }
        }
    }

    private fun parseFactor(): (Double) -> Double {
        if (eat('+')) return parseFactor()
        if (eat('-')) {
            val f = parseFactor()
            return { x -> -f(x) }
        }

        val startPos = pos
        var node: ((Double) -> Double)?

        if (eat('(')) {
            node = parseExpression()
            eat(')')
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
            if (ch == 'e'.code || ch == 'E'.code) {
                val savePos = pos
                val saveCh = ch
                nextChar()
                if (ch == '+'.code || ch == '-'.code || (ch in '0'.code..'9'.code)) {
                    if (ch == '+'.code || ch == '-'.code) nextChar()
                    while (ch in '0'.code..'9'.code) nextChar()
                } else {
                    pos = savePos
                    ch = saveCh
                }
            }
            val num = str.substring(startPos, pos).toDoubleOrNull() ?: Double.NaN
            node = { num }
        } else if (ch == 'x'.code || ch == 'X'.code) {
            nextChar()
            node = { it }
        } else if (ch == 'π'.code) {
            nextChar()
            node = { PI }
        } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
            while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
            val fn = str.substring(startPos, pos).lowercase(Locale.US)
            when (fn) {
                "pi" -> node = { PI }
                "e" -> node = { E }
                "x" -> node = { it }
                else -> {
                    val arg = parseFactor()
                    node = when (fn) {
                        "sin" -> if (useRadians) { x -> sin(arg(x)) } else { x -> sin(Math.toRadians(arg(x))) }
                        "cos" -> if (useRadians) { x -> cos(arg(x)) } else { x -> cos(Math.toRadians(arg(x))) }
                        "tan" -> if (useRadians) { x -> tan(arg(x)) } else { x -> tan(Math.toRadians(arg(x))) }
                        "asin", "arcsin" -> if (useRadians) { x -> asin(arg(x)) } else { x -> Math.toDegrees(asin(arg(x))) }
                        "acos", "arccos" -> if (useRadians) { x -> acos(arg(x)) } else { x -> Math.toDegrees(acos(arg(x))) }
                        "atan", "arctan" -> if (useRadians) { x -> atan(arg(x)) } else { x -> Math.toDegrees(atan(arg(x))) }
                        "sinh" -> { x -> sinh(arg(x)) }
                        "cosh" -> { x -> cosh(arg(x)) }
                        "tanh" -> { x -> tanh(arg(x)) }
                        "sqrt" -> { x ->
                            val v = arg(x)
                            if (v >= 0.0) sqrt(v) else Double.NaN
                        }
                        "cbrt" -> { x ->
                            val v = arg(x)
                            if (v >= 0.0) v.pow(1.0 / 3.0) else -(-v).pow(1.0 / 3.0)
                        }
                        "abs" -> { x -> abs(arg(x)) }
                        "ln" -> { x ->
                            val v = arg(x)
                            if (v > 0.0) ln(v) else Double.NaN
                        }
                        "log" -> { x ->
                            val v = arg(x)
                            if (v > 0.0) log10(v) else Double.NaN
                        }
                        "log2" -> { x ->
                            val v = arg(x)
                            if (v > 0.0) log2(v) else Double.NaN
                        }
                        "exp" -> { x ->
                            val v = arg(x)
                            if (v < 700.0) exp(v) else Double.NaN
                        }
                        "diff", "derivative" -> {
                            val h = 1e-6
                            { x -> (arg(x + h) - arg(x - h)) / (2.0 * h) }
                        }
                        "floor" -> { x -> floor(arg(x)) }
                        "ceil", "ceiling" -> { x -> ceil(arg(x)) }
                        "round" -> { x -> round(arg(x)) }
                        "sign", "signum", "sgn" -> { x -> sign(arg(x)) }
                        else -> { _ -> Double.NaN }
                    }
                }
            }
        } else {
            return { Double.NaN }
        }

        if (eat('^')) {
            val exponent = parseFactor()
            val base = node
            return { x ->
                val b = base(x)
                val p = exponent(x)
                realPow(b, p)
            }
        }

        return node
    }
}

@Composable
fun GraphingScreen(textColor: Color, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("graph_prefs", android.content.Context.MODE_PRIVATE)

    var equations by remember {
        val saved = prefs.getString("equations_list", null)
        val initialList = if (!saved.isNullOrEmpty()) {
            saved.split(";;").mapIndexed { index, item ->
                val parts = item.split("::")
                val expr = parts[0]
                val color = if (parts.size > 1) {
                    try {
                        Color(parts[1].toLong(16).toInt())
                    } catch (_: Exception) {
                        GRAPH_PALETTE.getOrElse(index) {
                            hsvToColor((index * 137.507764f) % 360f)
                        }
                    }
                } else {
                    GRAPH_PALETTE.getOrElse(index) {
                        hsvToColor((index * 137.507764f) % 360f)
                    }
                }
                val isVisible = if (parts.size > 2) parts[2] != "0" else true
                GraphEq(index, expr, color, isVisible)
            }
        } else {
            listOf(GraphEq(0, "sin(x)", GRAPH_PALETTE[0], true))
        }
        mutableStateOf(initialList)
    }

    var nextId by remember { mutableIntStateOf((equations.maxOfOrNull { it.id } ?: 0) + 1) }
    var focusedEqId by remember { mutableIntStateOf(equations.firstOrNull()?.id ?: 0) }
    var useRadians by remember { mutableStateOf(true) }
    var showAnalysis by remember { mutableStateOf(true) }
    var selectedSpecialPoint by remember { mutableStateOf<GraphSpecialPoint?>(null) }
    var colorPickerEqId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(equations) {
        val str = equations.joinToString(";;") { eq ->
            val hex = String.format(Locale.US, "%08x", eq.color.toArgb())
            val vis = if (eq.isVisible) "1" else "0"
            "${eq.expr}::$hex::$vis"
        }
        prefs.edit().putString("equations_list", str).apply()
    }

    var scale by remember { mutableFloatStateOf(70f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var touchX by remember { mutableStateOf<Float?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val quickSnippets = listOf("sin(x)", "cos(x)", "tan(x)", "x^2", "sqrt(x)", "ln(x)", "exp(x)", "abs(x)", "1/x", "π")

    Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
        ToolTopBar(
            title = Translator.tr("Graphing Mode"),
            textColor = textColor,
            onBack = onBack,
            actions = {
                // RAD / DEG Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentOrange.copy(alpha = 0.15f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            useRadians = !useRadians
                            selectedSpecialPoint = null
                        }
                ) {
                    Text(
                        text = if (useRadians) "RAD" else "DEG",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Special Points Analysis Toggle
                IconButton(
                    onClick = {
                        showAnalysis = !showAnalysis
                        if (!showAnalysis) selectedSpecialPoint = null
                    }
                ) {
                    Icon(
                        Icons.Filled.Analytics,
                        contentDescription = Translator.tr("Kök ve Ekstremum Analizi"),
                        tint = if (showAnalysis) AccentOrange else textColor.copy(alpha = 0.4f)
                    )
                }
            }
        )

        // Quick Function Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickSnippets.forEach { snippet ->
                SuggestionChip(
                    onClick = {
                        val targetId = if (equations.any { it.id == focusedEqId }) focusedEqId else equations.firstOrNull()?.id
                        if (targetId != null) {
                            equations = equations.map { eq ->
                                if (eq.id == targetId) {
                                    val newText = if (eq.expr.isBlank()) snippet else "${eq.expr} + $snippet"
                                    eq.copy(expr = newText)
                                } else eq
                            }
                        } else {
                            val newColor = pickNextGraphColor(equations, nextId)
                            equations = listOf(GraphEq(nextId, snippet, newColor, true))
                            focusedEqId = nextId
                            nextId++
                        }
                    },
                    label = { Text(snippet, fontSize = 12.sp, color = textColor) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = textColor.copy(alpha = 0.06f)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = textColor.copy(alpha = 0.18f)
                    )
                )
            }
        }

        // Equation List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(equations, key = { it.id }) { eq ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(eq.color)
                            .border(1.5.dp, textColor.copy(alpha = 0.35f), CircleShape)
                            .clickable {
                                focusedEqId = eq.id
                                colorPickerEqId = eq.id
                            }
                    ) {
                        val isBright = (eq.color.red * 0.299f + eq.color.green * 0.587f + eq.color.blue * 0.114f) > 0.6f
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = Translator.tr("Renk Değiştir"),
                            tint = if (isBright) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = eq.expr,
                        onValueChange = { newExpr ->
                            focusedEqId = eq.id
                            selectedSpecialPoint = null
                            equations = equations.map { if (it.id == eq.id) it.copy(expr = newExpr) else it }
                        },
                        placeholder = { Text(Translator.tr("y = f(x)  (örn: sin(x), x^2 - 4)"), color = textColor.copy(alpha = 0.4f), fontSize = 13.sp) },
                        supportingText = {
                            val clean = eq.expr.replace(Regex("""^[fFgGhH]_?\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]+\s*"""), "")
                                .replace(Regex("""^[yY]\s*(?:\(\s*x\s*\))?\s*[:=]+\s*"""), "").trim()
                            val resolved = GraphCasCache.cache[clean]
                            if (!resolved.isNullOrBlank() && resolved != clean) {
                                Text("= $resolved", color = eq.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (eq.isVisible) 1f else 0.55f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = eq.color,
                            focusedBorderColor = eq.color,
                            unfocusedBorderColor = textColor.copy(alpha = 0.25f)
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = textColor)
                    )
                    IconButton(
                        onClick = {
                            equations = equations.map {
                                if (it.id == eq.id) it.copy(isVisible = !it.isVisible) else it
                            }
                            selectedSpecialPoint = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (eq.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (eq.isVisible) Translator.tr("Gizle") else Translator.tr("Göster"),
                            tint = if (eq.isVisible) eq.color else textColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (equations.size > 1) {
                                equations = equations.filter { it.id != eq.id }
                            } else {
                                equations = equations.map { it.copy(expr = "") }
                            }
                            selectedSpecialPoint = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                TextButton(
                    onClick = {
                        val newId = nextId
                        val newColor = pickNextGraphColor(equations, newId)
                        equations = equations + GraphEq(newId, "", newColor, true)
                        focusedEqId = newId
                        nextId++
                    },
                    modifier = Modifier.padding(start = 20.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = AccentOrange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Translator.tr("Add Equation"), color = AccentOrange, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.68f)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(textColor.copy(alpha = 0.05f))
                .border(1.dp, textColor.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (zoom.isFinite() && zoom > 0f) {
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(5f, 5000f)
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (newScale != oldScale && centroid != Offset.Unspecified && w > 0f && h > 0f) {
                                val zoomFactor = newScale / oldScale
                                val cX = centroid.x - w / 2f
                                val cY = centroid.y - h / 2f
                                val newOffX = (offsetX - cX) * zoomFactor + cX + (if (pan.x.isFinite()) pan.x else 0f)
                                val newOffY = (offsetY - cY) * zoomFactor + cY + (if (pan.y.isFinite()) pan.y else 0f)
                                if (newOffX.isFinite()) offsetX = newOffX.coerceIn(-100000f, 100000f)
                                if (newOffY.isFinite()) offsetY = newOffY.coerceIn(-100000f, 100000f)
                            } else {
                                if (pan.x.isFinite()) offsetX = (offsetX + pan.x).coerceIn(-100000f, 100000f)
                                if (pan.y.isFinite()) offsetY = (offsetY + pan.y).coerceIn(-100000f, 100000f)
                            }
                            scale = newScale
                            touchX = null
                            selectedSpecialPoint = null
                        }
                    }
                }
        ) {
            val specialPointsCache = remember { mutableListOf<GraphSpecialPoint>() }
            val currentScale by rememberUpdatedState(scale)
            val currentOffsetX by rememberUpdatedState(offsetX)
            val currentOffsetY by rememberUpdatedState(offsetY)
            val currentShowAnalysis by rememberUpdatedState(showAnalysis)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val curScale = currentScale
                                val curOffsetX = currentOffsetX
                                val curOffsetY = currentOffsetY
                                val curShowAnalysis = currentShowAnalysis
                                val centerX = size.width / 2f + curOffsetX
                                val centerY = size.height / 2f + curOffsetY
                                if (curScale > 0f && curScale.isFinite()) {
                                    // Check if user tapped close to a special point first
                                    val nearestSpecial = if (curShowAnalysis) {
                                        specialPointsCache.minByOrNull { sp ->
                                            val px = centerX + (sp.mathX * curScale).toFloat()
                                            val py = centerY - (sp.mathY * curScale).toFloat()
                                            hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                                        }?.takeIf { sp ->
                                            val px = centerX + (sp.mathX * curScale).toFloat()
                                            val py = centerY - (sp.mathY * curScale).toFloat()
                                            hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble()) <= 36.0
                                        }
                                    } else null

                                    if (nearestSpecial != null) {
                                        selectedSpecialPoint = nearestSpecial
                                        val px = centerX + (nearestSpecial.mathX * curScale).toFloat()
                                        touchX = if (px.isFinite()) px else null
                                    } else {
                                        selectedSpecialPoint = null
                                        touchX = tapOffset.x
                                    }
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                val curScale = currentScale
                                val curOffsetX = currentOffsetX
                                val centerX = size.width / 2f + curOffsetX
                                if (curScale > 0f && curScale.isFinite()) {
                                    val scaleDouble = curScale.toDouble()
                                    val mathX: Double = (tapOffset.x - centerX).toDouble() / scaleDouble
                                    val unitStep = niceGridStep(curScale)
                                    val snapUnit = unitStep / 2.0
                                    val snappedMathX: Double = if (snapUnit > 0.0) round(mathX / snapUnit) * snapUnit else mathX
                                    val snappedPixelX: Float = centerX + (snappedMathX * scaleDouble).toFloat()
                                    if (snappedPixelX.isFinite()) {
                                        selectedSpecialPoint = null
                                        touchX = snappedPixelX
                                    }
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val safeCenterY = if (offsetY.isFinite()) h / 2f + offsetY else h / 2f
                val safeCenterX = if (offsetX.isFinite()) w / 2f + offsetX else w / 2f

                val subGridColor = textColor.copy(alpha = 0.04f)
                val gridColor = textColor.copy(alpha = 0.11f)
                val axisColor = textColor.copy(alpha = 0.55f)
                val textStyle = TextStyle(color = textColor.copy(alpha = 0.72f), fontSize = 10.sp)

                val unitStep = niceGridStep(scale)
                val pixelStep = (unitStep * scale).toFloat()
                val subPixelStep = pixelStep / 5f

                // Minor sub-grid
                if (subPixelStep >= 12f && subPixelStep.isFinite()) {
                    var subX = safeCenterX % subPixelStep
                    if (subX < 0) subX += subPixelStep
                    var subLoopX = 0
                    while (subX <= w && subLoopX++ < 500) {
                        drawLine(color = subGridColor, start = Offset(subX, 0f), end = Offset(subX, h), strokeWidth = 1f)
                        subX += subPixelStep
                    }
                    var subY = safeCenterY % subPixelStep
                    if (subY < 0) subY += subPixelStep
                    var subLoopY = 0
                    while (subY <= h && subLoopY++ < 500) {
                        drawLine(color = subGridColor, start = Offset(0f, subY), end = Offset(w, subY), strokeWidth = 1f)
                        subY += subPixelStep
                    }
                }

                // Major Grid & Labels
                val labelY = if (h >= 30f) safeCenterY.coerceIn(6f, h - 20f) else safeCenterY
                val labelX = if (w >= 50f) safeCenterX.coerceIn(6f, w - 42f) else safeCenterX

                if (pixelStep >= 10f && pixelStep.isFinite()) {
                    var currentX = safeCenterX % pixelStep
                    if (currentX < 0) currentX += pixelStep
                    var loopX = 0
                    while (currentX <= w && loopX++ < 500) {
                        drawLine(color = gridColor, start = Offset(currentX, 0f), end = Offset(currentX, h), strokeWidth = 1.2f)
                        val mathX = (currentX - safeCenterX) / scale.toDouble()
                        if (abs(mathX) > unitStep * 0.05) {
                            val label = formatGridLabel(mathX, unitStep)
                            val labelXCoord = (currentX + 3f).coerceAtMost(if (w > 35f) w - 30f else currentX)
                            try {
                                drawText(
                                    textMeasurer,
                                    text = label,
                                    style = textStyle,
                                    topLeft = Offset(labelXCoord, labelY + 3f)
                                )
                            } catch (_: Throwable) {}
                        }
                        currentX += pixelStep
                    }

                    var currentY = safeCenterY % pixelStep
                    if (currentY < 0) currentY += pixelStep
                    var loopY = 0
                    while (currentY <= h && loopY++ < 500) {
                        drawLine(color = gridColor, start = Offset(0f, currentY), end = Offset(w, currentY), strokeWidth = 1.2f)
                        val mathY = -(currentY - safeCenterY) / scale.toDouble()
                        if (abs(mathY) > unitStep * 0.05) {
                            val label = formatGridLabel(mathY, unitStep)
                            val labelYCoord = if (h >= 20f) (currentY + 2f).coerceIn(2f, h - 16f) else currentY
                            try {
                                drawText(
                                    textMeasurer,
                                    text = label,
                                    style = textStyle,
                                    topLeft = Offset(labelX + 4f, labelYCoord)
                                )
                            } catch (_: Throwable) {}
                        }
                        currentY += pixelStep
                    }
                }

                // Draw Main Axes
                if (safeCenterY in 0f..h) {
                    drawLine(color = axisColor, start = Offset(0f, safeCenterY), end = Offset(w, safeCenterY), strokeWidth = 2.8f)
                }
                if (safeCenterX in 0f..w) {
                    drawLine(color = axisColor, start = Offset(safeCenterX, 0f), end = Offset(safeCenterX, h), strokeWidth = 2.8f)
                }

                // Draw Equations & Collect Special Points
                val stepPx = 1.0f
                val drawnPoints = mutableListOf<Triple<GraphEq, Offset, Pair<Double, Double>>>()
                val detectedSpecials = mutableListOf<GraphSpecialPoint>()

                val validEquations = equations.filter { it.expr.isNotBlank() && it.isVisible }

                // Compile each equation once per frame using FastCompiler (fallback to evaluateMath if needed)
                val compiledMap = validEquations.associate { eq ->
                    val compiled = FastCompiler(eq.expr, useRadians = useRadians).compile()
                    eq.id to { x: Double ->
                        try {
                            if (!x.isFinite()) Double.NaN
                            else {
                                val res = compiled(x)
                                if (res.isFinite()) res else evaluateMath(eq.expr, x, useRadians = useRadians)
                            }
                        } catch (_: Throwable) {
                            Double.NaN
                        }
                    }
                }

                validEquations.forEach { eq ->
                    val compiledFn = compiledMap[eq.id] ?: return@forEach
                    val path = Path()
                    var isDrawing = false
                    var lastPixelX = Float.NaN
                    var lastPixelY = Float.NaN
                    var lastMathY = Double.NaN
                    var lastMathX = Double.NaN
                    var prevSlope = Double.NaN

                    val clipTop = -h * 0.5f
                    val clipBottom = h * 1.5f

                    var pixelX = 0f
                    while (pixelX <= w) {
                        val mathX = (pixelX - safeCenterX) / scale.toDouble()
                        val mathY = try { compiledFn(mathX) } catch (_: Throwable) { Double.NaN }

                        if (mathY.isFinite()) {
                            val pixelY = safeCenterY - (mathY * scale).toFloat()

                            // Asymptote jump check (opposite infinities crossing, e.g. 1/x or tan(x))
                            val isOppositeJump = !lastMathY.isNaN() && (
                                (lastMathY * mathY < 0.0 && (kotlin.math.abs(lastMathY) > 1.5 || kotlin.math.abs(mathY) > 1.5) && (kotlin.math.abs(pixelY - lastPixelY) > 40f || kotlin.math.abs(mathY - lastMathY) > 3.0)) ||
                                ((pixelY < 0f && lastPixelY > h) || (pixelY > h && lastPixelY < 0f)) ||
                                (kotlin.math.abs(pixelY - lastPixelY) > h * 0.45f && kotlin.math.abs(mathY - lastMathY) > 8.0)
                            )

                            // High-frequency oscillation / singularity knot prevention near x = 0 (e.g. sin(1/x) where |y| <= 2.0)
                            val isOscillationKnot = !lastMathY.isNaN() && kotlin.math.abs(mathX) < 0.12 && kotlin.math.abs(pixelY - lastPixelY) > 50f && kotlin.math.abs(mathY) <= 2.0 && kotlin.math.abs(lastMathY) <= 2.0

                            if (isOppositeJump) {
                                // 1. Adaptively extend left branch towards asymptote until it exits the screen
                                if (isDrawing && lastPixelY.isFinite() && lastPixelY in clipTop..clipBottom) {
                                    var leftX = lastMathX
                                    var rightX = mathX
                                    var leftPy = lastPixelY
                                    var leftMy = lastMathY
                                    for (iter in 0 until 14) {
                                        val midX = 0.5 * (leftX + rightX)
                                        val midY = try { compiledFn(midX) } catch (_: Throwable) { Double.NaN }
                                        if (midY.isFinite() && (midY * leftMy > 0.0)) {
                                            val midPy = safeCenterY - (midY * scale).toFloat()
                                            leftX = midX
                                            leftPy = midPy
                                            leftMy = midY
                                            if (midPy < clipTop || midPy > clipBottom) break
                                        } else {
                                            rightX = midX
                                        }
                                    }
                                    val clampedLeftPy = leftPy.coerceIn(clipTop, clipBottom)
                                    val clampedLeftPx = (safeCenterX + leftX * scale).toFloat()
                                    path.lineTo(clampedLeftPx, clampedLeftPy)
                                }

                                isDrawing = false

                                // 2. Adaptively enter right branch from off-screen boundary towards current point
                                if (pixelY in clipTop..clipBottom) {
                                    var leftX = lastMathX
                                    var rightX = mathX
                                    var rightPy = pixelY
                                    var rightMy = mathY
                                    for (iter in 0 until 14) {
                                        val midX = 0.5 * (leftX + rightX)
                                        val midY = try { compiledFn(midX) } catch (_: Throwable) { Double.NaN }
                                        if (midY.isFinite() && (midY * rightMy > 0.0)) {
                                            val midPy = safeCenterY - (midY * scale).toFloat()
                                            rightX = midX
                                            rightPy = midPy
                                            rightMy = midY
                                            if (midPy < clipTop || midPy > clipBottom) break
                                        } else {
                                            leftX = midX
                                        }
                                    }
                                    val clampedRightPy = rightPy.coerceIn(clipTop, clipBottom)
                                    val clampedRightPx = (safeCenterX + rightX * scale).toFloat()
                                    path.moveTo(clampedRightPx, clampedRightPy)
                                    val clampedY = pixelY.coerceIn(clipTop, clipBottom)
                                    path.lineTo(pixelX, clampedY)
                                    isDrawing = true
                                }
                            } else if (isOscillationKnot) {
                                isDrawing = false
                            } else {
                                val bothOffTop = pixelY < clipTop && lastPixelY < clipTop
                                val bothOffBottom = pixelY > clipBottom && lastPixelY > clipBottom
                                val isOffscreenSkip = bothOffTop || bothOffBottom

                                if (isOffscreenSkip || !pixelY.isFinite()) {
                                    isDrawing = false
                                } else {
                                    val clampedY = pixelY.coerceIn(clipTop, clipBottom)
                                    if (!isDrawing) {
                                        // Entering into visible canvas from undefined or off-screen: adaptively find entry point
                                        if (lastMathX.isFinite() && pixelY in clipTop..clipBottom) {
                                            var leftX = lastMathX
                                            var rightX = mathX
                                            var rightPy = pixelY
                                            var rightMy = mathY
                                            for (iter in 0 until 14) {
                                                val midX = 0.5 * (leftX + rightX)
                                                val midY = try { compiledFn(midX) } catch (_: Throwable) { Double.NaN }
                                                if (midY.isFinite() && (midY * rightMy > 0.0)) {
                                                    val midPy = safeCenterY - (midY * scale).toFloat()
                                                    rightX = midX
                                                    rightPy = midPy
                                                    rightMy = midY
                                                    if (midPy < clipTop || midPy > clipBottom) break
                                                } else {
                                                    leftX = midX
                                                }
                                            }
                                            val clampedEntryPy = rightPy.coerceIn(clipTop, clipBottom)
                                            val clampedEntryPx = (safeCenterX + rightX * scale).toFloat()
                                            path.moveTo(clampedEntryPx, clampedEntryPy)
                                            path.lineTo(pixelX, clampedY)
                                        } else {
                                            path.moveTo(pixelX, clampedY)
                                        }
                                        isDrawing = true
                                    } else {
                                        // Rapid vertical change: sub-sample intermediate points for smooth curve
                                        if (!lastPixelY.isNaN() && kotlin.math.abs(pixelY - lastPixelY) > 20f && (mathY * lastMathY > 0.0)) {
                                            for (step in 1..3) {
                                                val frac = step / 4.0
                                                val subPx = lastPixelX + frac * (pixelX - lastPixelX)
                                                val subMx = lastMathX + frac * (mathX - lastMathX)
                                                val subMy = try { compiledFn(subMx) } catch (_: Throwable) { Double.NaN }
                                                if (subMy.isFinite() && (subMy * mathY > 0.0)) {
                                                    val subPy = safeCenterY - (subMy * scale).toFloat()
                                                    val clampedSubPy = subPy.coerceIn(clipTop, clipBottom)
                                                    path.lineTo(subPx.toFloat(), clampedSubPy)
                                                }
                                            }
                                        }
                                        path.lineTo(pixelX, clampedY)
                                    }
                                }
                            }

                            // Special Points Detection
                            if (showAnalysis && detectedSpecials.size < 40) {
                                // 1. Root Detection (sign change across 0)
                                if (!lastMathY.isNaN() && ((lastMathY <= 0.0 && mathY >= 0.0) || (lastMathY >= 0.0 && mathY <= 0.0))) {
                                    val denom = mathY - lastMathY
                                    val rootX = if (kotlin.math.abs(denom) > 1e-12) lastMathX + (-lastMathY / denom) * (mathX - lastMathX) else lastMathX
                                    if (rootX.isFinite()) {
                                        val rootY = try { compiledFn(rootX) } catch (_: Throwable) { Double.NaN }
                                        if (rootY.isFinite() && kotlin.math.abs(rootY) < 0.25 && detectedSpecials.none { it.type == SpecialPointType.ROOT && kotlin.math.abs(it.mathX - rootX) < (6.0 / scale) }) {
                                            detectedSpecials.add(GraphSpecialPoint(rootX, 0.0, SpecialPointType.ROOT, eq.color, eq.expr))
                                        }
                                    }
                                }

                                // 2. Local Extrema Detection (slope sign change)
                                val currSlope = (mathY - lastMathY) / (mathX - lastMathX)
                                if (prevSlope.isFinite() && currSlope.isFinite() && kotlin.math.abs(currSlope) < 1e3 && kotlin.math.abs(prevSlope) < 1e3) {
                                    val extX = 0.5 * (lastMathX + mathX)
                                    if (extX.isFinite()) {
                                        val extY = try { compiledFn(extX) } catch (_: Throwable) { Double.NaN }
                                        if (extY.isFinite()) {
                                            if (prevSlope > 1e-6 && currSlope <= -1e-6) {
                                                if (detectedSpecials.none { it.type == SpecialPointType.MAX && kotlin.math.abs(it.mathX - extX) < (8.0 / scale) }) {
                                                    detectedSpecials.add(GraphSpecialPoint(extX, extY, SpecialPointType.MAX, eq.color, eq.expr))
                                                }
                                            } else if (prevSlope < -1e-6 && currSlope >= 1e-6) {
                                                if (detectedSpecials.none { it.type == SpecialPointType.MIN && kotlin.math.abs(it.mathX - extX) < (8.0 / scale) }) {
                                                    detectedSpecials.add(GraphSpecialPoint(extX, extY, SpecialPointType.MIN, eq.color, eq.expr))
                                                }
                                            }
                                        }
                                    }
                                }
                                prevSlope = currSlope
                            }

                            lastPixelX = pixelX
                            lastPixelY = pixelY
                            lastMathY = mathY
                            lastMathX = mathX
                        } else {
                            // If drawing, extend left branch towards undefined point until off-screen
                            if (isDrawing && lastPixelY.isFinite() && lastPixelY in clipTop..clipBottom) {
                                var leftX = lastMathX
                                var rightX = mathX
                                var leftPy = lastPixelY
                                var leftMy = lastMathY
                                for (iter in 0 until 14) {
                                    val midX = 0.5 * (leftX + rightX)
                                    val midY = try { compiledFn(midX) } catch (_: Throwable) { Double.NaN }
                                    if (midY.isFinite() && (midY * leftMy > 0.0)) {
                                        val midPy = safeCenterY - (midY * scale).toFloat()
                                        leftX = midX
                                        leftPy = midPy
                                        leftMy = midY
                                        if (midPy < clipTop || midPy > clipBottom) break
                                    } else {
                                        rightX = midX
                                    }
                                }
                                val clampedLeftPy = leftPy.coerceIn(clipTop, clipBottom)
                                val clampedLeftPx = (safeCenterX + leftX * scale).toFloat()
                                path.lineTo(clampedLeftPx, clampedLeftPy)
                            }
                            isDrawing = false
                            lastPixelY = Float.NaN
                            lastMathY = Double.NaN
                            lastMathX = mathX
                            lastPixelX = pixelX
                            prevSlope = Double.NaN
                        }
                        pixelX += stepPx
                    }

                    try {
                        drawPath(
                            path = path,
                            color = eq.color,
                            style = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    } catch (_: Throwable) {}

                    // Trace point at touchX
                    touchX?.let { tx ->
                        if (tx.isFinite()) {
                            val traceMathX = (tx - safeCenterX) / scale.toDouble()
                            try {
                                val traceMathY = compiledFn(traceMathX)
                                if (traceMathY.isFinite()) {
                                    val tracePixelY = safeCenterY - (traceMathY * scale).toFloat()
                                    if (tracePixelY.isFinite() && tracePixelY in -50f..(h + 50f)) {
                                        drawnPoints.add(Triple(eq, Offset(tx, tracePixelY), Pair(traceMathX, traceMathY)))
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                }

                // 3. Intersections between pairs of equations
                if (showAnalysis && validEquations.size >= 2) {
                    val maxPairs = 6
                    var pairCount = 0
                    for (i in 0 until validEquations.size) {
                        if (pairCount >= maxPairs || detectedSpecials.size >= 40) break
                        for (j in i + 1 until validEquations.size) {
                            if (pairCount >= maxPairs || detectedSpecials.size >= 40) break
                            pairCount++
                            val eq1 = validEquations[i]
                            val eq2 = validEquations[j]
                            val fn1 = compiledMap[eq1.id] ?: continue
                            val fn2 = compiledMap[eq2.id] ?: continue
                            var prevDiff = Double.NaN
                            var prevX = 0.0
                            var px = 0f
                            while (px <= w && detectedSpecials.size < 40) {
                                val mx = (px - safeCenterX) / scale.toDouble()
                                if (mx.isFinite()) {
                                    val y1 = try { fn1(mx) } catch (_: Throwable) { Double.NaN }
                                    val y2 = try { fn2(mx) } catch (_: Throwable) { Double.NaN }
                                    val diff = y1 - y2
                                    if (diff.isFinite() && prevDiff.isFinite()) {
                                        if ((prevDiff <= 0.0 && diff >= 0.0) || (prevDiff >= 0.0 && diff <= 0.0)) {
                                            val denom = diff - prevDiff
                                            val interX = if (abs(denom) > 1e-12) prevX + (-prevDiff / denom) * (mx - prevX) else prevX
                                            if (interX.isFinite()) {
                                                val interY = try { fn1(interX) } catch (_: Throwable) { Double.NaN }
                                                if (interY.isFinite() && detectedSpecials.none { it.type == SpecialPointType.INTERSECTION && abs(it.mathX - interX) < (8.0 / scale) }) {
                                                    detectedSpecials.add(
                                                        GraphSpecialPoint(
                                                            interX,
                                                            interY,
                                                            SpecialPointType.INTERSECTION,
                                                            Color(0xFFFFD600),
                                                            "${eq1.expr} = ${eq2.expr}"
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    prevDiff = diff
                                    prevX = mx
                                }
                                px += 6f
                            }
                        }
                    }
                }

                specialPointsCache.clear()
                specialPointsCache.addAll(detectedSpecials)

                // Draw Special Points (Roots, Extrema, Intersections)
                if (showAnalysis) {
                    detectedSpecials.forEach { sp ->
                        if (sp.mathX.isFinite() && sp.mathY.isFinite()) {
                            val px = safeCenterX + (sp.mathX * scale).toFloat()
                            val py = safeCenterY - (sp.mathY * scale).toFloat()
                            if (px.isFinite() && py.isFinite() && px in 0f..w && py in 0f..h) {
                                drawCircle(color = textColor.copy(alpha = 0.85f), radius = 6.5f, center = Offset(px, py))
                                drawCircle(color = sp.color, radius = 4.2f, center = Offset(px, py))
                            }
                        }
                    }
                }

                // Draw Tracing Pointer
                touchX?.let { tx ->
                    if (tx.isFinite() && tx in 0f..w) {
                        drawLine(
                            color = textColor.copy(alpha = 0.35f),
                            start = Offset(tx, 0f),
                            end = Offset(tx, h),
                            strokeWidth = 2f
                        )
                        drawnPoints.forEach { (eq, offset, mathCoords) ->
                            if (offset.x.isFinite() && offset.y.isFinite()) {
                                drawCircle(color = textColor, radius = 8.5f, center = offset)
                                drawCircle(color = eq.color, radius = 6f, center = offset)

                                val label = String.format(Locale.US, "(%.3f, %.3f)", mathCoords.first, mathCoords.second)
                                val labelXPos = if (offset.x + 130f > w) (offset.x - 125f).coerceAtLeast(6f) else offset.x + 10f
                                val labelYPos = if (h >= 30f) (offset.y - 24f).coerceIn(6f, h - 24f) else offset.y
                                if (labelXPos.isFinite() && labelYPos.isFinite()) {
                                    try {
                                        drawText(
                                            textMeasurer,
                                            text = label,
                                            style = TextStyle(
                                                color = textColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                background = textColor.copy(alpha = 0.12f)
                                            ),
                                            topLeft = Offset(labelXPos, labelYPos)
                                        )
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                }
            }

            // Selected Special Point Info Badge (Top Left of Canvas)
            selectedSpecialPoint?.let { sp ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(sp.color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${Translator.tr(sp.type.labelTr)} • ${sp.eqExpr}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                            Text(
                                text = String.format(Locale.US, "x = %.4f,  y = %.4f", sp.mathX, sp.mathY),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Floating Zoom & Reset Controls (Bottom Right of Canvas)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scale = (scale * 1.35f).coerceIn(5f, 5000f)
                        touchX = null
                        selectedSpecialPoint = null
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    contentColor = textColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                }
                SmallFloatingActionButton(
                    onClick = {
                        scale = (scale / 1.35f).coerceIn(5f, 5000f)
                        touchX = null
                        selectedSpecialPoint = null
                    },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    contentColor = textColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                }
                SmallFloatingActionButton(
                    onClick = {
                        scale = 70f
                        offsetX = 0f
                        offsetY = 0f
                        touchX = null
                        selectedSpecialPoint = null
                    },
                    containerColor = AccentOrange.copy(alpha = 0.9f),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Reset View", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    // Color Picker Dialog
    colorPickerEqId?.let { targetId ->
        val currentEq = equations.find { it.id == targetId }
        if (currentEq != null) {
            AlertDialog(
                onDismissRequest = { colorPickerEqId = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Translator.tr("Grafik Rengi Seç"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (currentEq.expr.isNotBlank()) "y = ${currentEq.expr}" else "y = f(x)",
                            fontSize = 13.sp,
                            color = textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )

                        // 24 Curated Swatches Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(GRAPH_PALETTE) { paletteColor ->
                                val isSelected = paletteColor.toArgb() == currentEq.color.toArgb()
                                val isBright = (paletteColor.red * 0.299f + paletteColor.green * 0.587f + paletteColor.blue * 0.114f) > 0.6f
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(paletteColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) textColor else textColor.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            equations = equations.map {
                                                if (it.id == targetId) it.copy(color = paletteColor) else it
                                            }
                                            colorPickerEqId = null
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (isBright) Color.Black else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Button to generate a unique procedural color
                        OutlinedButton(
                            onClick = {
                                val customColor = hsvToColor(
                                    hue = ((targetId * 137.507764f) + (System.currentTimeMillis() % 360).toFloat()) % 360f,
                                    saturation = 0.90f,
                                    value = 0.96f
                                )
                                equations = equations.map {
                                    if (it.id == targetId) it.copy(color = customColor) else it
                                }
                                colorPickerEqId = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Translator.tr("Benzersiz Yeni Renk Üret"), color = textColor, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { colorPickerEqId = null }) {
                        Text(Translator.tr("Kapat"), color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
