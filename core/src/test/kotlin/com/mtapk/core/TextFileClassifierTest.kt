package com.mtapk.core

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFileClassifierTest {

    @Test
    fun `known text extensions are always treated as text`() {
        val dir = createTempDirectory("mtapk-textclassifier-test").toFile()
        val smali = File(dir, "Main.smali").apply { writeText(".class public LMain;") }
        val xml = File(dir, "AndroidManifest.xml").apply { writeText("<manifest/>") }

        assertTrue(TextFileClassifier.isLikelyText(smali))
        assertTrue(TextFileClassifier.isLikelyText(xml))
    }

    @Test
    fun `binary content with NUL bytes is not treated as text`() {
        val dir = createTempDirectory("mtapk-textclassifier-test").toFile()
        val binary = File(dir, "classes.dex")
        binary.writeBytes(byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x00, 0x00, 0x01, 0x02, 0x00, 0x00))

        assertFalse(TextFileClassifier.isLikelyText(binary))
    }

    @Test
    fun `plain text without a known extension is sniffed as text`() {
        val dir = createTempDirectory("mtapk-textclassifier-test").toFile()
        val noExt = File(dir, "README").apply { writeText("hello world\nsecond line\n") }

        assertTrue(TextFileClassifier.isLikelyText(noExt))
    }
}
