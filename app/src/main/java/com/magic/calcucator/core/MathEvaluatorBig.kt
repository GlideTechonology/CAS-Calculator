package com.magic.calcucator.core

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.Locale
import kotlin.math.*

class MathEvaluatorBig(
    private val str: String,
    private val variableX: BigDecimal? = null,
    private val base: Int = 10,
    private val useRadians: Boolean = false
) {
    var pos = -1
    var ch = 0

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

    fun eat(stringToEat: String): Boolean {
        while (ch == ' '.code) nextChar()
        val startPos = pos
        var match = true
        for (i in stringToEat.indices) {
            if (pos < str.length && str[pos].uppercaseChar() == stringToEat[i].uppercaseChar()) {
                pos++
            } else {
                match = false
                break
            }
        }
        if (match) {
            ch = if (pos < str.length) str[pos].code else -1
            return true
        } else {
            pos = startPos
            ch = if (pos < str.length) str[pos].code else -1
            return false
        }
    }

    var lastWasPercent = false

    fun parse(): BigDecimal {
        nextChar()
        var x = parseExpression()
        if (lastWasPercent) {
            x = x.divide(BigDecimal(100), MathContext.DECIMAL128)
            lastWasPercent = false
        }
        while (ch == ' '.code) nextChar()
        if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    fun parseExpression(): BigDecimal {
        var x = parseTerm()
        var factorWasPercent = lastWasPercent
        while (true) {
            if (eat('+'.code)) {
                var y = parseTerm()
                if (lastWasPercent) {
                    y = x.multiply(y, MathContext.DECIMAL128).divide(BigDecimal(100), MathContext.DECIMAL128)
                    lastWasPercent = false
                }
                x = x.add(y, MathContext.DECIMAL128)
                factorWasPercent = false
            } else if (eat('-'.code)) {
                var y = parseTerm()
                if (lastWasPercent) {
                    y = x.multiply(y, MathContext.DECIMAL128).divide(BigDecimal(100), MathContext.DECIMAL128)
                    lastWasPercent = false
                }
                x = x.subtract(y, MathContext.DECIMAL128)
                factorWasPercent = false
            } else if (eat("<<")) {
                val y = parseTerm().toInt()
                x = BigDecimal(x.toBigInteger().shiftLeft(y))
                factorWasPercent = false
            } else if (eat(">>")) {
                val y = parseTerm().toInt()
                x = BigDecimal(x.toBigInteger().shiftRight(y))
                factorWasPercent = false
            } else if (eat('&'.code)) {
                x = BigDecimal(x.toBigInteger().and(parseTerm().toBigInteger()))
                factorWasPercent = false
            } else if (eat('|'.code)) {
                x = BigDecimal(x.toBigInteger().or(parseTerm().toBigInteger()))
                factorWasPercent = false
            } else if (eat('#'.code)) {
                x = BigDecimal(x.toBigInteger().xor(parseTerm().toBigInteger()))
                factorWasPercent = false
            } else {
                lastWasPercent = factorWasPercent
                return x
            }
        }
    }

    fun parseTerm(): BigDecimal {
        var x = parseFactor()
        var factorWasPercent = lastWasPercent
        while (true) {
            if (eat('*'.code)) {
                var y = parseFactor()
                if (lastWasPercent) {
                    y = y.divide(BigDecimal(100), MathContext.DECIMAL128)
                    lastWasPercent = false
                }
                x = x.multiply(y, MathContext.DECIMAL128)
                factorWasPercent = false
            } else if (eat('/'.code)) {
                var divisor = parseFactor()
                if (lastWasPercent) {
                    divisor = divisor.divide(BigDecimal(100), MathContext.DECIMAL128)
                    lastWasPercent = false
                }
                if (divisor.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                x = x.divide(divisor, MathContext.DECIMAL128)
                factorWasPercent = false
            } else if (ch == '('.code || ch == 'π'.code || ch == 'e'.code || ch == 'x'.code || ch == 'X'.code || ch == '√'.code || ch == '∛'.code ||
                (base == 10 && ch in 'a'.code..'z'.code) ||
                (ch in '0'.code..'9'.code) || ch == '.'.code ||
                (base > 10 && ((ch in 'A'.code..'F'.code) || (ch in 'a'.code..'f'.code)))) {
                var y = parseFactor()
                if (lastWasPercent) {
                    y = y.divide(BigDecimal(100), MathContext.DECIMAL128)
                    lastWasPercent = false
                }
                x = x.multiply(y, MathContext.DECIMAL128)
                factorWasPercent = false
            } else {
                lastWasPercent = factorWasPercent
                return x
            }
        }
    }

    private fun toAngleRadians(v: Double): Double {
        return if (useRadians) v else Math.toRadians(v)
    }

    private fun fromAngleRadians(rad: Double): Double {
        return if (useRadians) rad else Math.toDegrees(rad)
    }

    fun parseFactor(): BigDecimal {
        if (eat('+'.code)) return parseFactor()
        if (eat('-'.code)) return parseFactor().negate()
        if (eat('~'.code)) return BigDecimal(parseFactor().toBigInteger().not())

        var x: BigDecimal
        val startPos = pos
        if (eat('('.code)) {
            x = parseExpression()
            eat(')'.code)
        } else if (eat('{'.code)) {
            x = parseExpression()
            eat('}'.code)
        } else if ((ch in '0'.code..'9'.code) || ch == '.'.code || (base > 10 && (ch in 'A'.code..'F'.code || ch in 'a'.code..'f'.code))) {
            while ((ch in '0'.code..'9'.code) || ch == '.'.code || (base > 10 && (ch in 'A'.code..'F'.code || ch in 'a'.code..'f'.code))) nextChar()
            if (base == 10 && (ch == 'E'.code || ch == 'e'.code)) {
                val savePos = pos
                val saveCh = ch
                nextChar()
                if (ch == '+'.code || ch == '-'.code || ch in '0'.code..'9'.code) {
                    if (ch == '+'.code || ch == '-'.code) nextChar()
                    while (ch in '0'.code..'9'.code) nextChar()
                } else {
                    pos = savePos
                    ch = saveCh
                }
            }
            val numStr = str.substring(startPos, this.pos)
            x = if (base != 10 && !numStr.contains(".")) {
                BigDecimal(BigInteger(numStr, base))
            } else {
                BigDecimal(numStr)
            }
        } else if (ch == 'π'.code) {
            nextChar()
            x = BigDecimal.valueOf(Math.PI)
        } else if (ch == 'e'.code) {
            nextChar()
            x = BigDecimal.valueOf(Math.E)
        } else if (ch == 'x'.code || ch == 'X'.code) {
            nextChar()
            x = variableX ?: throw RuntimeException("x variable not provided")
        } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch == '√'.code || ch == '∛'.code) {
            while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch == '√'.code || ch == '∛'.code) nextChar()
            val func = str.substring(startPos, this.pos).lowercase()
            if (func == "pi") {
                x = BigDecimal.valueOf(Math.PI)
            } else {
                x = parseFactor()
                val dVal = x.toDouble()
                x = when (func) {
                    "sqrt", "√" -> {
                        if (x.compareTo(BigDecimal.ZERO) < 0) throw ArithmeticException("Square root of negative number")
                        if (x.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
                        else {
                            if (dVal.isFinite() && dVal > 0.0) {
                                BigDecimal.valueOf(sqrt(dVal))
                            } else {
                                val exp = x.precision() - x.scale() - 1
                                val adjExp = if (exp % 2 != 0) exp - 1 else exp
                                val m = x.movePointLeft(adjExp).toDouble()
                                val sqrtM = sqrt(m)
                                val halfExp = adjExp / 2
                                BigDecimal.valueOf(sqrtM).movePointRight(halfExp)
                            }
                        }
                    }
                    "cbrt", "∛" -> BigDecimal.valueOf(cbrt(dVal))
                    "abs" -> x.abs()
                    "floor" -> BigDecimal.valueOf(floor(dVal))
                    "ceil", "ceiling" -> BigDecimal.valueOf(ceil(dVal))
                    "sign", "sgn" -> BigDecimal.valueOf(sign(dVal))
                    "exp" -> BigDecimal.valueOf(exp(dVal))
                    "sin" -> {
                        if (!useRadians) {
                            val deg = dVal % 360.0
                            if (deg == 0.0 || deg == 180.0 || deg == -180.0) BigDecimal.ZERO
                            else BigDecimal.valueOf(sin(Math.toRadians(deg)))
                        } else {
                            BigDecimal.valueOf(sin(dVal))
                        }
                    }
                    "cos" -> {
                        if (!useRadians) {
                            val deg = dVal % 360.0
                            if (deg == 90.0 || deg == -90.0 || deg == 270.0 || deg == -270.0) BigDecimal.ZERO
                            else BigDecimal.valueOf(cos(Math.toRadians(deg)))
                        } else {
                            BigDecimal.valueOf(cos(dVal))
                        }
                    }
                    "tan" -> {
                        if (!useRadians) {
                            val deg = dVal % 360.0
                            when {
                                deg == 0.0 || deg == 180.0 || deg == -180.0 -> BigDecimal.ZERO
                                deg == 90.0 || deg == -90.0 || deg == 270.0 || deg == -270.0 -> throw ArithmeticException("Undefined")
                                else -> BigDecimal.valueOf(tan(Math.toRadians(deg)))
                            }
                        } else {
                            val c = cos(dVal)
                            if (abs(c) < 1e-12) throw ArithmeticException("Undefined")
                            BigDecimal.valueOf(tan(dVal))
                        }
                    }
                    "cot" -> {
                        if (!useRadians) {
                            val deg = dVal % 360.0
                            when {
                                deg == 90.0 || deg == -90.0 || deg == 270.0 || deg == -270.0 -> BigDecimal.ZERO
                                deg == 0.0 || deg == 180.0 || deg == -180.0 -> throw ArithmeticException("Undefined")
                                else -> BigDecimal.valueOf(1.0 / tan(Math.toRadians(deg)))
                            }
                        } else {
                            val s = sin(dVal)
                            if (abs(s) < 1e-12) throw ArithmeticException("Undefined")
                            BigDecimal.valueOf(cos(dVal) / s)
                        }
                    }
                    "sec" -> {
                        val c = cos(toAngleRadians(dVal))
                        if (abs(c) < 1e-12) throw ArithmeticException("Undefined")
                        BigDecimal.valueOf(1.0 / c)
                    }
                    "csc" -> {
                        val s = sin(toAngleRadians(dVal))
                        if (abs(s) < 1e-12) throw ArithmeticException("Undefined")
                        BigDecimal.valueOf(1.0 / s)
                    }
                    "asin", "arcsin" -> {
                        if (dVal < -1.0 || dVal > 1.0) throw ArithmeticException("Domain error")
                        BigDecimal.valueOf(fromAngleRadians(asin(dVal)))
                    }
                    "acos", "arccos" -> {
                        if (dVal < -1.0 || dVal > 1.0) throw ArithmeticException("Domain error")
                        BigDecimal.valueOf(fromAngleRadians(acos(dVal)))
                    }
                    "atan", "arctan" -> BigDecimal.valueOf(fromAngleRadians(atan(dVal)))
                    "sinh" -> BigDecimal.valueOf(sinh(dVal))
                    "cosh" -> BigDecimal.valueOf(cosh(dVal))
                    "tanh" -> BigDecimal.valueOf(tanh(dVal))
                    "coth" -> {
                        val sh = sinh(dVal)
                        if (abs(sh) < 1e-12) throw ArithmeticException("Undefined")
                        BigDecimal.valueOf(cosh(dVal) / sh)
                    }
                    "log" -> {
                        if (x.compareTo(BigDecimal.ZERO) <= 0) throw ArithmeticException("Log of non-positive number")
                        if (dVal.isFinite() && dVal > 0.0) {
                            BigDecimal.valueOf(log10(dVal))
                        } else {
                            val exp = x.precision() - x.scale() - 1
                            val m = x.movePointLeft(exp).toDouble()
                            BigDecimal.valueOf(log10(m) + exp)
                        }
                    }
                    "ln" -> {
                        if (x.compareTo(BigDecimal.ZERO) <= 0) throw ArithmeticException("Ln of non-positive number")
                        if (dVal.isFinite() && dVal > 0.0) {
                            BigDecimal.valueOf(ln(dVal))
                        } else {
                            val exp = x.precision() - x.scale() - 1
                            val m = x.movePointLeft(exp).toDouble()
                            BigDecimal.valueOf(ln(m) + exp * ln(10.0))
                        }
                    }
                    else -> throw RuntimeException("Unknown function: $func")
                }
            }
        } else {
            throw RuntimeException("Unexpected: " + ch.toChar())
        }

        if (eat('^'.code)) {
            val exponent = parseFactor()
            val intExp = try {
                val stripped = exponent.stripTrailingZeros()
                if (stripped.scale() <= 0) stripped.intValueExact() else null
            } catch (_: Exception) {
                null
            }
            if (intExp != null && abs(intExp) <= 999999) {
                if (intExp >= 0) {
                    x = x.pow(intExp, MathContext.DECIMAL128)
                } else {
                    if (x.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Division by zero")
                    val denom = x.pow(-intExp, MathContext.DECIMAL128)
                    x = BigDecimal.ONE.divide(denom, MathContext.DECIMAL128)
                }
            } else {
                val yDouble = exponent.toDouble()
                if (yDouble.isFinite()) {
                    val n = floor(yDouble).toInt()
                    val f = yDouble - n
                    if (x.compareTo(BigDecimal.ZERO) > 0 && abs(n) <= 999999 && (f > 0.0 || f < 0.0)) {
                        val fracPow = BigDecimal.valueOf(x.toDouble().pow(f))
                        val intPow = if (n >= 0) {
                            x.pow(n, MathContext.DECIMAL128)
                        } else {
                            BigDecimal.ONE.divide(x.pow(-n, MathContext.DECIMAL128), MathContext.DECIMAL128)
                        }
                        x = intPow.multiply(fracPow, MathContext.DECIMAL128)
                    } else {
                        val resPow = realPow(x.toDouble(), yDouble)
                        if (resPow.isNaN() || resPow.isInfinite()) throw ArithmeticException("Invalid power")
                        x = BigDecimal.valueOf(resPow)
                    }
                } else {
                    val resPow = realPow(x.toDouble(), yDouble)
                    if (resPow.isNaN() || resPow.isInfinite()) throw ArithmeticException("Invalid power")
                    x = BigDecimal.valueOf(resPow)
                }
            }
        }
        if (eat('!'.code)) {
            val end = x.toInt()
            if (end < 0) throw RuntimeException("Negative factorial")
            if (end > 99999) throw RuntimeException("Factorial too large")

            fun prodTree(l: Int, r: Int): BigInteger {
                if (Thread.currentThread().isInterrupted) throw RuntimeException("Cancelled")
                if (l > r) return BigInteger.ONE
                if (l == r) return BigInteger.valueOf(l.toLong())
                if (r - l == 1) return BigInteger.valueOf(l.toLong() * r.toLong())
                val m = (l + r) / 2
                return prodTree(l, m).multiply(prodTree(m + 1, r))
            }

            x = if (end == 0 || end == 1) {
                BigDecimal(BigInteger.ONE)
            } else {
                BigDecimal(prodTree(2, end))
            }
        }

        if (eat('%'.code)) {
            lastWasPercent = true
        } else {
            lastWasPercent = false
        }

        return x
    }
}

class MathEvaluator(private val str: String, private val variableX: Double? = null, private val useRadians: Boolean = false) {
    fun parse(): Double {
        return MathEvaluatorBig(str, variableX?.let { BigDecimal.valueOf(it) }, 10, useRadians).parse().toDouble()
    }
}

fun evaluateMathBig(
    expression: String,
    xVal: BigDecimal? = null,
    base: Int = 10,
    useRadians: Boolean = false
): BigDecimal {
    var str = expression.trim()
    if (str.startsWith("y", ignoreCase = true) && str.drop(1).trimStart().startsWith("=")) {
        str = str.substringAfter("=").trim()
    } else if (str.startsWith("f(x)", ignoreCase = true) && str.drop(4).trimStart().startsWith("=")) {
        str = str.substringAfter("=").trim()
    }
    // Convert |expr| to abs(expr) when in decimal/graphing mode
    if (base == 10 && str.contains("|")) {
        val absRegex = Regex("""\|([^|]+)\|""")
        while (absRegex.containsMatchIn(str)) {
            str = absRegex.replace(str, "abs($1)")
        }
    }
    str = str.replace(Regex("""\bXOR\b""", RegexOption.IGNORE_CASE), "#")
        .replace(Regex("""\bAND\b""", RegexOption.IGNORE_CASE), "&")
        .replace(Regex("""\bOR\b""", RegexOption.IGNORE_CASE), "|")
        .replace(Regex("""\bNOT\b""", RegexOption.IGNORE_CASE), "~")
    str = str.replace(" ", "").replace("×", "*").replace("÷", "/").replace("{", "(").replace("}", ")")
    return MathEvaluatorBig(str, xVal, base, useRadians).parse()
}

fun evaluateMath(expression: String, xVal: Double? = null, useRadians: Boolean = true): Double {
    var cleanExpr = expression.trim()
    cleanExpr = cleanExpr.replace(Regex("""^[fFgGhH]\d*\s*\(\s*[a-zA-Z]\s*\)\s*[:=]\s*"""), "")
    cleanExpr = cleanExpr.replace(Regex("""^[yY]\s*(?:\(\s*x\s*\))?\s*[:=]\s*"""), "")
    val casResolved = GraphCasCache.resolve(cleanExpr)
    if (casResolved.isNotBlank() && casResolved != cleanExpr) {
        cleanExpr = casResolved
    }
    return evaluateMathBig(cleanExpr, xVal?.let { BigDecimal.valueOf(it) }, 10, useRadians).toDouble()
}

fun formatScientificBigDecimal(res: BigDecimal, precision: Int = 6): String {
    if (res.compareTo(BigDecimal.ZERO) == 0) return "0"
    val isNeg = res.signum() < 0
    val stripped = res.abs().stripTrailingZeros()
    var exp = stripped.precision().toLong() - stripped.scale().toLong() - 1L
    val shift = if (exp > Int.MAX_VALUE) Int.MAX_VALUE else if (exp < Int.MIN_VALUE) Int.MIN_VALUE else exp.toInt()
    var mantissa = stripped.movePointLeft(shift)
    var roundedMantissa = mantissa.setScale(precision, java.math.RoundingMode.HALF_UP)
    if (roundedMantissa.compareTo(BigDecimal.TEN) >= 0) {
        mantissa = roundedMantissa.movePointLeft(1)
        exp += 1L
        roundedMantissa = mantissa.setScale(precision, java.math.RoundingMode.HALF_UP)
    }
    val expSign = if (exp >= 0) "+" else "-"
    val absExp = abs(exp)
    val expStr = if (absExp < 10) "0$absExp" else "$absExp"
    val signStr = if (isNeg) "-" else ""
    return "$signStr${roundedMantissa.toPlainString()}E$expSign$expStr"
}

fun trimIncompleteTrailingTokens(expression: String): String {
    var evalStr = expression.trim()
    var changed = true
    while (changed && evalStr.isNotEmpty()) {
        changed = false
        evalStr = evalStr.trimEnd()
        val trailingKeywords = listOf("<<", ">>", "XOR", "AND", "OR", "NOT")
        for (kw in trailingKeywords) {
            if (evalStr.endsWith(kw, ignoreCase = true)) {
                evalStr = evalStr.dropLast(kw.length).trimEnd()
                changed = true
            }
        }
        while (evalStr.isNotEmpty() && "+-*×/^÷(.&|~#{,".contains(evalStr.last())) {
            evalStr = evalStr.dropLast(1).trimEnd()
            changed = true
        }
    }
    return evalStr
}

fun safeEvaluate(expression: String, xVal: Double? = null, base: Int = 10): String {
    try {
        if (expression.isBlank()) return ""

        var evalStr = trimIncompleteTrailingTokens(expression)
        if (evalStr.isEmpty()) return ""

        val openParens = evalStr.count { it == '(' }
        val closeParens = evalStr.count { it == ')' }
        if (openParens > closeParens) {
            evalStr += ")".repeat(openParens - closeParens)
        }
        val openBraces = evalStr.count { it == '{' }
        val closeBraces = evalStr.count { it == '}' }
        if (openBraces > closeBraces) {
            evalStr += "}".repeat(openBraces - closeBraces)
        }

        val res = evaluateMathBig(evalStr, xVal?.let { BigDecimal.valueOf(it) }, base)

        if (base != 10) {
            val bi = res.toBigInteger()
            return if (bi.signum() < 0) {
                val mask64 = BigInteger("FFFFFFFFFFFFFFFF", 16)
                bi.and(mask64).toString(base).uppercase()
            } else {
                bi.toString(base).uppercase()
            }
        }

        val finalRes = res.toDouble()
        if (finalRes.isNaN()) {
            return ""
        }

        val isExtremelySmall = res.compareTo(BigDecimal.ZERO) != 0 && res.abs() < BigDecimal("1e-5")
        val isExtremelyLarge = res.abs() >= BigDecimal("1e11")
        if (isExtremelyLarge || isExtremelySmall) {
            return formatScientificBigDecimal(res)
        }

        val doubleRes = if (abs(finalRes - round(finalRes)) < 1e-10) round(finalRes) else finalRes

        return if (doubleRes % 1.0 == 0.0 && abs(doubleRes) <= Long.MAX_VALUE.toDouble()) {
            doubleRes.toLong().toString()
        } else {
            res.stripTrailingZeros().toPlainString()
        }
    } catch (e: Exception) {
        return "Err"
    }
}

fun evaluateStrict(expression: String, base: Int = 10): String {
    try {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return ""

        val trailingBad = listOf("+", "-", "*", "×", "/", "÷", "^", "&", "|", "~", "(", "(-", "{", "AND", "OR", "XOR", "NOT", "<<", ">>")
        for (tb in trailingBad) {
            if (trimmed.endsWith(tb, ignoreCase = true)) {
                return "Err"
            }
        }

        val openParens = trimmed.count { it == '(' }
        val closeParens = trimmed.count { it == ')' }
        if (openParens != closeParens) {
            return "Err"
        }
        val openBraces = trimmed.count { it == '{' }
        val closeBraces = trimmed.count { it == '}' }
        if (openBraces != closeBraces) {
            return "Err"
        }

        val res = evaluateMathBig(trimmed, null, base)

        if (base != 10) {
            val bi = res.toBigInteger()
            return if (bi.signum() < 0) {
                val mask64 = BigInteger("FFFFFFFFFFFFFFFF", 16)
                bi.and(mask64).toString(base).uppercase()
            } else {
                bi.toString(base).uppercase()
            }
        }

        val finalRes = res.toDouble()
        if (finalRes.isNaN()) {
            return "Err"
        }

        val isExtremelySmall = res.compareTo(BigDecimal.ZERO) != 0 && res.abs() < BigDecimal("1e-5")
        val isExtremelyLarge = res.abs() >= BigDecimal("1e11")
        if (isExtremelyLarge || isExtremelySmall) {
            return formatScientificBigDecimal(res)
        }

        val doubleRes = if (abs(finalRes - round(finalRes)) < 1e-10) round(finalRes) else finalRes

        return if (doubleRes % 1.0 == 0.0 && abs(doubleRes) <= Long.MAX_VALUE.toDouble()) {
            doubleRes.toLong().toString()
        } else {
            res.stripTrailingZeros().toPlainString()
        }
    } catch (_: Exception) {
        return "Err"
    }
}

fun evaluateForBaseConversions(expression: String, base: Int): BigInteger? {
    return try {
        val clean = trimIncompleteTrailingTokens(expression)
        if (clean.isEmpty()) return BigInteger.ZERO
        var evalStr = clean
        val openParens = evalStr.count { it == '(' }
        val closeParens = evalStr.count { it == ')' }
        if (openParens > closeParens) {
            evalStr += ")".repeat(openParens - closeParens)
        }
        val res = evaluateMathBig(evalStr, null, base).toBigInteger()
        if (base != 10 && res.bitLength() == 64) {
            BigInteger.valueOf(res.toLong())
        } else {
            res
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Calculates b^p over the real numbers.
 * Supports negative bases with integer or rational exponents having odd denominators (e.g. 1/3, 2/3, 5/3),
 * returning the real branch (-1)^m * |b|^p instead of NaN.
 */
fun realPow(b: Double, p: Double): Double {
    if (b.isNaN() || p.isNaN()) return Double.NaN
    if (!p.isFinite()) return Double.NaN
    if (b >= 0.0) {
        val res = b.pow(p)
        val roundedRes = Math.round(res).toDouble()
        return if (abs(res - roundedRes) < 1e-10) roundedRes else res
    }

    val pRound = Math.round(p)
    if (abs(p - pRound) < 1e-9) {
        val sign = if (pRound % 2L == 0L) 1.0 else -1.0
        val res = sign * (-b).pow(p)
        val roundedRes = Math.round(res).toDouble()
        return if (abs(res - roundedRes) < 1e-10) roundedRes else res
    }

    for (q in 3..99 step 2) {
        val mVal = p * q
        val mRound = Math.round(mVal)
        if (abs(mVal - mRound) < 1e-5) {
            val sign = if (mRound % 2L == 0L) 1.0 else -1.0
            val res = sign * (-b).pow(p)
            val roundedRes = Math.round(res).toDouble()
            return if (abs(res - roundedRes) < 1e-10) roundedRes else res
        }
    }

    return Double.NaN
}
