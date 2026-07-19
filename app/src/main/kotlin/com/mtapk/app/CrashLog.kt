package com.mtapk.app

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "Mtapk"
private const val CRASH_LOG_FILE_NAME = "last_error.txt"

/**
 * Lightweight on-device diagnostics: writes the failure to logcat (for anyone
 * with adb) and also to a plain text file under internal storage, since most
 * users of this app won't have adb handy. The file survives a crash/relaunch
 * so it can be shown on the home screen for the user to copy and report back.
 */
fun Context.logCrash(where: String, throwable: Throwable) {
    Log.e(TAG, "Error in $where", throwable)
    runCatching {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val text = buildString {
            appendLine("[$timestamp] $where")
            appendLine(throwable.stackTraceToString())
        }
        File(filesDir, CRASH_LOG_FILE_NAME).writeText(text)
    }
}

fun Context.readLastCrashLog(): String? =
    File(filesDir, CRASH_LOG_FILE_NAME).takeIf { it.exists() }?.readText()

fun Context.clearLastCrashLog() {
    File(filesDir, CRASH_LOG_FILE_NAME).delete()
}
