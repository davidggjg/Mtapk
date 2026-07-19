package com.mtapk.core

import java.io.File

/**
 * Decides whether a file inside a decoded APK is safe to open in a plain-text editor.
 * Known text extensions (smali, xml, yml, ...) are trusted outright; anything else is
 * sniffed for NUL bytes / control characters that would indicate binary content
 * (classes.dex, resources.arsc, .png, .so, ...).
 */
object TextFileClassifier {

    private val TEXT_EXTENSIONS = setOf(
        "xml", "txt", "json", "yml", "yaml", "smali", "properties", "gradle", "kts",
        "java", "kt", "md", "html", "htm", "css", "js", "ts", "pro", "cfg", "ini",
        "csv", "sql", "toml", "aidl", "rs", "config", "manifest"
    )

    fun isLikelyText(file: File, sniffBytes: Int = 8000): Boolean {
        if (!file.isFile || file.length() == 0L) return file.isFile
        val ext = file.extension.lowercase()
        if (ext in TEXT_EXTENSIONS) return true
        return sniffIsText(file, sniffBytes)
    }

    private fun sniffIsText(file: File, sniffBytes: Int): Boolean {
        val bytes = file.inputStream().use { input ->
            val buffer = ByteArray(sniffBytes)
            val read = input.read(buffer)
            if (read <= 0) return true
            buffer.copyOf(read)
        }
        var suspicious = 0
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i == 0) return false
            if (i < 0x09 || i in 0x0E..0x1F) suspicious++
        }
        return suspicious.toDouble() / bytes.size < 0.01
    }
}
