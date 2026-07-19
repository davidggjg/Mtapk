package com.mtapk.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtapk.app.ui.FileBrowserScreen
import com.mtapk.app.ui.HomeScreen
import com.mtapk.app.ui.TextEditorScreen
import com.mtapk.app.ui.theme.MtapkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MtapkTheme {
                MtapkApp()
            }
        }
    }
}

@Composable
private fun MtapkApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val pickApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.pickApk(uri, queryDisplayName(context, uri))
        }
    }

    val createZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportTo(uri)
        }
    }

    LaunchedEffect(viewModel.editorError) {
        val message = viewModel.editorError
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearEditorError()
        }
    }

    LaunchedEffect(viewModel.exportState) {
        when (val state = viewModel.exportState) {
            is ExportState.Done -> {
                snackbarHostState.showSnackbar("קובץ ה-ZIP נשמר בהצלחה")
                viewModel.clearExportState()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearExportState()
            }
            else -> Unit
        }
    }

    BackHandler(enabled = viewModel.openedFile == null && viewModel.pathStack.size > 1) {
        viewModel.navigateUp()
    }

    // The individual screens each own a Scaffold (with their own top bar handling
    // window insets), so this outer Scaffold only hosts the snackbar overlay and
    // deliberately ignores its own content padding.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        val state = viewModel.decompileState
        when {
            viewModel.openedFile != null -> {
                TextEditorScreen(
                    fileName = viewModel.openedFile!!.name,
                    text = viewModel.editorText,
                    isDirty = viewModel.editorDirty,
                    onTextChange = viewModel::updateEditorText,
                    onSave = { viewModel.saveEditorFile() },
                    onClose = viewModel::closeEditor
                )
            }
            state is DecompileState.Success -> {
                FileBrowserScreen(
                    pathStack = viewModel.pathStack,
                    entries = viewModel.currentEntries,
                    isZipping = viewModel.exportState is ExportState.Zipping,
                    onEntryClick = { entry ->
                        if (entry.isDirectory) viewModel.navigateInto(entry) else viewModel.openFile(entry)
                    },
                    onNavigateUp = viewModel::navigateUp,
                    onRestart = viewModel::reset,
                    onExportClick = { createZipLauncher.launch(viewModel.suggestedZipName) }
                )
            }
            else -> {
                HomeScreen(
                    isLoading = state is DecompileState.Loading,
                    errorMessage = (state as? DecompileState.Error)?.message,
                    onPickApkClick = { pickApkLauncher.launch(arrayOf("*/*")) },
                    crashLog = viewModel.lastCrashLog,
                    onDismissCrashLog = viewModel::dismissCrashLog
                )
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            return cursor.getString(nameIndex) ?: "app.apk"
        }
    }
    return "app.apk"
}
