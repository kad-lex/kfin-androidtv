package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

/**
 * Slot for screen-specific tool buttons rendered at the end of the status bar
 * (after the clock). Screens set [content] while they are visible and clear it
 * when they go away.
 */
object CatalogBarTools {
	val content = mutableStateOf<(@Composable RowScope.() -> Unit)?>(null)
}
