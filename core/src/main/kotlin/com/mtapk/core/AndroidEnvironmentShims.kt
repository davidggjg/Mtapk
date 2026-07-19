package com.mtapk.core

import java.io.File

/**
 * apktool-lib was built for a desktop JVM and reads a couple of
 * Oracle/HotSpot-specific system properties in static initializers, with no
 * null check:
 *
 * - `sun.arch.data.model` (brut.util.OSDetection): read and immediately
 *   `.toLowerCase()`'d -> NullPointerException the first time that class
 *   loads, surfacing as an ExceptionInInitializerError with no message.
 * - `user.home` (brut.androlib.res.Framework): used to compute a default
 *   framework-cache directory via java.nio.file.Paths.get(userHome, ...); a
 *   null userHome throws the same way.
 *
 * Neither exists on Android's runtime (only on a real desktop JVM), so both
 * must be set before any apktool-lib class is touched. This only ever sets a
 * property that's actually missing, so it's a no-op on a real desktop JVM
 * (used for this module's own tests) where both are already present.
 */
internal object AndroidEnvironmentShims {

    fun ensure(frameworkHomeDir: File) {
        if (System.getProperty("sun.arch.data.model") == null) {
            System.setProperty("sun.arch.data.model", "64")
        }
        if (System.getProperty("user.home") == null) {
            frameworkHomeDir.mkdirs()
            System.setProperty("user.home", frameworkHomeDir.absolutePath)
        }
    }
}
