package com.mtapk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onPickApkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = Modifier.height(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Mtapk",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "בחרו קובץ APK כדי לפרק אותו: לצפות בכל הקבצים שבתוכו, כולל קוד ה-smali והמשאבים, לערוך אותם, ולהוריד הכל כקובץ ZIP.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("מפרק את קובץ ה-APK... זה יכול לקחת קצת זמן", textAlign = TextAlign.Center)
        } else {
            Button(onClick = onPickApkClick) {
                Text("בחר קובץ APK")
            }
        }

        if (errorMessage != null && !isLoading) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}
