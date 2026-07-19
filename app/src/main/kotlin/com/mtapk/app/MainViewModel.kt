package com.mtapk.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mtapk.core.ApkDecompileException
import com.mtapk.core.ApkDecompiler
import com.mtapk.core.ApkRepackager
import com.mtapk.core.DecodeOptions
import com.mtapk.core.DirectoryLister
import com.mtapk.core.FileEntry
import com.mtapk.core.TextFileClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface DecompileState {
    data object Idle : DecompileState
    data object Loading : DecompileState
    data class Success(val rootDir: File, val apkDisplayName: String) : DecompileState
    data class Error(val message: String) : DecompileState
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Zipping : ExportState
    data object Done : ExportState
    data class Error(val message: String) : ExportState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var decompileState by mutableStateOf<DecompileState>(DecompileState.Idle)
        private set

    var exportState by mutableStateOf<ExportState>(ExportState.Idle)
        private set

    val pathStack = mutableStateListOf<File>()

    var currentEntries by mutableStateOf<List<FileEntry>>(emptyList())
        private set

    var openedFile by mutableStateOf<File?>(null)
        private set

    var editorText by mutableStateOf("")
        private set

    var editorDirty by mutableStateOf(false)
        private set

    var editorError by mutableStateOf<String?>(null)
        private set

    var lastCrashLog by mutableStateOf<String?>(null)
        private set

    init {
        lastCrashLog = getApplication<Application>().readLastCrashLog()
    }

    fun dismissCrashLog() {
        getApplication<Application>().clearLastCrashLog()
        lastCrashLog = null
    }

    val currentDir: File? get() = pathStack.lastOrNull()

    val suggestedZipName: String
        get() {
            val name = (decompileState as? DecompileState.Success)?.apkDisplayName ?: "app"
            val base = name.substringBeforeLast('.').ifBlank { "app" }
            return "${base}_extracted.zip"
        }

    fun pickApk(uri: Uri, displayName: String) {
        val context = getApplication<Application>()
        decompileState = DecompileState.Loading
        exportState = ExportState.Idle
        viewModelScope.launch {
            try {
                val outDir = withContext(Dispatchers.IO) {
                    val cacheApk = File(context.cacheDir, "input.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cacheApk.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw ApkDecompileException("לא ניתן היה לקרוא את הקובץ שנבחר")

                    val outputDir = File(context.filesDir, "decoded")
                    ApkDecompiler().decode(cacheApk, outputDir, DecodeOptions(forceOverwrite = true))
                    outputDir
                }
                pathStack.clear()
                pathStack.add(outDir)
                refreshCurrentEntries()
                decompileState = DecompileState.Success(outDir, displayName)
            } catch (e: ApkDecompileException) {
                decompileState = DecompileState.Error(e.message ?: "פירוק הקובץ נכשל")
            } catch (e: Throwable) {
                // Catches everything, including OutOfMemoryError and friends: an
                // uncaught Throwable here would otherwise crash the whole app
                // instead of showing an error screen.
                context.logCrash("pickApk", e)
                decompileState = DecompileState.Error(e.message ?: "שגיאה לא צפויה (${e::class.simpleName})")
            }
        }
    }

    fun navigateInto(entry: FileEntry) {
        if (!entry.isDirectory) return
        pathStack.add(entry.file)
        refreshCurrentEntries()
    }

    /** Returns true if it consumed the navigation (i.e. we were not already at the root). */
    fun navigateUp(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeAt(pathStack.lastIndex)
        refreshCurrentEntries()
        return true
    }

    fun refreshCurrentEntries() {
        val dir = currentDir ?: return
        currentEntries = DirectoryLister.list(dir)
    }

    fun openFile(entry: FileEntry) {
        if (entry.isDirectory) return
        val context = getApplication<Application>()
        viewModelScope.launch {
            editorError = null
            try {
                if (!TextFileClassifier.isLikelyText(entry.file)) {
                    editorError = "קובץ בינארי - לא ניתן לערוך כטקסט"
                    return@launch
                }
                val text = withContext(Dispatchers.IO) { entry.file.readText() }
                openedFile = entry.file
                editorText = text
                editorDirty = false
            } catch (e: Throwable) {
                context.logCrash("openFile", e)
                editorError = "פתיחת הקובץ נכשלה: ${e.message ?: e::class.simpleName}"
            }
        }
    }

    fun updateEditorText(text: String) {
        editorText = text
        editorDirty = true
    }

    fun saveEditorFile(onSaved: () -> Unit = {}) {
        val file = openedFile ?: return
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { file.writeText(editorText) }
                editorDirty = false
                onSaved()
            } catch (e: Throwable) {
                context.logCrash("saveEditorFile", e)
                editorError = "השמירה נכשלה: ${e.message ?: e::class.simpleName}"
            }
        }
    }

    fun closeEditor() {
        openedFile = null
        editorText = ""
        editorDirty = false
    }

    fun clearEditorError() {
        editorError = null
    }

    fun exportTo(destUri: Uri) {
        val rootDir = (decompileState as? DecompileState.Success)?.rootDir ?: return
        val context = getApplication<Application>()
        exportState = ExportState.Zipping
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val tmpZip = File(context.cacheDir, "export.zip")
                    ApkRepackager().zipDirectory(rootDir, tmpZip)
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        tmpZip.inputStream().use { it.copyTo(out) }
                    } ?: throw ApkDecompileException("לא ניתן לכתוב ליעד השמירה שנבחר")
                }
                exportState = ExportState.Done
            } catch (e: Throwable) {
                context.logCrash("exportTo", e)
                exportState = ExportState.Error(e.message ?: "יצירת הקובץ המקווץ נכשלה (${e::class.simpleName})")
            }
        }
    }

    fun clearExportState() {
        exportState = ExportState.Idle
    }

    fun reset() {
        decompileState = DecompileState.Idle
        exportState = ExportState.Idle
        pathStack.clear()
        currentEntries = emptyList()
        closeEditor()
    }
}
