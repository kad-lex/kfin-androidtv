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
	// KFIN: the halo can be tinted (e.g. with the poster's key color)
	color: Color = Color.White,
): Modifier = if (!enabled) this else if (onTop) drawWithContent {
	drawContent()
	drawGlow(cornerRadius, radius, alpha, color)
} else drawBehind {
	drawGlow(cornerRadius, radius, alpha, color)
}

fun Modifier.innerGlow(
	cornerRadius: Dp,
	enabled: Boolean = true,
	// KFIN: the glow can be tinted with the poster's key color
	color: Color = Color.White,
	// KFIN: glow strength can be raised (e.g. focused cards)
	strokeWidth: Dp = 2.dp,
): Modifier = if (!enabled) this else drawWithContent {
	drawContent()

	// KFIN: unified focus glow - 50% opacity
	val stepPx = 1.dp.toPx()
	val strokePx = strokeWidth.toPx()
	val cornerPx = cornerRadius.toPx()

	repeat(4) { i ->
		val insetPx = (i + 1) * stepPx
		drawRoundRect(
			color = color.copy(alpha = 0.5f * (1f - i / 4f)),
			topLeft = Offset(insetPx, insetPx),
			size = Size(size.width - insetPx * 2, size.height - insetPx * 2),
			cornerRadius = CornerRadius((cornerPx - insetPx).coerceAtLeast(0f)),
			style = Stroke(width = strokePx)
		)
	}
}

// KFIN: smooth glow that fades from the card edge towards the center -
// no hard boundary; used by the focused cards
fun Modifier.innerGlowFade(
	cornerRadius: Dp,
	color: Color,
	width: Dp,
	baseAlpha: Float = 0.8f,
): Modifier = drawWithContent {
	drawContent()

	val layers = 14
	val stepPx = width.toPx() / layers
	val cornerPx = cornerRadius.toPx()

	repeat(layers) { i ->
		val insetPx = i * stepPx + stepPx / 2f
		drawRoundRect(
			// KFIN: 80% opacity at the card edge, fading to the center; additive
			// blending keeps the glow soft without hard outlines
			color = color.copy(alpha = baseAlpha * (1f - i.toFloat() / layers)),
			topLeft = Offset(insetPx, insetPx),
			size = Size(
				(size.width - insetPx * 2).coerceAtLeast(0f),
				(size.height - insetPx * 2).coerceAtLeast(0f)
			),
			cornerRadius = CornerRadius((cornerPx - insetPx).coerceAtLeast(0f)),
			style = Stroke(width = stepPx),
			blendMode = androidx.compose.ui.graphics.BlendMode.Plus
		)
	}
}

private fun DrawScope.drawGlow(cornerRadius: Dp, radius: Dp, alpha: Float, color: Color = Color.White) {
	val glowPx = radius.toPx()
	val cornerPx = cornerRadius.toPx()
	val paint = Paint().apply {
		this.color = android.graphics.Color.argb(
			(alpha * 255).toInt(),
			(color.red * 255).toInt(),
			(color.green * 255).toInt(),
			(color.blue * 255).toInt()
		)
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
