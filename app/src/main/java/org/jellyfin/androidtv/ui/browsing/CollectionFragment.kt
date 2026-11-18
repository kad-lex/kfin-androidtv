package org.jellyfin.androidtv.ui.browsing

import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import androidx.leanback.widget.ObjectAdapter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.KfinListItemPresenter
import org.jellyfin.androidtv.ui.shared.KfinNav
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.request.GetItemsRequest

class CollectionFragment : EnhancedBrowseFragment() {
	override fun setupQueries(rowLoader: RowLoader) {
		val movies = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
			includeItemTypes = setOf(BaseItemKind.MOVIE),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_movies), movies, 100))

		val series = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
			includeItemTypes = setOf(BaseItemKind.SERIES),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_tv_series), series, 100))

		val others = GetItemsRequest(
			fields = ItemRepository.itemFields,
			parentId = mFolder.id,
			excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
		)
		mRows.add(BrowseRowDef(getString(R.string.lbl_other), others, 100))

		rowLoader.loadRows(mRows)
	}

	// KFIN: episodes/collection items become a 3-column grid (dynamic row
	// count) with cards 10% smaller; navigation keeps the column like the cast
	override fun loadRows(rows: MutableList<BrowseRowDef>) {
		super.loadRows(rows)

		// KFIN: collections keep posters - only seasons switch to the list view
		if (mFolder.type != BaseItemKind.SEASON) return

		val toChunk = mutableListOf<ListRow>()
		for (i in 0 until mRowsAdapter.size()) {
			val row = mRowsAdapter.get(i) as? ListRow ?: continue
			if (row.adapter is ItemRowAdapter) toChunk += row
		}

		toChunk.forEach { row ->
			chunkRowWhenLoaded(row, 0)
		}
	}

	// KFIN: poll the source row until its items arrive, then swap it for a
	// static 3-column chunk grid (observer notifications are unreliable here)
	private fun chunkRowWhenLoaded(row: ListRow, attempts: Int) {
		val adapter = row.adapter as ItemRowAdapter
		if (attempts > 50) return
		if (adapter.size() == 0) {
			android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
				{ chunkRowWhenLoaded(row, attempts + 1) }, 200)
			return
		}
		val index = mRowsAdapter.indexOf(row)
		if (index < 0) return
		val items = (0 until adapter.size()).map { adapter.get(it) as BaseRowItem }
		mRowsAdapter.remove(row)
		var insert = index
		// KFIN: one list row per episode - same look as the series page
		items.forEachIndexed { itemIndex, entry ->
			val single = MutableObjectAdapter<BaseRowItem>(KfinListItemPresenter())
			single.add(entry)
			val header = if (itemIndex == 0) row.headerItem else HeaderItem(itemIndex.toLong(), "")
			mRowsAdapter.add(insert++, ListRow(header, single))
		}
	}

	// KFIN: BACK from an episode returns the cursor to the exact card
	private var savedRowPosition = -1

	override fun onResume() {
		super.onResume()
		KfinNav.preserveColumn = true
		if (savedRowPosition >= 0) {
			val row = savedRowPosition
			savedRowPosition = -1
			mRowsFragment.verticalGridView?.post { focusSavedCard(row, 0) }
		} else {
			// KFIN: first entry into a season/collection lands on the first card
			mRowsFragment.verticalGridView?.post { focusSavedCard(0, 0) }
		}
	}

	private fun focusSavedCard(rowPosition: Int, attempts: Int) {
		val rv = mRowsFragment.verticalGridView ?: return
		val holder = rv.findViewHolderForAdapterPosition(rowPosition)
		if (holder == null) {
			if (attempts < 30) {
				rv.scrollToPosition(rowPosition)
				rv.postDelayed({ focusSavedCard(rowPosition, attempts + 1) }, 100)
			}
			return
		}
		val grid = findGrid(holder.itemView)
		val position = grid?.selectedPosition?.coerceAtLeast(0) ?: 0
		grid?.post {
			grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
				?: holder.itemView.requestFocus()
		}
	}

	private fun findGrid(view: android.view.View): androidx.leanback.widget.HorizontalGridView? = when (view) {
		is androidx.leanback.widget.HorizontalGridView -> view
		is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findGrid(view.getChildAt(it)) }
		else -> null
	}

	override fun onPause() {
		savedRowPosition = mRowsFragment.selectedPosition
		KfinNav.preserveColumn = false
		super.onPause()
	}
}
