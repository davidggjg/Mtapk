package com.mtapk.core

import brut.androlib.ApkDecoder
import brut.androlib.Config
import java.io.File

data class DecodeOptions(
    val decodeResourcesFull: Boolean = true,
    val decodeSourcesFull: Boolean = true,
    val decodeAssets: Boolean = true,
    val forceOverwrite: Boolean = true,
    // Kept at 1 deliberately: apktool-lib was built for desktop use and each
    // parallel decode job holds its own dex/resource model in memory, which
    // adds up fast against a phone's much smaller per-app heap. Sequential
    // decoding is slower but far less likely to OOM.
    val jobs: Int = 1
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

        AndroidEnvironmentShims.ensure(frameworkHomeFallbackDir())

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
        } catch (e: OutOfMemoryError) {
            // Not an Exception, so it would otherwise skip straight past any
            // catch(Exception) up the call chain and take the whole app down.
            throw ApkDecompileException(
                "אין מספיק זיכרון על המכשיר כדי לפרק את קובץ ה-APK הזה",
                e
            )
        } catch (e: Throwable) {
            val detail = e.message ?: e.cause?.message ?: e::class.qualifiedName ?: "unknown error"
            throw ApkDecompileException("Failed to decode APK: $detail", e)
        }
    }

    private fun frameworkHomeFallbackDir(): File {
        val tmp = System.getProperty("java.io.tmpdir")
        return File(tmp ?: ".", "mtapk-apktool-home")
    }

    companion object {
        private const val LIB_VERSION = "mtapk-1.0"
    }
}
