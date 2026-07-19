package com.mtapk.core

import java.io.File

data class FileEntry(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val isEditableText: Boolean
)

object DirectoryLister {

    /** Lists the immediate children of [dir], directories first, then files, both alphabetical. */
    fun list(dir: File): List<FileEntry> {
        require(dir.isDirectory) { "Not a directory: ${dir.path}" }
        val children = dir.listFiles() ?: emptyArray()
        return children
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            .map { f ->
                FileEntry(
                    file = f,
                    name = f.name,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isFile) f.length() else 0L,
                    isEditableText = f.isFile && TextFileClassifier.isLikelyText(f)
                )
            }
    }
}
