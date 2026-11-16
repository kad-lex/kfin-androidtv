package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
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
		contentAlignment = Alignment.TopCenter,
		// KFIN: crossfade - appears in place instead of sliding from the top
		enterTransition = fadeIn(
			animationSpec = tween(duration)
		),
		exitTransition = fadeOut(
			animationSpec = tween(duration)
		),
	) {
		SettingsLayout {
			screen()
		}
	}
}
