package org.jellyfin.androidtv.ui.browsing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarTools
import org.jellyfin.androidtv.ui.shared.toolbar.GlassIconButton

/**
 * Bridges the browse grid's tool buttons (sort, filters, letter jump) into the
 * status bar. The fragment registers itself while visible; buttons render with
 * the same focus style as the rest of the bar.
 */
object BrowseGridTools {
	private var fragment: BrowseGridFragment? = null
	private var favoritesOnly by mutableStateOf(false)
	private var unwatchedOnly by mutableStateOf(false)

	fun show(fragment: BrowseGridFragment) {
		this.fragment = fragment
		val filters = fragment.filtersSafe
		favoritesOnly = filters.isFavoriteOnly
		unwatchedOnly = filters.isUnwatchedOnly

		CatalogBarTools.content.value = {
			GlassIconButton(
				icon = ImageVector.vectorResource(R.drawable.ic_sort),
				contentDescription = fragment.getString(R.string.lbl_sort_by),
				onClick = { fragment.openSortMenu() }
			)
			if (fragment.isItemsQuery()) {
				GlassIconButton(
					icon = ImageVector.vectorResource(R.drawable.ic_unwatch),
					contentDescription = fragment.getString(R.string.lbl_unwatched),
					activated = unwatchedOnly,
					onClick = {
						unwatchedOnly = !unwatchedOnly
						fragment.applyUnwatchedFilter(unwatchedOnly)
					}
				)
			}
			GlassIconButton(
				icon = ImageVector.vectorResource(R.drawable.ic_heart),
				contentDescription = fragment.getString(R.string.lbl_favorite),
				activated = favoritesOnly,
				onClick = {
					favoritesOnly = !favoritesOnly
					fragment.applyFavoriteFilter(favoritesOnly)
				}
			)
			GlassIconButton(
				icon = ImageVector.vectorResource(R.drawable.ic_jump_letter),
				contentDescription = fragment.getString(R.string.lbl_by_letter),
				onClick = { fragment.openJumplist() }
			)
		}
	}

	fun hide() {
		fragment = null
		CatalogBarTools.content.value = null
	}
}
