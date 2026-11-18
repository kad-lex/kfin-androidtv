package org.jellyfin.androidtv.ui.shared

import android.view.View
import android.view.ViewParent
import android.view.ViewGroup
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ListRowView
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus

/**
 * KFIN: deterministic vertical navigation for horizontal rows (home, details).
 * Up/Down moves between rows and always lands on the LEFTMOST card; moving up
 * from the top row goes to the status bar home button; moving down from the
 * bottom row is blocked. Library grids are left to the default behaviour.
 */
object KfinNav {
	// KFIN: details pages register how to focus the first card of the first row
	var focusFirstRowAction: (() -> Unit)? = null

	// KFIN: browse pages register what rising from the FIRST row focuses
	// (the page title) instead of jumping straight to the status bar
	var topAction: (() -> Unit)? = null

	// KFIN: details pages navigate like the media grid - vertical moves keep
	// the column; edge positions lead nowhere
	var preserveColumn = false

	// KFIN: the last focused column - leanback lands on each grid's remembered
	// position, so details pages correct the landing back to this column
	var lastColumn = -1

	fun moveVertical(from: View, down: Boolean): Boolean {
		// find the row's HorizontalGridView and the rows RecyclerView above it;
		// a VerticalGridView reached BEFORE any HorizontalGridView means we are
		// inside a library grid - leave that to leanback
		var parent: ViewParent? = from.parent
		var rowGrid: HorizontalGridView? = null
		var rowView: View? = null
		var rowsRecycler: RecyclerView? = null
		var child: View = from
		while (parent is View) {
			when (parent) {
				is HorizontalGridView -> if (rowGrid == null) rowGrid = parent
				is VerticalGridView -> {
					if (rowGrid == null) return false
					rowsRecycler = parent
					rowView = child
					break
				}
			}
			child = parent
			parent = parent.parent
		}
		val grid = rowGrid ?: return false
		val recycler = rowsRecycler ?: return false
		val row = rowView ?: return false
		val layoutManager = recycler.layoutManager as? LinearLayoutManager ?: return false

		val rowPosition = layoutManager.getPosition(row)
		val target = rowPosition + if (down) 1 else -1

		// top row up: home goes through the watch button, content pages go
		// to the primary play button (never straight to the status bar)
		if (target < 0) {
			if (org.jellyfin.androidtv.ui.home.HomeFocusCoordinator.isActive) {
				org.jellyfin.androidtv.ui.home.HomeFocusCoordinator.moveUp()
			} else if (org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction.hasPrimary) {
				org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction.requestPrimary()
			} else if (topAction != null) {
				topAction?.invoke()
			} else {
				CatalogBarFocus.focusHome()
			}
			return true
		}
		// bottom row down -> blocked
		if (target >= layoutManager.itemCount) return true

		// KFIN: strictly vertical moves - the SAME index in the target row
		// (clamped to its size); nothing is remembered between moves
		val column = if (preserveColumn) grid.selectedPosition.coerceAtLeast(0) else 0
		val holder = recycler.findViewHolderForAdapterPosition(target)
		if (holder != null) {
			focusFirst(holder.itemView, column)
		} else {
			recycler.scrollToPosition(target)
			recycler.post {
				recycler.findViewHolderForAdapterPosition(target)?.let { focusFirst(it.itemView, column) }
			}
		}
		return true
	}

	private fun focusFirst(itemView: View, position: Int = 0) {
		val grid = findHorizontalGrid(itemView)
		if (grid == null) {
			// KFIN: the overview row holds no cards - it can never take focus.
			// Rising from the first card row lands on the primary action instead
			if (org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction.hasPrimary) {
				org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction.requestPrimary()
			} else if (org.jellyfin.androidtv.ui.home.HomeFocusCoordinator.isActive) {
				org.jellyfin.androidtv.ui.home.HomeFocusCoordinator.moveUp()
			} else {
				CatalogBarFocus.focusHome()
			}
			return
		}
		val target = position.coerceIn(0, (grid.adapter?.itemCount ?: 1) - 1)
		grid.setSelectedPosition(target)
		grid.post {
			// KFIN: the row itself can never take focus - fall back to the grid,
			// which focuses its selected card
			grid.findViewHolderForAdapterPosition(target)?.itemView?.requestFocus() ?: grid.requestFocus()
		}
	}

	private tailrec fun findHorizontalGrid(view: View): HorizontalGridView? = when (view) {
		is HorizontalGridView -> view
		is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findHorizontalGrid(view.getChildAt(it)) }
		else -> null
	}
}
