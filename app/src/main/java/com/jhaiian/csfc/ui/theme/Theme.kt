package com.jhaiian.csfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class CalculatorColors(
    val keyNumber: androidx.compose.ui.graphics.Color,
    val keyNumberText: androidx.compose.ui.graphics.Color,
    val keyOperator: androidx.compose.ui.graphics.Color,
    val keyOperatorText: androidx.compose.ui.graphics.Color,
    val keyActive: androidx.compose.ui.graphics.Color,
    val keyActiveText: androidx.compose.ui.graphics.Color,
    val displayExpression: androidx.compose.ui.graphics.Color,
    val displayResult: androidx.compose.ui.graphics.Color,
)

private val DarkCalculatorColors = CalculatorColors(
    keyNumber = CsfcKeyNumber,
    keyNumberText = CsfcKeyNumberText,
    keyOperator = CsfcKeyOperator,
    keyOperatorText = CsfcKeyOperatorText,
    keyActive = CsfcKeyActive,
    keyActiveText = CsfcKeyActiveText,
    displayExpression = CsfcDisplayExpression,
    displayResult = CsfcDisplayResult,
)

private val LightCalculatorColors = DarkCalculatorColors.copy(
    keyNumber = androidx.compose.ui.graphics.Color(0xFFE8E2EC),
    keyNumberText = androidx.compose.ui.graphics.Color(0xFF1D1B20),
)

val LocalCalculatorColors = staticCompositionLocalOf { DarkCalculatorColors }

private val CsfcDarkScheme = darkColorScheme(
    background = CsfcSurfaceDark,
    surface = CsfcSurfaceDark,
    primary = CsfcAccent,
    onPrimary = OnSurface,
    secondary = CsfcKeyActive,
    onSecondary = CsfcKeyActiveText,
    onBackground = OnSurface,
    onSurface = OnSurface,
)

private val CsfcLightScheme = lightColorScheme(
    primary = CsfcAccent,
    onPrimary = OnSurface,
    secondary = CsfcKeyActive,
    onSecondary = CsfcKeyActiveText,
)

/**
 * The reference design is dark-only, so this defaults to the dark palette regardless of
 * system setting. [followSystem] is left available for whoever wants a light variant later.
 */
@Composable
fun CSFCTheme(
    followSystem: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = if (followSystem) isSystemInDarkTheme() else true
    val colorScheme = if (useDark) CsfcDarkScheme else CsfcLightScheme
    val calculatorColors = if (useDark) DarkCalculatorColors else LightCalculatorColors

    CompositionLocalProvider(LocalCalculatorColors provides calculatorColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CsfcTypography,
            content = content,
        )
    }
}

object CalculatorTheme {
    val colors: CalculatorColors
        @Composable
        get() = LocalCalculatorColors.current
}
