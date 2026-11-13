package org.jellyfin.androidtv.ui.shared

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp

fun Modifier.whiteGlow(
	cornerRadius: Dp,
	radius: Dp,
	alpha: Float,
	enabled: Boolean = true,
	onTop: Boolean = false,
): Modifier = if (!enabled) this else if (onTop) drawWithContent {
	drawContent()
	drawGlow(cornerRadius, radius, alpha)
} else drawBehind {
	drawGlow(cornerRadius, radius, alpha)
}

private fun DrawScope.drawGlow(cornerRadius: Dp, radius: Dp, alpha: Float) {
	val glowPx = radius.toPx()
	val cornerPx = cornerRadius.toPx()
	val paint = Paint().apply {
		color = android.graphics.Color.WHITE
		this.alpha = (alpha * 255).toInt()
		isAntiAlias = true
		maskFilter = BlurMaskFilter(glowPx, BlurMaskFilter.Blur.NORMAL)
	}

	drawIntoCanvas { canvas ->
		canvas.nativeCanvas.drawRoundRect(
			-glowPx,
			-glowPx,
			size.width + glowPx,
			size.height + glowPx,
			cornerPx,
			cornerPx,
			paint
		)
	}
}
