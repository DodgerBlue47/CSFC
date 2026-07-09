package com.jhaiian.csfc.ui.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var uiState by mutableStateOf(CalculatorUiState())
        private set

    fun dispatch(action: CalculatorAction) {
        uiState = reduceCalculatorState(uiState, action)
    }

    // The field is read-only so the software keyboard never appears, but taps and
    // drags still move the cursor/selection and arrive here. Text itself never
    // changes through this path. Leaving "just evaluated" clears the result styling,
    // since touching the field again means the user is editing from that point.
    fun onFieldValueChange(new: TextFieldValue) {
        uiState = uiState.copy(fieldValue = new, justEvaluated = false)
    }
}
