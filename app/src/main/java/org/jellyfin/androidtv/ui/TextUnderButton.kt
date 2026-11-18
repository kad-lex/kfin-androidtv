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

	// KFIN: watch-button style - white outer ring at a 2.5px gap on focus
	private val ringPaint = android.graphics.Paint().apply {
		color = android.graphics.Color.WHITE
		isAntiAlias = true
		style = android.graphics.Paint.Style.STROKE
	}
	private var ringFraction = 0f

	override fun dispatchDraw(canvas: android.graphics.Canvas) {
		if (ringFraction > 0f) {
			ringPaint.alpha = (ringFraction * 255).toInt()
			ringPaint.strokeWidth = 2.dp(context).toFloat()
			val gap = 2.5f * resources.displayMetrics.density
			canvas.drawRoundRect(
				-gap, -gap,
				width + gap, height + gap,
				40.dp(context).toFloat(), 40.dp(context).toFloat(),
				ringPaint
			)
		}
		super.dispatchDraw(canvas)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		// KFIN: leave room for the focus ring so the parent row does not clip it
		val lp = layoutParams
		if (lp is android.widget.LinearLayout.LayoutParams) {
			val m = 3.dp(context)
			lp.leftMargin = m
			lp.rightMargin = m
			lp.topMargin = m / 2
			lp.bottomMargin = m / 2
			layoutParams = lp
		}
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
				ringFraction = focusFraction
				applyFocusColors()
				invalidate()
			}
			start()
		}
	}

	private fun applyFocusColors() {
		val f = focusFraction
		glassDrawable.setColor(lerpColor(GLASS_COLOR, FOCUSED_FILL_COLOR, f))
		glassDrawable.setStroke(1.dp(context), lerpColor(NORMAL_STROKE_COLOR, FOCUSED_STROKE_COLOR, f))
		// KFIN: content inverts with the fill; activated stays red like the progress bar
		val content = if (isActivated && f < 0.5f) ACTIVATED_COLOR else lerpColor(CONTENT_LIGHT_COLOR, CONTENT_DARK_COLOR, f)
		binding.label.setTextColor(content)
		binding.imageButton.setColorFilter(content)
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
		// KFIN: watch-button style - matte 45% glass; focused turns white 80%,
		// content inverts to dark
		private const val GLASS_COLOR = 0xFF000000.toInt()
		private const val NORMAL_STROKE_COLOR = 0xFFFFFFFF.toInt()
		private const val FOCUSED_STROKE_COLOR = 0xFFFFFFFF.toInt()
		private const val FOCUSED_FILL_COLOR = 0xD9FFFFFF.toInt()
		private const val CONTENT_LIGHT_COLOR = 0xFFFFFFFF.toInt()
		private const val CONTENT_DARK_COLOR = 0xE6000000.toInt()
		private const val ACTIVATED_COLOR = 0xFFFF0033.toInt()
		private const val PROGRESS_COLOR = 0xFFFF0033.toInt() // KFIN: YouTube red

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
