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

    private const val MAX_DIGITS = 15

    private val usSymbols = DecimalFormatSymbols(Locale.US)
    private val groupedFormat = DecimalFormat("#,##0.##########", usSymbols)
    private val plainFormat = DecimalFormat("0.##########", usSymbols)

    fun appendDigit(tokens: List<CalcToken>, digit: Char): List<CalcToken> {
        val last = tokens.lastOrNull()
        return if (last is CalcToken.Num) {
            if (last.text.count { it != '.' } >= MAX_DIGITS) return tokens
            tokens.dropLast(1) + CalcToken.Num(last.text + digit)
        } else {
            tokens + CalcToken.Num(digit.toString())
        }
    }

    fun appendDecimal(tokens: List<CalcToken>): List<CalcToken> {
        val last = tokens.lastOrNull()
        return if (last is CalcToken.Num) {
            if (last.text.contains('.')) tokens else tokens.dropLast(1) + CalcToken.Num(last.text + ".")
        } else {
            tokens + CalcToken.Num("0.")
        }
    }

    fun appendConstant(tokens: List<CalcToken>, symbol: String): List<CalcToken> =
        tokens + CalcToken.Const(symbol)

    fun appendFunction(tokens: List<CalcToken>, name: String): List<CalcToken> =
        tokens + CalcToken.Func(name) + CalcToken.LParen

    fun appendParen(tokens: List<CalcToken>): List<CalcToken> {
        val openCount = tokens.count { it is CalcToken.LParen }
        val closeCount = tokens.count { it is CalcToken.RParen }
        val last = tokens.lastOrNull()
        val canClose = openCount > closeCount && isCompleteValue(last)
        return tokens + if (canClose) CalcToken.RParen else CalcToken.LParen
    }

    fun appendPercent(tokens: List<CalcToken>): List<CalcToken> {
        val last = tokens.lastOrNull()
        return if (isCompleteValue(last)) tokens + CalcToken.Percent else tokens
    }

    fun appendFactorial(tokens: List<CalcToken>): List<CalcToken> {
        val last = tokens.lastOrNull()
        return if (isCompleteValue(last)) tokens + CalcToken.Factorial else tokens
    }

    // '-' after an operator stacks as a leading sign for the next operand instead of
    // replacing it, since "5 x -3" is a valid, common thing to type. Any other operator
    // tapped while one is already pending replaces the whole pending run.
    fun appendOperator(tokens: List<CalcToken>, newOp: String): List<CalcToken> {
        val last = tokens.lastOrNull()
        if (last == null || last is CalcToken.LParen) {
            return if (newOp == "−") tokens + CalcToken.Op("−") else tokens
        }
        val runLength = trailingOperatorRunLength(tokens)
        if (runLength == 0) return tokens + CalcToken.Op(newOp)
        val lastOp = last as CalcToken.Op
        return if (runLength == 1 && newOp == "−" && lastOp.symbol != "−") {
            tokens + CalcToken.Op("−")
        } else {
            tokens.dropLast(runLength) + CalcToken.Op(newOp)
        }
    }

    fun backspace(tokens: List<CalcToken>): List<CalcToken> {
        if (tokens.isEmpty()) return tokens
        val last = tokens.last()
        if (last is CalcToken.Num && last.text.length > 1) {
            return tokens.dropLast(1) + CalcToken.Num(last.text.dropLast(1))
        }
        val withoutLast = tokens.dropLast(1)
        return if (last is CalcToken.LParen && withoutLast.lastOrNull() is CalcToken.Func) {
            withoutLast.dropLast(1)
        } else {
            withoutLast
        }
    }

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

    fun rawNumberString(value: Double): String = plainFormat.format(value)

    fun formatResultForDisplay(value: Double): String {
        if (value == 0.0) return "0"
        val abs = Math.abs(value)
        return if (abs < 1e-9 || abs >= 1e15) {
            String.format(Locale.US, "%.6e", value)
        } else {
            groupedFormat.format(value)
        }
    }

    fun tokensToDisplayString(tokens: List<CalcToken>): String {
        val sb = StringBuilder()
        for (t in tokens) {
            when (t) {
                is CalcToken.Num -> sb.append(formatNumberForDisplay(t.text))
                is CalcToken.Op -> sb.append(t.symbol)
                is CalcToken.LParen -> sb.append('(')
                is CalcToken.RParen -> sb.append(')')
                is CalcToken.Func -> sb.append(t.name)
                is CalcToken.Const -> sb.append(t.symbol)
                is CalcToken.Percent -> sb.append('%')
                is CalcToken.Factorial -> sb.append('!')
            }
        }
        return sb.toString()
    }

    private fun isCompleteValue(token: CalcToken?): Boolean = token is CalcToken.Num ||
        token is CalcToken.RParen || token is CalcToken.Const || token is CalcToken.Percent ||
        token is CalcToken.Factorial

    private fun trailingOperatorRunLength(tokens: List<CalcToken>): Int {
        var count = 0
        var i = tokens.size - 1
        while (i >= 0 && tokens[i] is CalcToken.Op) { count++; i-- }
        return count
    }

    private fun balanceParens(tokens: List<CalcToken>): List<CalcToken> {
        val open = tokens.count { it is CalcToken.LParen }
        val close = tokens.count { it is CalcToken.RParen }
        return if (open > close) tokens + List(open - close) { CalcToken.RParen } else tokens
    }

    private fun formatNumberForDisplay(raw: String): String {
        if (raw.isEmpty()) return raw
        val parts = raw.split(".", limit = 2)
        val intPart = parts[0].ifEmpty { "0" }
        val grouped = intPart.toLongOrNull()?.let { groupedFormat.format(it).substringBefore('.') } ?: intPart
        return if (parts.size == 2) "$grouped.${parts[1]}" else grouped
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
