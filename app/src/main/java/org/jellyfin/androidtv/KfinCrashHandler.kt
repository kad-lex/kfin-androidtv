package org.jellyfin.androidtv

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File

/**
 * KFIN debug: last-resort handler that shows the stack trace on screen (and
 * saves it to a file) instead of silently dying.
 */
object KfinCrashHandler : Thread.UncaughtExceptionHandler {
	private var previous: Thread.UncaughtExceptionHandler? = null
	private lateinit var appContext: Context

	fun install(context: Context) {
		appContext = context.applicationContext
		previous = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler(this)
	}

	override fun uncaughtException(thread: Thread, throwable: Throwable) {
		val trace = buildString {
			appendLine("KFIN CRASH  thread=${thread.name}")
			appendLine("time=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
			appendLine("device=${Build.MANUFACTURER} ${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
			appendLine()
			append(throwable.stackTraceToString())
			var cause = throwable.cause
			while (cause != null) {
				appendLine()
				appendLine("Caused by:")
				append(cause.stackTraceToString())
				cause = cause.cause
			}
		}

		runCatching {
			File(appContext.getExternalFilesDir(null), "kfin-crash.log").writeText(trace)
		}

		runCatching {
			val intent = Intent(appContext, CrashActivity::class.java).apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
				putExtra(CrashActivity.EXTRA_TRACE, trace)
			}
			appContext.startActivity(intent)
		}

		previous?.uncaughtException(thread, throwable)
	}
}
