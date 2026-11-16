package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.view.children
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.ViewButtonAlphaPickerBinding

class AlphaPickerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr, defStyleRes) {
	var onAlphaSelected: (letter: Char) -> Unit = {}

	init {
		isFocusable = false
		isFocusableInTouchMode = false
		isHorizontalScrollBarEnabled = false

		val layout = LinearLayout(context)

		val pillRadius = 20f * resources.displayMetrics.density
		val strokeWidth = (1f * resources.displayMetrics.density).toInt()

		val letters = "#${resources.getString(R.string.byletter_letters)}"
		letters.forEach { letter ->
			val binding = ViewButtonAlphaPickerBinding.inflate(LayoutInflater.from(context), this, false)
			binding.button.apply {
				text = letter.toString()

				// KFIN: unified focus style - transparent capsule with white outline
				setBackgroundColor(android.graphics.Color.TRANSPARENT)
				foreground = null
				val pill = android.graphics.drawable.GradientDrawable().apply {
					shape = android.graphics.drawable.GradientDrawable.RECTANGLE
					cornerRadius = pillRadius
					setStroke(0, 0x00000000)
				}
				background = pill
				setOnFocusChangeListener { _, hasFocus ->
					pill.setStroke(
						if (hasFocus) strokeWidth else 0,
						if (hasFocus) 0xB3F8FAFC.toInt() else 0x00000000
					)
				}

				setOnClickListener { _ ->
					onAlphaSelected(letter)
				}
			}

			layout.addView(binding.root)
		}

		addView(layout)
	}

	fun focus(letter: Char) {
		children
			.filterIsInstance<Button>()
			.firstOrNull { it.text == letter.toString() }
			?.requestFocus()
	}
}
