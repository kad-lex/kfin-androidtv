package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlin.math.roundToInt
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jellyfin.androidtv.ui.base.dialog.DialogBase

@Composable
fun SettingsDialog(
	visible: Boolean,
	onDismissRequest: () -> Unit,
	modifier: Modifier = Modifier,
	screen: @Composable BoxScope.() -> Unit,
) {
	val duration = 280
	DialogBase(
		visible = visible,
		onDismissRequest = onDismissRequest,
		modifier = modifier,
		// KFIN: no dim - the calling page stays fully visible
		dimBackground = false,
		contentAlignment = Alignment.TopStart,
		// KFIN: crossfade - appears in place instead of sliding from the top
		enterTransition = fadeIn(
			animationSpec = tween(duration)
		),
		exitTransition = fadeOut(
			animationSpec = tween(duration)
		),
	) {
		// KFIN: flush right, 10px from the screen edge, raised above the status bar
		var contentWidth by remember { mutableStateOf(0) }
		var screenWidth by remember { mutableStateOf(0) }
		val density = LocalDensity.current
		Box(modifier = Modifier.fillMaxSize().onSizeChanged { screenWidth = it.width }) {
			Box(
				modifier = Modifier
					.align(Alignment.TopStart)
					.offset {
						val belowY = CatalogBarFocus.settingsButtonBottomY
						IntOffset(
							x = if (screenWidth > 0 && contentWidth > 0) (screenWidth - contentWidth - 10) else 0,
							y = if (belowY >= 0) (belowY - 115).roundToInt()
							else with(density) { 171.dp.toPx() }.roundToInt()
						)
					}
					.wrapContentSize()
					// KFIN: the settings menu at 75% scale, anchored top-right
					.graphicsLayer {
						scaleX = 0.75f
						scaleY = 0.75f
						transformOrigin = TransformOrigin(1f, 0f)
					}
					.onSizeChanged { contentWidth = it.width }
			) {
				SettingsLayout {
					screen()
				}
			}
		}
	}
}
