package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jellyfin.design.Tokens

@Composable
fun SettingsLayout(
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit,
) {
	// KFIN: same glass look as the status bar; drops from the top below the bar
	val panelShape = RoundedCornerShape(24.dp)

	Box(
		modifier = modifier
			.padding(
				start = Tokens.Space.spaceMd,
				top = 76.dp,
				end = Tokens.Space.spaceMd,
				bottom = Tokens.Space.spaceMd
			)
			.clip(panelShape)
			.background(Color(0xCC0B0D14))
			.border(
				width = 1.dp,
				brush = Brush.verticalGradient(
					colors = listOf(
						Color.White.copy(alpha = 0.10f),
						Color.White.copy(alpha = 0.05f)
					)
				),
				shape = panelShape
			)
			.width(350.dp)
			.fillMaxHeight()
			.focusRestorer(),
		content = content
	)
}
