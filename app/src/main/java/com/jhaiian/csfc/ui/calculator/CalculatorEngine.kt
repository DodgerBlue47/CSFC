package com.jhaiian.csfc.ui.calculator

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed interface CalcToken {
    data class Num(val text: String) : CalcToken
    data class Op(val symbol: String) : CalcToken
    data object LParen : CalcToken
    data object RParen : CalcToken
    data class Func(val name: String) : CalcToken
    data class Const(val symbol: String) : CalcToken
    data object Percent : CalcToken
    data object Factorial : CalcToken
}

object CalculatorEngine {

    // Longer patterns first so e.g. "10ˣ(" is matched before its leading digits are
    // read as a plain number, and "eˣ(" before a bare constant "e".
    private val functionPatterns = listOf(
        "sin⁻¹(" to "sin⁻¹",
        "cos⁻¹(" to "cos⁻¹",
        "tan⁻¹(" to "tan⁻¹",
        "sin(" to "sin",
        "cos(" to "cos",
        "tan(" to "tan",
        "log(" to "log",
        "ln(" to "ln",
        "10ˣ(" to "10ˣ",
        "eˣ(" to "eˣ",
        "x²(" to "x²",
        "√(" to "√",
    )

    private val usSymbols = DecimalFormatSymbols(Locale.US).apply { minusSign = '−' }
    private val groupedFormat = DecimalFormat("#,##0.##########", usSymbols)
    private val plainFormat = DecimalFormat("0.##########", usSymbols)

    // Turns the raw text the user has typed/edited into tokens. Returns null for text
    // that doesn't parse at the character level at all (e.g. cursor-editing left behind
    // half of a function name) so callers can fail soft instead of crashing.
    fun tokenize(raw: String): List<CalcToken>? {
        val tokens = mutableListOf<CalcToken>()
        var i = 0
        outer@ while (i < raw.length) {
            for ((pattern, name) in functionPatterns) {
                if (raw.startsWith(pattern, i)) {
                    tokens += CalcToken.Func(name)
                    tokens += CalcToken.LParen
                    i += pattern.length
                    continue@outer
                }
            }
            val c = raw[i]
            when {
                c.isDigit() || c == '.' -> {
                    val digits = StringBuilder()
                    while (i < raw.length && (raw[i].isDigit() || raw[i] == '.' || raw[i] == ',')) {
                        if (raw[i] != ',') digits.append(raw[i])
                        i++
                    }
                    tokens += CalcToken.Num(digits.toString())
                }
                c == '+' -> { tokens += CalcToken.Op("+"); i++ }
                c == '−' -> { tokens += CalcToken.Op("−"); i++ }
                c == '×' -> { tokens += CalcToken.Op("×"); i++ }
                c == '÷' -> { tokens += CalcToken.Op("÷"); i++ }
                c == '^' -> { tokens += CalcToken.Op("^"); i++ }
                c == '(' -> { tokens += CalcToken.LParen; i++ }
                c == ')' -> { tokens += CalcToken.RParen; i++ }
                c == '!' -> { tokens += CalcToken.Factorial; i++ }
                c == '%' -> { tokens += CalcToken.Percent; i++ }
                c == 'π' -> { tokens += CalcToken.Const("π"); i++ }
                c == 'e' -> { tokens += CalcToken.Const("e"); i++ }
                c == ',' -> { i++ }
                else -> return null
            }
        }
        return tokens
    }

    fun isCompleteValue(token: CalcToken?): Boolean = token is CalcToken.Num ||
        token is CalcToken.RParen || token is CalcToken.Const || token is CalcToken.Percent ||
        token is CalcToken.Factorial

    fun tryEvaluate(tokens: List<CalcToken>, isDegrees: Boolean): Double? {
        if (tokens.isEmpty()) return null
        return try {
            val balanced = balanceParens(tokens)
            val result = ExpressionParser(balanced, isDegrees).parseFully()
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (e: Exception) {
            null
        }
    }

    // Text fed back into the editable field after "=". Grouped like the preview was
    // for normal magnitudes (matches the reference behavior); for extreme values this
    // deliberately skips scientific notation, since "e" would otherwise be re-read as
    // Euler's number by the tokenizer if the user keeps calculating from it.
    fun continuationText(value: Double): String {
        if (value == 0.0) return "0"
        val abs = Math.abs(value)
        return if (abs < 1e-9 || abs >= 1e15) plainFormat.format(value) else groupedFormat.format(value)
    }

    fun formatResultForDisplay(value: Double): String {
        if (value == 0.0) return "0"
        val abs = Math.abs(value)
        return if (abs < 1e-9 || abs >= 1e15) {
            String.format(Locale.US, "%.6e", value)
        } else {
            groupedFormat.format(value)
        }
    }

    private fun balanceParens(tokens: List<CalcToken>): List<CalcToken> {
        val open = tokens.count { it is CalcToken.LParen }
        val close = tokens.count { it is CalcToken.RParen }
        return if (open > close) tokens + List(open - close) { CalcToken.RParen } else tokens
    }
}

// Recursive-descent parser. Precedence, low to high: + - , * / (with implicit
// multiplication between adjacent values), unary -, ^ (right-associative), postfix ! and %.
private class ExpressionParser(private val tokens: List<CalcToken>, private val isDegrees: Boolean) {
    private var pos = 0

    fun parseFully(): Double {
        val result = parseExpression()
        check(pos == tokens.size) { "Unexpected trailing tokens" }
        return result
    }

    private fun peek(): CalcToken? = tokens.getOrNull(pos)

    private fun startsValue(t: CalcToken?): Boolean =
        t is CalcToken.Num || t is CalcToken.Const || t is CalcToken.Func || t is CalcToken.LParen

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            val t = peek()
            when {
                t is CalcToken.Op && t.symbol == "+" -> { pos++; value += parseTerm() }
                t is CalcToken.Op && t.symbol == "−" -> { pos++; value -= parseTerm() }
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseUnary()
        while (true) {
            val t = peek()
            when {
                t is CalcToken.Op && t.symbol == "×" -> { pos++; value *= parseUnary() }
                t is CalcToken.Op && t.symbol == "÷" -> {
                    pos++
                    val divisor = parseUnary()
                    check(divisor != 0.0) { "Division by zero" }
                    value /= divisor
                }
                startsValue(t) -> value *= parseUnary()
                else -> return value
            }
        }
    }

    private fun parseUnary(): Double {
        val t = peek()
        return if (t is CalcToken.Op && t.symbol == "−") {
            pos++
            -parseUnary()
        } else {
            parsePower()
        }
    }

    private fun parsePower(): Double {
        val base = parsePostfix()
        val t = peek()
        return if (t is CalcToken.Op && t.symbol == "^") {
            pos++
            Math.pow(base, parseUnary())
        } else {
            base
        }
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()
        while (true) {
            when (peek()) {
                is CalcToken.Percent -> { pos++; value /= 100.0 }
                is CalcToken.Factorial -> { pos++; value = factorial(value) }
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        val t = peek() ?: error("Unexpected end of expression")
        return when (t) {
            is CalcToken.Num -> {
                pos++
                t.text.trimEnd('.').ifEmpty { "0" }.toDouble()
            }
            is CalcToken.Const -> {
                pos++
                if (t.symbol == "π") Math.PI else Math.E
            }
            is CalcToken.LParen -> {
                pos++
                val value = parseExpression()
                check(peek() is CalcToken.RParen) { "Missing closing parenthesis" }
                pos++
                value
            }
            is CalcToken.Func -> {
                pos++
                applyFunction(t.name, parsePostfix())
            }
            else -> error("Unexpected token")
        }
    }

    private fun applyFunction(name: String, x: Double): Double = when (name) {
        "sin" -> Math.sin(toRadiansIfNeeded(x))
        "cos" -> Math.cos(toRadiansIfNeeded(x))
        "tan" -> Math.tan(toRadiansIfNeeded(x))
        "sin⁻¹" -> { check(x in -1.0..1.0) { "Domain" }; fromRadiansIfNeeded(Math.asin(x)) }
        "cos⁻¹" -> { check(x in -1.0..1.0) { "Domain" }; fromRadiansIfNeeded(Math.acos(x)) }
        "tan⁻¹" -> fromRadiansIfNeeded(Math.atan(x))
        "ln" -> { check(x > 0.0) { "Domain" }; Math.log(x) }
        "log" -> { check(x > 0.0) { "Domain" }; Math.log10(x) }
        "eˣ" -> Math.exp(x)
        "10ˣ" -> Math.pow(10.0, x)
        "√" -> { check(x >= 0.0) { "Domain" }; Math.sqrt(x) }
        "x²" -> x * x
        else -> error("Unknown function $name")
    }

    private fun toRadiansIfNeeded(x: Double) = if (isDegrees) Math.toRadians(x) else x
    private fun fromRadiansIfNeeded(x: Double) = if (isDegrees) Math.toDegrees(x) else x

    private fun factorial(x: Double): Double {
        check(x >= 0.0 && x == Math.floor(x) && x <= 170.0) { "Domain" }
        var result = 1.0
        for (i in 2..x.toInt()) result *= i
        return result
    }
}
