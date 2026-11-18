package org.jellyfin.androidtv

import android.app.Activity
import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * KFIN debug: shows the uncaught exception that killed the app so it can be
 * reported without adb. The report can also be saved to the Downloads folder.
 */
class CrashActivity : Activity() {
	private lateinit var trace: String

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		trace = intent.getStringExtra(EXTRA_TRACE).orEmpty()

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(48, 48, 48, 48)
			setBackgroundColor(Color.BLACK)
		}

		val title = TextView(this).apply {
			text = "KFIN crash - сфотографируйте этот экран или сохраните отчёт"
			textSize = 22f
			setTextColor(Color.RED)
			gravity = Gravity.START
		}
		root.addView(title)

		// KFIN debug: save the report to the public Downloads folder
		val saveButton = Button(this).apply {
			text = "СОХРАНИТЬ ОТЧЁТ В ЗАГРУЗКИ"
			textSize = 16f
			setTextColor(Color.WHITE)
			setOnClickListener {
				val path = saveReport()
				text = if (path != null) "СОХРАНЕНО: $path" else "ОШИБКА СОХРАНЕНИЯ"
				isEnabled = false
			}
		}
		root.addView(saveButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
			topMargin = 24
		})

		val scroll = ScrollView(this)
		val body = TextView(this).apply {
			text = trace
			textSize = 13f
			setTextColor(Color.WHITE)
			typeface = android.graphics.Typeface.MONOSPACE
		}
		scroll.addView(body)
		root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

		setContentView(root)
	}

	private fun saveReport(): String? {
		val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
		val name = "kfin-crash-$stamp.log"

		return runCatching {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				val values = ContentValues().apply {
					put(MediaStore.Downloads.DISPLAY_NAME, name)
					put(MediaStore.Downloads.MIME_TYPE, "text/plain")
					put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
				}
				val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
					?: return null
				contentResolver.openOutputStream(uri)?.use { it.write(trace.toByteArray()) }
					?: return null
				"Загрузки/$name"
			} else {
				@Suppress("DEPRECATION")
				val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				val file = File(dir, name)
				file.writeText(trace)
				file.absolutePath
			}
		}.getOrNull()
	}

	companion object {
		const val EXTRA_TRACE = "org.kfin.extra.TRACE"
	}
}
