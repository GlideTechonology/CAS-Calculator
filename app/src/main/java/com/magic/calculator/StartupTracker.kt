package com.magic.calculator

import android.app.Activity
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/**
 * Measures startup time and logs it to Logcat ("CalcStartup" tag) + displays in Settings.
 *
 * Phases:
 *  - process → onCreate       : Application process initialization (Android + Application class)
 *  - onCreate → first compose : Initial Jetpack Compose layout computation
 *  - first compose → 1st frame: Measurement/draw and initial frame rendered on screen
 */
object StartupTracker {
    private const val TAG = "CalcStartup"

    private var isColdStart = true
    private var onCreateAt = 0L
    private var firstCompositionAt = 0L
    private var reported = false

    /** Cold start elapsed time from process creation to the first frame (ms). -1 = not measured. */
    var processToFirstFrameMs by mutableLongStateOf(-1L)
        private set
    var processToOnCreateMs by mutableLongStateOf(-1L)
        private set
    var onCreateToCompositionMs by mutableLongStateOf(-1L)
        private set
    var compositionToFrameMs by mutableLongStateOf(-1L)
        private set

    fun markOnCreate() {
        try {
            onCreateAt = SystemClock.uptimeMillis()
            firstCompositionAt = 0L
            reported = false
            if (isColdStart && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                processToOnCreateMs = onCreateAt - Process.getStartUptimeMillis()
            }
        } catch (_: Throwable) {}
    }

    fun markFirstComposition() {
        try {
            if (firstCompositionAt == 0L) firstCompositionAt = SystemClock.uptimeMillis()
        } catch (_: Throwable) {}
    }

    fun trackFirstFrame(activity: Activity) {
        try {
            val decor = activity.window?.decorView ?: return
            lateinit var listener: ViewTreeObserver.OnDrawListener
            listener = ViewTreeObserver.OnDrawListener {
                if (reported) return@OnDrawListener
                reported = true
                try {
                    decor.post {
                        try {
                            val now = SystemClock.uptimeMillis()
                            val composition = if (firstCompositionAt > 0) firstCompositionAt else now
                            onCreateToCompositionMs = composition - onCreateAt
                            compositionToFrameMs = now - composition
                            if (isColdStart && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                processToFirstFrameMs = now - Process.getStartUptimeMillis()
                            }
                            isColdStart = false
                            if (decor.viewTreeObserver.isAlive) {
                                decor.viewTreeObserver.removeOnDrawListener(listener)
                            }
                            try {
                                activity.reportFullyDrawn()
                            } catch (_: Throwable) {}
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }
            if (decor.viewTreeObserver.isAlive) {
                decor.viewTreeObserver.addOnDrawListener(listener)
            }
        } catch (_: Throwable) {}
    }
}
