package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.design.Tokens

private val CardLipHeight = 44.dp

@Composable
@Stable
fun ItemCard(
	size: DpSize,
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	footer: (@Composable () -> Unit)? = null,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	progress: Float? = null,
	shape: Shape = JellyfinTheme.shapes.medium,
	focused: Boolean = false,
) {
	val focusBorderWidth = with(LocalDensity.current) { 6.toDp() }
	val hasLip = footer != null || progress != null

	Box(
		modifier = modifier
			.width(size.width)
			.height(size.height + if (hasLip) CardLipHeight else 0.dp)
			.shadow(
				elevation = 5.dp,
				shape = shape,
				clip = false,
				ambientColor = Color.Black.copy(alpha = 0.8f),
				spotColor = Color.Black.copy(alpha = 0.8f)
			)
			.cardGlow(12.dp, enabled = focused)
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
			.then(if (focused) Modifier.border(focusBorderWidth, Tokens.Color.colorWhite.copy(alpha = 0.8f), shape) else Modifier)
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(size.height)
			) {
				image()
			}

			if (hasLip) {
				Column(modifier = Modifier.fillMaxWidth()) {
					if (progress != null) {
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 8.dp, vertical = 3.dp)
						) {
							Seekbar(
								progress = progress,
								enabled = false,
								modifier = Modifier
									.fillMaxWidth()
									.height(3.dp)
							)
						}
					}

					if (footer != null) {
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.height(CardLipHeight - if (progress != null) 9.dp else 0.dp),
							contentAlignment = Alignment.Center
						) {
							footer()
						}
					}
				}
			}
		}

		if (overlay != null) {
			Box(
				modifier = Modifier.fillMaxSize(),
				content = overlay
			)
		}
	}
}

private fun Modifier.cardGlow(cornerRadius: Dp, enabled: Boolean): Modifier = if (!enabled) this else drawBehind {
	val strokeWidth = 2.dp.toPx()
	listOf(2.dp, 4.dp, 6.dp, 8.dp).forEachIndexed { index, expand ->
		val expandPx = expand.toPx()
		drawRoundRect(
			color = Color.White.copy(alpha = 0.5f - index * 0.12f),
			topLeft = Offset(-expandPx, -expandPx),
			size = Size(size.width + expandPx * 2, size.height + expandPx * 2),
			cornerRadius = CornerRadius(cornerRadius.toPx() + expandPx),
			style = Stroke(width = strokeWidth)
		)
	}
}
