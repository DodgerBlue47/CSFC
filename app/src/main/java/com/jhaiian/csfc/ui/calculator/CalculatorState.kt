package com.jhaiian.csfc.ui.calculator

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

sealed interface CalculatorAction {
    data class Digit(val digit: Char) : CalculatorAction
    data object Decimal : CalculatorAction
    data class Operator(val symbol: String) : CalculatorAction
    data class FunctionKey(val name: String) : CalculatorAction
    data class ConstantKey(val symbol: String) : CalculatorAction
    data object Parenthesis : CalculatorAction
    data object Percent : CalculatorAction
    data object Factorial : CalculatorAction
    data object Backspace : CalculatorAction
    data object Clear : CalculatorAction
    data object Equals : CalculatorAction
    data object ToggleDegrees : CalculatorAction
    data object ToggleInverse : CalculatorAction
}

sealed interface ResultDisplay {
    data object Blank : ResultDisplay
    data object Error : ResultDisplay
    data class Value(val formatted: String) : ResultDisplay
}

private const val MAX_LENGTH = 100

data class CalculatorUiState(
    val fieldValue: TextFieldValue = TextFieldValue(""),
    val isDegrees: Boolean = true,
    val isInverse: Boolean = false,
    val isError: Boolean = false,
    val justEvaluated: Boolean = false,
) {
    // What operator, if any, sits immediately left of the cursor — used to highlight
    // the pending operator key the same way the reference design highlights it.
    val activeOperator: String?
        get() {
            val tokens = CalculatorEngine.tokenize(fieldValue.text.substring(0, fieldValue.selection.min)) ?: return null
            return (tokens.lastOrNull() as? CalcToken.Op)?.symbol
        }

    val resultDisplay: ResultDisplay
        get() = when {
            isError -> ResultDisplay.Error
            justEvaluated || fieldValue.text.isEmpty() -> ResultDisplay.Blank
            else -> CalculatorEngine.tokenize(fieldValue.text)
                ?.let { CalculatorEngine.tryEvaluate(it, isDegrees) }
                ?.let { ResultDisplay.Value(CalculatorEngine.formatResultForDisplay(it)) }
                ?: ResultDisplay.Blank
        }
}

fun reduceCalculatorState(state: CalculatorUiState, action: CalculatorAction): CalculatorUiState {
    if (state.isError && action != CalculatorAction.Clear) {
        return reduceCalculatorState(freshState(state), action)
    }

    // Digits/functions/constants/parens start a new entry after "="; operators and
    // postfix keys continue from the result that's now sitting in the field.
    fun startFresh() = if (state.justEvaluated) TextFieldValue("") else state.fieldValue

    return when (action) {
        CalculatorAction.Clear -> freshState(state)
        is CalculatorAction.Digit -> state.copy(
            fieldValue = insertText(startFresh(), action.digit.toString()),
            justEvaluated = false,
        )
        CalculatorAction.Decimal -> state.copy(fieldValue = insertText(startFresh(), "."), justEvaluated = false)
        is CalculatorAction.Operator -> state.copy(
            fieldValue = insertText(state.fieldValue, action.symbol),
            justEvaluated = false,
        )
        is CalculatorAction.FunctionKey -> state.copy(
            fieldValue = insertText(startFresh(), action.name + "("),
            justEvaluated = false,
        )
        is CalculatorAction.ConstantKey -> state.copy(
            fieldValue = insertText(startFresh(), action.symbol),
            justEvaluated = false,
        )
        CalculatorAction.Parenthesis -> {
            val base = startFresh()
            state.copy(fieldValue = insertText(base, smartParenText(base)), justEvaluated = false)
        }
        CalculatorAction.Percent -> if (isCompleteValueBeforeCursor(state.fieldValue)) {
            state.copy(fieldValue = insertText(state.fieldValue, "%"), justEvaluated = false)
        } else {
            state
        }
        CalculatorAction.Factorial -> if (isCompleteValueBeforeCursor(state.fieldValue)) {
            state.copy(fieldValue = insertText(state.fieldValue, "!"), justEvaluated = false)
        } else {
            state
        }
        CalculatorAction.Backspace -> state.copy(fieldValue = backspaceText(state.fieldValue), justEvaluated = false)
        CalculatorAction.Equals -> {
            val tokens = CalculatorEngine.tokenize(state.fieldValue.text)
            val result = tokens?.let { CalculatorEngine.tryEvaluate(it, state.isDegrees) }
            if (result == null) {
                state.copy(isError = true, justEvaluated = true)
            } else {
                val text = CalculatorEngine.continuationText(result)
                state.copy(fieldValue = TextFieldValue(text, TextRange(text.length)), justEvaluated = true)
            }
        }
        CalculatorAction.ToggleDegrees -> state.copy(isDegrees = !state.isDegrees)
        CalculatorAction.ToggleInverse -> state.copy(isInverse = !state.isInverse)
    }
}

private fun freshState(state: CalculatorUiState) =
    CalculatorUiState(isDegrees = state.isDegrees, isInverse = state.isInverse)

private fun insertText(value: TextFieldValue, insert: String): TextFieldValue {
    val text = value.text
    val start = value.selection.min
    val end = value.selection.max
    if (text.length - (end - start) + insert.length > MAX_LENGTH) return value
    val newText = text.substring(0, start) + insert + text.substring(end)
    return TextFieldValue(newText, TextRange(start + insert.length))
}

private fun backspaceText(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val start = value.selection.min
    val end = value.selection.max
    return when {
        start != end -> TextFieldValue(text.substring(0, start) + text.substring(end), TextRange(start))
        start > 0 -> TextFieldValue(text.substring(0, start - 1) + text.substring(start), TextRange(start - 1))
        else -> value
    }
}

private fun isCompleteValueBeforeCursor(value: TextFieldValue): Boolean {
    val tokens = CalculatorEngine.tokenize(value.text.substring(0, value.selection.min)) ?: return false
    return CalculatorEngine.isCompleteValue(tokens.lastOrNull())
}

private fun smartParenText(value: TextFieldValue): String {
    val before = value.text.substring(0, value.selection.min)
    val tokens = CalculatorEngine.tokenize(before) ?: return "("
    val openCount = tokens.count { it is CalcToken.LParen }
    val closeCount = tokens.count { it is CalcToken.RParen }
    return if (openCount > closeCount && CalculatorEngine.isCompleteValue(tokens.lastOrNull())) ")" else "("
}
