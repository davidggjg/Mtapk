package com.mtapk.core

import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApkRepackagerTest {

    @Test
    fun `zips nested directory tree preserving relative paths and content`() {
        val root = createTempDirectory("mtapk-repackage-test").toFile()
        val sourceDir = File(root, "decoded").apply { mkdirs() }
        File(sourceDir, "AndroidManifest.xml").writeText("<manifest/>")
        File(sourceDir, "res/values").mkdirs()
        File(sourceDir, "res/values/strings.xml").writeText("<resources/>")
        File(sourceDir, "smali/com/example").mkdirs()
        File(sourceDir, "smali/com/example/Main.smali").writeText(".class public LMain;")

        val outputZip = File(root, "out.zip")
        ApkRepackager().zipDirectory(sourceDir, outputZip)

        assertTrue(outputZip.exists())

        ZipFile(outputZip).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertEquals(
                setOf(
                    "AndroidManifest.xml",
                    "res/values/strings.xml",
                    "smali/com/example/Main.smali"
                ),
                names
            )

            val manifestEntry = zip.getEntry("AndroidManifest.xml")
            val content = zip.getInputStream(manifestEntry).bufferedReader().readText()
            assertEquals("<manifest/>", content)
        }
    }
}
