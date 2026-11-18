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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.vectorResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens

@Composable
@Stable
fun ItemCard(
	modifier: Modifier = Modifier,
	image: @Composable BoxScope.() -> Unit,
	overlay: (@Composable BoxScope.() -> Unit)? = null,
	// KFIN: fixed poster height - no caption lip, the card is the poster only
	posterHeight: androidx.compose.ui.unit.Dp,
	shape: Shape = JellyfinTheme.shapes.medium,
	focused: Boolean = false,
	// KFIN: accent color extracted from the poster's lower half - tints the border
	accentColor: Color? = null,
	// KFIN: play bubble on focus - media cards only, never persons/seasons
	showPlayBubble: Boolean = true,
) {
		// KFIN: the focus highlight fades between objects instead of jumping
	val focusAlpha by animateFloatAsState(
		targetValue = if (focused) 1f else 0f,
		animationSpec = tween(180),
		label = "cardFocus"
	)
	// KFIN: every card wears the same static border - white, 3px, 100%;
	// selection swaps it for a full-opacity accent border, 5px
	val restBorder = Color.White.copy(alpha = 1f)
	val focusBorder = (accentColor ?: Tokens.Color.colorWhite).copy(alpha = 1f)
	val borderColor = androidx.compose.ui.graphics.lerp(restBorder, focusBorder, focusAlpha)

	Column(
		modifier = modifier
			.clip(shape)
			.background(JellyfinTheme.colorScheme.surface, shape)
			.border((3f + 2f * focusAlpha).dp, borderColor, shape)
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

			// KFIN: focused card gets a centered play bubble in the selection
			// color (same 30dp size as the status bar icon buttons)
			if (showPlayBubble && focusAlpha > 0f) {
				Box(
					modifier = Modifier
						.align(androidx.compose.ui.Alignment.Center)
						.size(30.dp)
						.graphicsLayer { alpha = focusAlpha }
						.background(borderColor, androidx.compose.foundation.shape.CircleShape),
					contentAlignment = androidx.compose.ui.Alignment.Center
				) {
					org.jellyfin.androidtv.ui.base.Icon(
						imageVector = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(
							org.jellyfin.androidtv.R.drawable.ic_play
						),
						contentDescription = null,
						tint = Color(0xE6000000),
						modifier = Modifier.size(16.dp)
					)
				}
			}
		}

	}
}
