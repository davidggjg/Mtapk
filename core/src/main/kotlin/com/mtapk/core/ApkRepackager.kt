package com.mtapk.core

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packs a decoded (and possibly user-edited) APK directory tree back into a
 * plain zip file, preserving relative paths. This intentionally does not try
 * to recompile smali/resources into a signed, installable APK - it produces a
 * zip of the files exactly as they sit on disk.
 */
class ApkRepackager {

    fun zipDirectory(sourceDir: File, outputZip: File) {
        require(sourceDir.isDirectory) { "Not a directory: ${sourceDir.path}" }
        outputZip.parentFile?.mkdirs()

        val basePath = sourceDir.toPath()
        ZipOutputStream(FileOutputStream(outputZip).buffered()).use { zos ->
            sourceDir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.path }
                .forEach { file ->
                    val relativePath = basePath.relativize(file.toPath())
                        .toString()
                        .replace(File.separatorChar, '/')
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
        }
    }
}
