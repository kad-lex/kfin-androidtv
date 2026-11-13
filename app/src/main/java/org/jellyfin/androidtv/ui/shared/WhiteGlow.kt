package org.jellyfin.androidtv.ui.shared

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

fun Modifier.innerGlow(cornerRadius: Dp, enabled: Boolean = true): Modifier = if (!enabled) this else drawWithContent {
	drawContent()

	val stepPx = 1.dp.toPx()
	val strokeWidth = 1.5.dp.toPx()
	val cornerPx = cornerRadius.toPx()

	repeat(4) { i ->
		val insetPx = (i + 1) * stepPx
		drawRoundRect(
			color = Color.White.copy(alpha = 0.35f * (1f - i / 4f)),
			topLeft = Offset(insetPx, insetPx),
			size = Size(size.width - insetPx * 2, size.height - insetPx * 2),
			cornerRadius = CornerRadius((cornerPx - insetPx).coerceAtLeast(0f)),
			style = Stroke(width = strokeWidth)
		)
	}
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
