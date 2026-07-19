package com.mtapk.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    fileName: String,
    text: String,
    isDirty: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDiscardDialog by remember { mutableStateOf(false) }

    fun requestClose() {
        if (isDirty) showDiscardDialog = true else onClose()
    }

    BackHandler(onBack = ::requestClose)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { requestClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "סגור")
                    }
                },
                actions = {
                    IconButton(onClick = onSave, enabled = isDirty) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "שמור",
                            tint = if (isDirty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("לבטל שינויים שלא נשמרו?") },
            text = { Text("יש לך שינויים שלא נשמרו בקובץ הזה. לצאת בכל זאת?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) { Text("צא בלי לשמור") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("המשך עריכה") }
            }
        )
    }
}
