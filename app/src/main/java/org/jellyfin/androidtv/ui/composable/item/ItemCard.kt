package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens

@Composable
@Stable
fun ItemCard(
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	// KFIN: bottom lip rendered below the poster (captions, progress) -
	// only used where cards have captions (home rows)
	lip: (@Composable BoxScope.() -> Unit)? = null,
	lipColor: Color? = null,
	lipHeight: androidx.compose.ui.unit.Dp = 26.dp,
	// KFIN: fixed poster height - the card becomes poster + lip, the lip is
	// added below and does not shrink the poster
	posterHeight: androidx.compose.ui.unit.Dp,
	shape: Shape = JellyfinTheme.shapes.medium,
	focused: Boolean = false,
	// KFIN: accent color extracted from the poster's lower half - tints the border
	accentColor: Color? = null,
) {
	val focusBorderWidth = with(LocalDensity.current) { 3.toDp() }
	// KFIN: the focus highlight fades between objects instead of jumping
	val focusAlpha by animateFloatAsState(
		targetValue = if (focused) 1f else 0f,
		animationSpec = tween(180),
		label = "cardFocus"
	)
	val borderColor = accentColor ?: Tokens.Color.colorWhite

	// KFIN: border and rounding wrap poster + lip together; the lip eats into
	// the card height (the poster shrinks), it does not extend the card
	Column(
		modifier = modifier
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
			.then(
				if (focused || focusAlpha > 0f) Modifier.border(
					focusBorderWidth,
					borderColor.copy(alpha = 0.8f * focusAlpha),
					shape
				) else Modifier
			)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(posterHeight)
		) {
			image()

			if (overlay != null) {
				Box(
					modifier = Modifier.fillMaxSize(),
					content = overlay
				)
			}
		}

		// KFIN: lip below the poster holding caption + progress
		if (lip != null) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(lipHeight)
					.background(lipColor ?: Color(0xE60B0D14)),
				content = lip
			)
		}
	}
}
