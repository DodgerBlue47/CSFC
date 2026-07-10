package com.jhaiian.csfc.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhaiian.csfc.R
import com.jhaiian.csfc.ui.theme.CSFCTheme
import com.jhaiian.csfc.ui.theme.CalculatorTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BACKSPACE_INITIAL_DELAY_MS = 450L
private const val BACKSPACE_REPEAT_INTERVAL_MS = 90L
private const val CURSOR_BLINK_MS = 530L

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel(),
    onOpenCrashLogs: () -> Unit = {},
) {
    val uiState = viewModel.uiState
    val colors = CalculatorTheme.colors
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val resultText = when (val result = uiState.resultDisplay) {
        is ResultDisplay.Value -> result.formatted
        ResultDisplay.Error -> stringResource(R.string.error_label)
        ResultDisplay.Blank -> ""
    }
    val bigTextColor = if (uiState.justEvaluated) colors.displayResult else colors.displayExpression

    fun operatorContainer(symbol: String) = if (uiState.activeOperator == symbol) colors.keyActive else colors.keyOperator
    fun operatorContent(symbol: String) = if (uiState.activeOperator == symbol) colors.keyActiveText else colors.keyOperatorText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(modifier = Modifier.statusBarsPadding(), onOpenCrashLogs = onOpenCrashLogs)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            ExpressionField(
                fieldValue = uiState.fieldValue,
                color = bigTextColor,
                placeholder = stringResource(R.string.key_0),
                onTap = { viewModel.moveCursor(it) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = resultText,
                color = colors.displayResult,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(24.dp))
        }

        ExpandToggle(
            expanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp),
        )

        Column(
            modifier = Modifier
                .weight(1.7f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .navigationBarsPadding(),
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(280)) + fadeIn(tween(280)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(140)),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    ScientificRow(
                        listOf(
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_sqrt_inv else R.string.key_sqrt)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "x²" else "√"))
                            },
                            SciKey(stringResource(R.string.key_pi)) {
                                viewModel.dispatch(CalculatorAction.ConstantKey("π"))
                            },
                            SciKey(stringResource(R.string.key_power)) {
                                viewModel.dispatch(CalculatorAction.Operator("^"))
                            },
                            SciKey(stringResource(R.string.key_factorial)) {
                                viewModel.dispatch(CalculatorAction.Factorial)
                            },
                        ),
                    )
                    ScientificRow(
                        listOf(
                            SciKey(stringResource(if (uiState.isDegrees) R.string.key_deg else R.string.key_rad)) {
                                viewModel.dispatch(CalculatorAction.ToggleDegrees)
                            },
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_sin_inv else R.string.key_sin)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "sin⁻¹" else "sin"))
                            },
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_cos_inv else R.string.key_cos)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "cos⁻¹" else "cos"))
                            },
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_tan_inv else R.string.key_tan)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "tan⁻¹" else "tan"))
                            },
                        ),
                    )
                    ScientificRow(
                        listOf(
                            SciKey(stringResource(R.string.key_inv), highlighted = uiState.isInverse) {
                                viewModel.dispatch(CalculatorAction.ToggleInverse)
                            },
                            SciKey(stringResource(R.string.key_e)) {
                                viewModel.dispatch(CalculatorAction.ConstantKey("e"))
                            },
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_ln_inv else R.string.key_ln)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "eˣ" else "ln"))
                            },
                            SciKey(stringResource(if (uiState.isInverse) R.string.key_log_inv else R.string.key_log)) {
                                viewModel.dispatch(CalculatorAction.FunctionKey(if (uiState.isInverse) "10ˣ" else "log"))
                            },
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                KeyRow {
                    CalculatorKey(
                        stringResource(R.string.key_ac),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { viewModel.dispatch(CalculatorAction.Clear) },
                    )
                    CalculatorKey(
                        stringResource(R.string.key_parentheses),
                        containerColor = colors.keyOperator,
                        contentColor = colors.keyOperatorText,
                        onClick = { viewModel.dispatch(CalculatorAction.Parenthesis) },
                    )
                    CalculatorKey(
                        stringResource(R.string.key_percent),
                        containerColor = colors.keyOperator,
                        contentColor = colors.keyOperatorText,
                        onClick = { viewModel.dispatch(CalculatorAction.Percent) },
                    )
                    CalculatorKey(
                        stringResource(R.string.key_divide),
                        containerColor = operatorContainer("÷"),
                        contentColor = operatorContent("÷"),
                        onClick = { viewModel.dispatch(CalculatorAction.Operator("÷")) },
                    )
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_7), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('7')) })
                    CalculatorKey(stringResource(R.string.key_8), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('8')) })
                    CalculatorKey(stringResource(R.string.key_9), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('9')) })
                    CalculatorKey(
                        stringResource(R.string.key_multiply),
                        containerColor = operatorContainer("×"),
                        contentColor = operatorContent("×"),
                        onClick = { viewModel.dispatch(CalculatorAction.Operator("×")) },
                    )
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_4), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('4')) })
                    CalculatorKey(stringResource(R.string.key_5), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('5')) })
                    CalculatorKey(stringResource(R.string.key_6), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('6')) })
                    CalculatorKey(
                        stringResource(R.string.key_subtract),
                        containerColor = operatorContainer("−"),
                        contentColor = operatorContent("−"),
                        onClick = { viewModel.dispatch(CalculatorAction.Operator("−")) },
                    )
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_1), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('1')) })
                    CalculatorKey(stringResource(R.string.key_2), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('2')) })
                    CalculatorKey(stringResource(R.string.key_3), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('3')) })
                    CalculatorKey(
                        stringResource(R.string.key_add),
                        containerColor = operatorContainer("+"),
                        contentColor = operatorContent("+"),
                        onClick = { viewModel.dispatch(CalculatorAction.Operator("+")) },
                    )
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_0), containerColor = colors.keyNumber, contentColor = colors.keyNumberText, onClick = { viewModel.dispatch(CalculatorAction.Digit('0')) })
                    CalculatorKey(
                        stringResource(R.string.key_decimal),
                        containerColor = colors.keyNumber,
                        contentColor = colors.keyNumberText,
                        onClick = { viewModel.dispatch(CalculatorAction.Decimal) },
                    )
                    BackspaceKey(
                        containerColor = colors.keyNumber,
                        contentColor = colors.keyNumberText,
                        contentDescription = stringResource(R.string.content_description_backspace),
                        onRepeat = { viewModel.dispatch(CalculatorAction.Backspace) },
                    )
                    CalculatorKey(
                        stringResource(R.string.key_equals),
                        containerColor = colors.keyActive,
                        contentColor = colors.keyActiveText,
                        onClick = { viewModel.dispatch(CalculatorAction.Equals) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(modifier: Modifier = Modifier, onOpenCrashLogs: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.content_description_more_options),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_crash_logs)) },
                    leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onOpenCrashLogs()
                    },
                )
            }
        }
    }
}

// Not a real text field: a plain Text that reports its own layout, a tap gesture that
// resolves a tap position to a character index via that layout, and a manually blinked
// bar drawn at that character's rect. Avoids fighting the platform IME/focus system
// entirely, since this never becomes a genuine input-connected field.
@Composable
private fun ExpressionField(
    fieldValue: TextFieldValue,
    color: Color,
    placeholder: String,
    onTap: (Int) -> Unit,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var cursorVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    LaunchedEffect(fieldValue.text, fieldValue.selection) {
        cursorVisible = true
        while (true) {
            delay(CURSOR_BLINK_MS)
            cursorVisible = !cursorVisible
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (fieldValue.text.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.displayLarge,
                color = color,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = fieldValue.text,
            style = MaterialTheme.typography.displayLarge,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            layoutResult?.let { onTap(it.getOffsetForPosition(tapOffset)) }
                        },
                    )
                },
        )
        if (cursorVisible) {
            layoutResult?.let { lr ->
                val safeOffset = fieldValue.selection.start.coerceIn(0, fieldValue.text.length)
                val rect = lr.getCursorRect(safeOffset)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                        .width(2.dp)
                        .height(with(density) { rect.height.toDp() })
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun ExpandToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CalculatorTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.keyNumber)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "expandIcon",
        ) { currentlyExpanded ->
            Icon(
                imageVector = if (currentlyExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                contentDescription = if (currentlyExpanded) {
                    stringResource(R.string.content_description_collapse)
                } else {
                    stringResource(R.string.content_description_expand)
                },
                tint = colors.keyNumberText,
            )
        }
    }
}

private data class SciKey(
    val label: String,
    val highlighted: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
private fun ColumnScope.ScientificRow(keys: List<SciKey>) {
    val colors = CalculatorTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 3.dp),
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(if (key.highlighted) colors.keyActive else colors.keyOperator)
                    .clickable(onClick = key.onClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = key.label,
                    color = if (key.highlighted) colors.keyActiveText else colors.keyOperatorText,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        content = content,
    )
}

// Cells fill their row instead of forcing a 1:1 aspect ratio, so keys shorten into pills
// on their own once the scientific rows push row-height down, matching the reference image.
@Composable
private fun RowScope.CalculatorKey(
    label: String,
    containerColor: Color,
    contentColor: Color,
    fontSize: TextUnit = 30.sp,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(6.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = contentColor, fontSize = fontSize)
    }
}

// Fires once immediately on press (a normal tap), then keeps firing at a fixed interval
// for as long as the key is held. Ripple is driven manually since the gesture is custom.
@Composable
private fun RowScope.BackspaceKey(
    containerColor: Color,
    contentColor: Color,
    contentDescription: String,
    onRepeat: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(6.dp)
            .clip(CircleShape)
            .background(containerColor)
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        interactionSource.emit(press)
                        coroutineScope {
                            val repeatJob = launch {
                                onRepeat()
                                delay(BACKSPACE_INITIAL_DELAY_MS)
                                while (true) {
                                    onRepeat()
                                    delay(BACKSPACE_REPEAT_INTERVAL_MS)
                                }
                            }
                            val released = tryAwaitRelease()
                            repeatJob.cancel()
                            interactionSource.emit(
                                if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press),
                            )
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Backspace, contentDescription = contentDescription, tint = contentColor)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14121D, widthDp = 360, heightDp = 800)
@Composable
private fun CalculatorScreenPreview() {
    CSFCTheme { CalculatorScreen() }
}
