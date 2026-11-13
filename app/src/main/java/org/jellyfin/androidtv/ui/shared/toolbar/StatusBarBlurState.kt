package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.runtime.mutableIntStateOf

object StatusBarBlurState {
	val widthPx = mutableIntStateOf(0)
	val heightPx = mutableIntStateOf(0)
}
