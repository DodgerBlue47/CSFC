package com.jhaiian.csfc.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.csfc.R
import com.jhaiian.csfc.ui.theme.CSFCTheme
import com.jhaiian.csfc.ui.theme.CalculatorTheme

@Composable
fun CalculatorScreen() {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val colors = CalculatorTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(modifier = Modifier.statusBarsPadding())

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.sample_expression),
                color = colors.displayExpression,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sample_result),
                color = colors.displayResult,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
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
                    ScientificRow(R.string.key_sqrt, R.string.key_pi, R.string.key_power, R.string.key_factorial)
                    ScientificRow(R.string.key_deg, R.string.key_sin, R.string.key_cos, R.string.key_tan)
                    ScientificRow(R.string.key_inv, R.string.key_e, R.string.key_ln, R.string.key_log)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_ac), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    CalculatorKey(stringResource(R.string.key_parentheses), containerColor = colors.keyOperator, contentColor = colors.keyOperatorText)
                    CalculatorKey(stringResource(R.string.key_percent), containerColor = colors.keyOperator, contentColor = colors.keyOperatorText)
                    CalculatorKey(stringResource(R.string.key_divide), containerColor = colors.keyOperator, contentColor = colors.keyOperatorText)
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_7), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_8), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_9), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_multiply), containerColor = colors.keyActive, contentColor = colors.keyActiveText)
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_4), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_5), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_6), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_subtract), containerColor = colors.keyOperator, contentColor = colors.keyOperatorText)
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_1), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_2), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_3), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_add), containerColor = colors.keyOperator, contentColor = colors.keyOperatorText)
                }
                KeyRow {
                    CalculatorKey(stringResource(R.string.key_0), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorKey(stringResource(R.string.key_decimal), containerColor = colors.keyNumber, contentColor = colors.keyNumberText)
                    CalculatorIconKey(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.content_description_backspace),
                        containerColor = colors.keyNumber,
                        contentColor = colors.keyNumberText,
                    )
                    CalculatorKey(stringResource(R.string.key_equals), containerColor = colors.keyActive, contentColor = colors.keyActiveText)
                }
            }
        }
    }
}

@Composable
private fun TopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.content_description_more_options),
                tint = MaterialTheme.colorScheme.onBackground,
            )
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

@Composable
private fun ColumnScope.ScientificRow(vararg labelRes: Int) {
    val colors = CalculatorTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 3.dp),
    ) {
        labelRes.forEach { res ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(colors.keyOperator)
                    .clickable {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(res),
                    color = colors.keyOperatorText,
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

@Composable
private fun RowScope.CalculatorIconKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
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
        Icon(imageVector = icon, contentDescription = contentDescription, tint = contentColor)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14121D, widthDp = 360, heightDp = 800)
@Composable
private fun CalculatorScreenCollapsedPreview() {
    CSFCTheme { CalculatorScreen() }
}
