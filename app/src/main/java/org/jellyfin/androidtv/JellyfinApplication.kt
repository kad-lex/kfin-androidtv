package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import org.jellyfin.androidtv.telemetry.TelemetryService

class JellyfinApplication : Application() {
	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		TelemetryService.init(this)
	}

	override fun onCreate() {
		super.onCreate()
		// KFIN debug: show stack traces on screen instead of a silent death
		KfinCrashHandler.install(this)
	}
}
