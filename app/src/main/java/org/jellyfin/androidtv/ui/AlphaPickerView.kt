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
					// KFIN: focus is outline + 10% white fill, no glow
					pill.setColor(if (hasFocus) 0xF2FFFFFF.toInt() else 0x00000000)
					pill.setStroke(
						if (hasFocus) strokeWidth else 0,
						if (hasFocus) 0xB3F8FAFC.toInt() else 0x00000000
					)
					setTextColor(if (hasFocus) 0xF2000000.toInt() else android.graphics.Color.WHITE)
					val lp = layoutParams as? android.widget.LinearLayout.LayoutParams
					if (lp != null) {
						val density = resources.displayMetrics.density
						val grow = if (hasFocus) (3 * density).toInt() else 0
						lp.leftMargin = grow
						lp.rightMargin = grow
						lp.topMargin = grow / 2
						lp.bottomMargin = grow / 2
						layoutParams = lp
					}
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
