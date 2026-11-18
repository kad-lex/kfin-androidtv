package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalShapes

object ListControlDefaults {
	@ReadOnlyComposable
	@Composable
	fun colors(
		containerColor: Color = JellyfinTheme.colorScheme.listButton,
		// KFIN: focused state is a white border + inner glow, not a fill
		focusedContainerColor: Color = Color.Transparent,
	) = ListControlColors(
		containerColor = containerColor,
		focusedContainerColor = focusedContainerColor,
	)
}

@Composable
fun ListControl(
	headingContent: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	interactionSource: InteractionSource? = null,
	colors: ListControlColors = ListControlDefaults.colors(),
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	footerContent: (@Composable () -> Unit)? = null,
) {
	@Suppress("NAME_SHADOWING")
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val active = enabled && (focused || pressed)
	val backgroundColor = when {
		!enabled -> colors.containerColor
		active -> colors.focusedContainerColor
		else -> colors.containerColor
	}
	val controlShape = LocalShapes.current.large

	Box(
		modifier = modifier
			.fillMaxWidth()
			// KFIN: item height matches the watch button
			.padding(vertical = 6.dp)
			.then(
				// KFIN: selection is a colour fill only - no outline; the corner
				// radius matches the old outer ring so nothing sticks out
				if (active && colors.focusedContainerColor == Color.Transparent) Modifier
					.background(Color.White, RoundedCornerShape(22.dp))
				else Modifier
			)
			.background(backgroundColor, controlShape)
			.alpha(if (enabled) 1f else 0.4f),
	) {
		// KFIN: dark heading on the white selection pill
		val headingColor = if (active && colors.focusedContainerColor == Color.Transparent) Color(0xE6000000) else JellyfinTheme.colorScheme.listHeadline
		// KFIN: icons follow the text colour so they stay visible on the white fill
		org.jellyfin.androidtv.ui.base.ProvideTextStyle(
			value = org.jellyfin.androidtv.ui.base.LocalTextStyle.current.copy(color = headingColor)
		) {
		ListItemContent(
			headingContent = headingContent,
			overlineContent = overlineContent,
			captionContent = captionContent,
			leadingContent = leadingContent,
			trailingContent = trailingContent,
			footerContent = footerContent,
			headingStyle = JellyfinTheme.typography.listHeadline
				.copy(color = headingColor),
		)
		}
	}
}
