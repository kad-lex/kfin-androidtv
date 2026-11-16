package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jellyfin.design.Tokens

@Composable
fun Badge(
	modifier: Modifier = Modifier,
	shape: Shape = CircleShape,
	containerColor: Color = JellyfinTheme.colorScheme.badge,
	contentColor: Color = JellyfinTheme.colorScheme.onBadge,
	// KFIN: callers can scale the whole badge down (badge + content)
	minSize: Dp = 24.dp,
	contentPadding: Dp = Tokens.Space.spaceXs,
	textStyle: TextStyle = JellyfinTheme.typography.badge,
	// KFIN: status-bar style outline
	borderWidth: Dp = 0.dp,
	borderColor: Color = Color.Unspecified,
	content: @Composable BoxScope.() -> Unit,
) {
	ProvideTextStyle(textStyle.copy(color = contentColor)) {
		Box(
			modifier = modifier
				.defaultMinSize(minWidth = minSize, minHeight = minSize)
				.background(containerColor, shape)
				.then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier)
				.padding(contentPadding),
			contentAlignment = Alignment.Center,
		) {
			content()
		}
	}
}
