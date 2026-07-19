package com.mtapk.core

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Exercises the real apktool-lib dependency (not a fake): builds a tiny but
 * structurally valid "APK" (a zip containing only a hand-encoded binary
 * AndroidManifest.xml, see [MinimalAxmlFixture]) and runs it through
 * [ApkDecompiler], which delegates to apktool-lib's ApkDecoder/ResDecoder.
 *
 * This proves the Gradle dependency wiring (Maven Central + JitPack for the
 * smali/baksmali transitive deps) resolves and links correctly, and that the
 * real binary-XML manifest decoder produces readable XML - the core promise
 * of this app.
 */
class ApkDecompilerTest {

    @Test
    fun `decodes a minimal binary manifest into readable XML`() {
        val workDir = createTempDirectory("mtapk-decompile-test").toFile()
        val apkFile = File(workDir, "sample.apk")
        val outDir = File(workDir, "out")

        writeMinimalApk(apkFile, packageName = "com.mtapk.test")

        ApkDecompiler().decode(apkFile, outDir, DecodeOptions(forceOverwrite = true))

        val manifestOut = File(outDir, "AndroidManifest.xml")
        assertTrue(manifestOut.exists(), "decoded AndroidManifest.xml should exist")

        val text = manifestOut.readText()
        assertTrue(text.contains("manifest"), "decoded manifest should contain the <manifest> tag, was:\n$text")
        assertTrue(text.contains("com.mtapk.test"), "decoded manifest should contain the package name, was:\n$text")

        val apktoolYml = File(outDir, "apktool.yml")
        assertTrue(apktoolYml.exists(), "apktool.yml metadata should be written")
    }

    @Test
    fun `throws a typed exception for a missing apk file`() {
        val workDir = createTempDirectory("mtapk-decompile-test-missing").toFile()
        val missing = File(workDir, "does-not-exist.apk")
        val outDir = File(workDir, "out")

        var threw = false
        try {
            ApkDecompiler().decode(missing, outDir)
        } catch (e: ApkDecompileException) {
            threw = true
        }
        assertTrue(threw, "expected ApkDecompileException for a missing input file")
        assertFalse(outDir.exists())
    }

    private fun writeMinimalApk(apkFile: File, packageName: String) {
        val manifestBytes = MinimalAxmlFixture.buildManifest(packageName)
        ZipOutputStream(apkFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(manifestBytes)
            zos.closeEntry()
        }
    }
}
