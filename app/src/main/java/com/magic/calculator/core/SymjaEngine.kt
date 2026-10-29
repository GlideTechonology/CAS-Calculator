package com.magic.calculator.core

import com.magic.calculator.Translator
import org.matheclipse.core.eval.ExprEvaluator

object SymjaEngine {
    private val evaluator = ExprEvaluator()
    
    fun escapeLatexForHtml(tex: String): String {
        return tex
            .replace("<=", "\\le ")
            .replace(">=", "\\ge ")
            .replace("<", "\\lt ")
            .replace(">", "\\gt ")
    }

    fun evaluate(input: String, aiEnabled: Boolean = true): String {
        try {
            val result = evaluator.eval(input)
            val texResult = evaluator.eval("TeXForm($result)")
            var texStr = texResult.toString().removeSurrounding("\"", "\"")
            texStr = replaceHoldFactorialWithScientific(texStr)
            val safeTexStr = escapeLatexForHtml(texStr)
            
            val steps = java.lang.StringBuilder()
            steps.append("<div class='cas-header'><b>${Translator.tr("Sonuç:")}</b></div>")
            steps.append("<div class='cas-result-box'>$$ $safeTexStr $$</div>")
            return steps.toString()
        } catch (e: Exception) {
            return Translator.tr("Sözdizimi hatası: İfade eksik veya geçersiz bir sembol içeriyor.")
        }
    }
    
    fun findMatchingBrace(s: String, startIdx: Int, openCh: Char = '{', closeCh: Char = '}'): Int {
        var depth = 0
        for (i in startIdx until s.length) {
            if (s[i] == openCh) depth++
            else if (s[i] == closeCh) {
                depth--
                if (depth == 0) return i
            }
        }
        return -1
    }

    fun splitTopLevel(s: String, delimiter: Char = ','): List<String> {
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

    private fun parseSumRangeSpec(rng: String): Triple<String, String, String>? {
        val r = rng.trim()
        val m = Regex("""^([a-zA-Z]+)\s*=\s*(.+?)(?:\s*\*?\s*(?:to|\.\.)\s*\*?\s*|\s*,\s*)(.+)$""", RegexOption.IGNORE_CASE).find(r)
        if (m != null) {
            return Triple(m.groupValues[1].trim(), m.groupValues[2].trim(), m.groupValues[3].trim())
        }
        val m2 = Regex("""^([a-zA-Z]+)\s*,\s*([^,]+)\s*,\s*([^,]+)$""").find(r)
        if (m2 != null) {
            return Triple(m2.groupValues[1].trim(), m2.groupValues[2].trim(), m2.groupValues[3].trim())
        }
        return null
    }

    private fun parseIntegralRangeSpec(rng: String): Triple<String?, String, String>? {
        var r = rng.trim()
        if (r.startsWith("from ", ignoreCase = true)) {
            r = r.substring(5).trim()
        }
        val mVar = Regex("""^([a-zA-Z]+)\s*=\s*(.+?)(?:\s*\*?\s*(?:to|\.\.|->)\s*\*?\s*|\s*,\s*)(.+)$""", RegexOption.IGNORE_CASE).find(r)
        if (mVar != null) {
            return Triple(mVar.groupValues[1].trim(), mVar.groupValues[2].trim(), mVar.groupValues[3].trim())
        }
        val mTo = Regex("""^(.+?)(?:\s*\*?\s*(?:to|\.\.|->)\s*\*?\s*)(.+)$""", RegexOption.IGNORE_CASE).find(r)
        if (mTo != null) {
            return Triple(null, mTo.groupValues[1].trim(), mTo.groupValues[2].trim())
        }
        val m3 = Regex("""^([a-zA-Z]+)\s*,\s*([^,]+)\s*,\s*([^,]+)$""").find(r)
        if (m3 != null) {
            return Triple(m3.groupValues[1].trim(), m3.groupValues[2].trim(), m3.groupValues[3].trim())
        }
        val m2 = Regex("""^([^,]+)\s*,\s*([^,]+)$""").find(r)
        if (m2 != null) {
            return Triple(null, m2.groupValues[1].trim(), m2.groupValues[2].trim())
        }
        return null
    }

    private fun combineLogarithms(str: String): String {
        var s = str.replace(Regex("""(?i)\b(ln|log)\s*\("""), "Log(")
        var changed = true
        while (changed) {
            changed = false
            val matches = mutableListOf<Triple<Int, Int, String>>()
            var idx = 0
            while (idx < s.length) {
                val mIdx = s.indexOf("Log(", idx)
                if (mIdx == -1) break
                val openIdx = mIdx + 3
                val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
                if (closeIdx != -1) {
                    val arg = s.substring(openIdx + 1, closeIdx).trim()
                    matches.add(Triple(mIdx, closeIdx, arg))
                    idx = closeIdx + 1
                } else {
                    idx = mIdx + 4
                }
            }

            if (matches.size >= 2) {
                for (i in 0 until matches.size - 1) {
                    val (start1, end1, arg1) = matches[i]
                    val (start2, end2, arg2) = matches[i + 1]
                    val between = s.substring(end1 + 1, start2).trim()
                    if (between == "+") {
                        val combined = "Log(($arg1)*($arg2))"
                        s = s.substring(0, start1) + combined + s.substring(end2 + 1)
                        changed = true
                        break
                    } else if (between == "-") {
                        val combined = "Log(($arg1)/($arg2))"
                        s = s.substring(0, start1) + combined + s.substring(end2 + 1)
                        changed = true
                        break
                    }
                }
            }
        }

        val eqLogMatch = Regex("""^\s*Log\((.+)\)\s*(?:==|=)\s*Log\((.+)\)\s*$""").find(s)
        if (eqLogMatch != null) {
            return "(${eqLogMatch.groupValues[1]}) == (${eqLogMatch.groupValues[2]})"
        }
        val eqConstMatch = Regex("""^\s*Log\((.+)\)\s*(?:==|=)\s*(.+)$""").find(s)
        if (eqConstMatch != null) {
            val lhs = eqConstMatch.groupValues[1].trim()
            val rhs = eqConstMatch.groupValues[2].trim()
            return if (rhs == "0") {
                "($lhs) == 1"
            } else {
                "($lhs) == Exp($rhs)"
            }
        }

        return s
    }

    private fun extractTrailingDifferentials(expr: String): Pair<String, List<String>> {
        val diffs = mutableListOf<String>()
        var curr = expr.trim()
        val regex = Regex("""(?:[\s*]+|\))d([a-zA-Z]|theta)\s*$""", RegexOption.IGNORE_CASE)
        while (true) {
            val m = regex.find(curr) ?: break
            val v = m.groupValues[1]
            diffs.add(v)
            val start = m.range.first
            curr = if (curr[start] == ')') {
                curr.substring(0, start + 1).trim()
            } else {
                curr.substring(0, start).trim()
            }
        }
        diffs.reverse()
        return Pair(curr, diffs)
    }

    private fun parseMultipleIntegrals(s: String): String? {
        var curr = s.trim()
        val prefixes = mutableListOf<Triple<String?, String, String>>()

        while (true) {
            curr = curr.trim()
            val mLat = Regex("""^(?:int|integral)\s*(?:_\{([^}]+)\}|_\(([^)]+)\)|_([^\s^]+))\s*(?:\^\{([^}]+)\}|\^\(([^)]+)\)|\^([^\s]+))\s*\*?""", RegexOption.IGNORE_CASE).find(curr)
            if (mLat != null) {
                val lower = mLat.groupValues[1].ifEmpty { mLat.groupValues[2] }.ifEmpty { mLat.groupValues[3] }.trim()
                val upper = mLat.groupValues[4].ifEmpty { mLat.groupValues[5] }.ifEmpty { mLat.groupValues[6] }.trim()
                prefixes.add(Triple(null, lower, upper))
                curr = curr.substring(mLat.range.last + 1).trim().removePrefix("*").trim()
                continue
            }

            val mParen = Regex("""^(?:int|integral)\s*\(""", RegexOption.IGNORE_CASE).find(curr)
            if (mParen != null) {
                val openIdx = mParen.range.last
                val closeIdx = findMatchingBrace(curr, openIdx, '(', ')')
                if (closeIdx != -1) {
                    val inside = curr.substring(openIdx + 1, closeIdx).trim()
                    val spec = parseIntegralRangeSpec(inside)
                    if (spec != null) {
                        prefixes.add(spec)
                        curr = curr.substring(closeIdx + 1).trim().removePrefix("*").trim()
                        continue
                    }
                }
            }

            val mPlain = Regex("""^(?:int|integral)\s+(?:from\s+)?([a-zA-Z]+\s*=\s*[^,\s]+|[^,\s]+)\s+(?:to|\.\.|->)\s+([^\s]+)\s*\*?""", RegexOption.IGNORE_CASE).find(curr)
            if (mPlain != null) {
                val rawLower = mPlain.groupValues[1].trim()
                val upper = mPlain.groupValues[2].trim()
                var specVar: String? = null
                var lower = rawLower
                if (rawLower.contains("=")) {
                    val parts = rawLower.split("=", limit = 2)
                    specVar = parts[0].trim()
                    lower = parts[1].trim()
                }
                prefixes.add(Triple(specVar, lower, upper))
                curr = curr.substring(mPlain.range.last + 1).trim().removePrefix("*").trim()
                continue
            }

            break
        }

        if (prefixes.size >= 2) {
            val (cleanBody, diffs) = extractTrailingDifferentials(curr)
            val assigned = mutableListOf<Triple<String, String, String>>()
            for (i in prefixes.indices) {
                val (pVar, lower, upper) = prefixes[i]
                val varName = when {
                    pVar != null -> pVar
                    i < diffs.size -> diffs[i]
                    i == 0 -> "x"
                    i == 1 -> "y"
                    else -> "z"
                }
                assigned.add(Triple(varName, lower, upper))
            }

            var res = cleanBody
            for (i in assigned.indices.reversed()) {
                val (varName, lower, upper) = assigned[i]
                res = "Integrate[$res, {$varName, $lower, $upper}]"
            }
            return res
        }

        return null
    }

    private fun replaceFrac(str: String): String {
        var s = str
        while (s.contains(Regex("""\\frac\b"""))) {
            val match = Regex("""\\frac\b""").find(s) ?: break
            var i = match.range.last + 1
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length) break
            
            val num: String
            if (s[i] == '{') {
                val close = findMatchingBrace(s, i, '{', '}')
                if (close == -1) break
                num = s.substring(i + 1, close)
                i = close + 1
            } else {
                num = s[i].toString()
                i++
            }
            while (i < s.length && s[i].isWhitespace()) i++
            if (i >= s.length) break
            
            val den: String
            val endIdx: Int
            if (s[i] == '{') {
                val close = findMatchingBrace(s, i, '{', '}')
                if (close == -1) break
                den = s.substring(i + 1, close)
                endIdx = close + 1
            } else {
                den = s[i].toString()
                endIdx = i + 1
            }
            s = s.substring(0, match.range.first) + "(($num)/($den))" + s.substring(endIdx)
        }
        return s
    }

    private fun replaceSqrt(str: String): String {
        var s = str
        while (s.contains(Regex("""\\sqrt\b"""))) {
            val match = Regex("""\\sqrt\b""").find(s) ?: break
            var i = match.range.last + 1
            while (i < s.length && s[i].isWhitespace()) i++
            if (i < s.length && s[i] == '{') {
                val close = findMatchingBrace(s, i, '{', '}')
                if (close == -1) break
                val inner = s.substring(i + 1, close)
                s = s.substring(0, match.range.first) + "Sqrt[$inner]" + s.substring(close + 1)
            } else {
                break
            }
        }
        return s
    }

    fun deduceVar(expr: String): String {
        val matches = Regex("""\b([a-zA-Z])\b""").findAll(expr)
            .map { it.groupValues[1] }
            .filter { it !in listOf("E", "I", "Pi", "Degree", "e", "pi", "C", "d", "D") }
            .toList()
        if ("x" in matches) return "x"
        if ("t" in matches) return "t"
        if ("y" in matches) return "y"
        if (matches.isNotEmpty()) return matches.first()
        for (ch in expr) {
            if (ch in "xyztnuvwXYZTNUVW") return ch.toString()
        }
        return "x"
    }

    private fun extractTrailingDifferential(expr: String): Pair<String, String?> {
        val regex = Regex("""(?:[\s*]+|\))d([a-zA-Z]|theta)\s*$""", RegexOption.IGNORE_CASE)
        val match = regex.find(expr)
        if (match != null) {
            val varName = match.groupValues[1]
            val matchStart = match.range.first
            val cleanExpr = if (expr[matchStart] == ')') {
                expr.substring(0, matchStart + 1).trim()
            } else {
                expr.substring(0, matchStart).trim()
            }
            return Pair(cleanExpr, varName)
        }
        return Pair(expr.trim(), null)
    }

    private fun findCommonBasePower(a: Long, b: Long): Pair<Int, Int>? {
        if (a <= 1L || b <= 1L) return null
        if (a == b) return Pair(1, 1)
        val maxVal = maxOf(a, b)
        val limit = (kotlin.math.sqrt(maxVal.toDouble()).toLong() + 2L)
        val candidates = (2L..limit).toMutableList()
        val minVal = minOf(a, b)
        if (!candidates.contains(minVal)) {
            candidates.add(minVal)
        }
        for (c in candidates) {
            var p = 0
            var tempA = a
            while (tempA > 1L && tempA % c == 0L) {
                tempA /= c
                p++
            }
            if (tempA != 1L || p == 0) continue
            var q = 0
            var tempB = b
            while (tempB > 1L && tempB % c == 0L) {
                tempB /= c
                q++
            }
            if (tempB != 1L || q == 0) continue
            val g = gcd(p, q)
            return Pair(p / g, q / g)
        }
        return null
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return if (x < 0) -x else x
    }

    private fun simplifySingleExponentialEquation(eqStr: String): String {
        val eq = eqStr.trim()
        if (!eq.contains("==")) return eq
        val parts = eq.split("==")
        if (parts.size != 2) return eq
        val lhs = parts[0].trim()
        val rhs = parts[1].trim()

        val patExp = Regex("""^([\d.]+|[eE])\^(\([^\)]+\)|[a-zA-Z0-9_]+)$""")
        val patNum = Regex("""^([\d.]+)$""")

        val mLhs = patExp.matchEntire(lhs)
        val mRhsExp = patExp.matchEntire(rhs)
        val mRhsNum = patNum.matchEntire(rhs)

        if (mLhs != null && mRhsExp != null) {
            val baseAStr = mLhs.groupValues[1]
            var expU = mLhs.groupValues[2]
            if (expU.startsWith("(") && expU.endsWith(")")) expU = expU.substring(1, expU.length - 1)
            val baseBStr = mRhsExp.groupValues[1]
            var expV = mRhsExp.groupValues[2]
            if (expV.startsWith("(") && expV.endsWith(")")) expV = expV.substring(1, expV.length - 1)

            if (baseAStr.equals("E", ignoreCase = true) && baseBStr.equals("E", ignoreCase = true)) {
                return "$expU == $expV"
            }

            val a = baseAStr.toLongOrNull()
            val b = baseBStr.toLongOrNull()
            if (a != null && b != null && a > 1L && b > 1L) {
                val common = findCommonBasePower(a, b)
                if (common != null) {
                    val (p, q) = common
                    val leftTerm = if (p != 1) "$p*($expU)" else expU
                    val rightTerm = if (q != 1) "$q*($expV)" else expV
                    return "$leftTerm == $rightTerm"
                } else {
                    return "($expU)*Log[$a] == ($expV)*Log[$b]"
                }
            }
        } else if (mLhs != null && mRhsNum != null) {
            val baseAStr = mLhs.groupValues[1]
            var expU = mLhs.groupValues[2]
            if (expU.startsWith("(") && expU.endsWith(")")) expU = expU.substring(1, expU.length - 1)
            val bNumStr = mRhsNum.groupValues[1]

            if (baseAStr.equals("E", ignoreCase = true)) {
                return "$expU == Log[$bNumStr]"
            }

            val a = baseAStr.toLongOrNull()
            val b = bNumStr.toLongOrNull()
            if (a != null && b != null && a > 1L && b > 1L) {
                val common = findCommonBasePower(a, b)
                if (common != null) {
                    val (p, q) = common
                    return if (p == 1) "$expU == $q" else "$p*($expU) == $q"
                } else {
                    return "($expU)*Log[$a] == Log[$b]"
                }
            }
        }

        val mLhsNum = patNum.matchEntire(lhs)
        if (mLhsNum != null && mRhsExp != null) {
            return simplifySingleExponentialEquation("$rhs == $lhs")
        }

        return eq
    }

    fun preprocessExponentialEquations(s: String): String {
        val mSolve = Regex("""^(Solve|Reduce)\[\{(.*?)\}(.*)\]$""", RegexOption.DOT_MATCHES_ALL).matchEntire(s)
        if (mSolve != null) {
            val cmd = mSolve.groupValues[1]
            val innerEqs = mSolve.groupValues[2]
            val rest = mSolve.groupValues[3]
            val parts = splitTopLevel(innerEqs, ',')
            val newParts = parts.map { simplifySingleExponentialEquation(it) }
            return "$cmd[{${newParts.joinToString(", ")}}$rest]"
        }

        val parts = splitTopLevel(s, ',')
        if (parts.size > 1 && parts.any { it.contains("==") }) {
            val newParts = parts.map { part ->
                val subparts = part.split(Regex("""\s*(?:&&|\b(?:and|ve|und|et)\b)\s*""", RegexOption.IGNORE_CASE))
                subparts.map { simplifySingleExponentialEquation(it) }.joinToString(" && ")
            }
            return newParts.joinToString(", ")
        } else if (s.contains("==")) {
            return simplifySingleExponentialEquation(s)
        }
        return s
    }

    private fun convertListBrackets(input: String): String {
        var pre = input
            .replace(Regex("""(?i)\b(eigenvectors?|eigenvals?|eigenvalues?|eigensystem|det|inv|inverse|transpose|tr|ozvektor|özvektör|ozdeger|özdeğer)\s*\[\s*\["""), "$1[{{")
        if (pre != input && pre.endsWith("]]")) {
            pre = pre.substring(0, pre.length - 2) + "}}]"
        }
        val sb = java.lang.StringBuilder(pre)
        var i = 0
        while (i < sb.length) {
            if (sb[i] == '[') {
                val isFunc = i > 0 && (sb[i - 1].isLetterOrDigit() || sb[i - 1] == '_')
                if (!isFunc) {
                    var depth = 1
                    var j = i + 1
                    while (j < sb.length && depth > 0) {
                        if (sb[j] == '[') depth++
                        else if (sb[j] == ']') {
                            depth--
                            if (depth == 0) break
                        }
                        j++
                    }
                    if (j < sb.length && depth == 0) {
                        sb.setCharAt(i, '{')
                        sb.setCharAt(j, '}')
                    }
                }
            }
            i++
        }
        return sb.toString()
    }

    fun translateHumanCalculus(input: String): String {
        var s = normalizeInputText(input)
        if (s.startsWith("D[") && s.endsWith("]")) {
            return s
        }
        
        // 0. LaTeX & Symbol cleanups
        s = s.replace(Regex("""\\+\s*$"""), "").trim()
        s = s.replace(Regex("""\\(?=[\s,;+*/^=()]|$)"""), " ")
        s = s.replace("""\left""", "").replace("""\right""", "")
        s = s.replace("""\,""", " ").replace("""\;""", " ").replace("""\!""", "").replace("""\quad""", " ").replace("""\qquad""", " ")
        s = s.replace("""\lim""", "lim")
        s = s.replace("""\cdot""", "*").replace("""\times""", "*")
        s = s.replace("""\infty""", "Infinity").replace("∞", "Infinity")
        s = s.replace(Regex("""(?i)\b(infinity|inf)\b"""), "Infinity")
        s = s.replace("""\to""", "->").replace("""\rightarrow""", "->").replace("""\longrightarrow""", "->").replace("→", "->")
        s = s.replace("""\pi""", "Pi").replace("π", "Pi")
        s = s.replace("""\theta""", "theta").replace("θ", "theta")
        s = s.replace("""\partial""", "d").replace("∂", "d")
        s = s.replace(Regex("""\^\{([^}]+)\}"""), "^($1)")
        s = s.replace("""\operatorname""", "")
        s = s.replace("°", "*Degree")
        s = s.replace("√", "Sqrt")

        // Natural language arbitrary precision: e.g. "pi 50 basamak", "pi 50. basamak", "pi'nin 50 basamağı", "50 digits of pi"
        val piDigitsPat = Regex("""(?i)\b(?:(pi|e)(?:\'nin|\'un|\'in)?\s*(\d+)\s*\.?\s*(?:basama[kğ][ıi]?|digits?|places?)|(\d+)\s*\.?\s*(?:basama[kğ][ıi]?|digits?|places?)\s*(?:of|kadar)?\s*(pi|e))\b""")
        val mPiDigits = piDigitsPat.find(s)
        if (mPiDigits != null) {
            val constName = mPiDigits.groupValues[1].ifEmpty { mPiDigits.groupValues[4] }.replace("pi", "Pi", ignoreCase = true).replace("e", "E")
            val digits = mPiDigits.groupValues[2].ifEmpty { mPiDigits.groupValues[3] }
            return "N[$constName, $digits]"
        }
        
        // Inequalities LaTeX & Unicode
        s = s.replace("""\leqslant""", "<=").replace("""\leq""", "<=").replace("""\le""", "<=")
        s = s.replace("""\geqslant""", ">=").replace("""\geq""", ">=").replace("""\ge""", ">=")
        s = s.replace("""\neq""", "!=").replace("""\ne""", "!=")
        s = s.replace("≤", "<=").replace("≥", ">=").replace("≠", "!=")

        // Floor & Ceiling LaTeX & Unicode
        s = s.replace("""\lfloor""", "Floor(").replace("""\rfloor""", ")")
        s = s.replace("""\lceil""", "Ceiling(").replace("""\rceil""", ")")
        s = s.replace("⌊", "Floor(").replace("⌋", ")")
        s = s.replace("⌈", "Ceiling(").replace("⌉", ")")
        
        // Logical Connectives (Multilingual: EN, TR, DE, FR, ES, RU, AR)
        s = s.replace(Regex("""(?i)\b(and|ve|und|et|и)\b|(?<=\s)و(?=\s)"""), "&&")
        s = s.replace(Regex("""(?i)\b(or|veya|oder|ou|или)\b|(?<=\s)أو(?=\s)"""), "||")
        
        // Absolute Value |x|
        val absPat = Regex("""(?<!\|)\|([^|\n]+)\|(?!\|)""")
        while (absPat.containsMatchIn(s)) {
            s = absPat.replace(s, "Abs($1)")
        }
        
        // Diophantine & Integer keywords cleanup (Multilingual)
        s = s.replace(Regex("""(?i)\b(diophantine|diophant)\b"""), "")
             .replace(Regex("""(?i)\b(tam\s*say[ıi]lar(da|için|\s+çözüm(ü|leri)?)?|tam\s*say[ıi]|tamsay[ıi])(\s*(çözüm(ü|leri)?|cozum(u|leri)?|kök(ü|leri)?|kok(u|leri)?))?\b"""), "")
             .replace(Regex("""(?i)\b(over\s+integers?|in\s+integers?|integers?|enteros?|en\s+enteros?|ganze\s*zahlen?|in\s+ganzen\s*zahlen?|entiers?|dans\s+(les\s+)?entiers?|цел(ые|ых)\s*числ(а|ах)|в\s+целых\s*числах|الأعداد\s*الصحيحة|في\s+الأعداد\s*الصحيحة)\b"""), "")
             .replace(Regex("""\\in\s*\\mathbb\{[zZ]\}"""), "")
             .replace(Regex("""[∈\u2208]\s*[Zℤ]"""), "")
             .replace(Regex("""(?i)\bin\s+[zZ]\b"""), "")
             .replace(Regex("""(?i)\bfor\s+[a-zA-Z](\s*,\s*[a-zA-Z])*\b"""), "")
        
        // Strip backslashes from common function names: \sin -> sin, \cos -> cos, etc.
        s = s.replace(Regex("""\\(sin|cos|tan|cot|sec|csc|asin|acos|atan|arcsin|arccos|arctan|ln|log|exp|sqrt|floor|ceil|abs)\b""", RegexOption.IGNORE_CASE)) {
            it.groupValues[1]
        }
        
        // Convert sin^2(x) / sin^{2}(x) -> (sin(x))^2
        s = s.replace(Regex("""(?i)\b(sin|cos|tan|cot|sec|csc|ln|log)\^\{?(\d+)\}?\(([^)]+)\)""")) {
            "(${it.groupValues[1]}(${it.groupValues[3]}))^${it.groupValues[2]}"
        }
        
        // Check mixed partial derivatives with \frac: \frac{d^2}{dx dy} or \frac{\partial^2}{\partial x \partial y}
        val fracMixedMatch = Regex("""^\\frac\{[d∂]\^?(\d*)\}\{[d∂]\s*([a-zA-Z])\s*(?:\\?[,\s])*[d∂]?\s*([a-zA-Z])\}\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (fracMixedMatch != null) {
            return "D[${fracMixedMatch.groupValues[4].trim()}, ${fracMixedMatch.groupValues[2]}, ${fracMixedMatch.groupValues[3]}]"
        }

        // Check derivatives with \frac before frac expansion: \frac{d}{dx} or \frac{d^n}{dx^n}
        val fracOrderMatch = Regex("""^\\frac\{[d∂]\^(\d+)\}\{[d∂]\s*([a-zA-Z])\^\1\}\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (fracOrderMatch != null) {
            return "D[${fracOrderMatch.groupValues[3].trim()}, {${fracOrderMatch.groupValues[2]}, ${fracOrderMatch.groupValues[1]}}]"
        }
        val fracDMatch = Regex("""^\\frac\{[d∂]\}\{[d∂]\s*([a-zA-Z])\}\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (fracDMatch != null) {
            return "D[${fracDMatch.groupValues[2].trim()}, ${fracDMatch.groupValues[1]}]"
        }
        
        // Replace LaTeX \frac and \sqrt
        s = replaceFrac(s)
        s = replaceSqrt(s)
        s = denestRadicals(s)
        s = combineLogarithms(s)
        
        // Normalize calculus operators
        s = s.replace(Regex("""(?i)\b(integrate|integral|entegral|integre)\b"""), "int")
        s = s.replace(Regex("""(?i)\b(derivative|turev|türev)\b"""), "diff")
        s = s.replace("∫", "int ").replace("""\int""", "int ")
        s = s.replace("∑", "sum ").replace("""\sum""", "sum ")
        s = s.replace("∏", "prod ").replace("""\prod""", "prod ")
        s = s.replace("""\lim""", "lim")
        s = s.replace(Regex("""\s+"""), " ").trim()
        
        // 1. Derivatives:
        // Mixed partial derivatives: d^2/dxdy, d^2/(dx dy), d^2/(dxdy), with optional trailing *
        val dMixedMatch = Regex("""^[d∂]\^?(\d*)\s*/\s*\(?[d∂]\s*([a-zA-Z])\s*(?:\\?[,\s])*[d∂]?\s*([a-zA-Z])\)?\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (dMixedMatch != null) {
            return "D[${dMixedMatch.groupValues[4].trim()}, ${dMixedMatch.groupValues[2]}, ${dMixedMatch.groupValues[3]}]"
        }
        val dOrderMatch = Regex("""^[d∂]\^(\d+)/[d∂]\s*([a-zA-Z])\^\1\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (dOrderMatch != null) {
            return "D[${dOrderMatch.groupValues[3].trim()}, {${dOrderMatch.groupValues[2]}, ${dOrderMatch.groupValues[1]}}]"
        }
        val dMatch = Regex("""^[d∂]/[d∂]\s*([a-zA-Z])\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (dMatch != null) {
            return "D[${dMatch.groupValues[2].trim()}, ${dMatch.groupValues[1]}]"
        }
        val diffMatch = Regex("""^(?:diff|derivative|turev|türev)\s*\(\s*(.+?)(?:\s*,\s*([a-zA-Z]+)(?:\s*,\s*([a-zA-Z0-9]+))?)?\s*\)$""", RegexOption.IGNORE_CASE).find(s)
        if (diffMatch != null) {
            val expr = diffMatch.groupValues[1].trim()
            val var1 = diffMatch.groupValues[2].ifEmpty { deduceVar(expr) }.trim()
            val opt = diffMatch.groupValues[3].trim()
            return when {
                opt.isEmpty() -> "D[$expr, $var1]"
                opt.all { it.isDigit() } -> "D[$expr, {$var1, $opt}]"
                else -> "D[$expr, $var1, $opt]"
            }
        }
        
        // 2. Definite & Indefinite Integrals:
        // 2.0. Multiple Integrals (Double/Triple): int(0 to 1) int(0 to 1) ...
        val multiInt = parseMultipleIntegrals(s)
        if (multiInt != null) {
            return multiInt
        }

        // 2.1 LaTeX subscript/superscript definite integral: \int_{0}^{\infty} ... or int_0^\infty ...
        val defIntMatch = Regex("""^(?:int|integral)\s*(?:_\{([^}]+)\}|_\(([^)]+)\)|_([^\s^]+))\s*(?:\^\{([^}]+)\}|\^\(([^)]+)\)|\^([^\s]+))\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (defIntMatch != null) {
            val lower = defIntMatch.groupValues[1].ifEmpty { defIntMatch.groupValues[2] }.ifEmpty { defIntMatch.groupValues[3] }.trim()
            val upper = defIntMatch.groupValues[4].ifEmpty { defIntMatch.groupValues[5] }.ifEmpty { defIntMatch.groupValues[6] }.trim()
            val body = defIntMatch.groupValues[7].trim().removePrefix("*").trim()
            val (cleanExpr, trailingVar) = extractTrailingDifferential(body)
            val varName = trailingVar ?: deduceVar(cleanExpr)
            return "Integrate[$cleanExpr, {$varName, $lower, $upper}]"
        }

        // 2.2 Functional or parenthesized: int(0 to inf) e^(-x) dx or int(0 to inf, e^(-x) dx) or int(expr, x, a, b)
        val intStartMatch = Regex("""^(?:int|integral)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (intStartMatch != null) {
            val openIdx = intStartMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val outside = s.substring(closeIdx + 1).trim().removePrefix("*").trim()
                if (outside.isNotEmpty()) {
                    val spec = parseIntegralRangeSpec(inside)
                    if (spec != null) {
                        val (specVar, a, b) = spec
                        val (cleanExpr, trailingVar) = extractTrailingDifferential(outside)
                        val varName = specVar ?: trailingVar ?: deduceVar(cleanExpr)
                        return "Integrate[$cleanExpr, {$varName, $a, $b}]"
                    }
                } else {
                    val args = splitTopLevel(inside, ',')
                    if (args.size == 2) {
                        val spec0 = parseIntegralRangeSpec(args[0])
                        val spec1 = parseIntegralRangeSpec(args[1])
                        if (spec0 != null) {
                            val (cleanExpr, trailingVar) = extractTrailingDifferential(args[1])
                            val varName = spec0.first ?: trailingVar ?: deduceVar(cleanExpr)
                            return "Integrate[$cleanExpr, {$varName, ${spec0.second}, ${spec0.third}}]"
                        } else if (spec1 != null) {
                            val (cleanExpr, trailingVar) = extractTrailingDifferential(args[0])
                            val varName = spec1.first ?: trailingVar ?: deduceVar(cleanExpr)
                            return "Integrate[$cleanExpr, {$varName, ${spec1.second}, ${spec1.third}}]"
                        } else {
                            val (cleanExpr, trailingVar) = extractTrailingDifferential(args[0])
                            val varName = trailingVar ?: args[1].trim()
                            return "Integrate[$cleanExpr, $varName]"
                        }
                    } else if (args.size == 4) {
                        val (cleanExpr, _) = extractTrailingDifferential(args[0])
                        return "Integrate[$cleanExpr, {${args[1].trim()}, ${args[2].trim()}, ${args[3].trim()}}]"
                    } else if (args.size == 3) {
                        val (cleanExpr, trailingVar) = extractTrailingDifferential(args[0])
                        val varName = trailingVar ?: deduceVar(cleanExpr)
                        return "Integrate[$cleanExpr, {$varName, ${args[1].trim()}, ${args[2].trim()}}]"
                    } else if (args.size == 1) {
                        val (cleanExpr, trailingVar) = extractTrailingDifferential(args[0])
                        val varName = trailingVar ?: deduceVar(cleanExpr)
                        return "Integrate[$cleanExpr, $varName]"
                    }
                }
            }
        }

        // 2.3 Plain text definite integrals: int from a to b expr [dx] or int a to b expr [dx] or int a..b expr [dx]
        val mPlain = Regex("""^(?:int|integral)\s+(?:from\s+)?([a-zA-Z]+\s*=\s*[^,\s]+|[^,\s]+)\s+(?:to|\.\.|->)\s+([^\s]+)\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (mPlain != null) {
            val rawLower = mPlain.groupValues[1].trim()
            val upper = mPlain.groupValues[2].trim()
            val body = mPlain.groupValues[3].trim().removePrefix("*").trim()
            var specVar: String? = null
            var lower = rawLower
            if (rawLower.contains("=")) {
                val parts = rawLower.split("=", limit = 2)
                specVar = parts[0].trim()
                lower = parts[1].trim()
            }
            val (cleanExpr, trailingVar) = extractTrailingDifferential(body)
            val varName = specVar ?: trailingVar ?: deduceVar(cleanExpr)
            return "Integrate[$cleanExpr, {$varName, $lower, $upper}]"
        }

        // 2.4 Indefinite integrals: int expr [dx]
        val mIndef = Regex("""^(?:int|integral)\s+(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (mIndef != null) {
            val (cleanExpr, trailingVar) = extractTrailingDifferential(mIndef.groupValues[1].trim())
            val varName = trailingVar ?: deduceVar(cleanExpr)
            return "Integrate[$cleanExpr, $varName]"
        }
        
        // 4. Limits:
        val limPrefixMatch = Regex("""^(?:lim|limit)\s*_\{?\s*([a-zA-Z]+)\s*(?:->|to)\s*([^}]+)\}?\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
            ?: Regex("""^(?:lim|limit)\s*\(\s*([a-zA-Z]+)\s*(?:->|to)\s*([^)]+)\)\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
            ?: Regex("""^(?:lim|limit)\s+([a-zA-Z]+)\s*(?:->|to)\s*(\S+)\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (limPrefixMatch != null) {
            val varName = limPrefixMatch.groupValues[1].trim()
            var target = limPrefixMatch.groupValues[2].trim()
            val expr = limPrefixMatch.groupValues[3].trim().removePrefix("*").trim()
            var dirOpt = ""
            if (target.endsWith("^+") || target.endsWith("+")) {
                target = target.removeSuffix("^+").removeSuffix("+").trim()
                dirOpt = ", Direction -> -1"
            } else if (target.endsWith("^-") || target.endsWith("-")) {
                target = target.removeSuffix("^-").removeSuffix("-").trim()
                dirOpt = ", Direction -> 1"
            }
            return "Limit[$expr, $varName -> $target$dirOpt]"
        }

        // Functional parenthesized: lim(...) or limit(...)
        val limStartMatch = Regex("""^(?:lim|limit)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (limStartMatch != null) {
            val openIdx = limStartMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val outside = s.substring(closeIdx + 1).trim().removePrefix("*").trim()
                val parts = splitTopLevel(inside, ',')

                if (outside.isNotEmpty()) {
                    var varName = "x"
                    var target = "0"
                    if (parts.size >= 2) {
                        varName = parts[0].trim()
                        target = parts[1].trim()
                    } else if (parts.size == 1 && parts[0].contains("->")) {
                        val sub = parts[0].split("->", limit = 2)
                        varName = sub[0].trim()
                        target = sub[1].trim()
                    }
                    var dirOpt = ""
                    if (target.endsWith("^+") || target.endsWith("+")) {
                        target = target.removeSuffix("^+").removeSuffix("+").trim()
                        dirOpt = ", Direction -> -1"
                    } else if (target.endsWith("^-") || target.endsWith("-")) {
                        target = target.removeSuffix("^-").removeSuffix("-").trim()
                        dirOpt = ", Direction -> 1"
                    }
                    return "Limit[$outside, $varName -> $target$dirOpt]"
                } else {
                    var expr = ""
                    var varName = ""
                    var target = ""
                    var dirOpt = ""
                    if (parts.size == 4) {
                        expr = parts[0].trim()
                        varName = parts[1].trim()
                        target = parts[2].trim()
                        val dir = parts[3].trim()
                        if (dir == "1" || dir == "-1") {
                            dirOpt = ", Direction -> $dir"
                        } else if (dir == "+" || dir == "\"+1\"" || dir == "\"+\"") {
                            dirOpt = ", Direction -> -1"
                        } else if (dir == "-" || dir == "\"-1\"" || dir == "\"-\"") {
                            dirOpt = ", Direction -> 1"
                        }
                    } else if (parts.size == 3) {
                        if (parts[1].contains("->")) {
                            expr = parts[0].trim()
                            val sub = parts[1].split("->", limit = 2)
                            varName = sub[0].trim()
                            target = sub[1].trim()
                            val dir = parts[2].trim()
                            if (dir == "1" || dir == "-1") {
                                dirOpt = ", Direction -> $dir"
                            } else if (dir == "+" || dir == "\"+1\"" || dir == "\"+\"") {
                                dirOpt = ", Direction -> -1"
                            } else if (dir == "-" || dir == "\"-1\"" || dir == "\"-\"") {
                                dirOpt = ", Direction -> 1"
                            }
                        } else if (parts[0].matches(Regex("""^[a-zA-Z]$"""))) {
                            varName = parts[0].trim()
                            target = parts[1].trim()
                            expr = parts[2].trim()
                        } else {
                            expr = parts[0].trim()
                            varName = parts[1].trim()
                            target = parts[2].trim()
                        }
                    } else if (parts.size == 2) {
                        val sub0 = if (parts[0].contains("->")) parts[0].split("->", limit = 2)
                                   else if (parts[0].contains("=")) parts[0].split("=", limit = 2)
                                   else if (Regex("""(?i)\bto\b""").containsMatchIn(parts[0])) parts[0].split(Regex("""(?i)\bto\b"""), limit = 2)
                                   else null
                        val sub1 = if (parts[1].contains("->")) parts[1].split("->", limit = 2)
                                   else if (parts[1].contains("=")) parts[1].split("=", limit = 2)
                                   else if (Regex("""(?i)\bto\b""").containsMatchIn(parts[1])) parts[1].split(Regex("""(?i)\bto\b"""), limit = 2)
                                   else null
                        if (sub0 != null && sub0[0].trim().matches(Regex("""^[a-zA-Z]$"""))) {
                            varName = sub0[0].trim()
                            target = sub0[1].trim()
                            expr = parts[1].trim()
                        } else if (sub1 != null && sub1[0].trim().matches(Regex("""^[a-zA-Z]$"""))) {
                            varName = sub1[0].trim()
                            target = sub1[1].trim()
                            expr = parts[0].trim()
                        } else {
                            expr = parts[0].trim()
                            target = parts[1].trim()
                            varName = deduceVar(expr)
                        }
                    } else if (parts.size == 1) {
                        expr = parts[0].trim()
                        varName = deduceVar(expr)
                        target = "0"
                    }
                    if (expr.isNotEmpty()) {
                        if (dirOpt.isEmpty()) {
                            if (target.endsWith("^+") || target.endsWith("+")) {
                                target = target.removeSuffix("^+").removeSuffix("+").trim()
                                dirOpt = ", Direction -> -1"
                            } else if (target.endsWith("^-") || target.endsWith("-")) {
                                target = target.removeSuffix("^-").removeSuffix("-").trim()
                                dirOpt = ", Direction -> 1"
                            }
                        }
                        return "Limit[$expr, $varName -> $target$dirOpt]"
                    }
                }
            }
        }
        
        // 5. Sum & Product:
        // LaTeX subscript/superscript: \sum_{n=1}^\infty expr or sum_{n=1}^Infinity expr
        val spLatMatch = Regex("""^(sum|prod)\s*_\{?\s*([a-zA-Z]+)\s*=\s*([^}]+?)\}?\s*(?:\^\{([^}]+?)\}|\^(\S+))\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (spLatMatch != null) {
            val func = if (spLatMatch.groupValues[1].lowercase() == "sum") "Sum" else "Product"
            val varName = spLatMatch.groupValues[2].trim()
            val a = spLatMatch.groupValues[3].trim()
            val b = spLatMatch.groupValues[4].ifEmpty { spLatMatch.groupValues[5] }.trim()
            val expr = spLatMatch.groupValues[6].trim().removePrefix("*").trim()
            return "$func[$expr, {$varName, $a, $b}]"
        }

        // Functional / parenthesized: sum( ... )
        val spStartMatch = Regex("""^(sum|prod)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (spStartMatch != null) {
            val func = if (spStartMatch.groupValues[1].lowercase() == "sum") "Sum" else "Product"
            val openIdx = spStartMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val outside = s.substring(closeIdx + 1).trim().removePrefix("*").trim()
                
                if (outside.isNotEmpty()) {
                    // Case 1: outside has the summand/multiplicand: sum(n=1 to inf) expr
                    val spec = parseSumRangeSpec(inside)
                    if (spec != null) {
                        return "$func[$outside, {${spec.first}, ${spec.second}, ${spec.third}}]"
                    }
                } else {
                    // Case 2: everything is inside sum(...)
                    val args = splitTopLevel(inside, ',')
                    if (args.size == 2) {
                        val spec0 = parseSumRangeSpec(args[0])
                        val spec1 = parseSumRangeSpec(args[1])
                        if (spec0 != null) {
                            return "$func[${args[1]}, {${spec0.first}, ${spec0.second}, ${spec0.third}}]"
                        } else if (spec1 != null) {
                            return "$func[${args[0]}, {${spec1.first}, ${spec1.second}, ${spec1.third}}]"
                        }
                    } else if (args.size == 4) {
                        // expr, var, a, b
                        return "$func[${args[0]}, {${args[1]}, ${args[2]}, ${args[3]}}]"
                    } else if (args.size == 3) {
                        val mEq = Regex("""^([a-zA-Z]+)\s*=\s*(.+)$""").find(args[1])
                        if (mEq != null) {
                            return "$func[${args[0]}, {${mEq.groupValues[1]}, ${mEq.groupValues[2]}, ${args[2]}}]"
                        } else {
                            val varGuess = deduceVar(args[0])
                            return "$func[${args[0]}, {$varGuess, ${args[1]}, ${args[2]}}]"
                        }
                    }
                }
            }
        }

        // Plain text: sum n=1 to inf expr
        val spPlainMatch = Regex("""^(sum|prod)\s+([a-zA-Z]+)\s*=\s*([^,)\s]+?)(?:\s*\*?\s*(?:to|\.\.)\s*\*?\s*|\s*,\s*)([^\s]+)\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (spPlainMatch != null) {
            val func = if (spPlainMatch.groupValues[1].lowercase() == "sum") "Sum" else "Product"
            val varName = spPlainMatch.groupValues[2].trim()
            val a = spPlainMatch.groupValues[3].trim()
            val b = spPlainMatch.groupValues[4].trim()
            val expr = spPlainMatch.groupValues[5].trim().removePrefix("*").trim()
            return "$func[$expr, {$varName, $a, $b}]"
        }

        // 6. Laplace & Inverse Laplace:
        val lapMatch = Regex("""^(?:laplace|laplacetransform)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (lapMatch != null) {
            val openIdx = lapMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val args = splitTopLevel(inside, ',')
                return when (args.size) {
                    3 -> "LaplaceTransform[${args[0].trim()}, ${args[1].trim()}, ${args[2].trim()}]"
                    2 -> "LaplaceTransform[${args[0].trim()}, ${args[1].trim()}, s]"
                    1 -> {
                        val v = deduceVar(args[0])
                        val target = if (v == "s") "t" else "s"
                        "LaplaceTransform[${args[0].trim()}, $v, $target]"
                    }
                    else -> s
                }
            }
        }
        val lapPlain = Regex("""^(?:laplace|laplacetransform)\s+(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (lapPlain != null) {
            val body = lapPlain.groupValues[1].trim().removePrefix("*").trim()
            val v = deduceVar(body)
            val target = if (v == "s") "t" else "s"
            return "LaplaceTransform[$body, $v, $target]"
        }

        val iltMatch = Regex("""^(?:ilt|inverselaplace|invlaplace|inverselaplacetransform)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (iltMatch != null) {
            val openIdx = iltMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val args = splitTopLevel(inside, ',')
                return when (args.size) {
                    3 -> "InverseLaplaceTransform[${args[0].trim()}, ${args[1].trim()}, ${args[2].trim()}]"
                    2 -> "InverseLaplaceTransform[${args[0].trim()}, ${args[1].trim()}, t]"
                    1 -> {
                        val v = deduceVar(args[0])
                        val target = if (v == "t") "s" else "t"
                        "InverseLaplaceTransform[${args[0].trim()}, $v, $target]"
                    }
                    else -> s
                }
            }
        }
        val iltPlain = Regex("""^(?:ilt|inverselaplace|invlaplace)\s+(.+)$""", RegexOption.IGNORE_CASE).find(s)
        if (iltPlain != null) {
            val body = iltPlain.groupValues[1].trim().removePrefix("*").trim()
            val v = deduceVar(body)
            val target = if (v == "t") "s" else "t"
            return "InverseLaplaceTransform[$body, $v, $target]"
        }

        val ceMatch = Regex("""^(?:complexexpand)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (ceMatch != null) {
            val openIdx = ceMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                return "ComplexExpand[$inside]"
            }
        }
        val eigVecMatch = Regex("""^(?:eigenvectors?|ozvektor|özvektör)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (eigVecMatch != null) {
            val openIdx = eigVecMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val convertedInside = convertListBrackets(inside)
                return "Eigenvectors[$convertedInside]"
            }
        }
        val eigSysMatch = Regex("""^(?:eigensystem)\s*\(""", RegexOption.IGNORE_CASE).find(s)
        if (eigSysMatch != null) {
            val openIdx = eigSysMatch.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            if (closeIdx != -1) {
                val inside = s.substring(openIdx + 1, closeIdx).trim()
                val convertedInside = convertListBrackets(inside)
                return "Eigensystem[$convertedInside]"
            }
        }

        s = preprocessExponentialEquations(s)
        
        return s
    }

    private fun simplifyFloorCeil(str: String): String {
        var res = str
        // Floor[expr] < int, <= int, > int, >= int, == int
        val pat = Regex("""(^|[(&|\s])Floor\[([^\]]+)\]\s*(<=|<|>=|>|==)\s*([+-]?\d+)(?!\s*[/^])([)&|\s]|$)""")
        res = pat.replace(res) { match ->
            val pre = match.groupValues[1]
            val expr = match.groupValues[2].trim()
            val op = match.groupValues[3]
            val c = match.groupValues[4].toInt()
            val post = match.groupValues[5]
            val transformed = when (op) {
                "<" -> "$expr < $c"
                "<=" -> "$expr < ${c + 1}"
                ">" -> "$expr >= ${c + 1}"
                ">=" -> "$expr >= $c"
                "==" -> "$c <= $expr && $expr < ${c + 1}"
                else -> match.value
            }
            "$pre$transformed$post"
        }
        
        // int < Floor[expr], etc.
        val patRev = Regex("""(^|[(&|\s])([+-]?\d+)\s*(<=|<|>=|>|==)\s*Floor\[([^\]]+)\]([)&|\s]|$)""")
        res = patRev.replace(res) { match ->
            val pre = match.groupValues[1]
            val c = match.groupValues[2].toInt()
            val op = match.groupValues[3]
            val expr = match.groupValues[4].trim()
            val post = match.groupValues[5]
            val transformed = when (op) {
                "<" -> "$expr >= ${c + 1}"
                "<=" -> "$expr >= $c"
                ">" -> "$expr < $c"
                ">=" -> "$expr < ${c + 1}"
                "==" -> "$c <= $expr && $expr < ${c + 1}"
                else -> match.value
            }
            "$pre$transformed$post"
        }

        // Ceiling[expr] < int, <= int, > int, >= int, == int
        val patCeil = Regex("""(^|[(&|\s])Ceiling\[([^\]]+)\]\s*(<=|<|>=|>|==)\s*([+-]?\d+)(?!\s*[/^])([)&|\s]|$)""")
        res = patCeil.replace(res) { match ->
            val pre = match.groupValues[1]
            val expr = match.groupValues[2].trim()
            val op = match.groupValues[3]
            val c = match.groupValues[4].toInt()
            val post = match.groupValues[5]
            val transformed = when (op) {
                "<" -> "$expr <= ${c - 1}"
                "<=" -> "$expr <= $c"
                ">" -> "$expr > $c"
                ">=" -> "$expr > ${c - 1}"
                "==" -> "${c - 1} < $expr && $expr <= $c"
                else -> match.value
            }
            "$pre$transformed$post"
        }

        // int < Ceiling[expr], etc.
        val patCeilRev = Regex("""(^|[(&|\s])([+-]?\d+)\s*(<=|<|>=|>|==)\s*Ceiling\[([^\]]+)\]([)&|\s]|$)""")
        res = patCeilRev.replace(res) { match ->
            val pre = match.groupValues[1]
            val c = match.groupValues[2].toInt()
            val op = match.groupValues[3]
            val expr = match.groupValues[4].trim()
            val post = match.groupValues[5]
            val transformed = when (op) {
                "<" -> "$expr > $c"
                "<=" -> "$expr > ${c - 1}"
                ">" -> "$expr <= ${c - 1}"
                ">=" -> "$expr <= $c"
                "==" -> "${c - 1} < $expr && $expr <= $c"
                else -> match.value
            }
            "$pre$transformed$post"
        }
        
        return res.trim()
    }

    private fun isSquare(n: Long): Pair<Boolean, Long> {
        if (n < 0) return Pair(false, 0L)
        val r = kotlin.math.sqrt(n.toDouble()).toLong()
        return Pair(r * r == n, r)
    }

    private fun denestRadicalCalc(A: Long, B: Long, D: Long, sign: String): String? {
        val disc = A * A - B * B * D
        val (hasSq, C) = isSquare(disc)
        if (!hasSq) return null
        val t1Num = A + C
        val t2Num = A - C
        return if (t1Num % 2L == 0L && t2Num % 2L == 0L) {
            val t1 = t1Num / 2L
            val t2 = t2Num / 2L
            val (sq1, r1) = isSquare(t1)
            val (sq2, r2) = isSquare(t2)
            val s1 = if (sq1) r1.toString() else "Sqrt[$t1]"
            val s2 = if (sq2) r2.toString() else "Sqrt[$t2]"
            "($s1 $sign $s2)"
        } else {
            val s1Val = 2L * (A + C)
            val s2Val = 2L * (A - C)
            val (sq1, r1) = isSquare(s1Val)
            val (sq2, r2) = isSquare(s2Val)
            val s1 = if (sq1) r1.toString() else "Sqrt[$s1Val]"
            val s2 = if (sq2) r2.toString() else "Sqrt[$s2Val]"
            "(($s1 $sign $s2)/2)"
        }
    }

    fun denestRadicals(str: String): String {
        val pat1 = Regex("""(?i)\b(?:sqrt)\s*[\[\(]\s*(\d+)\s*([+-])\s*(?:(\d+)\s*\*?\s*)?(?:sqrt)\s*[\[\(]\s*(\d+)\s*[\]\)]\s*[\]\)]""")
        val pat2 = Regex("""(?i)\b(?:sqrt)\s*[\[\(]\s*(?:(\d+)\s*\*?\s*)?(?:sqrt)\s*[\[\(]\s*(\d+)\s*[\]\)]\s*\+\s*(\d+)\s*[\]\)]""")
        
        var res = str
        var prev = ""
        while (prev != res) {
            prev = res
            res = pat1.replace(res) { m ->
                val A = m.groupValues[1].toLongOrNull() ?: return@replace m.value
                val sign = m.groupValues[2]
                val B = m.groupValues[3].ifEmpty { "1" }.toLongOrNull() ?: 1L
                val D = m.groupValues[4].toLongOrNull() ?: return@replace m.value
                denestRadicalCalc(A, B, D, sign) ?: m.value
            }
            res = pat2.replace(res) { m ->
                val B = m.groupValues[1].ifEmpty { "1" }.toLongOrNull() ?: 1L
                val D = m.groupValues[2].toLongOrNull() ?: return@replace m.value
                val A = m.groupValues[3].toLongOrNull() ?: return@replace m.value
                denestRadicalCalc(A, B, D, "+") ?: m.value
            }
        }
        return res
    }

    private val mathFuncsList = listOf(
        "arcsin", "arccos", "arctan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "coth",
        "sin", "cos", "tan", "cot", "sec", "csc",
        "ln", "log", "exp", "sqrt", "floor", "ceil",
        "arg", "re", "im", "conjugate", "gamma"
    )
    private val mathFuncsPattern = mathFuncsList.joinToString("|")

    fun preprocessImplicitMultiplication(str: String): String {
        var s = str
        
        // 1. Function without parens: e.g. sin x -> sin(x), cos 2x -> cos(2*x), sin theta -> sin(theta)
        val argPat = Regex("""(?i)\b($mathFuncsPattern)\s+([0-9]*[a-zA-Z]+(?:\^[0-9]+)?|[0-9]+(?:\.[0-9]+)?)\b""")
        var prev = ""
        while (prev != s) {
            prev = s
            s = argPat.replace(s) { m ->
                val fn = m.groupValues[1]
                val rawArg = m.groupValues[2]
                val argFixed = rawArg.replace(Regex("""(\d+)([a-zA-Z])"""), "$1*$2")
                "$fn($argFixed)"
            }
        }
        
        // 1.5. Imaginary unit: 4i -> 4*I, 4*i -> 4*I, standalone i in arithmetic
        s = s.replace(Regex("""(?i)(\d)\s*\*?\s*i\b"""), "$1*I")
        s = s.replace(Regex("""(?<=[+*/(,\s-])i(?=[+*/),\s^]|$)"""), "I")

        // 2. Number followed by variable, func, or paren: 2x -> 2*x, 2sin(x) -> 2*sin(x), 5( -> 5*(
        // Only standalone numbers (not digits inside identifiers like Log10) are affected.
        // Exclude keyword 'to' (e.g. 1 to inf in sums or ranges)
        s = s.replace(Regex("""(?i)\b(\d+(?:\.\d+)?)\s*(?!to\b)([a-zA-Z])"""), "$1*$2")
        s = s.replace(Regex("""\b(\d+(?:\.\d+)?)\s*\("""), "$1*(")
        
        // 3. Closing paren followed by opening paren, digit, func, or variable:
        s = s.replace(Regex("""\)\s*\("""), ")*(")
        s = s.replace(Regex("""\)\s*([0-9])"""), ")*$1")
        s = s.replace(Regex("""(?i)\)\s*($mathFuncsPattern)\b"""), ")*$1")
        s = s.replace(Regex("""\)\s*([a-zA-Z])"""), ")*$1")
        
        // 4. Variable followed by func: x sin(x) -> x*sin(x), x cos(x) -> x*cos(x)
        s = s.replace(Regex("""(?i)\b([a-zA-Z])\s*($mathFuncsPattern)\b""")) { m ->
            val v = m.groupValues[1]
            val fn = m.groupValues[2]
            val full = m.value.lowercase().trim()
            if (canonicalFunctionNames.containsKey(full) || full in mathFuncsList ||
                v.lowercase() in mathFuncsList || canonicalFunctionNames.containsKey(v.lowercase())
            ) {
                m.value
            } else {
                "$v*$fn"
            }
        }
        
        // 5. Identifier followed by open paren: decide generically whether it is a function call.
        //    - Known function names / aliases (any case) and capitalized names are function calls.
        //    - Single letters (except the derivative operator d, capital N, and n with precision/constants) and named constants are multiplied.
        s = s.replace(Regex("""\b([a-zA-Z]+)\s*\(""")) { m ->
            val name = m.groupValues[1]
            val openIdx = m.range.last
            val closeIdx = findMatchingBrace(s, openIdx, '(', ')')
            val inside = if (closeIdx != -1) s.substring(openIdx + 1, closeIdx) else ""
            if (isFunctionCallName(name, inside)) m.value else "$name*("
        }
        
        return s
    }

    private val namedConstants = setOf("pi", "e", "i", "inf", "infinity", "theta", "alpha", "beta", "lambda", "phi", "omega", "mu", "sigma", "tau", "degree")
    private val structuralKeywords = setOf("to", "from", "matrix")

    private fun isFunctionCallName(name: String, inside: String = ""): Boolean {
        val lower = name.lowercase()
        if (lower in namedConstants) return false
        if (inside.contains(",")) return true
        if (name == "N") return true
        if (lower == "d" || lower == "pdsolve" || lower == "dsolve") return true
        if (lower == "n") {
            if (inside.trim().lowercase() in listOf("pi", "e", "π")) return true
            return false
        }
        if (lower in structuralKeywords) return true
        if (canonicalFunctionNames.containsKey(lower)) return true
        // Unknown multi-letter identifiers typed with a capital letter are treated as explicit
        // Symja function names (e.g. Fibonacci(10)); lowercase unknown words are implicit products.
        return name[0].isUpperCase()
    }

    /**
     * Single source of truth for function names: maps every accepted spelling (any letter case,
     * common aliases and Turkish names) to the canonical Symja function name.
     */
    private val canonicalFunctionNames: Map<String, String> by lazy {
        val canonical = listOf(
            "Log", "Log10", "Log2", "Sin", "Cos", "Tan", "Cot", "Sec", "Csc",
            "ArcSin", "ArcCos", "ArcTan", "ArcCot", "ArcSec", "ArcCsc",
            "Sinh", "Cosh", "Tanh", "Coth", "Sech", "Csch", "ArcSinh", "ArcCosh", "ArcTanh", "ArcCoth",
            "Exp", "Sqrt", "CubeRoot", "Surd", "Integrate", "NIntegrate", "D", "Limit", "Sum", "Product",
            "Series", "Normal", "Solve", "NSolve", "DSolve", "PDSolve", "Reduce", "FindRoot", "Factor", "Expand",
            "Simplify", "FullSimplify", "Together", "Apart", "Cancel", "Collect", "TrigExpand", "TrigReduce",
            "TrigToExp", "ExpToTrig", "Numerator", "Denominator", "Coefficient", "CoefficientList", "Exponent",
            "PolynomialQuotient", "PolynomialRemainder", "PolynomialGCD", "PolynomialLCM", "Roots",
            "Floor", "Ceiling", "Round", "IntegerPart", "FractionalPart", "Abs", "Sign", "Mod", "GCD", "LCM",
            "FactorInteger", "PrimeQ", "Prime", "Binomial", "Factorial", "Gamma", "Beta", "Erf",
            "Arg", "Re", "Im", "Conjugate", "ComplexExpand", "Det", "Inverse", "Transpose", "Tr",
            "Eigenvalues", "Eigenvectors", "Eigensystem", "MatrixRank", "RowReduce", "NullSpace", "Dot",
            "Cross", "Norm", "LaplaceTransform", "InverseLaplaceTransform", "FourierTransform",
            "Max", "Min", "Mean", "Median", "Variance", "StandardDeviation", "Total", "Fibonacci", "N"
        )
        val map = HashMap<String, String>()
        for (c in canonical) map[c.lowercase()] = c
        val aliases = mapOf(
            "ln" to "Log", "lg" to "Log10", "asin" to "ArcSin", "acos" to "ArcCos", "atan" to "ArcTan",
            "acot" to "ArcCot", "asinh" to "ArcSinh", "acosh" to "ArcCosh", "atanh" to "ArcTanh",
            "arcsinh" to "ArcSinh", "arccosh" to "ArcCosh", "arctanh" to "ArcTanh",
            "cbrt" to "CubeRoot", "int" to "Integrate", "integral" to "Integrate", "nint" to "NIntegrate",
            "derivative" to "D", "diff" to "D", "turev" to "D", "türev" to "D",
            "lim" to "Limit", "prod" to "Product", "taylor" to "Series", "maclaurin" to "Series", "seri" to "Series", "series" to "Series",
            "ceil" to "Ceiling", "sgn" to "Sign", "inv" to "Inverse", "trace" to "Tr",
            "eigenvals" to "Eigenvalues", "ozdeger" to "Eigenvalues", "özdeğer" to "Eigenvalues",
            "eigenvector" to "Eigenvectors", "ozvektor" to "Eigenvectors", "özvektör" to "Eigenvectors",
            "laplace" to "LaplaceTransform", "ilt" to "InverseLaplaceTransform",
            "inverselaplace" to "InverseLaplaceTransform", "invlaplace" to "InverseLaplaceTransform",
            "fact" to "Factorial", "binom" to "Binomial", "ncr" to "Binomial",
            "ebob" to "GCD", "ekok" to "LCM", "average" to "Mean", "rank" to "MatrixRank",
            "n" to "N"
        )
        map.putAll(aliases)
        map
    }

    private val canonicalFunctionSet: Set<String> by lazy { canonicalFunctionNames.values.toSet() }

    /**
     * Rewrites every function call name (any spelling / case / alias) to its canonical Symja name
     * and converts the call's balanced parentheses into Symja brackets: integrate(...) -> Integrate[...].
     */
    fun canonicalizeFunctionCalls(input: String): String {
        val normalizedN = input.replace(Regex("""(?<![A-Za-z0-9_])n\s*\["""), "N[")
        val renamed = Regex("""(?<![A-Za-z0-9_])([\p{L}][\p{L}0-9]*)\s*\(""").replace(normalizedN) { m ->
            val name = m.groupValues[1]
            val canon = if (name.length == 1 && name != "d" && name != "D" && name != "n" && name != "N") null else canonicalFunctionNames[name.lowercase()]
            if (canon != null) "$canon(" else m.value
        }
        val sb = java.lang.StringBuilder(renamed)
        val callPat = Regex("""(?<![A-Za-z0-9_])([A-Z][A-Za-z0-9]*)\(""")
        var from = 0
        while (from < sb.length) {
            val m = callPat.find(sb, from) ?: break
            val openIdx = m.range.last
            if (m.groupValues[1] in canonicalFunctionSet) {
                val close = findMatchingBrace(sb.toString(), openIdx, '(', ')')
                if (close != -1) {
                    sb.setCharAt(openIdx, '[')
                    sb.setCharAt(close, ']')
                }
            }
            from = openIdx + 1
        }
        return sb.toString()
    }

    /**
     * Converts any Leibniz derivative representation into standard Symja D[body, ...] form:
     * e.g. (d)/(dx)(expr), d/dx(expr), \frac{d}{dx}(expr), (d^2)/(dx^2)(expr), \frac{d^2}{dx dy}(expr).
     */
    fun normalizeLeibnizDerivatives(input: String): String {
        val s = input.trim()
        val clean = s.replace("""\partial""", "d").replace("∂", "d")
        
        // 1. Mixed partial: \frac{d^2}{dx dy} or d^2/(dx dy) or d^2/dxdy
        val mixedPat = Regex("""^\s*\(?\s*(?:\\frac\{d\^(\d+)\}\{d\s*([a-zA-Z])\s*(?:\\?[,\s])*d\s*([a-zA-Z])\}|\(?d\^(\d+)\)?\s*/\s*\(?d\s*([a-zA-Z])\s*(?:\\?[,\s])*d\s*([a-zA-Z])\)?)\s*\)?\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE)
        val mMixed = mixedPat.find(clean)
        if (mMixed != null) {
            val v1 = mMixed.groupValues[2].ifEmpty { mMixed.groupValues[5] }
            val v2 = mMixed.groupValues[3].ifEmpty { mMixed.groupValues[6] }
            val rawBody = mMixed.groupValues[7].trim()
            val body = if (rawBody.startsWith("(") && rawBody.endsWith(")") && findMatchingBrace(rawBody, 0, '(', ')') == rawBody.length - 1) {
                rawBody.substring(1, rawBody.length - 1).trim()
            } else rawBody
            return "D[$body, $v1, $v2]"
        }

        // 2. Higher order single variable: \frac{d^n}{dx^n} or d^n/dx^n or (d^n)/(dx^n)
        val orderPat = Regex("""^\s*\(?\s*(?:\\frac\{d\^(\d+)\}\{d\s*([a-zA-Z])\^\1\}|\(?d\^(\d+)\)?\s*/\s*\(?d\s*([a-zA-Z])\^\3\)?)\s*\)?\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE)
        val mOrder = orderPat.find(clean)
        if (mOrder != null) {
            val order = mOrder.groupValues[1].ifEmpty { mOrder.groupValues[3] }
            val v = mOrder.groupValues[2].ifEmpty { mOrder.groupValues[4] }
            val rawBody = mOrder.groupValues[5].trim()
            val body = if (rawBody.startsWith("(") && rawBody.endsWith(")") && findMatchingBrace(rawBody, 0, '(', ')') == rawBody.length - 1) {
                rawBody.substring(1, rawBody.length - 1).trim()
            } else rawBody
            return "D[$body, {$v, $order}]"
        }

        // 3. First order: (d)/(dx), d/(dx), (d)/dx, d/dx, \frac{d}{dx}, ((d)/(dx)), (d/dx)
        val firstPat = Regex("""^\s*\(?\s*(?:\\frac\{d\}\{d\s*([a-zA-Z])\}|\(?d\)?\s*/\s*\(?d\s*([a-zA-Z])\)?)\s*\)?\s*\*?\s*(.+)$""", RegexOption.IGNORE_CASE)
        val mFirst = firstPat.find(clean)
        if (mFirst != null) {
            val v = mFirst.groupValues[1].ifEmpty { mFirst.groupValues[2] }
            val rawBody = mFirst.groupValues[3].trim()
            val body = if (rawBody.startsWith("(") && rawBody.endsWith(")") && findMatchingBrace(rawBody, 0, '(', ')') == rawBody.length - 1) {
                rawBody.substring(1, rawBody.length - 1).trim()
            } else rawBody
            return "D[$body, $v]"
        }

        return s
    }

    /**
     * Lexically normalizes input text: normalizes Turkish Unicode letters (İ -> I, ı -> i)
     * and Leibniz derivative notations into canonical expressions.
     */
    fun normalizeInputText(input: String): String {
        var s = input.trim().replace("$", "").replace("`", "")
        // Sanitize trailing backslashes or stray backslashes that cause \[RawComma] crashes in Symja
        s = s.replace(Regex("""\\+\s*$"""), "").trim()
        s = s.replace(Regex("""\\(?=[\s,;+*/^=()]|$)"""), " ")
        s = s.replace('İ', 'I').replace('ı', 'i')
        s = normalizeLeibnizDerivatives(s)
        return s.trim()
    }

    /**
     * Recursively normalizes arguments for Symja calculus commands (Integrate, D, Limit, Sum, Product, Solve)
     * so that missing variables or plain argument lists are converted into valid Symja argument specifications.
     */
    fun normalizeCalculusArguments(s: String): String {
        val callRegex = Regex("""(?<![A-Za-z0-9_])(Integrate|D|Limit|Sum|Product|Solve|Series)\[""")
        var res = s
        var fromPos = 0
        while (true) {
            val m = callRegex.find(res, fromPos) ?: break
            val head = m.groupValues[1]
            val openIdx = m.range.last
            val closeIdx = findMatchingBrace(res, openIdx, '[', ']')
            if (closeIdx == -1) break

            val inner = res.substring(openIdx + 1, closeIdx).trim()
            val normalizedInner = normalizeCalculusArguments(inner)
            val args = splitTopLevel(normalizedInner, ',')
            var newCall: String? = null
            when (head) {
                "Integrate" -> {
                    when {
                        args.size == 1 -> {
                            val v = deduceVar(args[0])
                            newCall = "Integrate[${args[0]}, $v]"
                        }
                        args.size == 3 && !args[1].startsWith("{") -> {
                            val v = deduceVar(args[0])
                            newCall = "Integrate[${args[0]}, {$v, ${args[1]}, ${args[2]}}]"
                        }
                        args.size == 4 -> {
                            newCall = "Integrate[${args[0]}, {${args[1]}, ${args[2]}, ${args[3]}}]"
                        }
                        args.size == 2 && !args[1].startsWith("{") && (args[1].contains("..") || args[1].contains(" to ")) -> {
                            val spec = parseIntegralRangeSpec(args[1])
                            if (spec != null) {
                                val v = spec.first ?: deduceVar(args[0])
                                newCall = "Integrate[${args[0]}, {$v, ${spec.second}, ${spec.third}}]"
                            }
                        }
                    }
                }
                "D" -> {
                    when {
                        args.size == 1 -> {
                            val v = deduceVar(args[0])
                            newCall = "D[${args[0]}, $v]"
                        }
                        args.size == 3 && args[2].all { it.isDigit() } -> {
                            newCall = "D[${args[0]}, {${args[1]}, ${args[2]}}]"
                        }
                    }
                }
                "Limit" -> {
                    when {
                        args.size == 2 && !args[1].contains("->") -> {
                            val v = deduceVar(args[0])
                            newCall = "Limit[${args[0]}, $v -> ${args[1]}]"
                        }
                        args.size == 3 && !args[1].contains("->") -> {
                            newCall = "Limit[${args[0]}, ${args[1]} -> ${args[2]}]"
                        }
                        args.size == 4 && !args[1].contains("->") -> {
                            val dir = args[3].trim()
                            val dirOpt = when (dir) {
                                "-1", "1" -> ", Direction -> $dir"
                                "+", "\"+1\"", "\"+\"" -> ", Direction -> -1"
                                "-", "\"-1\"", "\"-\"" -> ", Direction -> 1"
                                else -> ", Direction -> $dir"
                            }
                            newCall = "Limit[${args[0]}, ${args[1]} -> ${args[2]}$dirOpt]"
                        }
                    }
                }
                "Sum", "Product" -> {
                    when {
                        args.size == 3 && !args[1].startsWith("{") -> {
                            val v = deduceVar(args[0])
                            newCall = "$head[${args[0]}, {$v, ${args[1]}, ${args[2]}}]"
                        }
                        args.size == 4 -> {
                            newCall = "$head[${args[0]}, {${args[1]}, ${args[2]}, ${args[3]}}]"
                        }
                    }
                }
                "Solve" -> {
                    when {
                        args.size == 1 -> {
                            val v = deduceVar(args[0])
                            newCall = "Solve[${args[0]}, $v]"
                        }
                    }
                }
                "Series" -> {
                    when {
                        args.size == 4 -> {
                            newCall = "Series[${args[0]}, {${args[1]}, ${args[2]}, ${args[3]}}]"
                        }
                        args.size == 3 && !args[1].startsWith("{") -> {
                            newCall = "Series[${args[0]}, {${args[1]}, 0, ${args[2]}}]"
                        }
                        args.size == 2 && !args[1].startsWith("{") -> {
                            val v = deduceVar(args[0])
                            if (args[1].all { it.isDigit() }) {
                                newCall = "Series[${args[0]}, {$v, 0, ${args[1]}}]"
                            } else {
                                newCall = "Series[${args[0]}, {${args[1]}, 0, 5}]"
                            }
                        }
                        args.size == 1 -> {
                            val v = deduceVar(args[0])
                            newCall = "Series[${args[0]}, {$v, 0, 5}]"
                        }
                    }
                }
            }

            if (newCall != null) {
                res = res.substring(0, m.range.first) + newCall + res.substring(closeIdx + 1)
                fromPos = m.range.first + newCall.length
            } else {
                fromPos = openIdx + 1
            }
        }
        return res
    }

    fun replaceAbsWithSqrtSq(expr: String): String {
        var res = expr
        while (true) {
            val m = Regex("""\bAbs\[""").find(res) ?: break
            val openIdx = m.range.last
            val closeIdx = findMatchingBrace(res, openIdx, '[', ']')
            if (closeIdx == -1) break
            val inner = res.substring(openIdx + 1, closeIdx).trim()
            val replacedInner = replaceAbsWithSqrtSq(inner)
            val sub = "Sqrt[($replacedInner)^2]"
            res = res.substring(0, m.range.first) + sub + res.substring(closeIdx + 1)
        }
        return res
    }

    fun prepareRealDomainDerivative(cmd: String): String {
        if (!cmd.startsWith("D[") || !cmd.endsWith("]")) return cmd
        val inner = cmd.substring(2, cmd.length - 1)
        val args = splitTopLevel(inner, ',')
        if (args.size < 2) return cmd
        val f = args[0].trim()
        val rest = args.drop(1).joinToString(", ") { it.trim() }
        if (f.contains("Abs[")) {
            val fReal = replaceAbsWithSqrtSq(f)
            return "D[$fReal, $rest]"
        }
        return cmd
    }

    fun simplifyRealDerivativeResult(res: String): String {
        var s = res
        // Sqrt[(u)^2] -> Abs[u]
        s = s.replace(Regex("""Sqrt\[\(([^)]+)\)\^2\]"""), "Abs[$1]")
        s = s.replace(Regex("""Sqrt\[([a-zA-Z0-9_]+)\^2\]"""), "Abs[$1]")
        // Abs'[u] or Derivative[1][Abs][u] -> ((u)/Abs[u])
        s = s.replace(Regex("""Derivative\[1\]\[Abs\]\[([^\]]+)\]"""), "(($1)/Abs[$1])")
        s = s.replace(Regex("""Abs'\s*\[([^\]]+)\]"""), "(($1)/Abs[$1])")
        s = s.replace(Regex("""Abs'\s*\(([^)]+)\)"""), "(($1)/Abs($1))")
        // Abs[u]^2 -> (u)^2
        s = s.replace(Regex("""Abs\[([^\]]+)\]\s*\^\s*2"""), "(($1)^2)")
        s = s.replace(Regex("""Abs\[([^\]]+)\]\s*\*\s*Abs\[\1\]"""), "(($1)^2)")
        return s
    }

    fun preprocessDifferentialEquation(input: String): Pair<String, Boolean> {
        val trimmed = input.trim()
        val hasPrime = Regex("""[a-zA-Z]['\u2032\u2019\u2033\u201d"]+""").containsMatchIn(trimmed)
        val hasEquality = Regex("""(?<![=!<>])=(?![=>])|====""").containsMatchIn(trimmed)
        val hasSolveOde = Regex("""(?i)\b(solve|dsolve)\s*[\(\[]""").containsMatchIn(trimmed)
        val hasOdeDerivNotation = Regex("""(?:\\frac\{d\^?\d*[a-zA-Z]\}\{d[a-zA-Z]\^?\d*\}|d\^?\d*[a-zA-Z]/d[a-zA-Z])""").containsMatchIn(trimmed)
        
        val isOdeCandidate = hasPrime || (hasOdeDerivNotation && (hasEquality || hasSolveOde)) || (hasSolveOde && hasPrime)
        if (!isOdeCandidate) {
            return Pair(trimmed, false)
        }

        var cleaned = trimmed
            .replace('′', '\'')
            .replace('’', '\'')
            .replace('`', '\'')
            .replace("″", "''")
            .replace("”", "''")
            .replace("\"", "''")
        cleaned = cleaned.replace("""\left""", "").replace("""\right""", "")

        // If wrapped in dsolve(...) or solve(...)
        val wrapMatch = Regex("""^(?:dsolve|solve)\s*[\(\[]\s*(.+?)\s*[\)\]]$""", RegexOption.IGNORE_CASE).find(cleaned)
        var inner = if (wrapMatch != null) wrapMatch.groupValues[1].trim() else cleaned

        // Helper to recursively unwrap bracketed/braced lists and split by top-level comma
        fun unpackParts(expr: String): List<String> {
            var t = expr.trim()
            while ((t.startsWith("{") && t.endsWith("}")) || 
                   (t.startsWith("[") && t.endsWith("]")) ||
                   (t.startsWith("(") && t.endsWith(")"))) {
                val candidate = t.substring(1, t.length - 1).trim()
                val parts = splitTopLevel(candidate, ',')
                if (parts.size > 1) {
                    return parts.flatMap { unpackParts(it) }
                } else if (candidate.isNotEmpty()) {
                    t = candidate
                } else {
                    break
                }
            }

            val topLevel = splitTopLevel(t, ',')
            if (topLevel.size > 1) {
                return topLevel.flatMap { unpackParts(it) }
            }

            // Split by logical AND: &&, and, ve, und, et
            val subparts = t.split(Regex("""\s*(?:&&|\b(?:and|ve|und|et)\b)\s*""", RegexOption.IGNORE_CASE))
            if (subparts.size > 1) {
                return subparts.flatMap { unpackParts(it) }
            }

            return if (t.isNotEmpty()) listOf(t) else emptyList()
        }

        fun isOdeEquation(item: String): Boolean {
            val t = item.trim()
            return t.contains("=") || t.contains("'") || t.contains("′") || 
                   Regex("""(?:\\frac\{d|d[a-zA-Z]/d)""").containsMatchIn(t)
        }

        val rawParts = unpackParts(inner)
        val eqParts = mutableListOf<String>()
        val varParts = mutableListOf<String>()

        for (p in rawParts) {
            val t = p.trim()
            if (isOdeEquation(t)) {
                eqParts.add(t)
            } else {
                // Single variable or function call like y(x) / y[x]
                val funcMatch = Regex("""^([a-zA-Z])\s*[\(\[]\s*([a-zA-Z])\s*[\)\]]$""").find(t)
                if (funcMatch != null) {
                    varParts.add(funcMatch.groupValues[1])
                    varParts.add(funcMatch.groupValues[2])
                } else if (Regex("""^[a-zA-Z]$""").matches(t)) {
                    varParts.add(t)
                }
            }
        }

        // Dependent variable(s): find all primed variables (e.g. x', y')
        val primedVars = mutableListOf<String>()
        for (eq in eqParts) {
            for (m in Regex("""([a-zA-Z])['\u2032\u2019]+""").findAll(eq)) {
                val v = m.groupValues[1]
                if (v !in primedVars) primedVars.add(v)
            }
            val dMatch = Regex("""d([a-zA-Z])/d([a-zA-Z])""").find(eq)
            if (dMatch != null) {
                val v = dMatch.groupValues[1]
                if (v !in primedVars) primedVars.add(v)
            }
            val fracMatch = Regex("""\\frac\{d\^?\d*([a-zA-Z])\}\{d([a-zA-Z])\^?\d*\}""").find(eq)
            if (fracMatch != null) {
                val v = fracMatch.groupValues[1]
                if (v !in primedVars) primedVars.add(v)
            }
            val icMatch = Regex("""([a-zA-Z])\s*[\(\[]\s*[^)\]]+\s*[\)\]]\s*==?""").find(eq)
            if (icMatch != null) {
                val v = icMatch.groupValues[1]
                if (v !in primedVars && primedVars.isEmpty()) primedVars.add(v)
            }
        }

        val explicitDeps = mutableListOf<String>()
        var explicitIndep: String? = null

        for (v in varParts) {
            if (v in primedVars) {
                if (v !in explicitDeps) explicitDeps.add(v)
            } else if (explicitDeps.isEmpty() && primedVars.isEmpty()) {
                explicitDeps.add(v)
            } else if (explicitIndep == null) {
                explicitIndep = v
            } else if (v !in explicitDeps) {
                explicitDeps.add(v)
            }
        }

        val depVars = when {
            explicitDeps.isNotEmpty() -> explicitDeps
            primedVars.isNotEmpty() -> primedVars
            else -> listOf("y")
        }

        // Independent variable
        var indepVar = explicitIndep
        if (indepVar == null) {
            val depPattern = depVars.joinToString("|")
            val paramMatch = Regex("""\b(?:$depPattern)['\u2032\u2019]*\s*[\(\[]\s*([a-zA-Z])\s*[\)\]]""").find(cleaned)
            if (paramMatch != null && paramMatch.groupValues[1] !in depVars) {
                indepVar = paramMatch.groupValues[1]
            } else {
                val allVars = mutableListOf<String>()
                for (eq in eqParts) {
                    for (m in Regex("""\b([a-zA-Z])\b""").findAll(eq)) {
                        val v = m.groupValues[1]
                        if (v !in depVars && v !in listOf("e", "E", "i", "I", "pi", "Pi", "d", "D", "C")) {
                            if (v !in allVars) allVars.add(v)
                        }
                    }
                }
                indepVar = when {
                    allVars.contains("t") -> "t"
                    allVars.contains("x") -> "x"
                    allVars.isNotEmpty() -> allVars.first()
                    else -> if (depVars.contains("x")) "t" else "x"
                }
            }
        }

        val processedEqs = mutableListOf<String>()
        for (eq in eqParts) {
            var s = eq

            // Implicit multiplication before variable replacement so numbers before vars don't block \b
            // e.g. 2y -> 2*y, 2(x) -> 2*(x), )x -> )*x
            s = s.replace(Regex("""(?i)(\d)\s*([a-zA-Z])"""), "$1*$2")
                 .replace(Regex("""(\d)\s*\("""), "$1*(")
                 .replace(Regex("""\)\s*(\d)"""), ")*$1")
                 .replace(Regex("""\)\s*\("""), ")*(")
                 .replace(Regex("""\)\s*([a-zA-Z])"""), ")*$1")

            // Deriv notations and variable parameterization for each depVar
            for (dVar in depVars) {
                s = s.replace(Regex("""\\frac\{d\^?2$dVar\}\{d$indepVar\^?2\}"""), "$dVar''[$indepVar]")
                s = s.replace(Regex("""d\^?2$dVar/d$indepVar\^?2"""), "$dVar''[$indepVar]")
                s = s.replace(Regex("""\\frac\{d$dVar\}\{d$indepVar\}"""), "$dVar'[$indepVar]")
                s = s.replace(Regex("""d$dVar/d$indepVar"""), "$dVar'[$indepVar]")

                // Initial condition points or parameter calls: y'(0), y(0), y'(t), y(t)
                s = s.replace(Regex("""\b$dVar(\x27*)\s*[\(\[]\s*([^)\],]+)\s*[\)\]]""")) { m ->
                    val primes = m.groupValues[1]
                    val v = m.groupValues[2].trim()
                    "$dVar$primes[$v]"
                }

                // Unparameterized primes
                s = s.replace(Regex("""\b$dVar\x27\x27\x27(?!\w|\[|\x27)"""), "$dVar'''[$indepVar]")
                s = s.replace(Regex("""\b$dVar\x27\x27(?!\w|\[|\x27)"""), "$dVar''[$indepVar]")
                s = s.replace(Regex("""\b$dVar\x27(?!\w|\[|\x27)"""), "$dVar'[$indepVar]")

                // Bare depVar not followed by [ or '
                s = s.replace(Regex("""\b$dVar\b(?!\s*\[|\s*\x27)"""), "$dVar[$indepVar]")
            }

            // Clean = into ==
            s = s.replace(Regex("(?<![=!<>])=(?![=>])"), "==")
            if (!s.contains("==")) {
                s = "$s == 0"
            }
            processedEqs.add(s.trim())
        }

        val dsolveExpr = if (depVars.size > 1) {
            val targetVars = depVars.joinToString(", ") { "$it[$indepVar]" }
            "DSolve[{${processedEqs.joinToString(", ")}}, {$targetVars}, $indepVar]"
        } else {
            "DSolve[{${processedEqs.joinToString(", ")}}, ${depVars[0]}[$indepVar], $indepVar]"
        }

        return Pair(dsolveExpr, true)
    }

    fun convertOdeToDNotation(cmd: String): String {
        var s = cmd
        s = s.replace(Regex("""\b([a-zA-Z])\x27\x27\x27\s*\[\s*([a-zA-Z][a-zA-Z0-9]*)\s*\]"""), "D[$1[$2], {$2, 3}]")
             .replace(Regex("""\b([a-zA-Z])\x27\x27\s*\[\s*([a-zA-Z][a-zA-Z0-9]*)\s*\]"""), "D[$1[$2], {$2, 2}]")
             .replace(Regex("""\b([a-zA-Z])\x27\s*\[\s*([a-zA-Z][a-zA-Z0-9]*)\s*\]"""), "D[$1[$2], $2]")
             .replace(Regex("""\b([a-zA-Z])\x27\x27\x27\s*\(\s*([a-zA-Z][a-zA-Z0-9]*)\s*\)"""), "D[$1[$2], {$2, 3}]")
             .replace(Regex("""\b([a-zA-Z])\x27\x27\s*\(\s*([a-zA-Z][a-zA-Z0-9]*)\s*\)"""), "D[$1[$2], {$2, 2}]")
             .replace(Regex("""\b([a-zA-Z])\x27\s*\(\s*([a-zA-Z][a-zA-Z0-9]*)\s*\)"""), "D[$1[$2], $2]")
        return s
    }

    fun solveOdeAnalytically(dsolveCmd: String): Pair<String, String?>? {
        return try {
            if (!dsolveCmd.startsWith("DSolve[") || !dsolveCmd.endsWith("]")) return null
            val inner = dsolveCmd.substring(7, dsolveCmd.length - 1)
            val args = splitTopLevel(inner, ',')
            if (args.size < 3) return null

            val eqArg = args[0].trim()
            val depSpec = args[1].trim().removeSurrounding("{", "}").trim()
            val indepVar = args[2].trim()
            val depVar = depSpec.substringBefore("[").substringBefore("(").trim()

            val eqList = if (eqArg.startsWith("{") && eqArg.endsWith("}")) {
                splitTopLevel(eqArg.substring(1, eqArg.length - 1), ',')
            } else {
                listOf(eqArg)
            }
            if (eqList.isEmpty()) return null

            val mainOde = eqList.firstOrNull { it.contains("[$indepVar]") || it.contains("($indepVar)") || it.contains("'") || it.contains("D[") } ?: eqList.first()
            val icList = eqList.filter { it != mainOde }

            if (!mainOde.contains("==")) return null
            val odeParts = mainOde.split("==", limit = 2)
            val rawDiff = "(${odeParts[0]}) - (${odeParts[1]})"
            val subbed = rawDiff
                .replace(Regex("""\bD\[\s*$depVar\[$indepVar\]\s*,\s*\{\s*$indepVar\s*,\s*3\s*\}\s*\]"""), "DY3")
                .replace(Regex("""\bD\[\s*$depVar\[$indepVar\]\s*,\s*\{\s*$indepVar\s*,\s*2\s*\}\s*\]"""), "DY2")
                .replace(Regex("""\bD\[\s*$depVar\[$indepVar\]\s*,\s*$indepVar\s*\]"""), "DY1")
                .replace("$depVar'''[$indepVar]", "DY3").replace("$depVar'''($indepVar)", "DY3")
                .replace("$depVar''[$indepVar]", "DY2").replace("$depVar''($indepVar)", "DY2")
                .replace("$depVar'[$indepVar]", "DY1").replace("$depVar'($indepVar)", "DY1")
                .replace("$depVar[$indepVar]", "DY0").replace("$depVar($indepVar)", "DY0")

            val expanded = evaluator.eval("Expand($subbed)").toString()
            val a = evaluator.eval("Simplify(Coefficient($expanded, DY2, 1))").toString()
            val b = evaluator.eval("Simplify(Coefficient($expanded, DY1, 1))").toString()
            val c = evaluator.eval("Simplify(Coefficient($expanded, DY0, 1))").toString()
            val rem = evaluator.eval("Simplify(-(($expanded) - ($a)*DY2 - ($b)*DY1 - ($c)*DY0))").toString()

            val odeCleanTex = escapeLatexForHtml(texOf(mainOde))
            val sb = java.lang.StringBuilder()

            // 1. Second Order Linear with Constant Coefficients: a*y'' + b*y' + c*y = 0
            if (a != "0" && rem == "0") {
                val charPoly = evaluator.eval("Expand(($a)*r^2 + ($b)*r + ($c))").toString()
                val delta = evaluator.eval("Simplify(($b)^2 - 4*($a)*($c))").toString()
                val charRootsRaw = evaluator.eval("Solve($charPoly == 0, r)").toString()
                val charRootsClean = cleanRootsOutput(texOf(charRootsRaw), "r")

                sb.append("<b>${Translator.tr("1. Lineer Sabit Katsayılı Diferansiyel Denklem:")}</b><br/>")
                sb.append("$$ $odeCleanTex $$<br/>")
                sb.append("<b>${Translator.tr("2. Karakteristik Denklem")} ($ $depVar = e^{r $indepVar} $):</b><br/>")
                sb.append("$$ ${escapeLatexForHtml(texOf(charPoly))} = 0, \\quad \\Delta = ${escapeLatexForHtml(texOf(delta))} $$<br/>")
                sb.append("<b>${Translator.tr("3. Karakteristik Kökler:")}</b><br/>")
                sb.append("$$ ${escapeLatexForHtml(charRootsClean)} $$<br/>")

                // Compute general solution in Symja syntax
                val genSolMath: String
                val genSolLatex: String
                val deltaNum = evalRealDouble(delta)

                if (delta == "0" || deltaNum == 0.0) {
                    val r0 = evaluator.eval("Simplify(-($b)/(2*($a)))").toString()
                    val r0Tex = texOf(r0)
                    genSolMath = "(C_1 + C_2*$indepVar)*Exp(($r0)*$indepVar)"
                    genSolLatex = "$depVar($indepVar) = (C_1 + C_2 $indepVar) e^{$r0Tex $indepVar}"
                } else if ((deltaNum != null && deltaNum < 0.0) || delta.contains("-")) {
                    val alpha = evaluator.eval("Simplify(-($b)/(2*($a)))").toString()
                    val beta = evaluator.eval("Simplify(Sqrt(-($delta))/(2*($a)))").toString()
                    val alphaTex = texOf(alpha)
                    val betaTex = texOf(beta)
                    if (alpha == "0" || evalRealDouble(alpha) == 0.0) {
                        genSolMath = "C_1*Cos(($beta)*$indepVar) + C_2*Sin(($beta)*$indepVar)"
                        genSolLatex = "$depVar($indepVar) = C_1 \\cos($betaTex $indepVar) + C_2 \\sin($betaTex $indepVar)"
                    } else {
                        genSolMath = "Exp(($alpha)*$indepVar)*(C_1*Cos(($beta)*$indepVar) + C_2*Sin(($beta)*$indepVar))"
                        genSolLatex = "$depVar($indepVar) = e^{$alphaTex $indepVar} \\left( C_1 \\cos($betaTex $indepVar) + C_2 \\sin($betaTex $indepVar) \\right)"
                    }
                } else {
                    val r1 = evaluator.eval("Simplify((-($b) + Sqrt($delta))/(2*($a)))").toString()
                    val r2 = evaluator.eval("Simplify((-($b) - Sqrt($delta))/(2*($a)))").toString()
                    val r1Tex = texOf(r1)
                    val r2Tex = texOf(r2)
                    genSolMath = "C_1*Exp(($r1)*$indepVar) + C_2*Exp(($r2)*$indepVar)"
                    genSolLatex = "$depVar($indepVar) = C_1 e^{$r1Tex $indepVar} + C_2 e^{$r2Tex $indepVar}"
                }

                if (icList.isNotEmpty()) {
                    sb.append("<b>${Translator.tr("4. Genel Çözüm:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(genSolLatex)} $$<br/>")

                    // Solve for C_1, C_2 using ICs
                    val icEqs = mutableListOf<String>()
                    val dGenSol = evaluator.eval("D($genSolMath, $indepVar)").toString()
                    for (ic in icList) {
                        val icClean = ic.replace("==", "=")
                        val mPrime = Regex("""$depVar'\s*[\(\[]\s*([^)\],]+)\s*[\)\]]\s*=\s*(.+)""").find(icClean)
                        val mBase = Regex("""$depVar\s*[\(\[]\s*([^)\],]+)\s*[\)\]]\s*=\s*(.+)""").find(icClean)
                        if (mPrime != null) {
                            val x0 = mPrime.groupValues[1].trim()
                            val y0 = mPrime.groupValues[2].trim()
                            icEqs.add("ReplaceAll($dGenSol, $indepVar -> ($x0)) == ($y0)")
                        } else if (mBase != null) {
                            val x0 = mBase.groupValues[1].trim()
                            val y0 = mBase.groupValues[2].trim()
                            icEqs.add("ReplaceAll($genSolMath, $indepVar -> ($x0)) == ($y0)")
                        }
                    }

                    var solvedLatex = genSolLatex
                    if (icEqs.isNotEmpty()) {
                        val cSol = try {
                            evaluator.eval("Solve({${icEqs.joinToString(", ")}}, {C_1, C_2})").toString()
                        } catch (_: Exception) { "" }
                        if (cSol.isNotEmpty() && !cSol.contains("Solve")) {
                            val partSol = evaluator.eval("ReplaceAll($genSolMath, $cSol)").toString()
                            val partTex = texOf(partSol)
                            solvedLatex = "$depVar($indepVar) = $partTex"
                        }
                    }
                    val icTex = icList.joinToString(", ") { escapeLatexForHtml(texOf(it)) }
                    sb.append("<b>${Translator.tr("5. Başlangıç Koşulu Uygulaması")} ($ $icTex $):</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(solvedLatex)} $$")
                    return Pair(solvedLatex, sb.toString())
                } else {
                    sb.append("<b>${Translator.tr("4. Genel Çözüm:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(genSolLatex)} $$")
                    return Pair(genSolLatex, sb.toString())
                }
            } else if (a == "0" && b != "0") {
                // 1st order linear: b*y' + c*y = rem
                val pExpr = evaluator.eval("Simplify(($c)/($b))").toString()
                val qExpr = evaluator.eval("Simplify(($rem)/($b))").toString()
                val intP = evaluator.eval("Integrate($pExpr, $indepVar)").toString()
                val muExpr = evaluator.eval("Simplify(Exp($intP))").toString()
                val pTex = escapeLatexForHtml(texOf(pExpr))
                val muTex = escapeLatexForHtml(texOf(muExpr))

                sb.append("<b>${Translator.tr("1. Birinci Mertebe Lineer Diferansiyel Denklem ve İntegral Çarpanı:")}</b><br/>")
                sb.append("$$ $odeCleanTex \\implies \\mu($indepVar) = e^{\\int $pTex \\,d$indepVar} = $muTex $$<br/>")

                val genSolMath: String
                val genSolLatex: String
                if (qExpr == "0") {
                    genSolMath = "C_1 / ($muExpr)"
                    val solTex = texOf("Simplify(C_1 / ($muExpr))")
                    genSolLatex = "$depVar($indepVar) = $solTex"
                } else {
                    val intMuQ = evaluator.eval("Integrate(($muExpr)*($qExpr), $indepVar)").toString()
                    genSolMath = "(($intMuQ) + C_1) / ($muExpr)"
                    val solTex = texOf("Simplify(($intMuQ) / ($muExpr))")
                    genSolLatex = "$depVar($indepVar) = $solTex + \\frac{C_1}{$muTex}"
                }

                if (icList.isNotEmpty()) {
                    sb.append("<b>${Translator.tr("2. Genel Çözüm:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(genSolLatex)} $$<br/>")
                    var solvedLatex = genSolLatex
                    val icClean = icList.first().replace("==", "=")
                    val mBase = Regex("""$depVar\s*[\(\[]\s*([^)\],]+)\s*[\)\]]\s*=\s*(.+)""").find(icClean)
                    if (mBase != null) {
                        val x0 = mBase.groupValues[1].trim()
                        val y0 = mBase.groupValues[2].trim()
                        val cSol = try {
                            evaluator.eval("Solve(ReplaceAll($genSolMath, $indepVar -> ($x0)) == ($y0), C_1)").toString()
                        } catch (_: Exception) { "" }
                        if (cSol.isNotEmpty() && !cSol.contains("Solve")) {
                            val partSol = evaluator.eval("ReplaceAll($genSolMath, $cSol)").toString()
                            val partTex = texOf(partSol)
                            solvedLatex = "$depVar($indepVar) = $partTex"
                        }
                    }
                    val icTex = icList.joinToString(", ") { escapeLatexForHtml(texOf(it)) }
                    sb.append("<b>${Translator.tr("3. Başlangıç Koşulu Uygulaması")} ($ $icTex $):</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(solvedLatex)} $$")
                    return Pair(solvedLatex, sb.toString())
                } else {
                    sb.append("<b>${Translator.tr("2. Genel Çözüm:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(genSolLatex)} $$")
                    return Pair(genSolLatex, sb.toString())
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun formatPdeLatex(rawEq: String): String {
        var s = rawEq.trim()
        s = s.replace("==", " = ")
        // D[u(x, t), t] or D[u, t] -> \frac{\partial u}{\partial t}
        s = s.replace(Regex("""D\[\s*([a-zA-Z]+)(?:\([^\)]+\))?\s*,\s*([a-zA-Z])\s*\]""")) { m ->
            "\\frac{\\partial ${m.groupValues[1]}}{\\partial ${m.groupValues[2]}}"
        }
        // Leibniz \partial or \frac{du}{dt} or du/dt
        s = s.replace(Regex("""\\frac\{\\partial\s*([a-zA-Z]+)(?:\([^\)]+\))?\}\{\\partial\s*([a-zA-Z])\}"""), "\\frac{\\partial $1}{\\partial $2}")
        s = s.replace(Regex("""(?i)\b([a-zA-Z])\_([a-zA-Z])\b"""), "\\frac{\\partial $1}{\\partial $2}")
        s = s.replace("*", " \\cdot ")
        return s
    }

    fun preprocessPartialDifferentialEquation(input: String): Triple<String, String, Boolean>? {
        val trimmed = input.trim()
        val hasPdsolve = Regex("""(?i)\b(pdsolve)\s*[\(\[]""").containsMatchIn(trimmed)
        val hasPartialDeriv = trimmed.contains("""\partial""") || trimmed.contains("∂") || 
                              Regex("""D\[\s*[a-zA-Z]+(?:\([^\)]+\))?\s*,\s*[a-zA-Z]\s*\]""").containsMatchIn(trimmed) ||
                              Regex("""(?i)\b[a-zA-Z]\_([a-zA-Z])\b""").containsMatchIn(trimmed)

        if (!hasPdsolve && !hasPartialDeriv) return null

        val wrapMatch = Regex("""^(?:pdsolve)\s*[\(\[]\s*(.+?)\s*[\)\]]$""", RegexOption.IGNORE_CASE).find(trimmed)
        val inner = if (wrapMatch != null) wrapMatch.groupValues[1].trim() else trimmed
        val parts = splitTopLevel(inner, ',')
        val eqRaw = parts[0].trim()
        val targetSpec = if (parts.size > 1) parts[1].trim() else "u"
        val depVar = Regex("""^([a-zA-Z]+)""").find(targetSpec)?.groupValues?.get(1) ?: "u"

        var cleanEq = eqRaw.replace("""\left""", "").replace("""\right""", "")
        cleanEq = cleanEq.replace("""\partial""", "d").replace("∂", "d")
        
        // Normalize Leibniz partials: \frac{d u}{d t} -> D[u, t], du/dt -> D[u, t], u_t -> D[u, t]
        cleanEq = cleanEq.replace(Regex("""\\frac\{d\s*$depVar(?:\([^\)]+\))?\}\{d\s*([a-zA-Z])\}"""), "D[$depVar, $1]")
        cleanEq = cleanEq.replace(Regex("""\\frac\{d\}\{d\s*([a-zA-Z])\}\s*$depVar(?:\([^\)]+\))?"""), "D[$depVar, $1]")
        cleanEq = cleanEq.replace(Regex("""d$depVar/d([a-zA-Z])"""), "D[$depVar, $1]")
        cleanEq = cleanEq.replace(Regex("""(?i)\b$depVar\_([a-zA-Z])(?:\([^\)]+\))?\b"""), "D[$depVar, $1]")

        // Replace D[u(...), var] with D[u, var]
        cleanEq = cleanEq.replace(Regex("""D\[\s*$depVar\s*\([^\)]+\)\s*,\s*([a-zA-Z])\s*\]"""), "D[$depVar, $1]")

        // Find all partial derivative variables: e.g. D[u, t], D[u, x]
        val derivMatches = Regex("""D\[\s*$depVar\s*,\s*([a-zA-Z])\s*\]""").findAll(cleanEq).toList()
        val indepVars = derivMatches.map { it.groupValues[1] }.distinct()
        if (indepVars.size < 2) {
            val pdeCleanTex = escapeLatexForHtml(formatPdeLatex(eqRaw))
            val sb = java.lang.StringBuilder()
            sb.append("<b>${Translator.tr("1. Kısmi Diferansiyel Denklem (PDE):")}</b><br/>")
            sb.append("$$ $pdeCleanTex $$<br/>")
            sb.append("<b>${Translator.tr("2. Çözüm Metodolojisi:")}</b><br/>")
            sb.append("${Translator.tr("Bu ifade çok değişkenli bir Kısmi Diferansiyel Denklemdir (PDE). Yerel sembolik motor 1. mertebe karakteristik taşıma çözümlerini doğrudan sunar; sınır ve başlangıç koşullu karmaşık PDE'lerin Fourier / Laplace dönüşümü veya değişkenlerine ayırma ile adım adım çözümü için aşağıdaki AI butonunu kullanabilirsiniz.")}<br/>")

            return Triple(pdeCleanTex, sb.toString(), false)
        }

        val v1 = indepVars[0]
        val v2 = indepVars[1]

        // Substitute dummy symbols DVAR1, DVAR2
        var subbed = cleanEq
            .replace("D[$depVar, $v1]", "DVAR1")
            .replace("D[$depVar, $v2]", "DVAR2")

        if (!subbed.contains("==") && subbed.contains("=")) {
            subbed = subbed.replace(Regex("(?<![=!<>])=(?![=>])"), "==")
        }
        val odeParts = if (subbed.contains("==")) subbed.split("==", limit = 2) else listOf(subbed, "0")
        val diff = "(${odeParts[0]}) - (${odeParts[1]})"
        val expanded = try { evaluator.eval("Expand($diff)").toString() } catch (_: Exception) { "" }
        if (expanded.isNotEmpty()) {
            val a = evaluator.eval("Simplify(Coefficient($expanded, DVAR1, 1))").toString()
            val b = evaluator.eval("Simplify(Coefficient($expanded, DVAR2, 1))").toString()
            val rem = evaluator.eval("Simplify(($expanded) - ($a)*DVAR1 - ($b)*DVAR2)").toString()

            if (a != "0" && b != "0" && rem == "0") {
                // First-order linear transport equation: a * du/dv1 + b * du/dv2 = 0
                // Characteristic curve: a*v2 - b*v1 = C
                val vSpace = if (v1.lowercase() in listOf("t", "time")) v2 else v1
                val cSpace = if (v1.lowercase() in listOf("t", "time")) b else a
                val vTime = if (v1.lowercase() in listOf("t", "time")) v1 else v2
                val cTime = if (v1.lowercase() in listOf("t", "time")) a else b

                var charCurve = evaluator.eval("Simplify(($cTime)*$vSpace - ($cSpace)*$vTime)").toString()
                if (charCurve.trim().startsWith("-")) {
                    charCurve = evaluator.eval("Simplify(-($charCurve))").toString()
                }
                val charTex = escapeLatexForHtml(texOf(charCurve))
                val targetFuncTex = if (targetSpec.contains("(") && targetSpec.endsWith(")")) targetSpec else "$depVar($vSpace, $vTime)"
                val solTex = "$targetFuncTex = F\\left($charTex\\right)"
                val pdeLatex = formatPdeLatex(eqRaw)

                val sb = java.lang.StringBuilder()
                sb.append("<b>${Translator.tr("1. 1. Mertebe Lineer Taşıma Denklemi (PDE):")}</b><br/>")
                sb.append("$$ ${escapeLatexForHtml(pdeLatex)} $$<br/>")
                sb.append("<b>${Translator.tr("2. Karakteristikler Yöntemi (Method of Characteristics):")}</b><br/>")
                sb.append("$$ \\frac{d$vTime}{${escapeLatexForHtml(texOf(cTime))}} = \\frac{d$vSpace}{${escapeLatexForHtml(texOf(cSpace))}} = \\frac{d$depVar}{0} $$<br/>")
                sb.append("$$ ${escapeLatexForHtml(texOf(cTime))} \\,d$vSpace - ${escapeLatexForHtml(texOf(cSpace))} \\,d$vTime = 0 \\implies $charTex = C_1 $$<br/>")
                sb.append("<b>${Translator.tr("3. Genel Çözüm:")}</b><br/>")
                sb.append("$$ $solTex \\quad \\left(F \\in C^1\\right) $$")

                return Triple(solTex, sb.toString(), true)
            }
        }

        // General PDE fallback: provide structured PDE information
        val pdeCleanTex = escapeLatexForHtml(formatPdeLatex(eqRaw))
        val sb = java.lang.StringBuilder()
        sb.append("<b>${Translator.tr("1. Kısmi Diferansiyel Denklem (PDE):")}</b><br/>")
        sb.append("$$ $pdeCleanTex $$<br/>")
        sb.append("<b>${Translator.tr("2. Çözüm Metodolojisi:")}</b><br/>")
        sb.append("${Translator.tr("Bu ifade çok değişkenli bir Kısmi Diferansiyel Denklemdir (PDE). Yerel sembolik motor 1. mertebe karakteristik taşıma çözümlerini doğrudan sunar; sınır ve başlangıç koşullu karmaşık PDE'lerin Fourier / Laplace dönüşümü veya değişkenlerine ayırma ile adım adım çözümü için aşağıdaki AI butonunu kullanabilirsiniz.")}<br/>")

        return Triple(pdeCleanTex, sb.toString(), false)
    }

    fun findMatchingBracePos(s: String, openIdx: Int, openChar: Char = '{', closeChar: Char = '}'): Int {
        var depth = 0
        var i = openIdx
        while (i < s.length) {
            val c = s[i]
            if (c == openChar) {
                depth++
            } else if (c == closeChar) {
                depth--
                if (depth == 0) {
                    return i
                }
            }
            i++
        }
        return -1
    }

    fun unwrapOuterListDelimiters(s: String): String {
        var res = s.trim()
        var changed = true
        while (changed) {
            changed = false
            val t = res.trim()

            // 1. \left\{ ... \right\}
            if (t.startsWith("""\left\{""")) {
                val openIdx = t.indexOf('{')
                if (openIdx != -1) {
                    val closeIdx = findMatchingBracePos(t, openIdx, '{', '}')
                    if (closeIdx == t.length - 1) {
                        val rightIdx = t.lastIndexOf("""\right""", closeIdx)
                        if (rightIdx != -1 && rightIdx >= closeIdx - 7) {
                            res = t.substring(openIdx + 1, rightIdx).trim()
                            changed = true
                            continue
                        }
                    }
                }
            }

            // 2. \{ ... \}
            if (t.startsWith("""\{""")) {
                val openIdx = 1
                val closeIdx = findMatchingBracePos(t, openIdx, '{', '}')
                if (closeIdx == t.length - 1 && closeIdx > 0 && t[closeIdx - 1] == '\\') {
                    res = t.substring(2, closeIdx - 1).trim()
                    changed = true
                    continue
                }
            }

            // 3. { ... }
            if (t.startsWith("{")) {
                val closeIdx = findMatchingBracePos(t, 0, '{', '}')
                if (closeIdx == t.length - 1) {
                    res = t.substring(1, closeIdx).trim()
                    changed = true
                    continue
                }
            }
        }
        return res
    }

    fun cleanCompositeFactors(tex: String): String {
        var s = tex
        s = s.replace(Regex("""-\s*1\s*(?:\\left)?\(\s*-[iI]\s*(?:\\right)?\)\s*(?:\\cdot|\*|\s*)"""), "+ i ")
            .replace(Regex("""-\s*(?:\\left)?\(\s*-[iI]\s*(?:\\right)?\)\s*(?:\\cdot|\*|\s*)"""), "+ i ")
            .replace(Regex("""\+\s*1\s*(?:\\left)?\(\s*-[iI]\s*(?:\\right)?\)\s*(?:\\cdot|\*|\s*)"""), "- i ")
            .replace(Regex("""\+\s*(?:\\left)?\(\s*-[iI]\s*(?:\\right)?\)\s*(?:\\cdot|\*|\s*)"""), "- i ")
            .replace(Regex("""(?<![0-9a-zA-Z_])1\s*(?:\\left)?\(\s*-[iI]\s*(?:\\right)?\)\s*(?:\\cdot|\*|\s*)"""), "- i ")
            .replace("-1(-i)", "+i").replace("-(-i)", "+i")
            .replace("-1(-I)", "+I").replace("-(-I)", "+I")
            .replace("1(-i)", "-i").replace("1(-I)", "-I")
            .replace(Regex("""\bi\s+\\frac\{\\sqrt\{([^}]+)\}\}\{([^}]+)\}"""), """\frac{i\sqrt{$1}}{$2}""")
            .replace(Regex("""\bi\s*\\cdot\s*"""), "i ")
            .replace(Regex("""\+\s*\+"""), "+")
            .replace(Regex("""\+\s*-"""), "-")
            .replace(Regex("""-\s*-"""), "+")
            .replace(Regex("""-\s*\+"""), "-")
        return s.trim()
    }

    fun cleanEngineOutput(tex: String, isOde: Boolean = false, solveVars: List<String> = emptyList()): String {
        var res = tex.trim()

        // 1. ODE Constant notation: C(1) -> C_1, C[1] -> C_1, \text{C}(1) -> C_1
        res = res.replace(Regex("""(?:\\text\{C\}|C)\s*[\(\[](\d+)[\)\]]"""), "C_$1")

        // 2. Erroneous composite factor syntax: -1(-i) -> +i, -(-i) -> +i, -1(-I) -> +I
        res = cleanCompositeFactors(res)

        // 2.5 Replace software comparison operator == with mathematical equality =
        res = res.replace(Regex("""\\text\{\s*==\s*\}"""), "=")
            .replace("==", "=")

        // 3. ODE Solution stripping: \left\{\left\{y(x) \to ...\right\}\right\}
        val isOdeSolution = isOde || res.contains(Regex("""\b[a-zA-Z](?:\\left\s*\(|\()\s*[a-zA-Z]\s*(?:\\right\s*\)|\))\s*(?:\\to|->|=)"""))
        if (isOdeSolution) {
            var unwrapped = unwrapOuterListDelimiters(res)
            unwrapped = unwrapped.replace(Regex("""\\to|->"""), "=")
            unwrapped = cleanCompositeFactors(unwrapped)
            return unwrapped
        }

        // 4. Equation Solve roots stripping: \left\{\left\{x \to ...\right\}, \left\{x \to ...\right\}\right\}
        val isSolveResult = res.contains(Regex("""(?<![a-zA-Z0-9_])[a-zA-Z]\s*(?:\\to|->)""")) &&
                (res.contains("""\left\{""") || res.contains("""\{""") || res.contains("{"))
        if (isSolveResult) {
            val varName = solveVars.firstOrNull() ?: "x"
            return cleanRootsOutput(res, varName)
        }

        return res
    }

    /**
     * Computes roots of a quadratic Solve[...] directly from coefficients and renders them
     * in standard form. Complex roots are written as a ± bi (e.g. -1/2 ± (√3/2) i).
     * Returns null if the equation is not a quadratic with numeric coefficients.
     */
    fun formatQuadraticRootsTex(solveCmd: String, v: String): String? {
        return try {
            if (!solveCmd.startsWith("Solve[") || !solveCmd.endsWith("]")) return null
            val args = splitTopLevel(solveCmd.substring(6, solveCmd.length - 1), ',')
            if (args.size != 2 || !args[0].contains("==") || args[1].trim() != v) return null
            val eqParts = args[0].split("==", limit = 2)
            val diff = evaluator.eval("Together((${eqParts[0]}) - (${eqParts[1]}))").toString()
            val num = evaluator.eval("Expand(Numerator($diff))").toString()
            if (num.contains("Abs")) return null
            if (evaluator.eval("Exponent($num, $v)").toString() != "2") return null
            val a = evaluator.eval("Coefficient($num, $v, 2)").toString()
            val b = evaluator.eval("Coefficient($num, $v, 1)").toString()
            val c = evaluator.eval("Coefficient($num, $v, 0)").toString()
            // Only numeric (real) coefficients; symbolic ones are left to the engine
            if (evalRealDouble(a) == null || evalRealDouble(b) == null || evalRealDouble(c) == null) return null
            val delta = evaluator.eval("Simplify(($b)^2 - 4*($a)*($c))").toString()
            val deltaVal = evalRealDouble(delta) ?: return null

            if (deltaVal < -1e-12) {
                val re = evaluator.eval("Simplify(-($b)/(2*($a)))").toString()
                var im = evaluator.eval("Simplify(Sqrt(-($delta))/(2*($a)))").toString()
                if ((evalRealDouble(im) ?: 0.0) < 0) im = evaluator.eval("Simplify(-($im))").toString()
                val reTex = texOf(re)
                val imTex = texOf(im)
                val imPart = if (im == "1" || im == "1.0") "i" else "$imTex\\,i"
                if (re == "0" || evalRealDouble(re) == 0.0) {
                    "${v}_1 = -$imPart, \\quad ${v}_2 = $imPart"
                } else {
                    "${v}_1 = $reTex - $imPart, \\quad ${v}_2 = $reTex + $imPart"
                }
            } else if (kotlin.math.abs(deltaVal) <= 1e-12) {
                val r = evaluator.eval("Simplify(-($b)/(2*($a)))").toString()
                "$v = ${texOf(r)}"
            } else {
                var r1 = evaluator.eval("Simplify((-($b) - Sqrt($delta))/(2*($a)))").toString()
                var r2 = evaluator.eval("Simplify((-($b) + Sqrt($delta))/(2*($a)))").toString()
                if ((evalRealDouble(r1) ?: 0.0) > (evalRealDouble(r2) ?: 0.0)) { val t = r1; r1 = r2; r2 = t }
                "${v}_1 = ${texOf(r1)}, \\quad ${v}_2 = ${texOf(r2)}"
            }
        } catch (_: Exception) {
            null
        }
    }

    fun cleanRootsOutput(rawTex: String, varName: String = "x"): String {
        var res = cleanCompositeFactors(rawTex)
        res = res.replace(Regex("""^[a-zA-Z]\s*=\s*(?=\\left\{|\\\{|\{)"""), "")
        res = unwrapOuterListDelimiters(res)

        // Already formatted (x_1 = ..., x_2 = ...) -> don't prefix again
        if (!res.contains("\\to") && !res.contains("->") &&
            Regex("""(?<![a-zA-Z])[a-zA-Z]_\{?\d+\}?\s*=""").containsMatchIn(res)) {
            return res
        }

        // Split top-level by comma so inner LaTeX command braces like \frac{1}{2} remain untouched
        val rawRules = splitTopLevel(res, ',')
        val cleanedRoots = mutableListOf<String>()
        var actualVar = varName

        for (r in rawRules) {
            var trimmed = r.trim().removePrefix("""\quad""").removePrefix("""\qquad""").trim()
            trimmed = unwrapOuterListDelimiters(trimmed)
            trimmed = cleanCompositeFactors(trimmed)

            val varMatch = Regex("""^([a-zA-Z](?:_\{?\d+\}?)?)\s*(?:\\to|->|=)\s*(.+)$""").find(trimmed)
            if (varMatch != null) {
                val matchedVar = varMatch.groupValues[1].substringBefore('_')
                if (actualVar == varName || actualVar == "x") {
                    actualVar = matchedVar
                }
                val valStr = unwrapOuterListDelimiters(varMatch.groupValues[2].trim())
                cleanedRoots.add(cleanCompositeFactors(valStr))
            } else {
                cleanedRoots.add(trimmed)
            }
        }

        return when {
            cleanedRoots.size == 2 -> {
                "${actualVar}_1 = ${cleanedRoots[0]}, \\quad ${actualVar}_2 = ${cleanedRoots[1]}"
            }
            cleanedRoots.size == 1 -> "$actualVar = ${cleanedRoots[0]}"
            cleanedRoots.isNotEmpty() -> cleanedRoots.mapIndexed { idx, root -> "${actualVar}_${idx + 1} = $root" }.joinToString(", \\quad ")
            else -> rawTex
        }
    }

    private fun texOf(exprStr: String): String {
        return try {
            val raw = evaluator.eval("TeXForm($exprStr)").toString().removeSurrounding("\"", "\"")
            cleanEngineOutput(raw, isOde = false, solveVars = emptyList())
        } catch (_: Exception) {
            cleanEngineOutput(exprStr, isOde = false, solveVars = emptyList())
        }
    }

    private fun evalRealDouble(exprStr: String): Double? {
        return try {
            val nStr = evaluator.eval("N($exprStr)").toString().trim()
            if (nStr.contains("I") || nStr.contains("Indeterminate") || nStr.contains("ComplexInfinity") || nStr.contains("Infinity")) null
            else nStr.replace("*^", "E").toDoubleOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun symjaExprToStandardMath(symjaStr: String): String {
        var res = symjaStr.trim()
        if (res.isEmpty() || res.contains(Regex("""\bI\b""")) || res.contains("Indeterminate") || res.contains("ComplexInfinity")) {
            return ""
        }
        res = res.replace("ArcSin", "asin")
            .replace("ArcCos", "acos")
            .replace("ArcTan", "atan")
            .replace("Sinh", "sinh")
            .replace("Cosh", "cosh")
            .replace("Tanh", "tanh")
            .replace("Sin", "sin")
            .replace("Cos", "cos")
            .replace("Tan", "tan")
            .replace("Cot", "cot")
            .replace("Sec", "sec")
            .replace("Csc", "csc")
            .replace("Sqrt", "sqrt")
            .replace("Cbrt", "cbrt")
            .replace("Abs", "abs")
            .replace("Log", "ln")
            .replace("Exp", "exp")
            .replace(Regex("""\bPi\b"""), "pi")
            .replace(Regex("""\bE\b"""), "e")
        res = res.replace('[', '(').replace(']', ')')
        return res
    }

    /**
     * Symbolically evaluates a mathematical or calculus expression using Symja CAS
     * and returns the simplified result in standard mathematical syntax (suitable for FastCompiler).
     * Supports:
     * - Leibniz derivatives: d/dx(sin(x)), (d/dx)(sin(x)), \frac{d}{dx}(sin(x)), d^2/dx^2(...)
     * - Function derivatives: diff(sin(x)), derivative(sin(x)), turev(sin(x)), D[sin(x), x]
     * - Integrals: int(2x), integrate(sin(x)), \int 2x dx
     * - Simplifications: simplify(sin(x)^2 + cos(x)^2), expand(...)
     */
    fun simplifyToMathExpression(input: String): String? {
        try {
            var s = input.trim()
            if (s.isEmpty()) return null

            // Strip optional leading function label like "y =", "Y =", "f(x) ="
            s = s.replace(Regex("""^[fFgGhH]_?\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]+\s*"""), "")
            s = s.replace(Regex("""^[yY]\s*(?:\(\s*x\s*\))?\s*[:=]+\s*"""), "").trim()

            // Quick check: does the expression contain any calculus or CAS syntax?
            val hasCasKeyword = Regex("""(?:[d∂]\s*/\s*[d∂]|[d∂]\^[0-9]+|\\frac\{[d∂]\}|\b(diff|derivative|turev|türev|int|integrate|entegral|lim|limit|simplify|expand|factor)\b|[∫∑∏]|D\[)""", RegexOption.IGNORE_CASE).containsMatchIn(s)
            if (!hasCasKeyword) return null

            // Replace any inline Leibniz derivatives: d/dx(body) or (d/dx)(body) -> D[body, x]
            val inlinePat = Regex("""(?:\(?d\s*/\s*d([a-zA-Z])\)?|\\frac\{d\}\{d([a-zA-Z])\})\s*(?:\*\s*)?\(([^()]+(?:\([^()]*\)[^()]*)*)\)""", RegexOption.IGNORE_CASE)
            s = inlinePat.replace(s) { m ->
                val v = m.groupValues[1].ifEmpty { m.groupValues[2] }
                val body = m.groupValues[3].trim()
                "D[$body, $v]"
            }

            // Normalize higher-order or remaining Leibniz representations
            s = normalizeLeibnizDerivatives(s)

            // Convert human calculus expressions to canonical Symja forms
            s = translateHumanCalculus(s)
            s = canonicalizeFunctionCalls(s)
            s = normalizeCalculusArguments(s)

            val isCasCommand = s.startsWith("D[") || s.startsWith("Integrate[") ||
                    s.startsWith("Limit[") || s.startsWith("Simplify[") ||
                    s.startsWith("Expand[") || s.startsWith("Factor[") ||
                    s.contains("D[") || s.contains("Integrate[")

            if (!isCasCommand) {
                val target = extractDerivativeTarget(input)
                if (target != null) {
                    s = "D[${target.first}, ${target.second}]"
                } else {
                    return null
                }
            }

            val evalRes = evaluator.eval(s)
            var resStr = evalRes.toString()

            // If Symja left it unevaluated or returned original command
            if (resStr.startsWith("D[") || resStr.startsWith("Integrate[") || resStr.startsWith("Limit[") ||
                resStr.contains("Hold[") || resStr == s
            ) {
                return null
            }

            if (s.startsWith("D[") || s.contains("D[")) {
                val realSimp = simplifyRealDerivativeResult(resStr)
                if (realSimp != resStr) {
                    try {
                        val finalRes = evaluator.eval(realSimp)
                        resStr = finalRes.toString()
                    } catch (_: Exception) {}
                }
            }

            val standard = symjaExprToStandardMath(resStr)
            return if (standard.isNotBlank()) standard else null
        } catch (_: Throwable) {
            return null
        }
    }

    fun computeFactorialScientific(n: Long): Pair<Double, Long> {
        if (n <= 0) return Pair(1.0, 0L)
        try {
            val symjaLog10 = evaluator.eval("N(LogGamma(${n + 1})/Log(10), 25)").toString().trim()
            if (symjaLog10.isNotEmpty() && !symjaLog10.contains("LogGamma") && !symjaLog10.contains("N(")) {
                val parts = symjaLog10.split(".")
                val exp = parts[0].toLongOrNull()
                if (exp != null && parts.size > 1) {
                    val frac = ("0." + parts[1]).toDoubleOrNull()
                    if (frac != null) {
                        val mantissa = Math.pow(10.0, frac)
                        return Pair(mantissa, exp)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback: Pure Kotlin high-precision Stirling series
        val nD = n.toDouble()
        val pi = Math.PI
        val lnN = Math.log(nD)
        val invN = 1.0 / nD
        val invN2 = invN * invN

        val t1 = (1.0 / 12.0) * invN
        val t3 = (1.0 / 360.0) * invN * invN2
        val t5 = (1.0 / 1260.0) * invN * invN2 * invN2
        val t7 = (1.0 / 1680.0) * invN * invN2 * invN2 * invN2

        val lnFact = nD * (lnN - 1.0) + 0.5 * Math.log(2.0 * pi * nD) + t1 - t3 + t5 - t7
        val ln10 = Math.log(10.0)
        val log10Fact = lnFact / ln10

        val exp = Math.floor(log10Fact).toLong()
        val frac = log10Fact - exp
        val mantissa = Math.pow(10.0, frac)
        return Pair(mantissa, exp)
    }

    fun formatFactorialScientific(n: Long): String {
        val (rawMantissa, rawExp) = computeFactorialScientific(n)
        var mantissa = rawMantissa
        var exp = rawExp
        if (mantissa >= 10.0) {
            mantissa /= 10.0
            exp += 1L
        } else if (mantissa < 1.0) {
            mantissa *= 10.0
            exp -= 1L
        }
        val mantissaStr = String.format(java.util.Locale.US, "%.10f", mantissa).trimEnd('0').trimEnd('.')
        return "$mantissaStr \\times 10^{$exp}"
    }

    fun replaceHoldFactorialWithScientific(expr: String): String {
        val holdPat = Regex("""(?i)(?:\\text\{Hold\}|Hold)\s*(?:\\left)?\s*[\(\[]\s*(?:Factorial\s*[\(\[]\s*)?(\d+)\s*!?(?:\s*[\)\]])?\s*(?:\\right)?\s*[\)\]]""")
        return holdPat.replace(expr) { m ->
            val n = m.groupValues[1].toLongOrNull()
            if (n != null && n > 1000) {
                formatFactorialScientific(n)
            } else {
                m.value
            }
        }
    }

    private fun extractAbsSubexpressions(expr: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        var idx = 0
        while (idx < expr.length) {
            val found = expr.indexOf("Abs[", idx)
            if (found == -1) break
            val closeIdx = findMatchingBrace(expr, found + 3, '[', ']')
            if (closeIdx == -1) break
            val fullToken = expr.substring(found, closeIdx + 1)
            val inner = expr.substring(found + 4, closeIdx)
            list.add(Pair(fullToken, inner))
            idx = found + 4
        }
        return list.distinctBy { it.first }.sortedBy { it.first.length }
    }

    private fun expandAbsSignBranches(expr: String, maxBranches: Int = 16): List<String> {
        val absTokens = extractAbsSubexpressions(expr)
        if (absTokens.isEmpty()) return listOf(expr)
        var branches = listOf(expr)
        for ((fullAbs, absInner) in absTokens) {
            if (branches.size >= maxBranches) break
            val next = mutableListOf<String>()
            for (b in branches) {
                next.add(b.replace(fullAbs, "($absInner)"))
                next.add(b.replace(fullAbs, "(-($absInner))"))
            }
            branches = next.distinct()
        }
        return branches
    }

    private fun extractRealRootsSymja(expr: String, v: String, verifyInOriginal: Boolean = true): List<Pair<String, Double>> {
        val results = mutableListOf<Pair<String, Double>>()
        val rulePat = Regex("""\b${Regex.escape(v)}\s*->\s*([^},]+)""")

        fun collectFromSmooth(smoothExpr: String) {
            val candidates = listOf(
                "Solve(Numerator(Together($smoothExpr)) == 0, $v)",
                "Solve($smoothExpr == 0, $v)"
            )
            for (cmd in candidates) {
                try {
                    val solved = evaluator.eval(cmd).toString()
                    if (solved.contains("->")) {
                        for (m in rulePat.findAll(solved)) {
                            val rSym = m.groupValues[1].trim()
                            val rVal = evalRealDouble(rSym)
                            if (rVal != null && rVal.isFinite()) {
                                if (!verifyInOriginal) {
                                    results.add(Pair(rSym, rVal))
                                } else {
                                    val check = evalRealDouble("Abs(ReplaceAll($expr, $v -> ($rVal)))")
                                    if (check != null && check < 1e-5) {
                                        results.add(Pair(rSym, rVal))
                                    }
                                }
                            }
                        }
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        if (expr.contains("Abs[")) {
            val branches = expandAbsSignBranches(expr)
            for (b in branches) {
                collectFromSmooth(b)
            }
        } else {
            collectFromSmooth(expr)
        }

        return results
            .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
            .sortedBy { it.second }
    }

    private fun splitTopLevelPower(expr: String): Pair<String, String>? {
        var s = expr.trim()
        while (s.startsWith("(") && s.endsWith(")") && findMatchingBrace(s, 0, '(', ')') == s.length - 1) {
            s = s.substring(1, s.length - 1).trim()
        }
        var depthPar = 0
        var depthSqr = 0
        var depthCur = 0
        for (i in s.indices) {
            when (s[i]) {
                '(' -> depthPar++
                ')' -> depthPar--
                '[' -> depthSqr++
                ']' -> depthSqr--
                '{' -> depthCur++
                '}' -> depthCur--
                '^' -> if (depthPar == 0 && depthSqr == 0 && depthCur == 0) {
                    val base = s.substring(0, i).trim()
                    val exp = s.substring(i + 1).trim()
                    if (base.isNotEmpty() && exp.isNotEmpty()) {
                        return Pair(base, exp)
                    }
                }
            }
        }
        return null
    }

    private fun rewriteTrigToQuotients(expr: String): String {
        var s = expr
        val trigFuncs = listOf("Cot", "cot", "Tan", "tan", "Csc", "csc", "Sec", "sec")
        for (fn in trigFuncs) {
            var idx = 0
            while (idx < s.length) {
                val found = s.indexOf(fn, idx, ignoreCase = true)
                if (found == -1) break
                val beforeOk = (found == 0) || (!s[found - 1].isLetterOrDigit() && s[found - 1] != '_')
                if (!beforeOk) {
                    idx = found + fn.length
                    continue
                }
                val afterIdx = found + fn.length
                if (afterIdx < s.length && (s[afterIdx] == '[' || s[afterIdx] == '(')) {
                    val openCh = s[afterIdx]
                    val closeCh = if (openCh == '[') ']' else ')'
                    val closeIdx = findMatchingBrace(s, afterIdx, openCh, closeCh)
                    if (closeIdx != -1) {
                        val arg = s.substring(afterIdx + 1, closeIdx)
                        val fnLower = fn.lowercase()
                        val replacement = when (fnLower) {
                            "cot" -> "(Cos[$arg]/Sin[$arg])"
                            "tan" -> "(Sin[$arg]/Cos[$arg])"
                            "csc" -> "(1/Sin[$arg])"
                            "sec" -> "(1/Cos[$arg])"
                            else -> null
                        }
                        if (replacement != null) {
                            s = s.substring(0, found) + replacement + s.substring(closeIdx + 1)
                            idx = found + replacement.length
                            continue
                        }
                    }
                }
                idx = found + fn.length
            }
        }
        return s
    }

    /**
     * Universal Absolute-Value Integral Solver:
     * Handles ANY definite or indefinite integral containing one or more Abs[...] expressions
     * by symbolically finding roots of each Abs argument via Symja Solve[...] and partitioning the domain.
     * Returns Pair(resolvedSymjaExpr, analyticalStepsHtml).
     */
    private fun solveAbsIntegral(s: String): Pair<String, String>? {
        if (!s.startsWith("Integrate[") || !s.endsWith("]") || !s.contains("Abs[")) return null
        val innerContent = s.substring(10, s.length - 1)
        val args = splitTopLevel(innerContent, ',')
        if (args.size != 2) return null

        val integrand = args[0].trim()
        val varSpec = args[1].trim()
        val absTokens = extractAbsSubexpressions(integrand)
        if (absTokens.isEmpty()) return null

        // Case 1: Definite Integral Integrate[integrand, {v, a, b}]
        if (varSpec.startsWith("{") && varSpec.endsWith("}")) {
            val bounds = splitTopLevel(varSpec.substring(1, varSpec.length - 1), ',')
            if (bounds.size != 3) return null
            val v = bounds[0].trim()
            val aSym = bounds[1].trim()
            val bSym = bounds[2].trim()
            val aVal = evalRealDouble(aSym) ?: return null
            val bVal = evalRealDouble(bSym) ?: return null

            val signFlipRoots = mutableListOf<Pair<String, Double>>()
            for ((_, absInner) in absTokens) {
                signFlipRoots.addAll(extractRealRootsSymja(absInner, v))
            }

            val lo = kotlin.math.min(aVal, bVal)
            val hi = kotlin.math.max(aVal, bVal)
            val interiorRoots = signFlipRoots
                .filter { it.second > lo + 1e-7 && it.second < hi - 1e-7 }
                .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
                .sortedBy { if (aVal <= bVal) it.second else -it.second }

            val partition = mutableListOf<Pair<String, Double>>()
            partition.add(Pair(aSym, aVal))
            partition.addAll(interiorRoots)
            partition.add(Pair(bSym, bVal))

            val subIntegrals = mutableListOf<String>()
            val subIntegralTexPieces = mutableListOf<String>()
            val subIntegralValTexPieces = mutableListOf<String>()

            for (i in 0 until partition.size - 1) {
                val (tStartSym, tStartVal) = partition[i]
                val (tEndSym, tEndVal) = partition[i + 1]
                val midVal = 0.5 * (tStartVal + tEndVal)
                var smoothIntegrand = integrand
                for ((fullAbs, absInner) in absTokens) {
                    val signVal = evalRealDouble("ReplaceAll($absInner, $v -> ($midVal))") ?: 1.0
                    val replacement = if (signVal >= 0.0) "($absInner)" else "(-($absInner))"
                    smoothIntegrand = smoothIntegrand.replace(fullAbs, replacement)
                }
                val pieceCmd = "Integrate[$smoothIntegrand, {$v, $tStartSym, $tEndSym}]"
                subIntegrals.add(pieceCmd)
                val pieceSimpl = try { evaluator.eval("Simplify($smoothIntegrand)").toString() } catch (_: Exception) { smoothIntegrand }
                subIntegralTexPieces.add("\\int_{${texOf(tStartSym)}}^{${texOf(tEndSym)}} \\left(${escapeLatexForHtml(texOf(pieceSimpl))}\\right) \\,d$v")
                val pieceVal = try { evaluator.eval(pieceCmd).toString() } catch (_: Exception) { pieceCmd }
                subIntegralValTexPieces.add(escapeLatexForHtml(texOf(pieceVal)))
            }

            val resolvedCmd = "Simplify[" + subIntegrals.joinToString(" + ") + "]"
            val totalVal = try { evaluator.eval(resolvedCmd).toString() } catch (_: Exception) { "" }

            val steps = java.lang.StringBuilder()
            if (signFlipRoots.isNotEmpty()) {
                steps.append("<b>${Translator.tr("1. Kritik Nokta (Mutlak Değer Kökü):")}</b><br/>")
                val rootsTex = signFlipRoots
                    .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
                    .joinToString(", \\quad ") { "$v = ${escapeLatexForHtml(texOf(it.first))}" }
                steps.append("$$ $rootsTex $$<br/>")
            }
            steps.append("<b>${Translator.tr("3. Parçalı İntegral Ayrımı")}:</b><br/>")
            steps.append("$$ ${subIntegralTexPieces.joinToString(" + ")} = ${subIntegralValTexPieces.joinToString(" + ")} = ${escapeLatexForHtml(texOf(totalVal))} $$")

            return Pair(resolvedCmd, steps.toString())
        }

        // Case 2: Indefinite Integral Integrate[integrand, v]
        val v = varSpec
        if (absTokens.size == 1) {
            val (fullAbs, absInner) = absTokens.first()
            val roots = extractRealRootsSymja(absInner, v)
            val smoothIntegrand = integrand.replace(fullAbs, "($absInner)")
            val anti = evaluator.eval("Integrate($smoothIntegrand, $v)").toString()
            if (!anti.contains("Integrate")) {
                val resolvedCmd = if (roots.size == 1) {
                    val rSym = roots.first().first
                    val antiAtRoot = evaluator.eval("ReplaceAll($anti, $v -> ($rSym))").toString()
                    "Simplify[(($absInner)/Abs[$absInner])*(($anti) - ($antiAtRoot))]"
                } else {
                    "Simplify[(($absInner)/Abs[$absInner])*($anti)]"
                }
                val finalTex = escapeLatexForHtml(texOf(evaluator.eval(resolvedCmd).toString()))
                val steps = java.lang.StringBuilder()
                if (roots.isNotEmpty()) {
                    steps.append("<b>${Translator.tr("1. Kritik Nokta (Mutlak Değer Kökü):")}</b><br/>")
                    val rootsTex = roots.joinToString(", \\quad ") { "$v = ${escapeLatexForHtml(texOf(it.first))}" }
                    steps.append("$$ ${escapeLatexForHtml(texOf(absInner))} = 0 \\implies $rootsTex $$<br/>")
                }
                steps.append("<b>${Translator.tr("2. Belirsiz İntegral (İlkel Fonksiyon):")}</b><br/>")
                steps.append("$$ F($v) = $finalTex + C $$")
                if (roots.size == 1) {
                    val rTex = escapeLatexForHtml(texOf(roots.first().first))
                    val rSym = roots.first().first
                    val antiAtRoot = evaluator.eval("ReplaceAll($anti, $v -> ($rSym))").toString()
                    val posPiece = escapeLatexForHtml(texOf("Simplify(($anti) - ($antiAtRoot))"))
                    val negPiece = escapeLatexForHtml(texOf("Simplify(-(($anti) - ($antiAtRoot)))"))
                    steps.append("<br/><b>${Translator.tr("3. Parçalı Fonksiyon Gösterimi:")}</b><br/>")
                    steps.append("$$ \\begin{cases} $negPiece + C, & $v \\lt $rTex \\\\ $posPiece + C, & $v \\ge $rTex \\end{cases} $$")
                }
                return Pair(resolvedCmd, steps.toString())
            }
        }
        return null
    }

    /**
     * Universal Symja-Powered Single-Variable Inequality Solver:
     * Solves rational, polynomial, radical, and absolute-value inequalities P(x)/Q(x) op 0
     * using exact symbolic roots and poles from Symja's Solve, Together, Numerator, and Denominator.
     */
    private fun solveGeneralInequalitySymja(rawExpr: String, v: String): Pair<String, String>? {
        val m = Regex("""^(.+?)(<=|>=|!=|(?<!-)>|<)(.+)$""").find(rawExpr.trim()) ?: return null
        val lhs = m.groupValues[1].trim()
        val op = m.groupValues[2].trim()
        val rhs = m.groupValues[3].trim()
        if (op == "!=" || lhs.contains("->") || rhs.contains("->") || rawExpr.contains("&&") || rawExpr.contains("||")) return null

        return try {
            val diff = evaluator.eval("Together(($lhs) - ($rhs))").toString()
            val num = evaluator.eval("Numerator($diff)").toString()
            val den = evaluator.eval("Denominator($diff)").toString()
            val denLhs = evaluator.eval("Denominator(Together($lhs))").toString()
            val denRhs = evaluator.eval("Denominator(Together($rhs))").toString()

            val poleRoots = mutableListOf<Pair<String, Double>>()
            if (den != "1" && den != "-1") poleRoots.addAll(extractRealRootsSymja(den, v, verifyInOriginal = false))
            if (denLhs != "1" && denLhs != "-1") poleRoots.addAll(extractRealRootsSymja(denLhs, v, verifyInOriginal = false))
            if (denRhs != "1" && denRhs != "-1") poleRoots.addAll(extractRealRootsSymja(denRhs, v, verifyInOriginal = false))
            val distinctPoles = poleRoots
                .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
                .sortedBy { it.second }

            val zeroRootsRaw = extractRealRootsSymja(num, v, verifyInOriginal = true)
            val distinctZeros = zeroRootsRaw
                .filter { z -> distinctPoles.none { p -> kotlin.math.abs(p.second - z.second) < 1e-5 } }
                .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
                .sortedBy { it.second }

            val absCornerRoots = mutableListOf<Pair<String, Double>>()
            for ((_, absInner) in extractAbsSubexpressions(diff)) {
                absCornerRoots.addAll(extractRealRootsSymja(absInner, v, verifyInOriginal = false))
            }

            val allCritical = (distinctZeros + distinctPoles + absCornerRoots)
                .distinctBy { kotlin.math.round(it.second * 100000.0).toLong() }
                .sortedBy { it.second }

            val inclusive = (op == "<=" || op == ">=")
            val opLatex = when (op) {
                "<=" -> "\\le"
                ">=" -> "\\ge"
                "<" -> "\\lt"
                ">" -> "\\gt"
                else -> op
            }

            fun satisfies(valSign: Double): Boolean {
                return when (op) {
                    "<", "<=" -> valSign < -1e-8
                    ">", ">=" -> valSign > 1e-8
                    else -> false
                }
            }

            fun isPole(rVal: Double) = distinctPoles.any { kotlin.math.abs(it.second - rVal) < 1e-5 }
            fun isZero(rVal: Double) = distinctZeros.any { kotlin.math.abs(it.second - rVal) < 1e-5 } && !isPole(rVal)
            fun pointIncluded(rVal: Double) = inclusive && isZero(rVal) && !isPole(rVal)

            fun evalAt(xVal: Double): Double? {
                return evalRealDouble("ReplaceAll($diff, $v -> ($xVal))")
            }

            if (allCritical.isEmpty()) {
                val testVal = evalAt(0.0) ?: evalAt(1.0) ?: return null
                val solLatex = if (satisfies(testVal)) {
                    "\\text{${Translator.tr("Tüm reel sayılar")} } (\\mathbb{R})"
                } else {
                    "\\text{${Translator.tr("Çözüm kümesi boştur")} } (\\emptyset)"
                }
                return Pair(solLatex, "")
            }

            data class SymSeg(
                val a: Pair<String, Double>?,
                val b: Pair<String, Double>?,
                val incA: Boolean,
                val incB: Boolean
            )

            val validSegs = mutableListOf<SymSeg>()

            val leftTest = allCritical.first().second - 2.345
            val leftVal = evalAt(leftTest)
            if (leftVal != null && satisfies(leftVal)) {
                validSegs.add(SymSeg(null, allCritical.first(), false, pointIncluded(allCritical.first().second)))
            }

            for (i in 0 until allCritical.size - 1) {
                val cA = allCritical[i]
                val cB = allCritical[i + 1]
                val mid = 0.5 * (cA.second + cB.second)
                val midVal = evalAt(mid)
                if (midVal != null && satisfies(midVal)) {
                    validSegs.add(SymSeg(cA, cB, pointIncluded(cA.second), pointIncluded(cB.second)))
                }
            }

            val rightTest = allCritical.last().second + 2.345
            val rightVal = evalAt(rightTest)
            if (rightVal != null && satisfies(rightVal)) {
                validSegs.add(SymSeg(allCritical.last(), null, pointIncluded(allCritical.last().second), false))
            }

            val merged = mutableListOf<SymSeg>()
            for (seg in validSegs) {
                if (merged.isNotEmpty()) {
                    val prev = merged.last()
                    if (prev.b != null && seg.a != null &&
                        kotlin.math.abs(prev.b.second - seg.a.second) < 1e-5 &&
                        (prev.incB || seg.incA)
                    ) {
                        merged[merged.lastIndex] = SymSeg(prev.a, seg.b, prev.incA, seg.incB)
                        continue
                    }
                }
                merged.add(seg)
            }

            // Also include any isolated points where f(r) == 0 for <= or >=
            val isolatedPoints = distinctZeros.filter { z ->
                inclusive && merged.none { seg ->
                    val lo = seg.a?.second ?: Double.NEGATIVE_INFINITY
                    val hi = seg.b?.second ?: Double.POSITIVE_INFINITY
                    z.second >= lo - 1e-5 && z.second <= hi + 1e-5
                }
            }

            val ineqParts = mutableListOf<String>()
            val intervalParts = mutableListOf<String>()

            for (seg in merged) {
                when {
                    seg.a == null && seg.b == null -> {
                        ineqParts.add("$v \\in \\mathbb{R}")
                        intervalParts.add("(-\\infty, \\infty)")
                    }
                    seg.a == null && seg.b != null -> {
                        val bTex = escapeLatexForHtml(texOf(seg.b.first))
                        val cmp = if (seg.incB) "\\le" else "\\lt"
                        val rB = if (seg.incB) "]" else ")"
                        ineqParts.add("$v $cmp $bTex")
                        intervalParts.add("(-\\infty, $bTex$rB")
                    }
                    seg.a != null && seg.b == null -> {
                        val aTex = escapeLatexForHtml(texOf(seg.a.first))
                        val cmp = if (seg.incA) "\\ge" else "\\gt"
                        val lA = if (seg.incA) "[" else "("
                        ineqParts.add("$v $cmp $aTex")
                        intervalParts.add("$lA$aTex, \\infty)")
                    }
                    seg.a != null && seg.b != null -> {
                        val aTex = escapeLatexForHtml(texOf(seg.a.first))
                        val bTex = escapeLatexForHtml(texOf(seg.b.first))
                        val cmpL = if (seg.incA) "\\le" else "\\lt"
                        val cmpR = if (seg.incB) "\\le" else "\\lt"
                        val lA = if (seg.incA) "[" else "("
                        val rB = if (seg.incB) "]" else ")"
                        ineqParts.add("$aTex $cmpL $v $cmpR $bTex")
                        intervalParts.add("$lA$aTex, $bTex$rB")
                    }
                }
            }
            for (iso in isolatedPoints) {
                val isoTex = escapeLatexForHtml(texOf(iso.first))
                ineqParts.add("$v = $isoTex")
                intervalParts.add("\\{$isoTex\\}")
            }

            val solLatex = if (ineqParts.isEmpty()) {
                "\\text{${Translator.tr("Çözüm kümesi boştur")} } (\\emptyset)"
            } else {
                ineqParts.joinToString(" \\;\\lor\\; ") + " \\quad \\left($v \\in " + intervalParts.joinToString(" \\cup ") + "\\right)"
            }

            val steps = java.lang.StringBuilder()
            steps.append("<b>${Translator.tr("1. Standart Form (Sadeleştirilmiş):")}</b><br/>")
            steps.append("$$ ${escapeLatexForHtml(texOf(diff))} $opLatex 0 $$<br/>")
            if (distinctZeros.isNotEmpty()) {
                steps.append("<b>${Translator.tr("2. Kök Analizi")} ($ P($v) = 0 $):</b><br/>")
                steps.append("$$ " + distinctZeros.joinToString(", \\quad ") { "$v = ${escapeLatexForHtml(texOf(it.first))}" } + " $$<br/>")
            }
            if (distinctPoles.isNotEmpty()) {
                steps.append("<b>${Translator.tr("3. Payda Kökleri (Tanımsız Noktalar,")} $ Q($v) \\neq 0 $):</b><br/>")
                steps.append("$$ " + distinctPoles.joinToString(", \\quad ") { "$v \\neq ${escapeLatexForHtml(texOf(it.first))}" } + " $$<br/>")
            }
            steps.append("<b>${Translator.tr("4. İşaret Tablosu ve Çözüm Kümesi:")}</b><br/>")
            steps.append("$$ $solLatex $$")

            Pair(solLatex, steps.toString())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates universal analytical step-by-step explanations directly using Symja's symbolic
     * AST and algebraic functions (Together, Numerator, Denominator, Coefficient, Factor, D, Integrate, Limit, Solve)
     * across ODEs, Limits, Integrals, Equations, and Functions.
     */
    private fun generateUniversalSymbolicSteps(s: String, texStr: String): String? {
        return try {
            // 0. Arbitrary Precision Evaluation: N[expr, prec] or N[expr]
            if (s.startsWith("N[") && s.endsWith("]")) {
                val inner = s.substring(2, s.length - 1).trim()
                val args = splitTopLevel(inner, ',')
                if (args.isNotEmpty()) {
                    val targetExpr = args[0].trim()
                    val prec = if (args.size > 1) args[1].trim() else null

                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Yüksek Duyarlıklı Sayısal Hesaplama:")}</b><br/>")
                    val targetTex = escapeLatexForHtml(texOf(targetExpr))
                    sb.append("$$ $targetTex \\approx ${escapeLatexForHtml(texStr)} $$<br/>")
                    if (prec != null) {
                        sb.append("<b>${Translator.tr("2. Hesaplanmış Basamak Sayısı (Duyarlık):")}</b> $prec ${Translator.tr("basamak")}<br/>")
                    }
                    return sb.toString()
                }
            }

            // 0.5. Differential Equations: DSolve[...]
            if (s.startsWith("DSolve[") && s.endsWith("]")) {
                val inner = s.substring(7, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size >= 3) {
                    val eqArg = args[0].trim()
                    val depSpec = args[1].trim().removeSurrounding("{", "}")
                    val indepVar = args[2].trim()
                    val depVar = depSpec.substringBefore("[").trim()

                    val eqList = if (eqArg.startsWith("{") && eqArg.endsWith("}")) {
                        splitTopLevel(eqArg.substring(1, eqArg.length - 1), ',')
                    } else {
                        listOf(eqArg)
                    }
                    val mainOde = eqList.firstOrNull { it.contains("[$indepVar]") } ?: eqList.firstOrNull { it.contains("'") } ?: eqList.first()
                    val icList = eqList.filter { it != mainOde }

                    if (mainOde.contains("==")) {
                        val odeParts = mainOde.split("==", limit = 2)
                        val rawDiff = "(${odeParts[0]}) - (${odeParts[1]})"
                        val subbed = rawDiff
                            .replace("$depVar'''[$indepVar]", "DY3")
                            .replace("$depVar''[$indepVar]", "DY2")
                            .replace("$depVar'[$indepVar]", "DY1")
                            .replace("$depVar[$indepVar]", "DY0")
                        val expanded = evaluator.eval("Expand($subbed)").toString()
                        val a = evaluator.eval("Simplify(Coefficient($expanded, DY2, 1))").toString()
                        val b = evaluator.eval("Simplify(Coefficient($expanded, DY1, 1))").toString()
                        val c = evaluator.eval("Simplify(Coefficient($expanded, DY0, 1))").toString()
                        val rem = evaluator.eval("Simplify(-(($expanded) - ($a)*DY2 - ($b)*DY1 - ($c)*DY0))").toString()

                        val sb = java.lang.StringBuilder()
                        if (a != "0") {
                            val odeCleanTex = escapeLatexForHtml(texOf(mainOde))
                            val charPoly = evaluator.eval("Expand(($a)*r^2 + ($b)*r + ($c))").toString()
                            val delta = evaluator.eval("Simplify(($b)^2 - 4*($a)*($c))").toString()
                            val charRoots = evaluator.eval("Solve($charPoly == 0, r)").toString()

                            sb.append("<b>${Translator.tr("1. Lineer Sabit Katsayılı Diferansiyel Denklem:")}</b><br/>")
                            sb.append("$$ $odeCleanTex $$<br/>")
                            sb.append("<b>${Translator.tr("2. Karakteristik Denklem")} ($ $depVar = e^{r $indepVar} $):</b><br/>")
                            sb.append("$$ ${escapeLatexForHtml(texOf(charPoly))} = 0, \\quad \\Delta = ${escapeLatexForHtml(texOf(delta))} $$<br/>")
                            if (icList.isNotEmpty()) {
                                val genSol = try {
                                    evaluator.eval("DSolve[$mainOde, $depVar[$indepVar], $indepVar]").toString()
                                } catch (_: Exception) { "" }
                                sb.append("<b>${Translator.tr("3. Karakteristik Kökler ve Genel Çözüm:")}</b><br/>")
                                sb.append("$$ ${escapeLatexForHtml(cleanRootsOutput(texOf(charRoots), "r"))} $$<br/>")
                                if (genSol.isNotEmpty() && !genSol.contains("DSolve")) {
                                    sb.append("$$ ${escapeLatexForHtml(cleanEngineOutput(texOf(genSol), isOde = true))} $$<br/>")
                                }
                                val icTex = icList.joinToString(", ") { escapeLatexForHtml(texOf(it)) }
                                sb.append("<b>${Translator.tr("4. Başlangıç Koşulu Uygulaması")} ($ $icTex $):</b><br/>")
                                sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                            } else {
                                sb.append("<b>${Translator.tr("3. Karakteristik Kökler ve Genel Çözüm:")}</b><br/>")
                                sb.append("$$ ${escapeLatexForHtml(cleanRootsOutput(texOf(charRoots), "r"))} $$")
                                sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                            }
                            return sb.toString()
                        } else if (b != "0") {
                            val pExpr = evaluator.eval("Simplify(($c)/($b))").toString()
                            val intP = evaluator.eval("Integrate($pExpr, $indepVar)").toString()
                            val muExpr = if (!intP.contains("Integrate")) {
                                evaluator.eval("Simplify(Exp($intP))").toString()
                            } else {
                                "Exp(Integrate($pExpr, $indepVar))"
                            }
                            sb.append("<b>${Translator.tr("1. Birinci Mertebe Lineer Diferansiyel Denklem ve İntegral Çarpanı:")}</b><br/>")
                            sb.append("$$ ${escapeLatexForHtml(texOf(mainOde))} \\implies \\mu($indepVar) = e^{\\int ${escapeLatexForHtml(texOf(pExpr))} \\,d$indepVar} = ${escapeLatexForHtml(texOf(muExpr))} $$<br/>")
                            if (icList.isNotEmpty()) {
                                val genSol = try {
                                    evaluator.eval("DSolve[$mainOde, $depVar[$indepVar], $indepVar]").toString()
                                } catch (_: Exception) { "" }
                                if (genSol.isNotEmpty() && !genSol.contains("DSolve")) {
                                    sb.append("<b>${Translator.tr("2. Genel Çözüm:")}</b><br/>")
                                    sb.append("$$ ${escapeLatexForHtml(cleanEngineOutput(texOf(genSol), isOde = true))} $$<br/>")
                                }
                                val icTex = icList.joinToString(", ") { escapeLatexForHtml(texOf(it)) }
                                sb.append("<b>${Translator.tr("3. Başlangıç Koşulu Uygulaması")} ($ $icTex $):</b><br/>")
                                sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                            } else {
                                sb.append("<b>${Translator.tr("2. Genel Çözüm:")}</b><br/>")
                                sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                            }
                            return sb.toString()
                        }
                    }
                }
            }

            // 1. Equations: Solve[lhs == rhs, v]
            if (s.startsWith("Solve[") && !s.contains(", {") && s.endsWith("]")) {
                val inner = s.substring(6, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size == 2 && args[0].contains("==")) {
                    val v = args[1].trim()
                    val eqParts = args[0].split("==", limit = 2)
                    val diff = evaluator.eval("Together((${eqParts[0]}) - (${eqParts[1]}))").toString()
                    val num = evaluator.eval("Expand(Numerator($diff))").toString()
                    val den = evaluator.eval("Simplify(Denominator($diff))").toString()
                    val degStr = evaluator.eval("Exponent($num, $v)").toString()
                    val deg = degStr.toIntOrNull() ?: 0

                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Standart Form (Sadeleştirilmiş):")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(texOf(diff))} = 0 $$<br/>")
                    if (den != "1" && den != "-1") {
                        sb.append("<b>${Translator.tr("3. Payda Kökleri (Tanımsız Noktalar,")} $ Q($v) \\neq 0 $):</b><br/>")
                        sb.append("$$ ${escapeLatexForHtml(texOf(den))} \\neq 0 $$<br/>")
                    }
                    if (deg == 1 && !num.contains("Abs[")) {
                        val a = evaluator.eval("Coefficient($num, $v, 1)").toString()
                        val b = evaluator.eval("Coefficient($num, $v, 0)").toString()
                        val sol = evaluator.eval("Simplify(-($b)/($a))").toString()
                        sb.append("<b>${Translator.tr("2. Birinci Dereceden Denklem Çözümü:")}</b><br/>")
                        sb.append("$$ \\left(${escapeLatexForHtml(texOf(a))}\\right)$v = ${escapeLatexForHtml(texOf("Simplify(-($b))"))} \\implies $v = ${escapeLatexForHtml(texOf(sol))} $$")
                        return sb.toString()
                    } else if (deg == 2 && !num.contains("Abs[")) {
                        val a = evaluator.eval("Coefficient($num, $v, 2)").toString()
                        val b = evaluator.eval("Coefficient($num, $v, 1)").toString()
                        val c = evaluator.eval("Coefficient($num, $v, 0)").toString()
                        val delta = evaluator.eval("Simplify(($b)^2 - 4*($a)*($c))").toString()
                        val deltaVal = evalRealDouble(delta)
                        sb.append("<b>${Translator.tr("2. Katsayılar ve Diskriminant")} ($\\Delta = b^2 - 4ac$):</b><br/>")
                        sb.append("$$ a = ${texOf(a)}, \\quad b = ${texOf(b)}, \\quad c = ${texOf(c)} $$")
                        sb.append("$$ \\Delta = \\left(${texOf(b)}\\right)^2 - 4\\left(${texOf(a)}\\right)\\left(${texOf(c)}\\right) = ${texOf(delta)} $$<br/>")
                        if (deltaVal != null && deltaVal < -1e-9) {
                            sb.append("<b>${Translator.tr("3. Karmaşık (İmajiner) Kökler")} ($\\Delta < 0$):</b><br/>")
                        } else if (deltaVal != null && kotlin.math.abs(deltaVal) <= 1e-9) {
                            sb.append("<b>${Translator.tr("3. Çakışık Tek Reel Kök")} ($\\Delta = 0$):</b><br/>")
                        } else {
                            sb.append("<b>${Translator.tr("3. Kuadratik Formül ile Kökler")} ($\\Delta > 0$):</b><br/>")
                        }
                        sb.append("$$ ${escapeLatexForHtml(cleanRootsOutput(texStr, v))} $$")
                        return sb.toString()
                    } else if (deg >= 3 && !num.contains("Abs[")) {
                        val factored = evaluator.eval("Factor($num)").toString()
                        if (factored != num) {
                            sb.append("<b>2. Factor($v):</b><br/>")
                            sb.append("$$ ${escapeLatexForHtml(texOf(factored))} = 0 $$<br/>")
                        }
                        sb.append("<b>${Translator.tr("2. Rasyonel Kök Teoremi ile Tam Sayı Kökleri:")}</b><br/>")
                        sb.append("$$ ${escapeLatexForHtml(cleanRootsOutput(texStr, v))} $$")
                        return sb.toString()
                    }
                    sb.append("<b>${Translator.tr("2. Genel Çözüm:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(cleanRootsOutput(texStr, v))} $$")
                    return sb.toString()
                }
            }

            // 2. Definite Integrals: Integrate[f, {v, a, b}]
            if (s.startsWith("Integrate[") && s.contains(", {") && s.endsWith("]")) {
                val inner = s.substring(10, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size == 2 && args[1].startsWith("{") && args[1].endsWith("}")) {
                    val f = args[0].trim()
                    val bounds = splitTopLevel(args[1].substring(1, args[1].length - 1), ',')
                    if (bounds.size == 3) {
                        val v = bounds[0].trim()
                        val a = bounds[1].trim()
                        val b = bounds[2].trim()
                        val anti = evaluator.eval("Integrate($f, $v)").toString()
                        if (!anti.contains("Integrate")) {
                            val fb = evaluator.eval("Simplify(ReplaceAll($anti, $v -> ($b)))").toString()
                            val fa = evaluator.eval("Simplify(ReplaceAll($anti, $v -> ($a)))").toString()
                            val sb = java.lang.StringBuilder()
                            sb.append("<b>${Translator.tr("1. Belirsiz İntegral (İlkel Fonksiyon):")}</b><br/>")
                            sb.append("$$ F($v) = \\int ${escapeLatexForHtml(texOf(f))} \\,d$v = ${escapeLatexForHtml(texOf(anti))} + C $$<br/>")
                            sb.append("<b>${Translator.tr("2. Kalkülüsün Temel Teoremi")} ($ F(b) - F(a) $):</b><br/>")
                            sb.append("$$ F(${texOf(b)}) - F(${texOf(a)}) = \\left(${escapeLatexForHtml(texOf(fb))}\\right) - \\left(${escapeLatexForHtml(texOf(fa))}\\right) = ${escapeLatexForHtml(texStr)} $$")
                            return sb.toString()
                        }
                    }
                }
            }

            // 3. Indefinite Integrals: Integrate[f, v]
            if (s.startsWith("Integrate[") && !s.contains(", {") && s.endsWith("]")) {
                val inner = s.substring(10, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size == 2) {
                    val f = args[0].trim()
                    val v = args[1].trim()
                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Belirsiz İntegral (İlkel Fonksiyon):")}</b><br/>")
                    sb.append("$$ F($v) = \\int ${escapeLatexForHtml(texOf(f))} \\,d$v = ${escapeLatexForHtml(texStr)} + C $$")
                    return sb.toString()
                }
            }

            // 3.5. Derivatives: D[f, v] or D[f, {v, n}]
            if (s.startsWith("D[") && s.endsWith("]")) {
                val inner = s.substring(2, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size >= 2) {
                    val f = args[0].trim()
                    val vSpec = args[1].trim()
                    val isHigherOrder = vSpec.startsWith("{") && vSpec.endsWith("}")
                    val v = if (isHigherOrder) vSpec.removeSurrounding("{", "}").substringBefore(",").trim() else vSpec
                    val order = if (isHigherOrder) vSpec.removeSurrounding("{", "}").substringAfter(",").trim() else "1"
                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Türevi Alınacak İfade:")}</b><br/>")
                    val derivOp = if (order != "1") "\\frac{d^{$order}}{d$v^{$order}}" else "\\frac{d}{d$v}"
                    sb.append("$$ $derivOp\\left(${escapeLatexForHtml(texOf(f))}\\right) $$<br/>")
                    sb.append("<b>${Translator.tr("2. Sadeleştirilmiş Türev:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                    return sb.toString()
                }
            }

            // 3.8. Series Expansion: Series[f, {x, x0, n}]
            if (s.startsWith("Series[") && s.endsWith("]")) {
                val inner = s.substring(7, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size >= 2) {
                    val f = args[0].trim()
                    val spec = args[1].trim().removeSurrounding("{", "}")
                    val specParts = splitTopLevel(spec, ',')
                    val v = specParts.getOrNull(0)?.trim() ?: "x"
                    val x0 = specParts.getOrNull(1)?.trim() ?: "0"
                    val n = specParts.getOrNull(2)?.trim() ?: "4"
                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Açılım Bilgileri:")}</b><br/>")
                    sb.append("$$ f($v) = ${escapeLatexForHtml(texOf(f))}, \\quad ${v}_0 = ${escapeLatexForHtml(texOf(x0))}, \\quad n = $n $$<br/>")
                    sb.append("<b>${Translator.tr("2. Taylor / Maclaurin Serisi Genel Formülü:")}</b><br/>")
                    val orderNum = n.toIntOrNull() ?: 4
                    val orderPlus1 = orderNum + 1
                    if (x0 == "0") {
                        sb.append("$$ f($v) = \\sum_{k=0}^{$n} \\frac{f^{(k)}(0)}{k!} $v^k + O($v^{$orderPlus1}) $$<br/>")
                    } else {
                        sb.append("$$ f($v) = \\sum_{k=0}^{$n} \\frac{f^{(k)}($x0)}{k!} ($v - $x0)^k + O(($v - $x0)^{$orderPlus1}) $$<br/>")
                    }
                    sb.append("<b>${Translator.tr("3. Seri Açılımı Sonucu:")}</b><br/>")
                    sb.append("$$ ${escapeLatexForHtml(texStr)} $$")
                    return sb.toString()
                }
            }

            // 4. Limits: Limit[f, v -> a]
            if (s.startsWith("Limit[") && s.endsWith("]")) {
                val inner = s.substring(6, s.length - 1)
                val args = splitTopLevel(inner, ',')
                if (args.size >= 2 && args[1].contains("->")) {
                    val f = args[0].trim()
                    val v = args[1].substringBefore("->").trim()
                    val target = args[1].substringAfter("->").trim()
                    val targetTex = escapeLatexForHtml(texOf(target))

                    val sb = java.lang.StringBuilder()
                    sb.append("<b>${Translator.tr("1. Limit İfadesi:")}</b><br/>")
                    sb.append("$$ \\lim_{$v \\to $targetTex} \\left(${escapeLatexForHtml(texOf(f))}\\right) $$<br/>")

                    // 4A. Check universal power indeterminate forms: u(v)^w(v) (1^\infty, 0^0, \infty^0)
                    val powParts = splitTopLevelPower(f)
                    if (powParts != null) {
                        val (baseExpr, expExpr) = powParts
                        val baseLim = try { evaluator.eval("Limit($baseExpr, $v -> $target)").toString() } catch (_: Exception) { "" }
                        val expLim = try { evaluator.eval("Limit(Abs($expExpr), $v -> $target)").toString() } catch (_: Exception) { "" }
                        if (baseLim == "1" && expLim.contains("Infinity")) {
                            val prodExpr = evaluator.eval("Simplify(($expExpr) * (($baseExpr) - 1))").toString()
                            val kLim = evaluator.eval("Limit($prodExpr, $v -> $target)").toString()
                            sb.append("<b>${Translator.tr("2. Belirsizlik Analizi ve Yaklaşım:")}</b><br/>")
                            sb.append("${Translator.tr("Euler Limit Kuralı")} ($ 1^{\\infty} $):<br/>")
                            sb.append("$$ \\lim_{$v \\to $targetTex} u($v)^{w($v)} = \\exp\\left(\\lim_{$v \\to $targetTex} w($v)\\cdot(u($v) - 1)\\right) $$<br/>")
                            sb.append("$$ \\lim_{$v \\to $targetTex} \\left(${escapeLatexForHtml(texOf(prodExpr))}\\right) = ${escapeLatexForHtml(texOf(kLim))} \\implies L = ${escapeLatexForHtml(texStr)} $$")
                            return sb.toString()
                        } else if ((baseLim == "0" && expLim == "0") || (baseLim.contains("Infinity") && expLim == "0")) {
                            val formTag = if (baseLim == "0") "0^0" else "\\infty^0"
                            val prodExpr = evaluator.eval("Simplify(($expExpr) * Log($baseExpr))").toString()
                            val kLim = evaluator.eval("Limit($prodExpr, $v -> $target)").toString()
                            sb.append("<b>${Translator.tr("2. Belirsizlik Analizi ve Yaklaşım:")}</b><br/>")
                            sb.append("${Translator.tr("Üstel Belirsizlik")} ($ $formTag $):<br/>")
                            sb.append("$$ \\lim_{$v \\to $targetTex} u($v)^{w($v)} = \\exp\\left(\\lim_{$v \\to $targetTex} w($v)\\cdot\\ln(u($v))\\right) $$<br/>")
                            sb.append("$$ \\lim_{$v \\to $targetTex} \\left(${escapeLatexForHtml(texOf(prodExpr))}\\right) = ${escapeLatexForHtml(texOf(kLim))} \\implies L = ${escapeLatexForHtml(texStr)} $$")
                            return sb.toString()
                        }
                    }

                    // 4B. Check universal 0/0, \infty/\infty, 0 * \infty, or \infty - \infty indeterminate form
                    val trigExpanded = rewriteTrigToQuotients(f)
                    val rawNum = try { evaluator.eval("Numerator($trigExpanded)").toString() } catch (_: Exception) { "" }
                    val rawDen = try { evaluator.eval("Denominator($trigExpanded)").toString() } catch (_: Exception) { "" }
                    val isRawFraction = rawDen.isNotEmpty() && rawDen != "1" && rawDen != "-1"

                    val togetherF = evaluator.eval("Together($trigExpanded)").toString()
                    var num = if (isRawFraction) rawNum else evaluator.eval("Numerator($togetherF)").toString()
                    var den = if (isRawFraction) rawDen else evaluator.eval("Denominator($togetherF)").toString()

                    var isZeroTimesInf = false
                    var isInfMinusInf = false

                    // Check if original expression is a difference of infinite terms: \infty - \infty
                    val termListStr = try { evaluator.eval("Apply(List, $f)").toString() } catch (_: Exception) { "" }
                    if (termListStr.startsWith("{") && termListStr.endsWith("}")) {
                        val terms = splitTopLevel(termListStr.substring(1, termListStr.length - 1), ',')
                        if (terms.size >= 2) {
                            val infCount = terms.count { term ->
                                val lim = try { evaluator.eval("Limit(Abs($term), $v -> $target)").toString() } catch (_: Exception) { "" }
                                lim.contains("Infinity")
                            }
                            if (infCount >= 2) {
                                isInfMinusInf = true
                            }
                        }
                    }

                    if (den == "1" || den == "-1") {
                        // Check if f is a product of factors u * w where u -> 0 and w -> \infty
                        val factorListStr = try { evaluator.eval("Apply(List, $f)").toString() } catch (_: Exception) { "" }
                        if (factorListStr.startsWith("{") && factorListStr.endsWith("}")) {
                            val factors = splitTopLevel(factorListStr.substring(1, factorListStr.length - 1), ',')
                            if (factors.size >= 2) {
                                val zeroFactor = factors.firstOrNull { factor ->
                                    val lim = try { evaluator.eval("Limit($factor, $v -> $target)").toString() } catch (_: Exception) { "" }
                                    lim == "0"
                                }
                                val infFactor = factors.firstOrNull { factor ->
                                    val lim = try { evaluator.eval("Limit(Abs($factor), $v -> $target)").toString() } catch (_: Exception) { "" }
                                    lim.contains("Infinity")
                                }
                                if (zeroFactor != null && infFactor != null) {
                                    isZeroTimesInf = true
                                    val otherFactors = factors.filter { it != zeroFactor }
                                    val infPart = if (otherFactors.size == 1) otherFactors[0] else otherFactors.joinToString(" * ")
                                    num = infPart
                                    den = "(1/($zeroFactor))"
                                }
                            }
                        }
                    } else if (trigExpanded != f && !isInfMinusInf) {
                        isZeroTimesInf = true
                    }

                    if (den != "1" && den != "-1") {
                        val limNum = try { evaluator.eval("Limit($num, $v -> $target)").toString() } catch (_: Exception) { "" }
                        val limDen = try { evaluator.eval("Limit($den, $v -> $target)").toString() } catch (_: Exception) { "" }
                        val isZeroOverZero = (limNum == "0" && limDen == "0")
                        val isInfOverInf = (limNum.contains("Infinity") && limDen.contains("Infinity"))
                        if (isZeroOverZero || isInfOverInf) {
                            sb.append("<b>${Translator.tr("2. Belirsizlik Analizi ve Yaklaşım:")}</b><br/>")
                            val formTag = if (isZeroOverZero) "\\frac{0}{0}" else "\\frac{\\infty}{\\infty}"
                            if (isInfMinusInf) {
                                sb.append("${Translator.tr("Ortak Payda ve Belirsizlik Dönüşümü")} ($ \\infty - \\infty \\to $formTag $):<br/>")
                                sb.append("$$ \\lim_{$v \\to $targetTex} \\left(${escapeLatexForHtml(texOf(f))}\\right) = \\lim_{$v \\to $targetTex} \\frac{${escapeLatexForHtml(texOf(num))}}{${escapeLatexForHtml(texOf(den))}} \\quad \\left($formTag\\right) $$<br/>")
                            } else if (isZeroTimesInf) {
                                sb.append("${Translator.tr("Çarpım Belirsizliği Dönüşümü")} ($ 0 \\cdot \\infty \\to $formTag $):<br/>")
                                sb.append("$$ \\lim_{$v \\to $targetTex} \\left(${escapeLatexForHtml(texOf(f))}\\right) = \\lim_{$v \\to $targetTex} \\frac{${escapeLatexForHtml(texOf(num))}}{${escapeLatexForHtml(texOf(den))}} \\quad \\left($formTag\\right) $$<br/>")
                            } else {
                                sb.append("$$ \\lim_{$v \\to $targetTex} \\frac{${escapeLatexForHtml(texOf(num))}}{${escapeLatexForHtml(texOf(den))}} \\quad \\left($formTag\\right) $$<br/>")
                            }
                            sb.append("${Translator.tr("L'Hôpital Kuralı (Pay ve paydanın türevi alınır):")}<br/>")
                            var curNum = num
                            var curDen = den
                            for (stepIdx in 1..4) {
                                val dNum = evaluator.eval("D($curNum, $v)").toString()
                                val dDen = evaluator.eval("D($curDen, $v)").toString()

                                // Keep fraction unified without hidden denominators like Csc, Sec, Cot, Tan
                                val dNumExp = rewriteTrigToQuotients(dNum)
                                val dDenExp = rewriteTrigToQuotients(dDen)
                                val fracCombined = evaluator.eval("Together(($dNumExp) / ($dDenExp))").toString()
                                val nextNum = evaluator.eval("Numerator($fracCombined)").toString()
                                val nextDen = evaluator.eval("Denominator($fracCombined)").toString()

                                curNum = if (nextDen != "1" && nextDen != "-1") nextNum else evaluator.eval("Simplify($dNum)").toString()
                                curDen = if (nextDen != "1" && nextDen != "-1") nextDen else evaluator.eval("Simplify($dDen)").toString()

                                val nNext = try { evaluator.eval("Limit($curNum, $v -> $target)").toString() } catch (_: Exception) { "" }
                                val dNext = try { evaluator.eval("Limit($curDen, $v -> $target)").toString() } catch (_: Exception) { "" }
                                val stillZero = (nNext == "0" && dNext == "0")
                                val stillInf = (nNext.contains("Infinity") && dNext.contains("Infinity"))
                                if ((stillZero || stillInf) && stepIdx < 4) {
                                    val tag = if (stillZero) "\\frac{0}{0}" else "\\frac{\\infty}{\\infty}"
                                    sb.append("$$ \\lim_{$v \\to $targetTex} \\frac{${escapeLatexForHtml(texOf(curNum))}}{${escapeLatexForHtml(texOf(curDen))}} \\quad ($tag) $$<br/>")
                                } else {
                                    sb.append("$$ \\lim_{$v \\to $targetTex} \\frac{${escapeLatexForHtml(texOf(curNum))}}{${escapeLatexForHtml(texOf(curDen))}} = ${escapeLatexForHtml(texStr)} $$")
                                    break
                                }
                            }
                            return sb.toString()
                        }
                    }

                    // 4C. Check if Direct Substitution is truly valid and defined
                    val directVal = try { evaluator.eval("ReplaceAll($f, $v -> $target)").toString() } catch (_: Exception) { "" }
                    val isDirectSubValid = directVal.isNotEmpty() &&
                        !directVal.contains("Indeterminate") &&
                        !directVal.contains("Infinity") &&
                        !directVal.contains("DirectedInfinity") &&
                        !directVal.contains("ComplexInfinity") &&
                        !directVal.contains("1/0") &&
                        !directVal.contains("DivideByZero") &&
                        !directVal.contains("Limit") &&
                        !directVal.contains("ReplaceAll") &&
                        !directVal.contains("D[") &&
                        try {
                            val diff = evaluator.eval("Simplify(($directVal) - ($texStr))").toString()
                            diff == "0" || diff == "0.0" || diff == "0."
                        } catch (_: Exception) { false }

                    if (isDirectSubValid) {
                        sb.append("<b>${Translator.tr("2. Doğrudan Yerine Koyma:")}</b><br/>")
                        sb.append("$$ f($targetTex) = ${escapeLatexForHtml(texOf(directVal))} \\implies L = ${escapeLatexForHtml(texStr)} $$")
                        return sb.toString()
                    }

                    // 4D. Fallback to analytical limit evaluation
                    sb.append("<b>${Translator.tr("2. Analitik Limit Değerlendirmesi:")}</b><br/>")
                    sb.append("$$ L = ${escapeLatexForHtml(texStr)} $$")
                    return sb.toString()
                }
            }

            // 5. Standalone single-variable function / expression analysis (e.g. |x^2 - 4| - 2 or x^3 - 3x)
            val freeVars = Regex("""\b([a-zA-Z])\b""").findAll(s)
                .map { it.groupValues[1] }
                .filter { it !in listOf("E", "I", "Pi", "Degree", "e", "pi", "C") }
                .distinct()
                .toList()
            val isPlainExpr = !s.contains("[") || s.contains("Abs[") || s.contains("Sin[") || s.contains("Cos[") ||
                              s.contains("Tan[") || s.contains("Log[") || s.contains("Exp[") || s.contains("Sqrt[")
            val isCommandExpr = s.startsWith("Solve[") || s.startsWith("Reduce[") || s.startsWith("DSolve[") ||
                                s.startsWith("Integrate[") || s.startsWith("Limit[") || s.startsWith("D[") ||
                                s.startsWith("Sum[") || s.startsWith("Product[") || s.startsWith("Series[") ||
                                s.startsWith("Det[") || s.startsWith("Inverse[") || s.startsWith("Transpose[") ||
                                s.startsWith("Eigen") || s.startsWith("Laplace") || s.startsWith("InverseLaplace") ||
                                s.startsWith("{{") || s.startsWith("N[")
            if (freeVars.size == 1 && isPlainExpr && !isCommandExpr) {
                val v = freeVars.first()
                val roots = extractRealRootsSymja(s, v, verifyInOriginal = true)
                val deriv = try { evaluator.eval("Simplify(D($s, $v))").toString() } catch (_: Exception) { "" }
                val sb = java.lang.StringBuilder()
                sb.append("<b>${Translator.tr("1. Sadeleştirilmiş Cebirsel İfade:")}</b><br/>")
                sb.append("$$ f($v) = ${escapeLatexForHtml(texStr)} $$<br/>")
                if (roots.isNotEmpty()) {
                    sb.append("<b>${Translator.tr("2. Kök Analizi")} ($ f($v) = 0 $):</b><br/>")
                    sb.append("$$ " + roots.joinToString(", \\quad ") { "$v = ${escapeLatexForHtml(texOf(it.first))}" } + " $$<br/>")
                }
                if (deriv.isNotEmpty() && !deriv.contains("D[") && !deriv.contains("Derivative")) {
                    sb.append("<b>3. $ f'($v) $:</b><br/>")
                    sb.append("$$ f'($v) = ${escapeLatexForHtml(texOf(deriv))} $$")
                }
                return sb.toString()
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    fun translateAndEvaluate(input: String, aiEnabled: Boolean = true, localStepsHtml: String? = null): String {
        return try {
            // Strip optional leading function label like "f1(x):", "f(x) =", "y =" (when not an ODE)
            var cleanedInput = input.trim()
            cleanedInput = normalizeInputText(cleanedInput)
            cleanedInput = cleanedInput.replace(Regex("""^[fFgGhH]_?\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]+\s*"""), "")
            if (!cleanedInput.contains("'") && !cleanedInput.contains("′") && !cleanedInput.contains("’")) {
                cleanedInput = cleanedInput.replace(Regex("""^y\s*(?:\(\s*x\s*\))?\s*[:=]+\s*"""), "")
            }

            // 0. Detect explicit Diophantine intent in input
            val isExplicitDiophantine = Regex("""(?i)\b(diophantine|diophant|tam\s*say[ıi]|tamsay[ıi]|integers?|enteros?|ganze\s*zahlen?|entiers?|цел(ые|ых)\s*числ(а|ах)|الأعداد\s*الصحيحة)\b|\\mathbb\{[zZ]\}|[∈\u2208]\s*[Zℤ]""").containsMatchIn(cleanedInput)

            var isLargeFactorialMode = false
            var largeFactStepsHtml: String? = null
            var largeFactExpr: String? = null
            var largeFactN: Long? = null
            var isPdeMode = false
            var isPdeUnsolved = false
            var pdeStepsHtml: String? = null
            var isOdeMode = false
            var odeStepsHtml: String? = null
            var isInequalityMode = false
            var isMultiVarEquationMode = false
            var isDerivativeMode = false
            var isLimitMode = false
            var isComplexMode = false
            var isMatrixMode = false
            var isSumOrProductMode = false
            var isAbsIndefiniteIntegral = false
            var isAbsDefiniteIntegral = false
            var isDefiniteIntegralMode = false
            var isIndefiniteIntegralMode = false
            var isMultipleIntegralMode = false
            var isLaplaceMode = false
            var isInverseLaplaceMode = false
            var isGammaMode = false
            var isEigenMode = false
            var isComplexExpandMode = false
            var isEquationSystemMode = false
            var isSeriesMode = false
            var absVarName = "x"
            var rawInequalityExpr: String? = null
            var inequalityStepsHtml: String? = null
            var absIntegralStepsHtml: String? = null
            var multiVarAlgTex: String? = null
            var multiVarDiophTex: String? = null
            var multiVarDiophNoSolution = false
            var vars: List<String> = emptyList()

            // 0.3. Check Large Factorial (> 1000) for scientific notation
            val largeFactMatch = Regex("""^(?:(?:Factorial|fact)\s*[\(\[]\s*(\d+)\s*[\)\]]|(\d+)\s*!)$""", RegexOption.IGNORE_CASE).find(cleanedInput)
            val parsedFactN = largeFactMatch?.let { m ->
                val nStr = m.groupValues[1].ifEmpty { m.groupValues[2] }
                nStr.toLongOrNull()
            }
            if (parsedFactN != null && parsedFactN > 1000) {
                isLargeFactorialMode = true
                largeFactN = parsedFactN
                largeFactExpr = "$parsedFactN!"
                val formattedSci = formatFactorialScientific(parsedFactN)
                val (_, exp) = computeFactorialScientific(parsedFactN)
                val totalDigits = exp + 1
                val sb = java.lang.StringBuilder()
                sb.append("<b>${Translator.tr("1. Büyük Faktöriyel Analizi (n > 1000):")}</b><br/>")
                sb.append("$$ $parsedFactN! \\approx $formattedSci $$<br/>")
                sb.append("<b>${Translator.tr("2. Basamak Sayısı Hesabı:")}</b><br/>")
                sb.append("${Translator.tr("Bu sayının 10 tabanındaki basamak sayısı:")}<br/>")
                sb.append("$$ \\lfloor \\log_{10}($parsedFactN!) \\rfloor + 1 = $exp + 1 = $totalDigits \\text{ ${Translator.tr("basamak")}} $$<br/>")
                sb.append("<b>${Translator.tr("3. Stirling Asimptotik Yaklaşımı:")}</b><br/>")
                sb.append("$$ \\ln(n!) \\approx n \\ln(n) - n + \\frac{1}{2}\\ln(2\\pi n) + \\frac{1}{12n} $$<br/>")
                sb.append("$$ $parsedFactN! \\approx $formattedSci $$")
                largeFactStepsHtml = sb.toString()
            }

            // 0.4. Check Partial Differential Equation (PDE) mapping
            val pdeResolved = if (isLargeFactorialMode) null else preprocessPartialDifferentialEquation(cleanedInput)
            val (odeExpr, isOde) = if (isLargeFactorialMode || pdeResolved != null) Pair("", false) else preprocessDifferentialEquation(cleanedInput)
            var s: String
            if (isLargeFactorialMode) {
                s = cleanedInput
            } else if (pdeResolved != null) {
                isPdeMode = true
                isPdeUnsolved = !pdeResolved.third
                s = cleanedInput
                pdeStepsHtml = pdeResolved.second
            } else if (isOde) {
                isOdeMode = true
                s = odeExpr
            } else {
                // 0.7. Preprocess implicit multiplication & trigonometric lexer (e.g. sin x cos x -> sin(x)*cos(x))
                val preprocessed = if (cleanedInput.startsWith("D[")) cleanedInput else preprocessImplicitMultiplication(cleanedInput)

                // 1. Translate human-friendly calculus (int, lim, sum, diff) & LaTeX structures
                s = translateHumanCalculus(preprocessed)
                if (s.startsWith("Integrate[Integrate[")) {
                    isMultipleIntegralMode = true
                } else if (s.startsWith("Integrate[")) {
                    if (s.contains(", {")) {
                        isDefiniteIntegralMode = true
                    } else {
                        isIndefiniteIntegralMode = true
                    }
                }
                if (s.startsWith("LaplaceTransform[")) {
                    isLaplaceMode = true
                }
                if (s.startsWith("InverseLaplaceTransform[")) {
                    isInverseLaplaceMode = true
                }
                if (s.startsWith("Gamma[")) {
                    isGammaMode = true
                }
                if (s.startsWith("Eigenvalues[") || s.startsWith("Eigenvectors[") || s.startsWith("Eigensystem[")) {
                    isEigenMode = true
                }
                if (s.startsWith("ComplexExpand[")) {
                    isComplexExpandMode = true
                }
                if (s.startsWith("D[")) {
                    isDerivativeMode = true
                }
                if (s.startsWith("Series[")) {
                    isSeriesMode = true
                }
                if (s.startsWith("Limit[")) {
                    isLimitMode = true
                }
                if (s.startsWith("Sum[") || s.startsWith("Product[")) {
                    isSumOrProductMode = true
                }
                if (s.startsWith("Arg[") || s.startsWith("Re[") || s.startsWith("Im[") || s.startsWith("Conjugate[")) {
                    isComplexMode = true
                }
                if (s.startsWith("Det[") || s.startsWith("Inverse[") || s.startsWith("Transpose[") || s.startsWith("Tr[") || s.startsWith("{{")) {
                    isMatrixMode = true
                }
                
                // 1.5. Denest radicals (e.g. sqrt(7 + 4*sqrt(3)) -> 2 + Sqrt[3])
                s = denestRadicals(s)
            
            // 2. Standalone list brackets to curly braces (without breaking Mathematica function calls)
            s = convertListBrackets(s)
            
            // 2.3. Implicit multiplication for numbers & parentheses: 5(3) -> 5*(3) (not inside identifiers like Log10)
            s = s.replace(Regex("""\b(\d+(?:\.\d+)?)\s*\("""), "$1*(")
                 .replace(Regex("""\)\s*(\d)"""), ")*$1")
                 .replace(Regex("""\)\s*\("""), ")*(")
            
            // 3. Single = to == (equations, preserving <=, >=, !=, ==)
            s = s.replace(Regex("(?<![=!<>])=(?![=>])"), "==")

            // 3.5. Simplify exponential equations (e.g. 2^x == 4^y -> x == 2*y)
            s = preprocessExponentialEquations(s)
            
            // Degree & symbols support
            s = s.replace("°", "*Degree")
                 .replace(Regex("(?i)\\b(deg|degree)\\b"), "Degree")
                 .replace("π", "Pi")
                 .replace("√", "Sqrt")
            
            // Constants
            s = s.replace(Regex("(?i)\\b(pi)\\b"), "Pi")
                 .replace(Regex("\\b(e)\\b"), "E")
                 .replace(Regex("(?i)\\b(infinity|inf)\\b"), "Infinity")
            
            // Canonicalize every function call (any case / alias) and convert to Symja brackets
            s = canonicalizeFunctionCalls(s)
            
            // Denest radicals again
            s = denestRadicals(s)

            // 2.5. Normalize iterator / limit argument forms by balanced argument parsing
            s = normalizeCalculusArguments(s)
            
            // 2.6. Equations & Inequalities & Diophantine
            if (s.startsWith("Series[")) isSeriesMode = true
            if (s.startsWith("Limit[")) isLimitMode = true
            if (s.startsWith("Sum[") || s.startsWith("Product[")) isSumOrProductMode = true
            if (s.startsWith("Arg[") || s.startsWith("Re[") || s.startsWith("Im[") || s.startsWith("Conjugate[")) isComplexMode = true
            if (s.startsWith("Det[") || s.startsWith("Inverse[") || s.startsWith("Transpose[") || s.startsWith("Tr[") || s.startsWith("{{")) isMatrixMode = true
            if (s.startsWith("Eigenvalues[") || s.startsWith("Eigenvectors[") || s.startsWith("Eigensystem[")) isEigenMode = true
            if (s.startsWith("ComplexExpand[")) isComplexExpandMode = true
            if (s.startsWith("Solve[") && s.contains(", {")) isEquationSystemMode = true
            if (s.startsWith("LaplaceTransform[")) isLaplaceMode = true
            if (s.startsWith("InverseLaplaceTransform[")) isInverseLaplaceMode = true
            if (s.startsWith("Gamma[")) isGammaMode = true
            if (s.startsWith("Integrate[Integrate[")) isMultipleIntegralMode = true
            val isExplicitCall = s.startsWith("Solve[") || s.startsWith("Reduce[") || s.startsWith("DSolve[") ||
                                 s.startsWith("Integrate[") || s.startsWith("Limit[") || s.startsWith("Sum[") ||
                                 s.startsWith("Product[") || s.startsWith("Series[") || s.startsWith("D[") ||
                                 s.startsWith("Arg[") || s.startsWith("Re[") || s.startsWith("Im[") || s.startsWith("Conjugate[") ||
                                 s.startsWith("Det[") || s.startsWith("Inverse[") || s.startsWith("Transpose[") || s.startsWith("Tr[") ||
                                 s.startsWith("Eigenvalues[") || s.startsWith("Eigenvectors[") || s.startsWith("Eigensystem[") || s.startsWith("ComplexExpand[") || s.startsWith("{{") ||
                                 s.startsWith("LaplaceTransform[") || s.startsWith("InverseLaplaceTransform[") || s.startsWith("Gamma[") ||
                                 s.startsWith("N[")
            if (!isExplicitCall) {
                s = simplifyFloorCeil(s)
            }
            val hasInequality = Regex("""(?<!-)>|<|<=|>=|!=|≠|≤|≥""").containsMatchIn(s)
            val hasEquality = s.contains("==")
            
            // Find single-letter variables: [a-zA-Z], excluding mathematical constants
            vars = Regex("""\b([a-zA-Z])\b""").findAll(s)
                .map { it.groupValues[1] }
                .filter { it !in listOf("E", "I", "Pi", "Degree", "e", "pi", "N") }
                .distinct()
                .toList()
            val varsArg = if (vars.size == 1) vars.first() else if (vars.size > 1) "{" + vars.joinToString(", ") + "}" else ""

            if (!isExplicitCall && hasInequality) {
                isInequalityMode = true
                rawInequalityExpr = s
                if (varsArg.isNotEmpty()) {
                    val domain = if (isExplicitDiophantine) ", Integers" else ", Reals"
                    s = "Reduce[$s, $varsArg$domain]"
                }
            } else if (!isExplicitCall && isExplicitDiophantine) {
                if (varsArg.isNotEmpty()) {
                    s = "Reduce[$s, $varsArg, Integers]"
                }
            } else if (!isExplicitCall && hasEquality) {
                val eqList = splitTopLevel(s, ',').flatMap { part ->
                    part.split(Regex("""\s*(?:&&|\b(?:and|ve|und|et)\b)\s*""", RegexOption.IGNORE_CASE))
                }.map { it.trim() }.filter { it.isNotEmpty() && it.contains("==") }

                if (eqList.size >= 2) {
                    isEquationSystemMode = true
                    s = "Solve[{" + eqList.joinToString(", ") + "}, $varsArg]"
                } else if (vars.size >= 2) {
                    isMultiVarEquationMode = true
                    val solveVar = if (vars.contains("y")) "y" else vars.first()
                    val algExpr = "Solve[$s, $solveVar]"
                    try {
                        val algRes = evaluator.eval(algExpr)
                        multiVarAlgTex = evaluator.eval("TeXForm($algRes)").toString().removeSurrounding("\"", "\"")
                    } catch (_: Exception) {}
                    
                    try {
                        val diophExpr = "Reduce[$s, $varsArg, Integers]"
                        val diophRes = evaluator.eval(diophExpr)
                        val dStr = diophRes.toString()
                        if (dStr == "False") {
                            multiVarDiophNoSolution = true
                        } else if (!dStr.startsWith("Reduce[") && !dStr.startsWith("Reduce(") && dStr != diophExpr) {
                            multiVarDiophTex = evaluator.eval("TeXForm($diophRes)").toString().removeSurrounding("\"", "\"")
                        }
                    } catch (_: Exception) {}
                } else if (vars.isNotEmpty()) {
                    val solveVar = if (vars.contains("x")) "x" else vars.first()
                    s = "Solve[$s, $solveVar]"
                }
            }
        }
        
        val originalSymjaCmd = s
        val absIntegralResolved = solveAbsIntegral(s)
        if (absIntegralResolved != null) {
            if (s.contains(", {")) isAbsDefiniteIntegral = true else isAbsIndefiniteIntegral = true
            val vMatch = Regex("""\b([a-zA-Z])\b""").find(s)
            if (vMatch != null) absVarName = vMatch.groupValues[1]
            s = absIntegralResolved.first
            absIntegralStepsHtml = absIntegralResolved.second
        } else if (s.startsWith("Integrate[Integrate[")) {
            isMultipleIntegralMode = true
        } else if (s.startsWith("Integrate[")) {
            if (s.contains(", {")) {
                isDefiniteIntegralMode = true
            } else {
                isIndefiniteIntegralMode = true
            }
        }
        
        // 3. Evaluate the translated string
        var texStr = ""
        var resStr = ""
            if (isLargeFactorialMode && largeFactN != null) {
                val formattedSci = formatFactorialScientific(largeFactN)
                texStr = formattedSci
                resStr = formattedSci
            } else if (isPdeMode && pdeResolved != null) {
                texStr = pdeResolved.first
                resStr = pdeResolved.first
            } else if (isMultiVarEquationMode && multiVarAlgTex != null) {
                texStr = multiVarAlgTex!!
                resStr = multiVarAlgTex!!
            } else if (isInequalityMode && !isExplicitDiophantine && vars.size == 1 && rawInequalityExpr != null && !rawInequalityExpr!!.contains("Floor") && !rawInequalityExpr!!.contains("Ceiling")) {
                val genIneq = solveGeneralInequalitySymja(rawInequalityExpr!!, vars.first())
                if (genIneq != null) {
                    texStr = genIneq.first
                    resStr = genIneq.first
                    inequalityStepsHtml = genIneq.second
                }
            }

            if (texStr.isEmpty()) {
                var result: Any? = null
                if (s.startsWith("D[")) {
                    isDerivativeMode = true
                    try {
                        val realCmd = prepareRealDomainDerivative(s)
                        val rawRes = evaluator.eval(realCmd)
                        val simpRes = simplifyRealDerivativeResult(rawRes.toString())
                        result = evaluator.eval(simpRes)
                    } catch (_: Exception) {
                        result = evaluator.eval(s)
                    }
                } else if (isOdeMode) {
                    var odeResult: Any? = null
                    try {
                        val r = evaluator.eval(s)
                        if (!r.toString().contains("DSolve")) {
                            odeResult = r
                        }
                    } catch (_: Exception) {}

                    if (odeResult == null) {
                        try {
                            val dNotation = convertOdeToDNotation(s)
                            val r = evaluator.eval(dNotation)
                            if (!r.toString().contains("DSolve")) {
                                odeResult = r
                            }
                        } catch (_: Exception) {}
                    }

                    if (odeResult == null) {
                        val analytical = solveOdeAnalytically(s)
                        if (analytical != null) {
                            texStr = analytical.first
                            resStr = analytical.first
                            odeStepsHtml = analytical.second
                        }
                    }

                    if (texStr.isEmpty()) {
                        result = odeResult ?: s
                    }
                } else {
                    try {
                        result = evaluator.eval(s)
                    } catch (e: Exception) {
                        // Fallback for Reduce[..., Reals] if Reals domain throws
                        if (s.startsWith("Reduce[") && s.endsWith(", Reals]")) {
                            val fallback = s.removeSuffix(", Reals]") + "]"
                            result = evaluator.eval(fallback)
                        } else {
                            throw e
                        }
                    }
                }
                if (result != null) {
                    resStr = result.toString()
                }
                if (isDerivativeMode) {
                    val realSimp = simplifyRealDerivativeResult(resStr)
                    if (realSimp != resStr) {
                        try {
                            val finalRes = evaluator.eval(realSimp)
                            resStr = finalRes.toString()
                            result = finalRes
                        } catch (_: Exception) {}
                    }
                }

                // If Solve[...] contains Abs[...] and remained unevaluated, solve via universal sign-branch root extraction
                if (s.startsWith("Solve[") && s.contains("Abs[") && resStr.contains("Solve") && vars.size == 1) {
                    val v = vars.first()
                    val inner = s.substring(6, s.length - 1)
                    val args = splitTopLevel(inner, ',')
                    if (args.size == 2 && args[0].contains("==")) {
                        val eqParts = args[0].split("==", limit = 2)
                        val roots = extractRealRootsSymja("(${eqParts[0]}) - (${eqParts[1]})", v, verifyInOriginal = true)
                        if (roots.isNotEmpty()) {
                            val formattedRules = "{{" + roots.joinToString("}, {") { "$v -> ${it.first}" } + "}}"
                            result = evaluator.eval(formattedRules)
                            resStr = result.toString()
                        }
                    }
                }

                if (resStr.contains("Sin") || resStr.contains("Cos") || resStr.contains("Tan")) {
                    try {
                        val simp = evaluator.eval("Simplify($s)")
                        if (simp.toString().length < resStr.length) {
                            result = simp
                        }
                    } catch (_: Exception) {}
                }
                if (resStr.contains("Sqrt")) {
                    try {
                        val denested = denestRadicals(resStr)
                        if (denested != resStr) {
                            result = evaluator.eval(denested)
                        } else {
                            val simp = evaluator.eval("FullSimplify($s)")
                            if (simp.toString().length < resStr.length) {
                                result = simp
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (texStr.isEmpty()) {
                    val texResult = try {
                        evaluator.eval("TeXForm($result)").toString().removeSurrounding("\"", "\"")
                    } catch (_: Exception) {
                        result?.toString() ?: ""
                    }
                    texStr = texResult
                }
                if (s.startsWith("N[") && resStr.length > texStr.length) {
                    texStr = resStr
                }
                texStr = replaceHoldFactorialWithScientific(texStr)
                resStr = replaceHoldFactorialWithScientific(resStr)
                texStr = cleanEngineOutput(texStr, isOde = isOdeMode, solveVars = vars)
            }

            // Quadratic equations: replace raw engine roots (e.g. -1(-i)·√3) with clean a + bi form
            if (!isOdeMode && s.startsWith("Solve[") && vars.size == 1) {
                formatQuadraticRootsTex(s, vars.first())?.let { texStr = it }
            }
            
            // Format True / False nicely
            if (texStr == "False" || texStr == "\\text{False}" || texStr == "{}" || texStr == "\\left\\{\\right\\}") {
                texStr = "\\text{${Translator.tr("Çözüm kümesi boştur")} } (\\emptyset)"
            } else if (texStr == "True" || texStr == "\\text{True}") {
                texStr = "\\text{${Translator.tr("Tüm reel sayılar")} } (\\mathbb{R})"
            }
            
            // Escape inequalities for HTML/MathJax safety (< -> \lt , > -> \gt , <= -> \le , >= -> \ge )
            val safeTexStr = escapeLatexForHtml(texStr)
            val safeMultiVarDiophTex = multiVarDiophTex?.let { escapeLatexForHtml(it) }

            val html = java.lang.StringBuilder()
            
            // If Symja could not solve this expression analytically
            val isUnsolved = isPdeUnsolved || texStr.contains("\\int") || texStr.contains("\\lim") || texStr.contains("Limit[") || texStr.contains("Limit(") || texStr.contains("\\text{Limit}") || texStr.contains("\\sum") || texStr.contains("\\prod") ||
                             texStr.contains("\\text{Reduce}") || texStr.contains("Reduce(") || texStr.contains("Reduce[") ||
                             texStr.contains("\\text{DSolve}") || texStr.contains("DSolve(") || texStr.contains("DSolve[") ||
                             texStr.contains("\\text{Solve}") || texStr.contains("Solve(") || texStr.contains("Solve[") ||
                             texStr.contains("\\text{D}") || texStr.contains("D(") || texStr.contains("D[") ||
                             texStr.contains("\\text{Sum}") || texStr.contains("Sum(") || texStr.contains("Sum[") ||
                             texStr.contains("\\text{Product}") || texStr.contains("Product(") || texStr.contains("Product[") ||
                             texStr.contains("\\text{Integrate}") || texStr.contains("Integrate(") || texStr.contains("Integrate[") ||
                             texStr.contains("\\text{LaplaceTransform}") || texStr.contains("LaplaceTransform(") || texStr.contains("LaplaceTransform[") ||
                             texStr.contains("\\text{InverseLaplaceTransform}") || texStr.contains("InverseLaplaceTransform(") || texStr.contains("InverseLaplaceTransform[") ||
                             texStr.contains("\\text{Eigenvectors}") || texStr.contains("Eigenvectors(") || texStr.contains("Eigenvectors[") ||
                             texStr.contains("\\text{ComplexExpand}") || texStr.contains("ComplexExpand(") || texStr.contains("ComplexExpand[") ||
                             (isInequalityMode && (resStr.contains("Floor") || resStr.contains("Ceiling") || resStr.contains("Reduce")))
            
            if (isUnsolved) {
                html.append("<div style='color: #856404; background: #fff3cd; padding: 10px; border-radius: 8px; margin-bottom: 10px; font-size: 0.9em;'>")
                if (isPdeMode) {
                    html.append("<b>${Translator.tr("Yerel CAS Motoru Tıkandı:")}</b><br/>${Translator.tr("Motor bu kısmi diferansiyel denklemi (PDE) analitik kapalı formda çözemedi. Adım adım ve ayrıntılı çözüm için lütfen aşağıdaki Yapay Zeka (AI) İle Çöz butonunu kullanın.")}</div>")
                } else if (isInequalityMode && (resStr.contains("Floor") || resStr.contains("Ceiling"))) {
                    html.append("<b>${Translator.tr("Yerel CAS Motoru Tıkandı:")}</b><br/>${Translator.tr("Bu eşitsizlik tam değer / taban fonksiyonu (\\lfloor x \\rfloor) içerdiğinden yerel sembolik motor analitik kapalı formda çözemedi. Adım adım ve tam çözüm kümesi için lütfen aşağıdaki Yapay Zeka (AI) İle Çöz butonunu kullanın.")}</div>")
                } else {
                    html.append("<b>${Translator.tr("Yerel CAS Motoru Tıkandı:")}</b><br/>${Translator.tr("Motor bu ifadeyi analitik olarak çözemediği için orijinal formülü geri döndürdü. Lütfen çözümü doğrudan aşağıdaki Yapay Zeka (AI) İle Çöz butonundan deneyin.")}</div>")
                }
            }
            
            if (isLargeFactorialMode) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Büyük Faktöriyel Sonucu (Bilimsel Gösterim):")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ $largeFactExpr \\approx " + safeTexStr + " $$</div>")
            } else if (isPdeMode) {
                val headerTitle = if (isPdeUnsolved) Translator.tr("Kısmi Diferansiyel Denklem (PDE):") else Translator.tr("Kısmi Diferansiyel Denklem (PDE) Çözümü:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isEquationSystemMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Denklem Sistemi (Analitik Olarak Çözülemedi):") else Translator.tr("Denklem Sistemi Çözümü:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isMultiVarEquationMode) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Cebirsel Çözüm (Sembolik):")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 10px;'>$$ " + safeTexStr + " $$</div>")
                
                if (safeMultiVarDiophTex != null) {
                    val formattedDioph = if (safeMultiVarDiophTex == "False" || safeMultiVarDiophTex == "\\text{False}") {
                        "\\text{${Translator.tr("Tam sayı çözümü yoktur")} } (\\emptyset)"
                    } else safeMultiVarDiophTex
                    html.append("<div style='font-size: 1.1em; margin-top: 15px;'><b>${Translator.tr("Tam Sayı (Diophantine) Çözümü")} (${vars.joinToString(", ")} \\in \\mathbb{Z}):</b></div>")
                    html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 10px;'>$$ " + formattedDioph + " $$</div>")
                } else if (multiVarDiophNoSolution) {
                    html.append("<div style='font-size: 1.1em; margin-top: 15px;'><b>${Translator.tr("Tam Sayı (Diophantine) Çözümü")} (${vars.joinToString(", ")} \\in \\mathbb{Z}):</b></div>")
                    html.append("<div style='font-size: 1.0em; margin-top: 8px; color: #555;'>$$ \\text{${Translator.tr("Tam sayı çözümü yoktur")} } (\\emptyset) $$</div>")
                } else {
                    html.append("<div style='font-size: 0.9em; margin-top: 12px; color: #666; font-style: italic;'>${Translator.tr("İpucu: Bu denklemin tam sayı (Diophantine) kökleri ve ayrıntılı çözümü için aşağıdaki AI İle Çözüm Adımları butonunu kullanabilirsiniz.")}</div>")
                }
            } else if (isExplicitDiophantine) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Tam Sayı (Diophantine) Çözümü")}:</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isMultipleIntegralMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Çok Katlı İntegral (Analitik Olarak Hesaplanamadı):") else Translator.tr("Çok Katlı İntegral Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isAbsIndefiniteIntegral) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("İntegral Sonucu:")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 10px;'>$$ " + safeTexStr + " + C $$</div>")
            } else if (isDefiniteIntegralMode || isAbsDefiniteIntegral) {
                val headerTitle = if (isUnsolved) Translator.tr("Belirli İntegral (Analitik Olarak Hesaplanamadı):") else Translator.tr("Belirli İntegral Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isIndefiniteIntegralMode) {
                val headerTitle = if (isUnsolved) Translator.tr("İntegral (Analitik Olarak Hesaplanamadı):") else Translator.tr("İntegral Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 10px;'>$$ " + safeTexStr + " + C $$</div>")
            } else if (isOdeMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Diferansiyel Denklem (Analitik Olarak Çözülemedi):") else Translator.tr("Diferansiyel Denklem Çözümü:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isInequalityMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Eşitsizlik (Analitik Olarak Çözülemedi):") else Translator.tr("Eşitsizlik Çözüm Kümesi:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isDerivativeMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Türev (Analitik Olarak Hesaplanamadı):") else Translator.tr("Türev Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isLimitMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Limit (Analitik Olarak Hesaplanamadı):") else Translator.tr("Limit Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isComplexMode) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Kompleks Sayı Sonucu:")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isEigenMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Özvektör (Analitik Olarak Hesaplanamadı):") else Translator.tr("Özvektör Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isComplexExpandMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Kompleks Genişletme (Analitik Olarak Hesaplanamadı):") else Translator.tr("Kompleks Genişletme Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isMatrixMode) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Matris Sonucu:")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isSeriesMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Seri Açılımı (Analitik Olarak Hesaplanamadı):") else Translator.tr("Seri Açılımı (Taylor / Maclaurin):")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isSumOrProductMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Seri Toplamı (Analitik Olarak Hesaplanamadı):") else Translator.tr("Seri Toplamı Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isLaplaceMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Laplace Dönüşümü (Analitik Olarak Hesaplanamadı):") else Translator.tr("Laplace Dönüşümü Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isInverseLaplaceMode) {
                val headerTitle = if (isUnsolved) Translator.tr("Ters Laplace Dönüşümü (Analitik Olarak Hesaplanamadı):") else Translator.tr("Ters Laplace Dönüşümü Sonucu:")
                html.append("<div style='font-size: 1.1em;'><b>$headerTitle</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else if (isGammaMode) {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Gamma Fonksiyonu Sonucu:")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            } else {
                html.append("<div style='font-size: 1.1em;'><b>${Translator.tr("Cebirsel Sonuç:")}</b></div>")
                html.append("<div style='font-size: 1.3em; margin-top: 10px; overflow-x: auto; padding-bottom: 15px;'>$$ " + safeTexStr + " $$</div>")
            }

            val effectiveStepsHtml = when {
                !largeFactStepsHtml.isNullOrBlank() -> largeFactStepsHtml
                !pdeStepsHtml.isNullOrBlank() -> pdeStepsHtml
                !odeStepsHtml.isNullOrBlank() -> odeStepsHtml
                !inequalityStepsHtml.isNullOrBlank() -> inequalityStepsHtml
                !absIntegralStepsHtml.isNullOrBlank() -> absIntegralStepsHtml
                !localStepsHtml.isNullOrBlank() -> localStepsHtml
                !isUnsolved -> generateUniversalSymbolicSteps(originalSymjaCmd, texStr)
                else -> null
            }

            if (!effectiveStepsHtml.isNullOrBlank()) {
                html.append("<details class=\"cas-steps-details\" open>")
                html.append("<summary class=\"cas-steps-summary\">${Translator.tr("Çözüm Adımlarını Göster / Gizle")}</summary>")
                html.append("<div class=\"cas-steps-body\">$effectiveStepsHtml</div>")
                html.append("</details>")
            }
            
            // AI Details Toggle
            if (aiEnabled) {
                val encodedEq = encodeForAskAI(cleanedInput)
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
        } catch (e: Throwable) {
            if (!localStepsHtml.isNullOrBlank()) {
                val html = java.lang.StringBuilder()
                html.append(localStepsHtml)
                if (aiEnabled) {
                    val encodedEq = encodeForAskAI(input)
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

            val html = java.lang.StringBuilder()
            html.append("<div class=\"cas-engine-error\" style='color: var(--error-color); background: var(--error-bg); padding: 10px; border-radius: 8px; margin-bottom: 10px; font-size: 0.9em;'>")
            html.append("${Translator.tr("Bu ifade standart bir matematik kuralından öte sezgisel bir yapı içeriyor olabilir (Örn: ... kullanımı veya eksik semboller).")}</div>")
            
            // AI Details Toggle
            if (aiEnabled) {
                val encodedEq = encodeForAskAI(input)
                html.append("<details class=\"ai-details\" style=\"margin-top: 15px;\">")
                html.append("<summary onclick=\"var err = document.querySelector('.cas-engine-error'); if(err) { err.remove(); } if(this.dataset.loaded != 'true') { Android.askAI('$encodedEq'); this.dataset.loaded='true'; }\" style=\"cursor: pointer; list-style: none; display: flex; align-items: center;\">")
                html.append("<hr style=\"flex: 1; border: 0; border-top: 1px dashed var(--ai-color); opacity: 0.5;\"/>")
                html.append("<div style=\"padding: 0 15px; color: var(--ai-color); font-weight: bold; font-size: 0.95em;\">${Translator.tr("Yapay Zeka (AI) İle Çöz")}</div>")
                html.append("<hr style=\"flex: 1; border: 0; border-top: 1px dashed var(--ai-color); opacity: 0.5;\"/>")
                html.append("</summary>")
                html.append("<div class=\"ai-steps-content\" style=\"padding-top: 15px;\">")
                html.append("</div></details>")
            }
            
            return html.toString()
        }
    }
}

object GraphCasCache {
    val cache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun resolve(expr: String): String {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return trimmed
        val clean = trimmed.replace(Regex("""^[fFgGhH]_?\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]+\s*"""), "")
            .replace(Regex("""^[yY]\s*(?:\(\s*x\s*\))?\s*[:=]+\s*"""), "").trim()
        return cache.computeIfAbsent(clean) { key ->
            val simplified = SymjaEngine.simplifyToMathExpression(key)
            if (!simplified.isNullOrBlank()) simplified else key
        }
    }
}

