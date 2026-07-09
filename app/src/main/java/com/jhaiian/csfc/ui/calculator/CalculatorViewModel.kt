package com.jhaiian.csfc.ui.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CalculatorViewModel : ViewModel() {

    var uiState by mutableStateOf(CalculatorUiState())
        private set

    fun dispatch(action: CalculatorAction) {
        uiState = reduceCalculatorState(uiState, action)
    }
}
