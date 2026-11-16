package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.ui.graphics.Color

/**
 * Colors used in the [BaseItemInfoRow].
 */
object InfoRowColors {
	val Transparent = Color.Transparent to Color.White
	// KFIN: status-bar glass capsule
	val Default = Color(0xCC0B0D14) to Color.White
	val Green = Color(0xCC0B0D14) to Color(0xFF3DDC84)
	val Red = Color(0xCC0B0D14) to Color(0xFFF2364D)
}
