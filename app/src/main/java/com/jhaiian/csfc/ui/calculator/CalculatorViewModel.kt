package com.jhaiian.csfc.ui.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var uiState by mutableStateOf(CalculatorUiState())
        private set

    fun dispatch(action: CalculatorAction) {
        uiState = reduceCalculatorState(uiState, action)
    }

    // Called when the expression is tapped; index is a character offset resolved from
    // the tap position via the displayed Text's own layout result.
    fun moveCursor(index: Int) {
        val clamped = index.coerceIn(0, uiState.fieldValue.text.length)
        uiState = uiState.copy(
            fieldValue = uiState.fieldValue.copy(selection = TextRange(clamped)),
            justEvaluated = false,
        )
    }
}
