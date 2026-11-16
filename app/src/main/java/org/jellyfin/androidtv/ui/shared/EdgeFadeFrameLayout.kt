package org.jellyfin.androidtv.ui.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout
import org.jellyfin.androidtv.util.dp

/**
 * KFIN: FrameLayout that erases its content's alpha near the top/bottom edges
 * (mask fade) instead of clipping it - scrolling cards dissolve smoothly.
 * The clip boundary itself does not move; only the visibility fades.
 */
class EdgeFadeFrameLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

	private var fadeTopPx = 10.dp(context).toFloat()
	private var fadeBottomPx = 56.dp(context).toFloat()

	// KFIN: the library grid keeps only the top fade, lifted a few pixels
	fun setTopFade(px: Float) {
		fadeTopPx = px
		invalidate()
	}

	fun setBottomFadeEnabled(enabled: Boolean) {
		fadeBottomPx = if (enabled) 56.dp(context).toFloat() else 0f
		invalidate()
	}
	private var maskPaint: Paint? = null

	override fun dispatchDraw(canvas: Canvas) {
		if (width == 0 || height == 0 || (fadeTopPx <= 0f && fadeBottomPx <= 0f)) {
			super.dispatchDraw(canvas)
			return
		}

		if (maskPaint == null) {
			maskPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
		}

		val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
		super.dispatchDraw(canvas)

		// top edge: short band so the first row is not dimmed - the fade starts
		// below the visible cards
		if (fadeTopPx > 0f) {
			maskPaint?.shader = LinearGradient(
				0f, 0f, 0f, fadeTopPx,
				intArrayOf(0x00000000, 0xFF000000.toInt()),
				null,
				Shader.TileMode.CLAMP
			)
			canvas.drawRect(0f, 0f, width.toFloat(), fadeTopPx, maskPaint!!)
		}

		// bottom edge: opaque -> transparent
		if (fadeBottomPx > 0f) {
			maskPaint?.shader = LinearGradient(
				0f, height - fadeBottomPx, 0f, height.toFloat(),
				intArrayOf(0xFF000000.toInt(), 0x00000000),
				null,
				Shader.TileMode.CLAMP
			)
			canvas.drawRect(0f, height - fadeBottomPx, width.toFloat(), height.toFloat(), maskPaint!!)
		}

		canvas.restoreToCount(layer)
	}
}
