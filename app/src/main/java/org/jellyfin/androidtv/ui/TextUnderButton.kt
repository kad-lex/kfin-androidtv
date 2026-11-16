package org.jellyfin.androidtv.ui

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import org.jellyfin.androidtv.databinding.TextUnderButtonBinding
import org.jellyfin.androidtv.util.AndroidVersion
import org.jellyfin.androidtv.util.dp

class TextUnderButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
	val binding = TextUnderButtonBinding.inflate(LayoutInflater.from(context), this, true)

	// KFIN: status-bar style capsule - glass fill, white border on focus,
	// and the whole capsule doubles as the resume progress bar (DUNE-style)
	private val glassDrawable = GradientDrawable().apply {
		shape = GradientDrawable.RECTANGLE
		cornerRadius = 40.dp(context).toFloat()
		setColor(GLASS_COLOR)
	}
	private val progressFillDrawable = GradientDrawable().apply {
		shape = GradientDrawable.RECTANGLE
		cornerRadius = 38.dp(context).toFloat()
		setColor(PROGRESS_COLOR)
	}
	private val progressFill = ClipDrawable(
		progressFillDrawable,
		Gravity.LEFT,
		ClipDrawable.HORIZONTAL
	)
	private val pillBackground = LayerDrawable(arrayOf(glassDrawable, progressFill)).apply {
		// keep the fill inside the capsule outline
		setLayerInset(1, 3.dp(context), 3.dp(context), 3.dp(context), 3.dp(context))
	}

	private var hasLabel = false

	// KFIN: white selection pill drawn larger than the button itself
	private val selectionPaint = android.graphics.Paint().apply {
		color = 0xF2FFFFFF.toInt()
		isAntiAlias = true
	}
	private var selectionFraction = 0f

	override fun dispatchDraw(canvas: android.graphics.Canvas) {
		if (selectionFraction > 0f) {
			selectionPaint.alpha = (selectionFraction * 255).toInt()
			val expand = 6.dp(context).toFloat()
			canvas.drawRoundRect(
				-expand, -expand,
				width + expand, height + expand,
				40.dp(context).toFloat(), 40.dp(context).toFloat(),
				selectionPaint
			)
		}
		super.dispatchDraw(canvas)
	}

	init {
		isFocusable = true
		isFocusableInTouchMode = true
		descendantFocusability = FOCUS_BLOCK_DESCENDANTS
		if (AndroidVersion.isAtLeastO) defaultFocusHighlightEnabled = false

		binding.pill.background = pillBackground
		updatePill(isFocused)
	}

	// Focus is tracked via drawable state so the border can never get stuck
	override fun drawableStateChanged() {
		super.drawableStateChanged()
		updatePill(isFocused)
	}

	private var focusFraction = 0f
	private var focusAnimator: android.animation.ValueAnimator? = null

	private fun updatePill(focused: Boolean) {
		// KFIN: fixed padding in both states - focus animates stroke + fill (no glow)
		val horizontal = if (hasLabel) 12.dp(context) else 9.dp(context)
		val vertical = 8.dp(context)
		binding.pill.setPadding(horizontal, vertical, horizontal, vertical)

		// KFIN: smooth focus transition instead of a hard jump
		focusAnimator?.cancel()
		focusAnimator = android.animation.ValueAnimator.ofFloat(focusFraction, if (focused) 1f else 0f).apply {
			duration = 180
			addUpdateListener {
				focusFraction = it.animatedValue as Float
				selectionFraction = focusFraction
				applyFocusColors()
				invalidate()
			}
			start()
		}
	}

	private fun applyFocusColors() {
		val f = focusFraction
		// KFIN: matte glass - white pill with dark content; focused turns dark
		glassDrawable.setColor(lerpColor(GLASS_COLOR, FOCUSED_FILL_COLOR, f))
		glassDrawable.setStroke(1.dp(context), lerpColor(NORMAL_STROKE_COLOR, FOCUSED_STROKE_COLOR, f))
		val contentColor = lerpColor(CONTENT_DARK_COLOR, CONTENT_LIGHT_COLOR, f)
		binding.label.setTextColor(contentColor)
		binding.imageButton.setColorFilter(contentColor)
		binding.pill.invalidate()
	}

	private fun lerpColor(a: Int, b: Int, t: Float): Int {
		val ar = a shr 16 and 0xFF; val ag = a shr 8 and 0xFF; val ab = a and 0xFF; val aa = a ushr 24
		val br = b shr 16 and 0xFF; val bg = b shr 8 and 0xFF; val bb = b and 0xFF; val ba = b ushr 24
		return ((aa + ((ba - aa) * t).toInt()) shl 24) or
			((ar + ((br - ar) * t).toInt()) shl 16) or
			((ag + ((bg - ag) * t).toInt()) shl 8) or
			(ab + ((bb - ab) * t).toInt())
	}

	fun setLabel(text: String?) {
		hasLabel = text != null
		binding.label.isVisible = hasLabel
		binding.label.text = text
		binding.imageButton.contentDescription = text
		updatePill(isFocused)
	}

	fun setIcon(@DrawableRes resource: Int, maxHeight: Int? = null) {
		binding.imageButton.setImageResource(resource)
		binding.imageButton.maxHeight = maxHeight ?: Int.MAX_VALUE
	}

	// KFIN: fills the capsule itself (DUNE-style); null/zero hides the fill
	fun setProgress(percent: Int?) {
		progressFill.level = when {
			percent == null || percent <= 0 -> 0
			else -> percent.coerceIn(0, 100) * 100
		}
	}

	fun setPadding(padding: Int?) {
		binding.imageButton.setPadding(padding?.dp(context) ?: 0)
	}

	companion object {
		private const val GLASS_COLOR = 0xCC0B0D14.toInt()
		private const val NORMAL_STROKE_COLOR = 0x1AFFFFFF
		// KFIN: selection - white pill larger than the button, dark content
		private const val FOCUSED_STROKE_COLOR = 0xB3F8FAFC.toInt()
		private const val FOCUSED_FILL_COLOR = 0xCC0B0D14.toInt()
		private const val CONTENT_DARK_COLOR = 0xFFFFFFFF.toInt()
		private const val CONTENT_LIGHT_COLOR = 0xF2000000.toInt()
		private const val PROGRESS_COLOR = 0x40FFFFFF

		@JvmStatic
		@Suppress("LongParameterList")
		fun create(
			context: Context,
			@DrawableRes icon: Int,
			maxHeight: Int? = null,
			padding: Int? = null,
			label: String? = null,
			onClickListener: OnClickListener
		) = TextUnderButton(context).apply {
			setLabel(label)
			setIcon(icon, maxHeight)
			setPadding(padding)
			setOnClickListener(onClickListener)
		}
	}
}
