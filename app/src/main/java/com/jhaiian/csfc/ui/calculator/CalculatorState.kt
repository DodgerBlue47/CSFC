package com.jhaiian.csfc.ui.calculator

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

data class CalculatorUiState(
    val tokens: List<CalcToken> = emptyList(),
    val isDegrees: Boolean = true,
    val isInverse: Boolean = false,
    val isError: Boolean = false,
    val justEvaluated: Boolean = false,
) {
    val activeOperator: String?
        get() = (tokens.lastOrNull() as? CalcToken.Op)?.symbol

    val resultDisplay: ResultDisplay
        get() = when {
            isError -> ResultDisplay.Error
            tokens.isEmpty() -> ResultDisplay.Blank
            else -> CalculatorEngine.tryEvaluate(tokens, isDegrees)
                ?.let { ResultDisplay.Value(CalculatorEngine.formatResultForDisplay(it)) }
                ?: ResultDisplay.Blank
        }
}

fun reduceCalculatorState(state: CalculatorUiState, action: CalculatorAction): CalculatorUiState {
    if (state.isError && action != CalculatorAction.Clear) {
        return reduceCalculatorState(freshState(state), action)
    }

    return when (action) {
        CalculatorAction.Clear -> freshState(state)
        is CalculatorAction.Digit -> state.copy(
            tokens = CalculatorEngine.appendDigit(if (state.justEvaluated) emptyList() else state.tokens, action.digit),
            justEvaluated = false,
        )
        CalculatorAction.Decimal -> state.copy(
            tokens = CalculatorEngine.appendDecimal(if (state.justEvaluated) emptyList() else state.tokens),
            justEvaluated = false,
        )
        is CalculatorAction.Operator -> state.copy(
            tokens = CalculatorEngine.appendOperator(
                if (state.justEvaluated) continuedTokens(state) else state.tokens,
                action.symbol,
            ),
            justEvaluated = false,
        )
        is CalculatorAction.FunctionKey -> state.copy(
            tokens = CalculatorEngine.appendFunction(if (state.justEvaluated) emptyList() else state.tokens, action.name),
            justEvaluated = false,
        )
        is CalculatorAction.ConstantKey -> state.copy(
            tokens = CalculatorEngine.appendConstant(if (state.justEvaluated) emptyList() else state.tokens, action.symbol),
            justEvaluated = false,
        )
        CalculatorAction.Parenthesis -> state.copy(
            tokens = CalculatorEngine.appendParen(if (state.justEvaluated) emptyList() else state.tokens),
            justEvaluated = false,
        )
        CalculatorAction.Percent -> state.copy(tokens = CalculatorEngine.appendPercent(state.tokens), justEvaluated = false)
        CalculatorAction.Factorial -> state.copy(tokens = CalculatorEngine.appendFactorial(state.tokens), justEvaluated = false)
        CalculatorAction.Backspace -> if (state.justEvaluated) {
            freshState(state)
        } else {
            state.copy(tokens = CalculatorEngine.backspace(state.tokens))
        }
        CalculatorAction.Equals -> {
            val result = CalculatorEngine.tryEvaluate(state.tokens, state.isDegrees)
            if (result == null) state.copy(isError = true, justEvaluated = true) else state.copy(justEvaluated = true)
        }
        CalculatorAction.ToggleDegrees -> state.copy(isDegrees = !state.isDegrees)
        CalculatorAction.ToggleInverse -> state.copy(isInverse = !state.isInverse)
    }
}

private fun freshState(state: CalculatorUiState) = CalculatorUiState(isDegrees = state.isDegrees, isInverse = state.isInverse)

// After "=", tapping an operator continues from the previous result rather than starting over.
private fun continuedTokens(state: CalculatorUiState): List<CalcToken> {
    val previous = CalculatorEngine.tryEvaluate(state.tokens, state.isDegrees) ?: return emptyList()
    return listOf(CalcToken.Num(CalculatorEngine.rawNumberString(previous)))
}
