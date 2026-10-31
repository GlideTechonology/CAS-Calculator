@file:Suppress("DEPRECATION")
package com.magic.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.magic.calculator.core.OcrHelper
import com.magic.calculator.core.WebAppInterface
import com.magic.calculator.core.solveCas

private fun replaceBalancedLatexConstructs(input: String): String {
    var s = input
    fun extractBraced(str: String, openBraceIdx: Int): Pair<String, Int>? {
        if (openBraceIdx >= str.length || str[openBraceIdx] != '{') return null
        var depth = 0
        for (i in openBraceIdx until str.length) {
            if (str[i] == '{') depth++
            else if (str[i] == '}') {
                depth--
                if (depth == 0) {
                    return Pair(str.substring(openBraceIdx + 1, i), i + 1)
                }
            }
        }
        return null
    }

    // Replace \frac{A}{B} from innermost/leftmost with balanced braces
    var guard = 0
    while (s.contains("\\frac{") && guard < 100) {
        guard++
        val idx = s.indexOf("\\frac{")
        val first = extractBraced(s, idx + 5) ?: break
        var nextIdx = first.second
        while (nextIdx < s.length && s[nextIdx] == ' ') nextIdx++
        val second = extractBraced(s, nextIdx) ?: break
        val num = replaceBalancedLatexConstructs(first.first)
        val den = replaceBalancedLatexConstructs(second.first)
        s = s.substring(0, idx) + "($num)/($den)" + s.substring(second.second)
    }

    // Replace \sqrt{A} with √(A)
    guard = 0
    while (s.contains("\\sqrt{") && guard < 100) {
        guard++
        val idx = s.indexOf("\\sqrt{")
        val inner = extractBraced(s, idx + 5) ?: break
        val cleanInner = replaceBalancedLatexConstructs(inner.first)
        s = s.substring(0, idx) + "√($cleanInner)" + s.substring(inner.second)
    }
    return s
}

private fun formatTargetLatex(t: String): String {
    val clean = t.trim().replace("$", "").replace("`", "")
    return when {
        clean.lowercase() in listOf("inf", "infinity", "oo") -> "\\infty"
        clean.lowercase() in listOf("-inf", "-infinity", "-oo") -> "-\\infty"
        clean.lowercase() in listOf("+inf", "+infinity", "+oo") -> "+\\infty"
        clean.endsWith("^+") || clean.endsWith("^-") -> {
            val num = clean.dropLast(2)
            val sign = clean.takeLast(1)
            "$num^{$sign}"
        }
        clean.endsWith("+") || clean.endsWith("-") -> {
            val num = clean.dropLast(1)
            val sign = clean.takeLast(1)
            if (num.isNotEmpty() && num.all { it.isDigit() || it == '.' }) "$num^{$sign}" else clean
        }
        clean.endsWith("^") -> {
            val num = clean.dropLast(1)
            if (num.isEmpty()) "{□}^{□}" else "$num^{□}"
        }
        else -> clean
    }
}

private fun formatMatrixLatex(matrixStr: String): String {
    val trimmed = matrixStr.trim()
    if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        val rowMatches = Regex("""[\[{](.*?)[\]}]""").findAll(inner).map { it.groupValues[1].trim() }.toList()
        if (rowMatches.isNotEmpty()) {
            val formattedRows = rowMatches.map { row ->
                row.split(",").joinToString(" & ") { it.trim() }
            }
            return "\\begin{pmatrix} " + formattedRows.joinToString(" \\\\ ") + " \\end{pmatrix}"
        }
    }
    return matrixStr
}

private fun splitTopLevelPreview(s: String, delimiter: Char = ','): List<String> {
    val parts = mutableListOf<String>()
    var depthPar = 0
    var depthSqr = 0
    var depthCur = 0
    var last = 0
    for (i in s.indices) {
        val c = s[i]
        if (c == '(') depthPar++
        else if (c == ')') depthPar--
        else if (c == '[') depthSqr++
        else if (c == ']') depthSqr--
        else if (c == '{') depthCur++
        else if (c == '}') depthCur--
        else if (c == delimiter && depthPar == 0 && depthSqr == 0 && depthCur == 0) {
            parts.add(s.substring(last, i).trim())
            last = i + 1
        }
    }
    parts.add(s.substring(last).trim())
    return parts
}

private fun deducePreviewVar(expr: String): String {
    val vars = Regex("""\b([a-zA-Z])\b""").findAll(expr)
        .map { it.groupValues[1] }
        .filter { it !in listOf("d", "e", "E", "i", "I", "C", "c", "N", "pi", "Pi") }
        .toList()
    return vars.firstOrNull() ?: "x"
}

fun formatInputForLatexPreview(raw: String): String {
    var s = raw.trim().replace("$", "").replace("`", "")
    if (s.isEmpty()) return ""

    // Normalize one-sided limit superscripts: e.g. 2^+ -> 2^{+}, 2^- -> 2^{-}
    s = s.replace(Regex("""(\d+)\^([+-])(?!\d)"""), "$1^{$2}")

    // 1. lim / limit with parens or brackets: lim(...) or lim[...]
    val mLim = Regex("""^(?:lim|limit)\s*[\(\[](.+)[\)\]]$""", RegexOption.IGNORE_CASE).find(s)
    if (mLim != null) {
        val inner = mLim.groupValues[1].trim()
        val parts = splitTopLevelPreview(inner, ',')
        var varName = "x"
        var target = "0"
        var expr = ""
        if (parts.size >= 3) {
            if (parts[0].matches(Regex("""^[a-zA-Z]$"""))) {
                varName = parts[0].trim()
                target = formatTargetLatex(parts[1])
                expr = parts.subList(2, parts.size).joinToString(",")
            } else if (parts[1].matches(Regex("""^[a-zA-Z]$"""))) {
                expr = parts[0].trim()
                varName = parts[1].trim()
                target = formatTargetLatex(parts[2])
            } else {
                expr = parts[0].trim()
                varName = parts[1].trim()
                target = formatTargetLatex(parts[2])
            }
        } else if (parts.size == 2) {
            val sub0 = if (parts[0].contains("->") || parts[0].contains("→")) parts[0].split(Regex("""->|→"""), limit = 2)
                       else if (parts[0].contains("=")) parts[0].split("=", limit = 2)
                       else if (Regex("""(?i)\bto\b""").containsMatchIn(parts[0])) parts[0].split(Regex("""(?i)\bto\b"""), limit = 2)
                       else null
            val sub1 = if (parts[1].contains("->") || parts[1].contains("→")) parts[1].split(Regex("""->|→"""), limit = 2)
                       else if (parts[1].contains("=")) parts[1].split("=", limit = 2)
                       else if (Regex("""(?i)\bto\b""").containsMatchIn(parts[1])) parts[1].split(Regex("""(?i)\bto\b"""), limit = 2)
                       else null
            if (sub0 != null && sub0[0].trim().matches(Regex("""^[a-zA-Z]$"""))) {
                varName = sub0[0].trim()
                target = formatTargetLatex(sub0[1])
                expr = parts[1].trim()
            } else if (sub1 != null && sub1[0].trim().matches(Regex("""^[a-zA-Z]$"""))) {
                varName = sub1[0].trim()
                target = formatTargetLatex(sub1[1])
                expr = parts[0].trim()
            } else {
                expr = parts[0].trim()
                target = formatTargetLatex(parts[1])
                varName = deducePreviewVar(expr)
            }
        } else if (parts.size == 1) {
            expr = parts[0].trim()
            varName = deducePreviewVar(expr)
        }
        val cleanExpr = expr.replace("*", " \\cdot ")
        return "\\lim_{$varName \\to $target} \\left( $cleanExpr \\right)"
    }

    // 2. lim_{x -> a} or \lim_{x \to a}
    val mSubLim = Regex("""^(?:\\?lim)\s*_\{?\s*([a-zA-Z]+)\s*(?:->|\\to|to|→)\s*([^}]+)\}?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
    if (mSubLim != null) {
        val v = mSubLim.groupValues[1].trim()
        val t = formatTargetLatex(mSubLim.groupValues[2])
        val body = mSubLim.groupValues[3].trim().removePrefix("*").trim()
        return "\\lim_{$v \\to $t} \\left( ${body.replace("*", " \\cdot ")} \\right)"
    }

    // 2.5 Plain unparenthesized lim: lim x -> 2 f(x)
    val mPlainLim = Regex("""^(?:\\?lim|limit)\s+([a-zA-Z]+)\s*(?:->|\\to|to|→)\s*(\S+)\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
    if (mPlainLim != null) {
        val v = mPlainLim.groupValues[1].trim()
        val t = formatTargetLatex(mPlainLim.groupValues[2])
        val body = mPlainLim.groupValues[3].trim().removePrefix("*").trim()
        return "\\lim_{$v \\to $t} \\left( ${body.replace("*", " \\cdot ")} \\right)"
    }

    // 3. inv(...) or inv[...]
    val mInv = Regex("""^(?:inv|inverse)\s*[\(\[](.+)[\)\]]$""", RegexOption.IGNORE_CASE).find(s)
    if (mInv != null) {
        val inner = mInv.groupValues[1].trim()
        val mat = formatMatrixLatex(inner)
        return "\\operatorname{inv}\\left($mat\\right)"
    }

    // 4. det(...) or det[...]
    val mDet = Regex("""^det\s*[\(\[](.+)[\)\]]$""", RegexOption.IGNORE_CASE).find(s)
    if (mDet != null) {
        val inner = mDet.groupValues[1].trim()
        val mat = formatMatrixLatex(inner)
        return "\\det\\left($mat\\right)"
    }

    // 5. int(...)
    val mInt = Regex("""^(?:int|integrate)\s*\((.+)\)$""", RegexOption.IGNORE_CASE).find(s)
    if (mInt != null) {
        val inner = mInt.groupValues[1].trim()
        val parts = splitTopLevelPreview(inner, ',')
        if (parts.size == 4) {
            val f = parts[0].trim().replace("*", " \\cdot ")
            val x = parts[1].trim()
            val a = formatTargetLatex(parts[2])
            val b = formatTargetLatex(parts[3])
            return "\\int_{$a}^{$b} $f \\,d$x"
        } else if (parts.size == 2) {
            val f = parts[0].trim().replace("*", " \\cdot ")
            val x = parts[1].trim()
            return "\\int $f \\,d$x"
        } else if (parts.size == 1) {
            val f = parts[0].trim().replace("*", " \\cdot ")
            val x = deducePreviewVar(f)
            return "\\int $f \\,d$x"
        }
    }

    // 6. sum(...)
    val mSum = Regex("""^(?:sum)\s*\((.+)\)$""", RegexOption.IGNORE_CASE).find(s)
    if (mSum != null) {
        val inner = mSum.groupValues[1].trim()
        val parts = splitTopLevelPreview(inner, ',')
        if (parts.size == 4) {
            val f = parts[0].trim().replace("*", " \\cdot ")
            val n = parts[1].trim()
            val a = formatTargetLatex(parts[2])
            val b = formatTargetLatex(parts[3])
            return "\\sum_{$n=$a}^{$b} $f"
        }
    }

    // 7. General expression cleanups
    var res = s
    res = res.replace(Regex("""(?i)\b(infinity|inf)\b"""), "\\infty")
    res = res.replace(Regex("""(?i)\bpi\b"""), "\\pi")
    res = res.replace("==", " = ")
    res = res.replace("<=", " \\le ").replace(">=", " \\ge ")
    res = res.replace("!=", " \\neq ")
    res = res.replace("*", " \\cdot ")

    // 8. Safely handle dangling superscripts and subscripts to prevent MathJax
    // "Missing superscript or subscript argument" errors while typing:
    // 1. Caret with no base and no exponent (e.g. "^", "+ ^", "a + ^ - b")
    res = res.replace(Regex("""(^|(?<=[+\-*/=(,\s{]))\s*\^\s*($|(?=[*/=),\s}]|[+-]\s))"""), "$1{□}^{□}")
    // 2. Caret with base but no exponent (e.g. "x^", "x^ ", "x^ + 2")
    res = res.replace(Regex("""\^\s*($|(?=[*/=),\s}]|[+-]\s))"""), "^{□}")
    // 3. Caret with no base, but has exponent (e.g. "^2", "+ ^x")
    res = res.replace(Regex("""(^|(?<=[+\-*/=(,\s{]))\s*\^"""), "$1{□}^")

    // 4. Underscore with no base and no subscript (e.g. "_", "+ _")
    res = res.replace(Regex("""(^|(?<=[+\-*/=(,\s{]))\s*_\s*($|(?=[+\-*/=),\s}]))"""), "$1{□}_{□}")
    // 5. Underscore with base but no subscript (e.g. "x_")
    res = res.replace(Regex("""_\s*($|(?=[+\-*/=),\s}]))"""), "_{□}")
    // 6. Underscore with no base, but has subscript (e.g. "_2")
    res = res.replace(Regex("""(^|(?<=[+\-*/=(,\s{]))\s*_"""), "$1{□}_")

    // 9. Auto-balance trailing unclosed braces or parentheses during typing
    if (res.endsWith("^{") || res.endsWith("_{")) {
        res += "□}"
    } else {
        val openBraces = res.count { it == '{' } - res.count { it == '}' }
        if (openBraces > 0) {
            res += "}".repeat(openBraces)
        }
    }
    val openParens = res.count { it == '(' } - res.count { it == ')' }
    if (openParens > 0) {
        res += ")".repeat(openParens)
    }

    return res
}

fun cleanCasResultForCopy(rawHtml: String): String {
    var text = rawHtml
    // Remove UI interactive controls, styles, and scripts
    text = text.replace(Regex("""(?s)<summary.*?>.*?</summary>"""), "")
    text = text.replace(Regex("""(?s)<button.*?>.*?</button>"""), "")
    text = text.replace(Regex("""(?s)<style.*?>.*?</style>"""), "")
    text = text.replace(Regex("""(?s)<script.*?>.*?</script>"""), "")

    // Format LaTeX matrices before replacing \\ with \n
    text = text.replace(Regex("""(?s)\\begin\{[bp]matrix\}(.*?)\\end\{[bp]matrix\}""")) { m ->
        val body = m.groupValues[1].trim().removeSuffix("\\\\").trim()
        val rows = body.split("\\\\").map { row ->
            "[" + row.split("&").joinToString(", ") { it.trim() } + "]"
        }
        "[" + rows.joinToString(", ") + "]"
    }

    // Convert line breaks and structural HTML elements to newlines
    text = text.replace(Regex("""(?i)<br\s*/?>"""), "\n")
    text = text.replace(Regex("""(?i)</(p|div|h[1-6]|tr)>"""), "\n")
    text = text.replace(Regex("""(?i)<hr\s*/?>"""), "\n---\n")
    text = text.replace(Regex("""(?i)<li\s*>"""), "• ")
    text = text.replace(Regex("""(?i)</li>"""), "\n")

    // Strip all remaining HTML tags safely without removing math < or >
    text = text.replace(Regex("""</?[a-zA-Z!][^>]*>"""), "")

    // Decode common HTML entities
    text = text.replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    // Strip $$ and ` delimiters
    text = text.replace("$$", "").replace("`", "")

    // Clean \left and \right delimiters FIRST so \le does not corrupt \left
    text = text.replace("\\left(", "(")
        .replace("\\right)", ")")
        .replace("\\left[", "[")
        .replace("\\right]", "]")
        .replace("\\left\\{", "{")
        .replace("\\right\\}", "}")
        .replace("\\left|", "|")
        .replace("\\right|", "|")
        .replace("\\left\\lfloor", "⌊")
        .replace("\\right\\rfloor", "⌋")
        .replace("\\left\\lceil", "⌈")
        .replace("\\right\\lceil", "⌉")
        .replace("\\left", "")
        .replace("\\right", "")

    // Balanced \frac{...}{...} and \sqrt{...} replacement
    text = replaceBalancedLatexConstructs(text)

    // Clean common LaTeX commands for plain text readability
    text = text.replace("\\lt", "<")
        .replace("\\gt", ">")
        .replace(Regex("""\\le(?:q)?(?![a-zA-Z])"""), "<=")
        .replace(Regex("""\\ge(?:q)?(?![a-zA-Z])"""), ">=")
        .replace("\\neq", "!=")
        .replace(Regex("""\\to(?![a-zA-Z])"""), " -> ")
        .replace("\\implies", " => ")
        .replace(Regex("""\\int(?![a-zA-Z])"""), "∫")
        .replace(Regex("""\\infty(?![a-zA-Z])"""), "∞")
        .replace(Regex("""\\in(?![a-zA-Z])"""), " ∈ ")
        .replace("\\cup", " ∪ ")
        .replace("\\lor", " ∨ ")
        .replace("\\land", " ∧ ")
        .replace("\\{", "{")
        .replace("\\}", "}")
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\\\", "\n")
        .replace("\\quad", " ")
        .replace("\\qquad", " ")
        .replace(Regex("""\\text(?:normal)?\{([^}]*)\}"""), "$1")
        .replace("\\lfloor", "⌊")
        .replace("\\rfloor", "⌋")
        .replace("\\lceil", "⌈")
        .replace("\\rceil", "⌉")
        .replace("\\emptyset", "∅")
        .replace("\\mathbb{R}", "ℝ")
        .replace("\\mathbb{Z}", "ℤ")
        .replace("\\approx", "≈")
        .replace(Regex("""\\pm(?![a-zA-Z])"""), "±")
        .replace("\\times", "×")
        .replace("\\cdot", "·")
        .replace(Regex("""\\pi(?![a-zA-Z])"""), "π")
        .replace("\\Delta", "Δ")
        .replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\lambda", "λ")
        .replace("\\mu", "μ")
        .replace("\\circ", "°")
        .replace("\\det", "det")
        .replace("\\exp", "exp")
        .replace("\\lim_", "lim_")
        .replace("\\log", "log")
        .replace("\\ln", "ln")
        .replace("\\sin", "sin")
        .replace("\\cos", "cos")
        .replace("\\tan", "tan")
        .replace(Regex("""\{([a-zA-Z0-9]+)\}"""), "$1")

    val lines = text.lines().map { it.trim() }
    val cleanLines = mutableListOf<String>()
    for (line in lines) {
        if (line.isNotEmpty()) {
            cleanLines.add(line)
        } else if (cleanLines.isNotEmpty() && cleanLines.last().isNotEmpty()) {
            cleanLines.add("")
        }
    }
    return cleanLines.joinToString("\n").trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasScreen(
    appSettings: AppSettings,
    textColor: Color,
    onBack: () -> Unit
) {
    val accentColor = LocalAccentColor.current
    val accentHex = String.format("#%06X", (0xFFFFFF and accentColor.toArgb()))
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isLight = appSettings.theme == AppTheme.LIGHT || (appSettings.theme in listOf(AppTheme.SYSTEM, AppTheme.MATERIAL_YOU) && !isSystemDark)
    val isDark = !isLight

    val latexColor = appSettings.keyTextColor?.let { Color(it) } ?: (if (isDark) Color.White else Color.Black)
    val latexHex = String.format("#%06X", (0xFFFFFF and latexColor.toArgb()))

    var equation by remember { mutableStateOf("") }
    var aiResult by remember { mutableStateOf("") }
    var baseResult by remember { mutableStateOf(Translator.tr("Denklem veya ifade girin.")) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

    var aiOptIn by remember { mutableStateOf(prefs.getInt("ai_opt_in", -1)) }
    var showAiSettings by remember { mutableStateOf(aiOptIn == -1) }
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var isOcrLoading by remember { mutableStateOf(false) }
    var showDrawingDialog by remember { mutableStateOf(false) }

    val imageCropLauncher = androidx.activity.compose.rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                isOcrLoading = true
                OcrHelper.recognizeText(context, uri, aiOptIn, onSuccess = { recognized ->
                    isOcrLoading = false
                    if (recognized.isBlank()) {
                        Toast.makeText(context, Translator.tr("Görselde herhangi bir yazı veya formül bulunamadı!"), Toast.LENGTH_LONG).show()
                    } else {
                        equation = recognized
                    }
                }, onError = { e ->
                    isOcrLoading = false
                    Toast.makeText(context, "${Translator.tr("Okuma hatası: ")}${e.message}", Toast.LENGTH_LONG).show()
                })
            } ?: run {
                Toast.makeText(context, Translator.tr("Hata: Kırpılan görsel alınamadı."), Toast.LENGTH_SHORT).show()
            }
        } else {
            result.error?.let { e ->
                Toast.makeText(context, "${Translator.tr("Kırpma iptal edildi veya hata oluştu: ")}${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(equation, aiOptIn, Translator.currentLang) {
        aiResult = ""
        val trimmed = equation.trim().replace("$", "").replace("`", "")
        if (trimmed.isBlank()) {
            baseResult = Translator.tr("Denklem veya ifade girin.")
            return@LaunchedEffect
        }

        val isIncomplete = trimmed.endsWith("^") || trimmed.endsWith("_") ||
                           trimmed == "^" || trimmed == "_" || trimmed == "$" ||
                           Regex("""[+\-*/=,]$""").containsMatchIn(trimmed) ||
                           trimmed in listOf("+", "-", "*", "/", "=") ||
                           (trimmed.endsWith("(") && !trimmed.contains(")"))

        if (isIncomplete) {
            baseResult = "<div style=\"opacity: 0.65; padding: 14px; text-align: center; font-size: 0.95em;\"><b>" + Translator.tr("İfadeyi tamamlayın...") + "</b></div>"
            return@LaunchedEffect
        }

        baseResult = "<div class=\"cas-loading\"><b>${Translator.tr("Hesaplanıyor...")}</b></div>"
        kotlinx.coroutines.delay(350)
        baseResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            solveCas(equation, aiOptIn == 1)
        }
    }

    if (showDrawingDialog) {
        DrawingCanvasDialog(
            onDismiss = { showDrawingDialog = false },
            onDone = { bitmap ->
                showDrawingDialog = false
                isOcrLoading = true
                OcrHelper.recognizeBitmap(context, bitmap, aiOptIn, onSuccess = { recognized ->
                    isOcrLoading = false
                    if (recognized.isBlank()) {
                        Toast.makeText(context, Translator.tr("Çizimde bir formül bulunamadı!"), Toast.LENGTH_LONG).show()
                    } else {
                        equation = recognized
                    }
                }, onError = { e ->
                    isOcrLoading = false
                    Toast.makeText(context, "${Translator.tr("Okuma hatası: ")}${e.message}", Toast.LENGTH_LONG).show()
                })
            }
        )
    }

    val quickCasSnippets = listOf(
        "d/dx" to "d/dx(3x^2 + 5x)",
        "∫ dx" to "int(x^2 - 4x)",
        "∫_a^b" to "int(x^2, x, 0, 3)",
        "lim" to "lim((x^2 - 4)/(x - 2), x, 2)",
        "∑" to "sum(1/n^2, n, 1, inf)",
        "2. Derece" to "x^2 - 5x + 6 = 0",
        "ODE" to "y'' - 3y' + 2y = 0",
        "Matris Det" to "det([[1,2],[3,4]])",
        "Matris Ters" to "inv([[1,2],[3,4]])",
        "√" to "sqrt(",
        "π" to "pi",
        "∞" to "inf",
        "≤" to "<=",
        "≥" to ">="
    )

    Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Translator.tr("Geri"), tint = textColor)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("CAS", color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))

            if (aiOptIn == 1) {
                IconButton(onClick = { showAiConfigDialog = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = Translator.tr("AI Ayarları"), tint = textColor)
                }
            }
        }

        if (showAiConfigDialog) {
            AiSettingsDialog(
                onDismiss = { showAiConfigDialog = false },
                onSave = {
                    aiOptIn = 1
                    showAiConfigDialog = false
                }
            )
        }

        if (showAiSettings) {
            AlertDialog(
                onDismissRequest = {
                    showAiSettings = false
                    prefs.edit().putInt("ai_opt_in", 0).putBoolean("ai_accepted", false).apply()
                    aiOptIn = 0
                },
                title = { Text(Translator.tr("Akıllı Matematik Asistanı (AI)")) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(Translator.tr("NEDEN YAPAY ZEKA?"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Translator.tr("Uygulamamız, hesaplamaları %100 kesin ve hatasız olarak kendi yerel cebirsel motoruyla yapar. Yapay Zeka (AI), sadece bulunan bu sonucun 'nasıl bulunduğunu' size bir öğretmen gibi adım adım açıklamak için kullanılır."), fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(Translator.tr("BİLGİLENDİRME & RİSKLER"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(Translator.tr("• Matematiksel Hata Payı: LLM modelleri hesaplama motoru değildir, bazen çok mantıklı görünen tamamen yanlış adımlar uydurabilirler. Doğruluğunu daima nihai sonuçla teyit edin."), fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(Translator.tr("Yapay Zeka (AI) özelliğini aktifleştirmek ister misiniz?"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Translator.tr("Not: Google (Gemini) API anahtarı tamamen ücretsizdir. Bu tercihinizi daha sonra Genel Ayarlar menüsünden sıfırlayabilirsiniz."), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAiSettings = false
                        prefs.edit().putInt("ai_opt_in", 1).putBoolean("ai_accepted", true).apply()
                        aiOptIn = 1
                        showAiConfigDialog = true
                    }) { Text(Translator.tr("Aktifleştir")) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAiSettings = false
                        prefs.edit().putInt("ai_opt_in", 0).putBoolean("ai_accepted", false).apply()
                        aiOptIn = 0
                    }) { Text(Translator.tr("Hayır, İstemiyorum")) }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = equation,
            onValueChange = { equation = it },
            label = {
                if (isOcrLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translator.tr("Görsel Çözümleniyor..."))
                    }
                } else {
                    Text(Translator.tr("Denklem Girin (örn: 2x + 5 = 15)"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (!isOcrLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (equation.isNotEmpty()) {
                            IconButton(onClick = { equation = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = Translator.tr("Temizle"), tint = textColor)
                            }
                        }
                        IconButton(onClick = { showDrawingDialog = true }) {
                            Icon(Icons.Filled.Create, contentDescription = Translator.tr("Ekrana Çiz"), tint = textColor)
                        }
                        IconButton(onClick = {
                            val options = CropImageOptions(
                                imageSourceIncludeGallery = true,
                                imageSourceIncludeCamera = true,
                                guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                                fixAspectRatio = false,
                                allowRotation = true,
                                allowFlipping = true,
                                scaleType = com.canhub.cropper.CropImageView.ScaleType.FIT_CENTER,
                                cropMenuCropButtonTitle = Translator.tr("Formülü Okut")
                            )
                            imageCropLauncher.launch(CropImageContractOptions(uri = null, cropImageOptions = options))
                        }) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = Translator.tr("Kamera ile Okut"), tint = textColor)
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.45f),
                focusedLabelColor = accentColor,
                unfocusedLabelColor = textColor.copy(alpha = 0.55f),
                cursorColor = accentColor
            ),
            textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 18.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick CAS Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCasSnippets.forEach { (label, template) ->
                SuggestionChip(
                    onClick = {
                        equation = if (template.length <= 5 && equation.isNotEmpty() && !template.contains("=")) {
                            equation + template
                        } else {
                            template
                        }
                    },
                    label = { Text(Translator.tr(label), fontSize = 12.sp, color = textColor) },
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

        Spacer(modifier = Modifier.height(8.dp))

        val latexCssColor = latexHex
        val dividerHex = if (isDark) "#FFFFFF" else "#000000"
        val stepBgHex = if (isDark) "rgba(255, 255, 255, 0.04)" else "rgba(0, 0, 0, 0.03)"
        val stepBorderHex = if (isDark) "rgba(255, 255, 255, 0.14)" else "rgba(0, 0, 0, 0.14)"
        val errBg = String.format("#%06X", (0xFFFFFF and MaterialTheme.colorScheme.errorContainer.toArgb()))
        val errOnBg = String.format("#%06X", (0xFFFFFF and MaterialTheme.colorScheme.onErrorContainer.toArgb()))

        val rawResult = if (aiResult.isNotEmpty()) {
            baseResult
                .replace(Regex("""<div class=["']cas-engine-error["'][^>]*>.*?</div>\s*""", RegexOption.DOT_MATCHES_ALL), "")
                .replace("<details class=\"ai-details\" style=\"margin-top: 15px;\">", "<details class=\"ai-details\" open style=\"margin-top: 15px;\">")
                .replace("<details style=\"margin-top: 15px;\">", "<details open style=\"margin-top: 15px;\">")
                .replace("onclick=\"if(this.dataset.loaded != 'true') { Android.askAI('", "onclick=\"//")
                .replace("<div class=\"ai-steps-content\" style=\"padding-top: 15px;\"></div></details>", "<div class=\"ai-steps-content\" style=\"padding-top: 15px;\">$aiResult</div></details>")
        } else baseResult

        val resultHtml = rawResult.replace("\n", "<br/>")

        val previewLatex = formatInputForLatexPreview(equation)
        val inputPreview = if (previewLatex.isNotBlank()) {
            "<b>${Translator.tr("Girdi Önizlemesi:")}</b><br/><div class=\"math-scroll\" style=\"font-size: 1.25em; text-align: center; margin: 8px 0; color: $latexCssColor;\">$$ $previewLatex $$</div><hr style=\"border: 0; border-top: 1px solid ${dividerHex}30; margin: 14px 0;\"/>"
        } else ""

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <script>
                window.MathJax = {
                  tex: {
                    inlineMath: [['$', '$'], ['\\(', '\\)']],
                    displayMath: [['$$', '$$'], ['\\[', '\\]']]
                  },
                  svg: {
                    fontCache: 'global'
                  }
                };
                </script>
                <script id="MathJax-script" async src="file:///android_asset/mathjax/tex-svg.js"></script>
                <style>
                    :root {
                        --ai-color: $latexCssColor;
                        --accent-color: $accentHex;
                        --error-bg: $errBg;
                        --error-color: $errOnBg;
                    }
                    details > summary {
                        transition: opacity 0.2s, transform 0.2s;
                        user-select: none;
                        -webkit-tap-highlight-color: transparent;
                    }
                    details > summary:active {
                        opacity: 0.6;
                        transform: scale(0.99);
                    }
                    details[open] summary ~ * {
                        animation: aiFadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
                    }
                    @keyframes aiFadeIn {
                        from { opacity: 0; transform: translateY(-6px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .cas-steps-details {
                        margin-top: 14px;
                        padding: 12px 14px;
                        border-radius: 12px;
                        background: $stepBgHex;
                        border: 1px solid $stepBorderHex;
                    }
                    .cas-steps-summary {
                        cursor: pointer;
                        font-weight: 700;
                        color: var(--accent-color);
                        font-size: 0.96em;
                    }
                    .cas-steps-body {
                        margin-top: 10px;
                        padding-left: 12px;
                        border-left: 3px solid var(--accent-color);
                        line-height: 1.65;
                        font-size: 0.95em;
                        overflow-x: auto;
                        color: $latexCssColor;
                    }
                    .ai-loading {
                        text-align: center;
                        color: var(--ai-color);
                        font-style: italic;
                        margin-bottom: 15px;
                        animation: aiPulse 1.5s infinite ease-in-out;
                    }
                    @keyframes aiPulse {
                        0% { opacity: 0.3; }
                        50% { opacity: 1; }
                        100% { opacity: 0.3; }
                    }
                    .cas-loading {
                        font-size: 1.1em;
                        margin-bottom: 8px;
                        opacity: 0.6;
                        animation: casPulse 1.2s infinite ease-in-out;
                    }
                    @keyframes casPulse {
                        0% { opacity: 0.3; }
                        50% { opacity: 0.9; }
                        100% { opacity: 0.3; }
                    }
                    .cas-engine-error:empty {
                        display: none !important;
                        margin: 0 !important;
                        padding: 0 !important;
                    }
                    body {
                        color: $latexCssColor;
                        background-color: transparent;
                        font-family: sans-serif;
                        font-size: 16px;
                        padding: 8px;
                        margin: 0;
                        overflow-x: auto;
                        word-break: break-word;
                        overflow-wrap: anywhere;
                    }
                    mjx-container {
                        max-width: 100% !important;
                        overflow-x: auto !important;
                        overflow-y: hidden !important;
                        padding-bottom: 4px;
                        color: $latexCssColor !important;
                    }
                    mjx-container svg {
                        color: $latexCssColor !important;
                        fill: currentColor !important;
                    }
                    .math-scroll {
                        color: $latexCssColor !important;
                        max-width: 100%;
                        overflow-x: auto;
                        overflow-y: hidden;
                        -webkit-overflow-scrolling: touch;
                        padding-bottom: 6px;
                    }
                </style>
            </head>
            <body>
                $inputPreview
                <div>$resultHtml</div>
            </body>
            </html>
        """.trimIndent()

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translator.tr("Sonuç / Adımlar (Grafik Gösterim):"), fontWeight = FontWeight.Bold, color = textColor)
                    if (equation.isNotBlank() && !rawResult.contains(Translator.tr("Denklem veya ifade girin.")) && !rawResult.contains("Denklem veya ifade girin.")) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val cleanText = cleanCasResultForCopy(rawResult)
                                val clip = ClipData.newPlainText("CAS Result", cleanText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, Translator.tr("Çözüm Kopyalandı"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = Translator.tr("Kopyala"),
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            this.settings.javaScriptEnabled = true
                            this.settings.domStorageEnabled = true
                            this.settings.allowFileAccess = true
                            this.settings.allowContentAccess = true
                            setBackgroundColor(0x00000000)
                            webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.settings.javaScriptEnabled = true
                        webView.settings.allowFileAccess = true
                        webView.settings.allowContentAccess = true
                        webView.addJavascriptInterface(
                            WebAppInterface(
                                context,
                                onRequestSettings = {
                                    if (aiOptIn == 1) showAiConfigDialog = true else showAiSettings = true
                                },
                                onAiResult = { result -> aiResult = result }
                            ),
                            "Android"
                        )
                        webView.setOnLongClickListener {
                            val clipboard = webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val cleanText = cleanCasResultForCopy(rawResult)
                            val clip = ClipData.newPlainText("CAS Result", cleanText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(webView.context, Translator.tr("Çözüm Kopyalandı"), Toast.LENGTH_SHORT).show()
                            true
                        }
                        webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
                    }
                )
            }
        }
    }
}
