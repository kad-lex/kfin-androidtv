package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.shared.whiteGlow
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
	val focusBorderWidth = with(LocalDensity.current) { 6.toDp() }

	val glowPadding = 5.dp

	Box(
		modifier = modifier
			.padding(glowPadding)
			.whiteGlow(cornerRadius = 12.dp, radius = 2.5.dp, alpha = 0.3f, enabled = focused)
			.shadow(
				elevation = 5.dp,
				shape = shape,
				ambientColor = Color.Black.copy(alpha = 0.8f),
				spotColor = Color.Black.copy(alpha = 0.8f)
			)
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
	}
}
