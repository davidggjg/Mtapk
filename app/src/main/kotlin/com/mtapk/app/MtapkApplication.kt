package com.mtapk.app

import android.app.Application

/**
 * Last-resort safety net for crashes that happen outside any of the
 * ViewModel's own try/catch blocks (e.g. during Compose recomposition on the
 * main thread). Records the crash to a local log file before handing off to
 * the platform's default handler, so a diagnosable message survives the crash.
 *
 * (The desktop-JVM system property workarounds apktool-lib needs on Android
 * live in core's ApkDecompiler, since they're really about safely driving
 * that dependency, not an app/UI concern.)
 */
class MtapkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash("uncaught on ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
