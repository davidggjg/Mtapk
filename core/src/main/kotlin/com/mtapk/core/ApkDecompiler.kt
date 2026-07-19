package com.mtapk.core

import brut.androlib.ApkDecoder
import brut.androlib.Config
import java.io.File

data class DecodeOptions(
    val decodeResourcesFull: Boolean = true,
    val decodeSourcesFull: Boolean = true,
    val decodeAssets: Boolean = true,
    val forceOverwrite: Boolean = true,
    val jobs: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
)

class ApkDecompileException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thin wrapper around apktool-lib's [ApkDecoder], which does the actual work of
 * splitting an APK into AndroidManifest.xml, decoded resources, per-dex smali
 * sources, assets, and native libraries on disk.
 */
class ApkDecompiler {

    fun decode(apkFile: File, outputDir: File, options: DecodeOptions = DecodeOptions()) {
        if (!apkFile.isFile) {
            throw ApkDecompileException("APK file not found: ${apkFile.path}")
        }

        val config = Config(LIB_VERSION)
        config.isForced = options.forceOverwrite
        config.jobs = options.jobs
        config.setDecodeSources(
            if (options.decodeSourcesFull) Config.DecodeSources.FULL else Config.DecodeSources.NONE
        )
        config.setDecodeResources(
            if (options.decodeResourcesFull) Config.DecodeResources.FULL else Config.DecodeResources.NONE
        )
        config.setDecodeAssets(
            if (options.decodeAssets) Config.DecodeAssets.FULL else Config.DecodeAssets.NONE
        )

        try {
            ApkDecoder(apkFile, config).decode(outputDir)
        } catch (e: Exception) {
            throw ApkDecompileException("Failed to decode APK: ${e.message}", e)
        }
    }

    companion object {
        private const val LIB_VERSION = "mtapk-1.0"
    }
}
