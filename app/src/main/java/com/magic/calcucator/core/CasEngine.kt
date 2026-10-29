package com.magic.calcucator.core

import com.magic.calcucator.Translator
import kotlin.math.*

private fun fmtNum(v: Double): String {
    if (abs(v) < 1e-9) return "0"
    val asLong = v.roundToLong()
    if (abs(v - asLong.toDouble()) < 1e-9) return asLong.toString()
    return (round(v * 10000.0) / 10000.0).toString()
}

object CasMemory {
    val matrices = mutableMapOf<String, List<List<Double>>>()

    fun parseMatrix(str: String): List<List<Double>> {
        val clean = str.replace(" ", "")
        val normalized = if (clean.startsWith("{{") && clean.endsWith("}}")) {
            "[[" + clean.substring(2, clean.length - 2).replace("},{", "],[") + "]]"
        } else clean
        if (!normalized.startsWith("[[") || !normalized.endsWith("]]")) {
            throw RuntimeException(Translator.tr("Matris formatı hatalı. Örn: [[1,2],[3,4]]"))
        }
        val content = normalized.substring(2, normalized.length - 2)
        val rows = content.split("],[")
        return rows.map { row ->
            row.split(",").map { it.toDouble() }
        }
    }

    fun toBracketString(m: List<List<Double>>): String {
        return "[[" + m.joinToString("],[") { row ->
            row.joinToString(",") { fmtNum(it) }
        } + "]]"
    }

    fun toLatex(m: List<List<Double>>): String {
        val sb = StringBuilder()
        sb.append("\\begin{bmatrix}")
        for (row in m) {
            sb.append(row.joinToString(" & ") { fmtNum(it) })
            sb.append(" \\\\ ")
        }
        sb.append("\\end{bmatrix}")
        return sb.toString()
    }

    fun determinant(m: List<List<Double>>): Double {
        val n = m.size
        if (m.any { it.size != n }) throw RuntimeException(Translator.tr("Determinant sadece kare matrisler için hesaplanabilir."))
        if (n == 1) return m[0][0]
        if (n == 2) return m[0][0] * m[1][1] - m[0][1] * m[1][0]
        if (n == 3) {
            return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) -
                m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0]) +
                m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0])
        }
        throw RuntimeException(Translator.tr("Adım adım determinant şu an maksimum 3x3 matrisler için destekleniyor."))
    }

    fun transpose(m: List<List<Double>>): List<List<Double>> {
        if (m.isEmpty()) return m
        val rows = m.size
        val cols = m[0].size
        val result = MutableList(cols) { MutableList(rows) { 0.0 } }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j][i] = m[i][j]
            }
        }
        return result
    }

    fun inverse(m: List<List<Double>>): List<List<Double>> {
        val n = m.size
        val d = determinant(m)
        if (abs(d) < 1e-9) throw RuntimeException(Translator.tr("Determinant 0 olduğu için matrisin tersi alınamaz (Singüler matris)."))

        if (n == 1) return listOf(listOf(1.0 / m[0][0]))
        if (n == 2) {
            return listOf(
                listOf(m[1][1] / d, -m[0][1] / d),
                listOf(-m[1][0] / d, m[0][0] / d)
            )
        }
        if (n == 3) {
            val adj = listOf(
                listOf(m[1][1] * m[2][2] - m[1][2] * m[2][1], -(m[0][1] * m[2][2] - m[0][2] * m[2][1]), m[0][1] * m[1][2] - m[0][2] * m[1][1]),
                listOf(-(m[1][0] * m[2][2] - m[1][2] * m[2][0]), m[0][0] * m[2][2] - m[0][2] * m[2][0], -(m[0][0] * m[1][2] - m[0][2] * m[1][0])),
                listOf(m[1][0] * m[2][1] - m[1][1] * m[2][0], -(m[0][0] * m[2][1] - m[0][1] * m[2][0]), m[0][0] * m[1][1] - m[0][1] * m[1][0])
            )
            return adj.map { row -> row.map { it / d } }
        }
        throw RuntimeException(Translator.tr("Adım adım ters alma şu an maksimum 3x3 matrisler için destekleniyor."))
    }
}

class Polynomial(val terms: Map<Int, Double>) {
    operator fun plus(other: Polynomial): Polynomial {
        val newTerms = terms.toMutableMap()
        for ((exp, coef) in other.terms) {
            newTerms[exp] = newTerms.getOrDefault(exp, 0.0) + coef
        }
        return Polynomial(newTerms.filterValues { abs(it) > 1e-9 })
    }

    operator fun minus(other: Polynomial): Polynomial {
        val newTerms = terms.toMutableMap()
        for ((exp, coef) in other.terms) {
            newTerms[exp] = newTerms.getOrDefault(exp, 0.0) - coef
        }
        return Polynomial(newTerms.filterValues { abs(it) > 1e-9 })
    }

    operator fun times(other: Polynomial): Polynomial {
        val newTerms = mutableMapOf<Int, Double>()
        for ((e1, c1) in terms) {
            for ((e2, c2) in other.terms) {
                val exp = e1 + e2
                val coef = c1 * c2
                newTerms[exp] = newTerms.getOrDefault(exp, 0.0) + coef
            }
        }
        return Polynomial(newTerms.filterValues { abs(it) > 1e-9 })
    }

    fun divAndRem(other: Polynomial): Pair<Polynomial, Polynomial> {
        val denDeg = other.degree()
        if (denDeg == 0 && abs(other.getCoef(0)) < 1e-9) throw RuntimeException(Translator.tr("Sıfıra bölme hatası."))

        var q = Polynomial(emptyMap())
        var r = this
        var guard = 0
        while (r.degree() >= denDeg && r.terms.isNotEmpty() && guard < 50) {
            guard++
            val rDeg = r.degree()
            val leadR = r.getCoef(rDeg)
            val leadD = other.getCoef(denDeg)

            val termCoef = leadR / leadD
            val termExp = rDeg - denDeg
            val termPoly = Polynomial(mapOf(termExp to termCoef))

            q += termPoly
            r -= (other * termPoly)
        }
        return Pair(q, r)
    }

    fun degree(): Int {
        if (terms.isEmpty()) return 0
        return terms.keys.maxOrNull() ?: 0
    }

    fun getCoef(exp: Int): Double = terms.getOrDefault(exp, 0.0)

    fun toLatex(): String {
        if (terms.isEmpty()) return "0"
        val sorted = terms.entries.sortedByDescending { it.key }
        val sb = StringBuilder()
        var firstAdded = false
        for (entry in sorted) {
            val exp = entry.key
            val coef = entry.value
            if (abs(coef) < 1e-9) continue

            if (firstAdded) {
                if (coef > 0) sb.append(" + ") else sb.append(" - ")
            } else {
                if (coef < 0) sb.append("-")
                firstAdded = true
            }

            val absCoef = abs(coef)
            if (abs(absCoef - 1.0) > 1e-9 || exp == 0) {
                sb.append(fmtNum(absCoef))
            }

            if (exp > 0) {
                sb.append("x")
                if (exp > 1) {
                    sb.append("^{").append(exp).append("}")
                }
            }
        }
        return sb.toString().ifEmpty { "0" }
    }

    fun evaluate(x: Double): Double {
        var sum = 0.0
        for ((exp, coef) in terms) {
            sum += coef * x.pow(exp.toDouble())
        }
        return sum
    }

    fun findRealRoots(): List<Double> {
        val deg = degree()
        if (deg == 0) return emptyList()
        if (deg == 1) {
            val a = getCoef(1)
            val b = getCoef(0)
            return if (abs(a) > 1e-9) listOf(-b / a) else emptyList()
        }
        if (deg == 2) {
            val a = getCoef(2)
            val b = getCoef(1)
            val c = getCoef(0)
            val delta = b * b - 4 * a * c
            if (delta < -1e-9) return emptyList()
            if (abs(delta) <= 1e-9) return listOf(-b / (2 * a))
            val r1 = (-b - sqrt(delta)) / (2 * a)
            val r2 = (-b + sqrt(delta)) / (2 * a)
            return listOf(r1, r2).sorted()
        }
        val roots = mutableListOf<Double>()
        for (i in -200..200) {
            val x = i.toDouble()
            if (abs(evaluate(x)) < 1e-7) {
                roots.add(x)
            }
        }
        return roots.distinctBy { (it * 10000).roundToLong() }.sorted()
    }
}

class RationalExpr(val num: Polynomial, val den: Polynomial) {
    operator fun plus(other: RationalExpr) = RationalExpr(num * other.den + other.num * den, den * other.den).simplify()
    operator fun minus(other: RationalExpr) = RationalExpr(num * other.den - other.num * den, den * other.den).simplify()
    operator fun times(other: RationalExpr) = RationalExpr(num * other.num, den * other.den).simplify()
    operator fun div(other: RationalExpr) = RationalExpr(num * other.den, den * other.num).simplify()

    fun simplify(): RationalExpr {
        val (q, r) = num.divAndRem(den)
        if (r.terms.isEmpty()) {
            return RationalExpr(q, Polynomial(mapOf(0 to 1.0)))
        }
        return this
    }

    fun toLatex(): String {
        if (den.degree() == 0 && abs(den.getCoef(0) - 1.0) < 1e-9) {
            return num.toLatex()
        }
        return "\\frac{" + num.toLatex() + "}{" + den.toLatex() + "}"
    }
}

fun parseRationalExpr(expr: String): RationalExpr {
    class RatParser(val str: String) {
        var pos = -1
        var ch = -1

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): RationalExpr {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) {
                val inner = parseFactor()
                return RationalExpr(Polynomial(mapOf(0 to -1.0)), Polynomial(mapOf(0 to 1.0))) * inner
            }

            var x: RationalExpr
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                val num = str.substring(startPos, pos).toDouble()
                x = RationalExpr(Polynomial(mapOf(0 to num)), Polynomial(mapOf(0 to 1.0)))
            } else if (ch == 'x'.code || ch == 'X'.code) {
                x = RationalExpr(Polynomial(mapOf(1 to 1.0)), Polynomial(mapOf(0 to 1.0)))
                nextChar()
            } else {
                throw RuntimeException("Beklenmeyen Karakter: " + ch.toChar())
            }

            if (eat('^'.code)) {
                val expStart = pos
                while (ch in '0'.code..'9'.code) nextChar()
                val exponent = str.substring(expStart, pos).toInt()
                if (exponent == 0) {
                    x = RationalExpr(Polynomial(mapOf(0 to 1.0)), Polynomial(mapOf(0 to 1.0)))
                } else {
                    var res = x
                    for (i in 1 until exponent) {
                        res *= x
                    }
                    x = res
                }
            }

            if (ch == 'x'.code || ch == 'X'.code || ch == '('.code || (ch in '0'.code..'9'.code) || ch == '.'.code) {
                x *= parseFactor()
            }

            return x
        }

        fun parseTerm(): RationalExpr {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    x /= divisor
                } else return x
            }
        }

        fun parseExpression(): RationalExpr {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parse(): RationalExpr {
            nextChar()
            val res = parseExpression()
            if (pos < str.length) throw RuntimeException("Beklenmeyen Karakter: " + ch.toChar())
            return res
        }
    }

    return RatParser(expr.replace(" ", "")).parse()
}

private fun formatUnifiedDirectHtml(
    title: String,
    resultLatex: String,
    stepsHtml: String?,
    rawInput: String,
    aiEnabled: Boolean
): String {
    val html = StringBuilder()
    html.append("<div style='font-size: 1.1em;'><b>$title</b></div>")
    html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ $resultLatex $$</div>")
    if (!stepsHtml.isNullOrBlank()) {
        html.append("<details class=\"cas-steps-details\" open>")
        html.append("<summary class=\"cas-steps-summary\">${Translator.tr("Çözüm Adımlarını Göster / Gizle")}</summary>")
        html.append("<div class=\"cas-steps-body\">$stepsHtml</div>")
        html.append("</details>")
    }
    if (aiEnabled) {
        val encodedEq = encodeForAskAI(rawInput)
        html.append("<details class=\"ai-details\" style=\"margin-top: 15px;\">")
        html.append("<summary onclick=\"if(this.dataset.loaded != 'true') { Android.askAI('$encodedEq'); this.dataset.loaded='true'; }\" style=\"cursor: pointer; list-style: none; display: flex; align-items: center;\">")
        html.append("<hr style=\"flex: 1; border: 0; border-top: 1px dashed var(--ai-color); opacity: 0.5;\"/>")
        html.append("<div style=\"padding: 0 15px; color: var(--ai-color); font-weight: bold; font-size: 0.95em;\">${Translator.tr("AI İle Çözüm Adımları")}</div>")
        html.append("<hr style=\"flex: 1; border: 0; border-top: 1px dashed var(--ai-color); opacity: 0.5;\"/>")
        html.append("</summary>")
        html.append("<div class=\"ai-steps-content\" style=\"padding-top: 15px;\">")
        html.append("</div></details>")
    }
    return html.toString()
}

/**
 * Universally extracts target expression and variable from any derivative representation:
 * D[expr, x], (d)/(dx)(expr), \frac{d}{dx}(expr), diff(expr, x), diff(expr), turev(expr), etc.
 */
fun extractDerivativeTarget(input: String): Pair<String, String>? {
    val norm = SymjaEngine.normalizeInputText(input)
    if (norm.startsWith("D[") && norm.endsWith("]")) {
        val inner = norm.substring(2, norm.length - 1)
        val args = SymjaEngine.splitTopLevel(inner, ',')
        if (args.isNotEmpty()) {
            val expr = args[0].trim()
            val wrt = if (args.size >= 2) args[1].trim().removeSurrounding("{", "}").substringBefore(",").trim() else SymjaEngine.deduceVar(expr)
            return Pair(expr, wrt)
        }
    }
    val m = Regex("""^(?:d/d([a-zA-Z])\s*\((.+)\)|(?:diff|derivative|turev|türev)\s*\(\s*(.+?)(?:\s*,\s*([a-zA-Z]))?\s*\))$""", RegexOption.IGNORE_CASE).find(input.trim())
    if (m != null) {
        val wrt = m.groupValues[1].ifEmpty { m.groupValues[4] }.ifEmpty { "x" }
        val expr = m.groupValues[2].ifEmpty { m.groupValues[3] }.trim()
        return Pair(expr, wrt)
    }
    return null
}

/**
 * Generates AST-based differentiation rule explanations for derivatives,
 * while delegating equations, inequalities, integrals, limits, and ODEs to SymjaEngine's
 * universal symbolic AST pipeline so steps and results are always 100% consistent.
 */
fun generateAnalyticalSteps(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null

    try {
        val target = extractDerivativeTarget(trimmed)
        if (target != null) {
            val (exprStr, wrt) = target
            val sym = parseSymExpr(exprStr)
            val ruleSteps = explainDiffSteps(sym, wrt)
            val finalDeriv = sym.diff(wrt).simplify()
            val sb = StringBuilder()
            sb.append("<b>${Translator.tr("1. Türevi Alınacak İfade:")}</b><br/>")
            sb.append("$$ \\frac{d}{d$wrt}\\left(${sym.toLatex()}\\right) $$<br/>")
            if (ruleSteps.isNotEmpty()) {
                sb.append("<b>${Translator.tr("2. Uygulanan Türev Kuralları:")}</b><br/>")
                ruleSteps.take(6).forEach { step ->
                    sb.append("• $step<br/>")
                }
            }
            sb.append("<b>${Translator.tr("3. Sadeleştirilmiş Türev:")}</b><br/>")
            sb.append("$$ ${finalDeriv.toLatex()} $$")
            return sb.toString()
        }
    } catch (_: Exception) {
        // Ignore and let symbolic engine proceed smoothly
    }
    return null
}

/**
 * Handles CasMemory matrix operations and expands saved matrix variables so both local steps
 * and Symja symbolic evaluation work seamlessly together.
 */
private fun handleOrExpandMatrixOperations(input: String, aiEnabled: Boolean): Pair<String, String?> {
    val trimmed = input.trim()

    // 1. Matrix Assignment: A = [[1, 2], [3, 4]]
    if (trimmed.contains("=") && !trimmed.contains("==") && (trimmed.contains("[[") || trimmed.contains("{{"))) {
        val parts = trimmed.split("=", limit = 2)
        val varName = parts[0].trim()
        val matStr = parts[1].trim()
        if (varName.matches(Regex("""^[a-zA-Z][a-zA-Z0-9_]*$"""))) {
            val matrix = CasMemory.parseMatrix(matStr)
            CasMemory.matrices[varName] = matrix
            val html = formatUnifiedDirectHtml(
                title = "${Translator.tr("Matris Hafızaya Kaydedildi")} ($varName):",
                resultLatex = "$varName = ${CasMemory.toLatex(matrix)}",
                stepsHtml = null,
                rawInput = input,
                aiEnabled = aiEnabled
            )
            return Pair(trimmed, html)
        }
    }

    // 2. Replace saved matrix variables in expressions like det(A), inv(A), eigenvals(A), transpose(A), A^(-1), A^T
    val cleanNoSpace = trimmed.replace(" ", "")

    // Check A^(-1) or inv(A) / inverse(A)
    val invVarMatch = Regex("""^([a-zA-Z0-9_]+)\^(\(-1\)|\{-1\})$""").find(cleanNoSpace)
    if (invVarMatch != null || cleanNoSpace.startsWith("inv(", ignoreCase = true) || cleanNoSpace.startsWith("inverse(", ignoreCase = true)) {
        val content = if (invVarMatch != null) {
            invVarMatch.groupValues[1]
        } else {
            cleanNoSpace.substring(cleanNoSpace.indexOf('(') + 1, cleanNoSpace.lastIndexOf(')'))
        }
        val matrix = if (content.contains("[[") || content.contains("{{")) {
            CasMemory.parseMatrix(content)
        } else {
            CasMemory.matrices[content]
        }
        if (matrix != null) {
            return try {
                val det = CasMemory.determinant(matrix)
                val inv = CasMemory.inverse(matrix)
                val steps = StringBuilder()
                steps.append("<b>${Translator.tr("1. Matris Determinantı:")}</b><br/>$$ \\det = ${fmtNum(det)} \\neq 0 $$<br/>")
                steps.append("<b>${Translator.tr("2. Ek Matris (Adjoint) / Determinant ile Ters Matris:")}</b><br/>$$ ${CasMemory.toLatex(inv)} $$")
                Pair("inv(${CasMemory.toBracketString(matrix)})", formatUnifiedDirectHtml(
                    title = Translator.tr("Matris Sonucu:"),
                    resultLatex = CasMemory.toLatex(inv),
                    stepsHtml = steps.toString(),
                    rawInput = input,
                    aiEnabled = aiEnabled
                ))
            } catch (_: Exception) {
                Pair("inv(${CasMemory.toBracketString(matrix)})", null)
            }
        }
    }

    // Check A^T or T(A) or transpose(A)
    val transVarMatch = Regex("""^([a-zA-Z0-9_]+)\^T$""").find(cleanNoSpace)
    if (transVarMatch != null || cleanNoSpace.startsWith("T(") || cleanNoSpace.startsWith("transpose(", ignoreCase = true)) {
        val content = if (transVarMatch != null) {
            transVarMatch.groupValues[1]
        } else {
            cleanNoSpace.substring(cleanNoSpace.indexOf('(') + 1, cleanNoSpace.lastIndexOf(')'))
        }
        val matrix = if (content.contains("[[") || content.contains("{{")) {
            CasMemory.parseMatrix(content)
        } else {
            CasMemory.matrices[content]
        }
        if (matrix != null) {
            val trans = CasMemory.transpose(matrix)
            return Pair("transpose(${CasMemory.toBracketString(matrix)})", formatUnifiedDirectHtml(
                title = Translator.tr("Matris Sonucu:"),
                resultLatex = CasMemory.toLatex(trans),
                stepsHtml = null,
                rawInput = input,
                aiEnabled = aiEnabled
            ))
        }
    }

    // Check det(A) or det([[...]])
    if (cleanNoSpace.startsWith("det(", ignoreCase = true)) {
        val content = cleanNoSpace.substring(4, cleanNoSpace.lastIndexOf(')'))
        val matrix = if (content.contains("[[") || content.contains("{{")) {
            CasMemory.parseMatrix(content)
        } else {
            CasMemory.matrices[content]
        }
        if (matrix != null) {
            return try {
                val det = CasMemory.determinant(matrix)
                val steps = StringBuilder()
                steps.append("<b>${Translator.tr("1. Kare Matris")} (${matrix.size}×${matrix.size}):</b><br/>$$ ${CasMemory.toLatex(matrix)} $$<br/>")
                if (matrix.size == 2) {
                    steps.append("<b>" + Translator.tr("2. 2×2 Determinant Kuralı ($ ad - bc $):") + "</b><br/>")
                    steps.append("$$ (${fmtNum(matrix[0][0])})(${fmtNum(matrix[1][1])}) - (${fmtNum(matrix[0][1])})(${fmtNum(matrix[1][0])}) = ${fmtNum(det)} $$")
                } else {
                    steps.append("<b>${Translator.tr("2. Sarrus / Kofaktör Açılımı Sonucu:")}</b><br/>$$ \\det = ${fmtNum(det)} $$")
                }
                Pair("det(${CasMemory.toBracketString(matrix)})", formatUnifiedDirectHtml(
                    title = Translator.tr("Matris Sonucu:"),
                    resultLatex = fmtNum(det),
                    stepsHtml = steps.toString(),
                    rawInput = input,
                    aiEnabled = aiEnabled
                ))
            } catch (_: Exception) {
                Pair("det(${CasMemory.toBracketString(matrix)})", null)
            }
        }
    }

    // Check eigenvals(A) / eigenvalues(A)
    if (cleanNoSpace.startsWith("eigenvals(", ignoreCase = true) || cleanNoSpace.startsWith("eigenvalues(", ignoreCase = true)) {
        val content = cleanNoSpace.substring(cleanNoSpace.indexOf('(') + 1, cleanNoSpace.lastIndexOf(')'))
        val matrix = if (content.contains("[[") || content.contains("{{")) {
            try { CasMemory.parseMatrix(content) } catch (_: Exception) { null }
        } else {
            CasMemory.matrices[content]
        }
        if (matrix != null && matrix.size == 2 && matrix[0].size == 2) {
            val a = matrix[0][0]; val b = matrix[0][1]
            val c = matrix[1][0]; val d = matrix[1][1]
            val tr = a + d
            val det = a * d - b * c
            val delta = tr * tr - 4 * det
            val steps = StringBuilder()
            steps.append("<b>${Translator.tr("1. Karakteristik Denklem ($\\det(A - \\lambda I) = 0$):")}</b><br/>")
            steps.append("$$ \\lambda^2 - (${fmtNum(tr)})\\lambda + (${fmtNum(det)}) = 0, \\quad \\Delta = ${fmtNum(delta)} $$<br/>")
            if (delta >= 0) {
                val l1 = (tr - sqrt(delta)) / 2.0
                val l2 = (tr + sqrt(delta)) / 2.0
                steps.append("<b>${Translator.tr("2. Özdeğerler (Reel ve Farklı):")}</b><br/>")
                steps.append("$$ \\lambda_1 = ${fmtNum(l1)}, \\quad \\lambda_2 = ${fmtNum(l2)} $$")
            }
            return Pair("eigenvalues(${CasMemory.toBracketString(matrix)})", SymjaEngine.translateAndEvaluate(
                "eigenvalues(${CasMemory.toBracketString(matrix)})",
                aiEnabled,
                steps.toString()
            ))
        }
    }

    // Expand any standalone stored matrix variable name in function calls
    var expanded = trimmed
    for ((name, mat) in CasMemory.matrices) {
        expanded = expanded.replace(Regex("""\b$name\b"""), CasMemory.toBracketString(mat))
    }
    return Pair(expanded, null)
}

/**
 * Unified CAS Solver Entry Point:
 * Evaluates symbolically via SymjaEngine while enriching with local analytical step-by-step
 * explanations and optional AI teacher breakdowns in a single cohesive UI format.
 */
fun solveCas(input: String, aiEnabled: Boolean = true): String {
    if (input.isBlank()) return Translator.tr("Denklem veya ifade girin.")

    val ignoredHeaders = setOf("text", "math", "plaintext", "copy", "latex", "cas", "kod", "code")
    val lines = input.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it.lowercase() !in ignoredHeaders }
    if (lines.size > 1) {
        val sb = StringBuilder()
        for ((idx, line) in lines.withIndex()) {
            sb.append(solveCas(line, aiEnabled && idx == lines.lastIndex))
            if (idx < lines.lastIndex) {
                sb.append("<hr style=\"border:0; border-top:1px dashed rgba(128,128,128,0.4); margin:14px 0;\"/>")
            }
        }
        return sb.toString()
    }

    // Strip leading function labels like "f1(x):", "f(x) =", "y =" when not an ODE
    var trimmed = (lines.firstOrNull() ?: input).trim()
    trimmed = SymjaEngine.normalizeInputText(trimmed)
    trimmed = trimmed.replace(Regex("""^[fFgGhH]_?\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]+\s*"""), "")
    if (!trimmed.contains("'") && !trimmed.contains("′") && !trimmed.contains("’")) {
        trimmed = trimmed.replace(Regex("""^y\s*(?:\(\s*x\s*\))?\s*[:=]+\s*"""), "")
    }

    val cleanCheck = trimmed.replace("$", "").replace("`", "").trim()
    if (cleanCheck in listOf("^", "_", "+", "-", "*", "/", "=", "%") || cleanCheck == "$") {
        return "<div class=\"cas-info\" style=\"padding: 10px; opacity: 0.75; text-align: center;\"><b>" + Translator.tr("Lütfen bir sayı veya değişken girin.") + "</b></div>"
    }
    if (cleanCheck.endsWith("^") || cleanCheck.endsWith("_")) {
        return "<div class=\"cas-info\" style=\"padding: 10px; opacity: 0.75; text-align: center;\"><b>" + Translator.tr("Lütfen üs veya indis değerini tamamlayın. (Örn: x^2)") + "</b></div>"
    }
    if (Regex("""[+\-*/=,]$""").containsMatchIn(cleanCheck)) {
        return "<div class=\"cas-info\" style=\"padding: 10px; opacity: 0.75; text-align: center;\"><b>" + Translator.tr("İfade işlem sembolü ile bitemez. Lütfen terimi tamamlayın.") + "</b></div>"
    }

    // 1. Check Matrix memory & operations
    try {
        val (expandedInput, directMatrixHtml) = handleOrExpandMatrixOperations(trimmed, aiEnabled)
        if (directMatrixHtml != null) {
            return directMatrixHtml
        }

        // 2. Generate AST-based derivative rule breakdown when applicable
        val localStepsHtml = generateAnalyticalSteps(expandedInput)

        // 3. Evaluate through unified symbolic engine with integrated universal steps & AI toggle
        return SymjaEngine.translateAndEvaluate(expandedInput, aiEnabled, localStepsHtml)
    } catch (e: Throwable) {
        val localStepsHtml = generateAnalyticalSteps(trimmed)
        return SymjaEngine.translateAndEvaluate(trimmed, aiEnabled, localStepsHtml)
    }
}
