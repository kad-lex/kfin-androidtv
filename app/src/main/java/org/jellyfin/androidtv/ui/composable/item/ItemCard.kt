package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens

@Composable
@Stable
fun ItemCard(
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	shape: Shape = JellyfinTheme.shapes.medium,
	focused: Boolean = false,
) {
	val focusBorderWidth = with(LocalDensity.current) { 7.toDp() }

	Box(
		modifier = modifier
			.shadow(
				elevation = 5.dp,
				shape = shape,
				clip = false,
				ambientColor = Color.Black.copy(alpha = 0.8f),
				spotColor = Color.Black.copy(alpha = 0.8f)
			)
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
			.then(if (focused) Modifier.border(focusBorderWidth, Tokens.Color.colorWhite.copy(alpha = 0.8f), shape) else Modifier)
			.then(if (focused) Modifier.innerGlow(12.dp) else Modifier)
	) {
		image()

		if (overlay != null) {
			Box(
				modifier = Modifier.fillMaxSize(),
				content = overlay
			)
		}
	}
}

private fun Modifier.innerGlow(cornerRadius: Dp, enabled: Boolean = true): Modifier = if (!enabled) this else drawWithContent {
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
