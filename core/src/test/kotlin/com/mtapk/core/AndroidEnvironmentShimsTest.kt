package com.mtapk.core

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidEnvironmentShimsTest {

    @Test
    fun `fills in sun-arch-data-model and user-home only when missing`() {
        val originalArch = System.getProperty("sun.arch.data.model")
        val originalHome = System.getProperty("user.home")
        try {
            System.clearProperty("sun.arch.data.model")
            System.clearProperty("user.home")

            val fallbackDir = File(createTempDirectory("mtapk-shims-test").toFile(), "apktool-home")
            AndroidEnvironmentShims.ensure(fallbackDir)

            assertEquals("64", System.getProperty("sun.arch.data.model"))
            assertEquals(fallbackDir.absolutePath, System.getProperty("user.home"))
            assertTrue(fallbackDir.exists())
        } finally {
            // Restore whatever the real desktop JVM running this test had, so
            // this test doesn't leak global JVM state into other tests.
            restoreOrClear("sun.arch.data.model", originalArch)
            restoreOrClear("user.home", originalHome)
        }
    }

    @Test
    fun `treats an empty string the same as missing`() {
        // Observed on a real device: user.home comes back as "" rather than
        // null, which a plain null-check misses - Paths.get("", ...) doesn't
        // throw, it just silently produces a bogus relative path.
        val originalArch = System.getProperty("sun.arch.data.model")
        val originalHome = System.getProperty("user.home")
        try {
            System.setProperty("sun.arch.data.model", "")
            System.setProperty("user.home", "")

            val fallbackDir = File(createTempDirectory("mtapk-shims-test-empty").toFile(), "apktool-home")
            AndroidEnvironmentShims.ensure(fallbackDir)

            assertEquals("64", System.getProperty("sun.arch.data.model"))
            assertEquals(fallbackDir.absolutePath, System.getProperty("user.home"))
        } finally {
            restoreOrClear("sun.arch.data.model", originalArch)
            restoreOrClear("user.home", originalHome)
        }
    }

    @Test
    fun `does not override an existing value`() {
        val originalArch = System.getProperty("sun.arch.data.model")
        val originalHome = System.getProperty("user.home")
        try {
            System.setProperty("sun.arch.data.model", "32")
            System.setProperty("user.home", "/some/existing/home")

            AndroidEnvironmentShims.ensure(File(createTempDirectory("mtapk-shims-test-2").toFile(), "apktool-home"))

            assertEquals("32", System.getProperty("sun.arch.data.model"))
            assertEquals("/some/existing/home", System.getProperty("user.home"))
        } finally {
            restoreOrClear("sun.arch.data.model", originalArch)
            restoreOrClear("user.home", originalHome)
        }
    }

    private fun restoreOrClear(key: String, value: String?) {
        if (value != null) System.setProperty(key, value) else System.clearProperty(key)
    }
}
