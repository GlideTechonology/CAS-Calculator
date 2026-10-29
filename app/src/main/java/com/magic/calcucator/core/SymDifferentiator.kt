package com.magic.calcucator.core

import com.magic.calcucator.Translator
import kotlin.math.*

private fun fmtSymDouble(v: Double): String {
    if (abs(v) < 1e-9) return "0"
    val roundedLong = v.roundToLong()
    if (abs(v - roundedLong.toDouble()) < 1e-9) return roundedLong.toString()
    return (round(v * 10000.0) / 10000.0).toString()
}

abstract class SymExpr {
    abstract fun diff(wrt: String = "x"): SymExpr
    abstract fun toLatex(): String
    open fun integrate(): SymExpr {
        throw RuntimeException(Translator.tr("İntegral alınamadı: Analitik çözüm çok karmaşık."))
    }
    abstract fun evaluate(xVal: Double): Double
    open fun simplify(): SymExpr = this
    open fun isZero(): Boolean = false
    open fun isOne(): Boolean = false
}

class SymConst(val v: Double) : SymExpr() {
    override fun diff(wrt: String) = SymConst(0.0)
    override fun integrate() = SymMul(this, SymVar("x")).simplify()
    override fun evaluate(xVal: Double) = v
    override fun toLatex(): String {
        if (abs(v - Math.PI) < 1e-7) return "\\pi"
        if (abs(v - Math.E) < 1e-7) return "e"
        return fmtSymDouble(v)
    }
    override fun isZero() = abs(v) < 1e-9
    override fun isOne() = abs(v - 1.0) < 1e-9
}

class SymVar(val name: String = "x") : SymExpr() {
    override fun diff(wrt: String) = if (name == wrt) SymConst(1.0) else SymConst(0.0)
    override fun toLatex() = name
    override fun integrate() = SymMul(SymConst(0.5), SymPow(this, 2.0))
    override fun evaluate(xVal: Double) = xVal
}

class SymAdd(val a: SymExpr, val b: SymExpr) : SymExpr() {
    override fun diff(wrt: String) = SymAdd(a.diff(wrt), b.diff(wrt)).simplify()
    override fun toLatex() = "${a.toLatex()} + ${b.toLatex()}"
    override fun integrate() = SymAdd(a.integrate(), b.integrate()).simplify()
    override fun evaluate(xVal: Double) = a.evaluate(xVal) + b.evaluate(xVal)
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        val sb = b.simplify()
        if (sa.isZero()) return sb
        if (sb.isZero()) return sa
        if (sa is SymConst && sb is SymConst) return SymConst(sa.v + sb.v)
        return SymAdd(sa, sb)
    }
}

class SymSub(val a: SymExpr, val b: SymExpr) : SymExpr() {
    override fun diff(wrt: String) = SymSub(a.diff(wrt), b.diff(wrt)).simplify()
    override fun toLatex(): String {
        val lb = if (b is SymAdd || b is SymSub) "(${b.toLatex()})" else b.toLatex()
        return "${a.toLatex()} - $lb"
    }
    override fun integrate() = SymSub(a.integrate(), b.integrate()).simplify()
    override fun evaluate(xVal: Double) = a.evaluate(xVal) - b.evaluate(xVal)
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        val sb = b.simplify()
        if (sb.isZero()) return sa
        if (sa.isZero()) return SymMul(SymConst(-1.0), sb).simplify()
        if (sa is SymConst && sb is SymConst) return SymConst(sa.v - sb.v)
        return SymSub(sa, sb)
    }
}

class SymMul(val a: SymExpr, val b: SymExpr) : SymExpr() {
    override fun diff(wrt: String) = SymAdd(SymMul(a.diff(wrt), b), SymMul(a, b.diff(wrt))).simplify()
    override fun toLatex(): String {
        if (a is SymConst && abs(a.v + 1.0) < 1e-9) {
            val lb = if (b is SymAdd || b is SymSub) "(${b.toLatex()})" else b.toLatex()
            return "-$lb"
        }
        val la = if (a is SymAdd || a is SymSub) "(${a.toLatex()})" else a.toLatex()
        val lb = if (b is SymAdd || b is SymSub) "(${b.toLatex()})" else b.toLatex()
        if (a is SymConst) return "$la $lb"
        return "$la \\cdot $lb"
    }
    override fun integrate(): SymExpr {
        if (a is SymConst) return SymMul(a, b.integrate()).simplify()
        if (b is SymConst) return SymMul(b, a.integrate()).simplify()

        if (a is SymVar && b is SymFunc && b.name == "exp" && b.a is SymVar) {
            return SymSub(SymMul(SymVar("x"), b), b).simplify()
        }
        if (b is SymVar && a is SymFunc && a.name == "exp" && a.a is SymVar) {
            return SymSub(SymMul(SymVar("x"), a), a).simplify()
        }
        if (a is SymVar && b is SymFunc && b.name == "sin" && b.a is SymVar) {
            return SymSub(SymFunc("sin", SymVar("x")), SymMul(SymVar("x"), SymFunc("cos", SymVar("x")))).simplify()
        }
        if (a is SymVar && b is SymFunc && b.name == "cos" && b.a is SymVar) {
            return SymAdd(SymFunc("cos", SymVar("x")), SymMul(SymVar("x"), SymFunc("sin", SymVar("x")))).simplify()
        }

        throw RuntimeException(Translator.tr("Çarpım integrali için kısmi integrasyon (By Parts) desteği sınırlıdır: ") + toLatex())
    }
    override fun evaluate(xVal: Double) = a.evaluate(xVal) * b.evaluate(xVal)
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        val sb = b.simplify()
        if (sa.isZero() || sb.isZero()) return SymConst(0.0)
        if (sa.isOne()) return sb
        if (sb.isOne()) return sa
        if (sa is SymConst && sb is SymConst) return SymConst(sa.v * sb.v)
        if (sa is SymConst && sb is SymMul && sb.a is SymConst) {
            return SymMul(SymConst(sa.v * sb.a.v), sb.b).simplify()
        }
        if (sb is SymConst && sa !is SymConst) {
            return SymMul(sb, sa).simplify()
        }
        // Rational multiplication: (u/v) * (p/q) -> (u*p) / (v*q)
        if (sa is SymDiv && sb is SymDiv) {
            return SymDiv(SymMul(sa.a, sb.a), SymMul(sa.b, sb.b)).simplify()
        }
        if (sa is SymDiv) {
            return SymDiv(SymMul(sa.a, sb), sa.b).simplify()
        }
        if (sb is SymDiv) {
            return SymDiv(SymMul(sa, sb.a), sb.b).simplify()
        }
        // Abs multiplication: |u| * |u| -> u^2
        if (sa is SymFunc && sa.name == "abs" && sb is SymFunc && sb.name == "abs" && sa.a.toLatex() == sb.a.toLatex()) {
            return SymPow(sa.a, 2.0).simplify()
        }
        // Variable multiplication: x * x -> x^2
        if (sa is SymVar && sb is SymVar && sa.name == sb.name) {
            return SymPow(sa, 2.0).simplify()
        }
        return SymMul(sa, sb)
    }
}

class SymDiv(val a: SymExpr, val b: SymExpr) : SymExpr() {
    override fun diff(wrt: String) = SymDiv(SymSub(SymMul(a.diff(wrt), b), SymMul(a, b.diff(wrt))), SymPow(b, 2.0)).simplify()
    override fun toLatex() = "\\frac{${a.toLatex()}}{${b.toLatex()}}"
    override fun integrate(): SymExpr {
        if (a is SymConst && b is SymVar) {
            return SymMul(a, SymFunc("ln", b)).simplify()
        }
        if (b is SymConst && !b.isZero()) {
            return SymMul(SymConst(1.0 / b.v), a.integrate()).simplify()
        }
        return super.integrate()
    }
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        val sb = b.simplify()
        if (sa.isZero()) return SymConst(0.0)
        if (sb.isOne()) return sa
        if (sa is SymConst && sb is SymConst && !sb.isZero()) {
            val q = sa.v / sb.v
            if (abs(q - q.roundToLong()) < 1e-9) return SymConst(q)
        }
        if (sa.toLatex() == sb.toLatex()) return SymConst(1.0)
        // u / u^2 -> 1 / u
        if (sb is SymPow && abs(sb.exp - 2.0) < 1e-9 && sa.toLatex() == sb.a.toLatex()) {
            return SymDiv(SymConst(1.0), sa).simplify()
        }
        // u^2 / u -> u
        if (sa is SymPow && abs(sa.exp - 2.0) < 1e-9 && sb.toLatex() == sa.a.toLatex()) {
            return sb
        }
        // (k * u) / u^2 -> k / u
        if (sa is SymMul && sb is SymPow && abs(sb.exp - 2.0) < 1e-9) {
            if (sa.a is SymConst && sa.b.toLatex() == sb.a.toLatex()) return SymDiv(sa.a, sa.b).simplify()
            if (sa.b is SymConst && sa.a.toLatex() == sb.a.toLatex()) return SymDiv(sa.b, sa.a).simplify()
        }
        // (u * v) / u -> v, (u * v) / v -> u
        if (sa is SymMul) {
            if (sa.a.toLatex() == sb.toLatex()) return sa.b
            if (sa.b.toLatex() == sb.toLatex()) return sa.a
        }
        // u / (u * v) -> 1 / v, u / (v * u) -> 1 / v
        if (sb is SymMul) {
            if (sa.toLatex() == sb.a.toLatex()) return SymDiv(SymConst(1.0), sb.b).simplify()
            if (sa.toLatex() == sb.b.toLatex()) return SymDiv(SymConst(1.0), sb.a).simplify()
        }
        return SymDiv(sa, sb)
    }
    override fun evaluate(xVal: Double) = a.evaluate(xVal) / b.evaluate(xVal)
}

class SymPow(val a: SymExpr, val exp: Double) : SymExpr() {
    override fun diff(wrt: String) = SymMul(SymMul(SymConst(exp), SymPow(a, exp - 1.0)), a.diff(wrt)).simplify()
    override fun toLatex(): String {
        val la = if (a is SymVar || a is SymConst || a is SymFunc) a.toLatex() else "(${a.toLatex()})"
        val expStr = fmtSymDouble(exp)
        return "$la^{$expStr}"
    }
    override fun integrate(): SymExpr {
        if (a is SymVar) {
            if (abs(exp + 1.0) < 1e-9) return SymFunc("ln", SymVar(a.name))
            return SymMul(SymConst(1.0 / (exp + 1.0)), SymPow(SymVar(a.name), exp + 1.0)).simplify()
        }
        throw RuntimeException(Translator.tr("Üstel (Zincir kuralı) integrali sınırlıdır."))
    }
    override fun evaluate(xVal: Double) = realPow(a.evaluate(xVal), exp)
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        if (sa.isZero()) return SymConst(0.0)
        if (sa.isOne()) return SymConst(1.0)
        if (abs(exp - 1.0) < 1e-9) return sa
        if (abs(exp) < 1e-9) return SymConst(1.0)
        if (sa is SymConst) return SymConst(realPow(sa.v, exp))
        return SymPow(sa, exp)
    }
}

class SymGenPow(val a: SymExpr, val b: SymExpr) : SymExpr() {
    override fun diff(wrt: String): SymExpr {
        val du = a.diff(wrt).simplify()
        val dv = b.diff(wrt).simplify()
        val term1 = SymMul(dv, SymFunc("ln", a)).simplify()
        val term2 = SymDiv(SymMul(b, du), a).simplify()
        return SymMul(this, SymAdd(term1, term2)).simplify()
    }
    override fun toLatex(): String {
        val la = if (a is SymVar || a is SymConst || a is SymFunc) a.toLatex() else "\\left(${a.toLatex()}\\right)"
        return "$la^{${b.toLatex()}}"
    }
    override fun evaluate(xVal: Double) = realPow(a.evaluate(xVal), b.evaluate(xVal))
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        val sb = b.simplify()
        if (sb is SymConst) return SymPow(sa, sb.v).simplify()
        if (sa.isOne()) return SymConst(1.0)
        return SymGenPow(sa, sb)
    }
}

class SymFunc(val name: String, val a: SymExpr) : SymExpr() {
    override fun diff(wrt: String): SymExpr {
        if (name == "ln") {
            if (a is SymFunc && a.name == "abs") {
                return SymDiv(a.a.diff(wrt), a.a).simplify()
            }
            return SymDiv(a.diff(wrt), a).simplify()
        }
        if (name == "abs") {
            return SymDiv(SymMul(a, a.diff(wrt)), this).simplify()
        }
        val innerDiff = a.diff(wrt)
        val deriv = when (name) {
            "exp" -> SymFunc("exp", a)
            "sin" -> SymFunc("cos", a)
            "cos" -> SymMul(SymConst(-1.0), SymFunc("sin", a))
            "tan" -> SymDiv(SymConst(1.0), SymPow(SymFunc("cos", a), 2.0))
            "cot" -> SymDiv(SymConst(-1.0), SymPow(SymFunc("sin", a), 2.0))
            "sqrt" -> SymDiv(SymConst(1.0), SymMul(SymConst(2.0), SymFunc("sqrt", a)))
            else -> SymConst(0.0)
        }
        return SymMul(deriv, innerDiff).simplify()
    }
    override fun toLatex() = when (name) {
        "exp" -> "e^{${a.toLatex()}}"
        "sqrt" -> "\\sqrt{${a.toLatex()}}"
        "abs" -> "\\left|${a.toLatex()}\\right|"
        else -> "\\$name\\left(${a.toLatex()}\\right)"
    }
    override fun integrate(): SymExpr {
        if (a is SymVar) {
            when (name) {
                "exp" -> return this
                "sin" -> return SymMul(SymConst(-1.0), SymFunc("cos", a))
                "cos" -> return SymFunc("sin", a)
                "ln" -> return SymSub(SymMul(SymVar(a.name), this), SymVar(a.name))
                "sqrt" -> return SymMul(SymConst(2.0 / 3.0), SymPow(a, 1.5))
                "abs" -> return SymMul(SymConst(0.5), SymMul(a, SymFunc("abs", a)))
            }
        }
        throw RuntimeException(Translator.tr("Fonksiyon integrali sınırlıdır."))
    }
    override fun evaluate(xVal: Double): Double {
        val inner = a.evaluate(xVal)
        return when (name) {
            "sin" -> sin(inner)
            "cos" -> cos(inner)
            "tan" -> tan(inner)
            "cot" -> 1.0 / tan(inner)
            "ln" -> ln(inner)
            "exp" -> exp(inner)
            "sqrt" -> sqrt(inner)
            "abs" -> abs(inner)
            else -> 0.0
        }
    }
    override fun simplify(): SymExpr {
        val sa = a.simplify()
        if (sa is SymConst && name == "abs") return SymConst(abs(sa.v))
        return SymFunc(name, sa)
    }
}

fun explainDiffSteps(expr: SymExpr, wrt: String = "x"): List<String> {
    val steps = mutableListOf<String>()
    fun walk(node: SymExpr) {
        when (node) {
            is SymAdd -> {
                steps.add("${Translator.tr("Toplam türev kuralı:")} $$ \\frac{d}{d$wrt}\\left(${node.a.toLatex()} + ${node.b.toLatex()}\\right) = \\frac{d}{d$wrt}\\left(${node.a.toLatex()}\\right) + \\frac{d}{d$wrt}\\left(${node.b.toLatex()}\\right) $$")
                walk(node.a)
                walk(node.b)
            }
            is SymSub -> {
                steps.add("${Translator.tr("Fark türev kuralı:")} $$ \\frac{d}{d$wrt}\\left(${node.a.toLatex()} - ${node.b.toLatex()}\\right) = \\frac{d}{d$wrt}\\left(${node.a.toLatex()}\\right) - \\frac{d}{d$wrt}\\left(${node.b.toLatex()}\\right) $$")
                walk(node.a)
                walk(node.b)
            }
            is SymMul -> {
                if (node.a is SymConst) {
                    val dInner = node.b.diff(wrt).simplify()
                    steps.add("${Translator.tr("Sabit çarpan ve kuvvet kuralı:")} $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${node.a.toLatex()} \\cdot \\left(${dInner.toLatex()}\\right) = ${node.diff(wrt).simplify().toLatex()} $$")
                } else {
                    val da = node.a.diff(wrt).simplify()
                    val db = node.b.diff(wrt).simplify()
                    steps.add("${Translator.tr("Çarpım türev kuralı")} $(u \\cdot v)' = u'v + uv'$: $$ \\left(${da.toLatex()}\\right)\\cdot\\left(${node.b.toLatex()}\\right) + \\left(${node.a.toLatex()}\\right)\\cdot\\left(${db.toLatex()}\\right) $$")
                }
            }
            is SymDiv -> {
                val da = node.a.diff(wrt).simplify()
                val db = node.b.diff(wrt).simplify()
                steps.add("${Translator.tr("Bölüm türev kuralı")} $(u/v)' = \\frac{u'v - uv'}{v^2}$: $$ \\frac{\\left(${da.toLatex()}\\right)\\left(${node.b.toLatex()}\\right) - \\left(${node.a.toLatex()}\\right)\\left(${db.toLatex()}\\right)}{\\left(${node.b.toLatex()}\\right)^2} $$")
            }
            is SymPow -> {
                val res = node.diff(wrt).simplify()
                steps.add("${Translator.tr("Kuvvet türev kuralı")} $\\frac{d}{d$wrt}(u^n) = n \\cdot u^{n-1} \\cdot u'$: $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
            }
            is SymGenPow -> {
                val res = node.diff(wrt).simplify()
                steps.add("${Translator.tr("Fonksiyon ve zincir türev kuralı:")} $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
            }
            is SymFunc -> {
                if (node.name == "ln" && node.a is SymFunc && node.a.name == "abs") {
                    val u = node.a.a
                    val res = node.diff(wrt).simplify()
                    steps.add("${Translator.tr("Doğal logaritma ve mutlak değer türev kuralı")} $ \\frac{d}{d$wrt}(\\ln|u|) = \\frac{u'}{u} $: $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
                    walk(u)
                    return
                } else if (node.name == "ln") {
                    val res = node.diff(wrt).simplify()
                    steps.add("${Translator.tr("Doğal logaritma türev kuralı")} $ \\frac{d}{d$wrt}(\\ln(u)) = \\frac{u'}{u} $: $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
                    walk(node.a)
                    return
                } else if (node.name == "abs") {
                    val res = node.diff(wrt).simplify()
                    steps.add("${Translator.tr("Mutlak değer türev kuralı")} $ \\frac{d}{d$wrt}(|u|) = \\frac{u \\cdot u'}{|u|} $: $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
                    walk(node.a)
                    return
                }
                val res = node.diff(wrt).simplify()
                steps.add("${Translator.tr("Fonksiyon ve zincir türev kuralı:")} $$ \\frac{d}{d$wrt}\\left(${node.toLatex()}\\right) = ${res.toLatex()} $$")
            }
            is SymVar -> {
                val res = node.diff(wrt).simplify()
                steps.add("$$ \\frac{d}{d$wrt}(${node.name}) = ${res.toLatex()} $$")
            }
            is SymConst -> {
                steps.add("${Translator.tr("Sabit sayının türevi sıfırdır:")} $$ \\frac{d}{d$wrt}(${node.toLatex()}) = 0 $$")
            }
        }
    }
    walk(expr)
    return steps
}

fun parseSymExpr(expr: String): SymExpr {
    class Parser(val str: String) {
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

        fun parsePrimary(): SymExpr {
            while (ch == ' '.code) nextChar()
            val startPos = pos
            var x: SymExpr
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (eat('|'.code)) {
                val inner = parseExpression()
                eat('|'.code)
                x = SymFunc("abs", inner)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = SymConst(str.substring(startPos, pos).toDouble())
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch == 'π'.code || ch == '√'.code) {
                if (ch == 'π'.code) {
                    nextChar()
                    x = SymConst(Math.PI)
                } else if (ch == '√'.code) {
                    nextChar()
                    val inner = if (eat('('.code)) {
                        val e = parseExpression()
                        eat(')'.code)
                        e
                    } else parsePrimary()
                    x = SymFunc("sqrt", inner)
                } else {
                    while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                    val name = str.substring(startPos, pos).lowercase()
                    when {
                        name == "pi" -> x = SymConst(Math.PI)
                        name == "e" -> {
                            if (eat('^'.code)) {
                                val expExpr = if (eat('('.code)) {
                                    val e = parseExpression()
                                    eat(')'.code)
                                    e
                                } else parseFactor()
                                return SymFunc("exp", expExpr)
                            }
                            x = SymConst(Math.E)
                        }
                        name in listOf("sin", "cos", "tan", "cot", "ln", "log", "exp", "sqrt", "abs") -> {
                            val inner = if (eat('('.code)) {
                                val e = parseExpression()
                                eat(')'.code)
                                e
                            } else parseFactor()
                            x = SymFunc(if (name == "log") "ln" else name, inner)
                        }
                        else -> x = SymVar(name)
                    }
                }
            } else {
                throw RuntimeException("Beklenmeyen Karakter: " + ch.toChar())
            }

            if (eat('^'.code)) {
                val isNeg = eat('-'.code)
                val rawExp = if (eat('('.code)) {
                    val e = parseExpression()
                    eat(')'.code)
                    e
                } else {
                    parsePrimary()
                }
                val signedExp = (if (isNeg) SymMul(SymConst(-1.0), rawExp) else rawExp).simplify()
                x = if (signedExp is SymConst) {
                    SymPow(x, signedExp.v)
                } else {
                    SymGenPow(x, signedExp)
                }
            }
            return x
        }

        fun parseFactor(): SymExpr {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return SymMul(SymConst(-1.0), parseFactor()).simplify()
            var x = parsePrimary()
            // Implicit multiplication: e.g. 3x^2, 2(x+1), x sin(x)
            while (ch == ' '.code) nextChar()
            while (ch == '('.code || ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code || ch == 'π'.code || ch == '√'.code) {
                x = SymMul(x, parsePrimary()).simplify()
                while (ch == ' '.code) nextChar()
            }
            return x
        }

        fun parseTerm(): SymExpr {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x = SymMul(x, parseFactor())
                else if (eat('/'.code)) x = SymDiv(x, parseFactor())
                else return x
            }
        }

        fun parseExpression(): SymExpr {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x = SymAdd(x, parseTerm())
                else if (eat('-'.code)) x = SymSub(x, parseTerm())
                else return x
            }
        }

        fun parse(): SymExpr {
            nextChar()
            val res = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < str.length) throw RuntimeException("Beklenmeyen Karakter: " + ch.toChar())
            return res.simplify()
        }
    }
    return Parser(expr.trim()).parse()
}
