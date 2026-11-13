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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.androidtv.ui.shared.whiteGlow
import org.jellyfin.design.Tokens

@Composable
@Stable
fun ItemCard(
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	footer: (@Composable () -> Unit)? = null,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	progress: Float? = null,
	shape: Shape = JellyfinTheme.shapes.medium,
	focused: Boolean = false,
) {
	val focusBorderWidth = with(LocalDensity.current) { 6.toDp() }

	Box(
		modifier = modifier
			.shadow(
				elevation = 5.dp,
				shape = shape,
				clip = false,
				ambientColor = Color.Black.copy(alpha = 0.8f),
				spotColor = Color.Black.copy(alpha = 0.8f)
			)
			.whiteGlow(cornerRadius = 12.dp, radius = 2.5.dp, alpha = 0.3f, enabled = focused)
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
			.then(if (focused) Modifier.border(focusBorderWidth, Tokens.Color.colorWhite.copy(alpha = 0.8f), shape) else Modifier)
	) {
		image()

		if (overlay != null) {
			Box(
				modifier = Modifier.fillMaxSize(),
				content = overlay
			)
		}

		if (footer != null || progress != null) {
			Column(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
			) {
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
					CardLip(content = footer)
				}
			}
		}
	}
}

@Composable
private fun CardLip(content: @Composable () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(Color(0xF20B0D14))
			.padding(horizontal = 8.dp, vertical = 5.dp),
		contentAlignment = Alignment.Center
	) {
		content()
	}
}
