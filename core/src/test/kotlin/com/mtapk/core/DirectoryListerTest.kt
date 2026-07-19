package com.mtapk.core

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class DirectoryListerTest {

    @Test
    fun `lists directories before files, both alphabetically`() {
        val dir = createTempDirectory("mtapk-dirlister-test").toFile()
        File(dir, "zeta.txt").writeText("z")
        File(dir, "alpha.txt").writeText("a")
        File(dir, "res").mkdirs()
        File(dir, "assets").mkdirs()

        val names = DirectoryLister.list(dir).map { it.name }

        assertEquals(listOf("assets", "res", "alpha.txt", "zeta.txt"), names)
    }
}
