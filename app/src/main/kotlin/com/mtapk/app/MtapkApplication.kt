package com.mtapk.app

import android.app.Application

/**
 * Last-resort safety net for crashes that happen outside any of the
 * ViewModel's own try/catch blocks (e.g. during Compose recomposition on the
 * main thread). Records the crash to a local log file before handing off to
 * the platform's default handler, so a diagnosable message survives the crash.
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
