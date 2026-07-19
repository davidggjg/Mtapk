package com.mtapk.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mtapk.core.FileEntry
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    pathStack: List<File>,
    entries: List<FileEntry>,
    isZipping: Boolean,
    onEntryClick: (FileEntry) -> Unit,
    onNavigateUp: () -> Boolean,
    onRestart: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atRoot = pathStack.size <= 1

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Text(
                            text = "/" + pathStack.drop(1).joinToString("/") { it.name },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (!atRoot) {
                        IconButton(onClick = { onNavigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                        }
                    } else {
                        IconButton(onClick = onRestart) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "אפליקציה חדשה")
                        }
                    }
                },
                actions = {
                    if (isZipping) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 12.dp))
                    } else {
                        IconButton(onClick = onExportClick) {
                            Icon(Icons.Default.Download, contentDescription = "הורד כקובץ ZIP")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("התיקייה ריקה", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(entries, key = { it.file.absolutePath }) { entry ->
                    FileRow(entry = entry, onClick = { onEntryClick(entry) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FileRow(entry: FileEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                entry.isDirectory -> Icons.Default.Folder
                entry.isEditableText -> Icons.Default.Description
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            },
            contentDescription = null,
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!entry.isDirectory) {
                Text(
                    text = formatSize(entry.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
    val value = bytes / 1024.0.pow(exp.toDouble())
    return "%.1f %s".format(value, units[exp - 1])
}
