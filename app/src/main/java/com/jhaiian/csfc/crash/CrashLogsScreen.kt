package com.jhaiian.csfc.crash

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jhaiian.csfc.R
import com.jhaiian.csfc.ui.theme.CalculatorTheme
import com.jhaiian.csfc.ui.theme.OnSurfaceMedium
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class CrashReport(val file: File, val title: String, val content: String)

private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
private val displayDateFormat = SimpleDateFormat("MMM d, yyyy  HH:mm:ss", Locale.US)

@Composable
fun CrashLogsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val colors = CalculatorTheme.colors

    var reports by remember { mutableStateOf<List<CrashReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReport by remember { mutableStateOf<CrashReport?>(null) }
    var confirmingClearAll by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            isLoading = true
            reports = withContext(Dispatchers.IO) {
                CrashHandler.deleteOldReports(context)
                CrashHandler.getCrashFiles(context).map { file ->
                    val nameWithoutExt = file.nameWithoutExtension.removePrefix("crash_")
                    val date = runCatching { fileDateFormat.parse(nameWithoutExt) }.getOrNull()
                    val title = date?.let { displayDateFormat.format(it) } ?: file.name
                    CrashReport(file, title, runCatching { file.readText() }.getOrDefault(""))
                }
            }
            isLoading = false
        }
    }

    fun deleteReport(report: CrashReport) {
        scope.launch {
            withContext(Dispatchers.IO) { report.file.delete() }
            reload()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.crash_logs_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { confirmingClearAll = true }, enabled = reports.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = stringResource(R.string.content_description_clear_all),
                    tint = if (reports.isNotEmpty()) MaterialTheme.colorScheme.onBackground else OnSurfaceMedium,
                )
            }
        }

        when {
            isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            reports.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.crash_logs_empty),
                    color = colors.keyOperatorText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(reports, key = { it.file.name }) { report ->
                    CrashReportRow(
                        title = report.title,
                        onClick = { selectedReport = report },
                        onCopy = { copyReportToClipboard(context, clipboardManager, report.content) },
                        onDelete = { deleteReport(report) },
                    )
                }
            }
        }
    }

    if (confirmingClearAll) {
        AlertDialog(
            onDismissRequest = { confirmingClearAll = false },
            title = { Text(stringResource(R.string.crash_clear_confirm_title)) },
            text = { Text(stringResource(R.string.crash_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingClearAll = false
                    scope.launch {
                        withContext(Dispatchers.IO) { CrashHandler.clearAllReports(context) }
                        reload()
                    }
                }) { Text(stringResource(R.string.crash_clear_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClearAll = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    selectedReport?.let { report ->
        CrashDetailDialog(
            title = report.title,
            content = report.content,
            onDismiss = { selectedReport = null },
            onCopy = { copyReportToClipboard(context, clipboardManager, report.content) },
            onDelete = {
                selectedReport = null
                deleteReport(report)
            },
        )
    }
}

@Composable
private fun CrashReportRow(
    title: String,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = CalculatorTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.keyNumber)
            .clickable(onClick = onClick)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.keyNumberText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
        )
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_copy), tint = colors.keyOperatorText)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = colors.keyOperatorText)
        }
    }
}

@Composable
private fun CrashDetailDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = CalculatorTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 560.dp),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(20.dp),
                )
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = content,
                        color = colors.keyNumberText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_back), color = colors.keyOperatorText)
                    }
                    TextButton(onClick = onCopy) {
                        Text(stringResource(R.string.action_copy), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun copyReportToClipboard(context: Context, clipboardManager: ClipboardManager, content: String) {
    clipboardManager.setText(AnnotatedString(content))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.crash_copied_toast), Toast.LENGTH_SHORT).show()
    }
}
