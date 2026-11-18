package org.jellyfin.androidtv.ui.presentation

import android.view.View
import androidx.core.view.isVisible
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.util.Utils

open class CustomListRowPresenter @JvmOverloads constructor(
	private val topPadding: Int? = null
) : ListRowPresenter() {
	init {
		headerPresenter = CustomRowHeaderPresenter()
	}

	override fun isUsingDefaultShadow() = false

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit

	// KFIN: leanback re-pads the grid when a row gets selected (20dp on top) -
	// zero it again so the cast rows keep their 1dp gap
	override fun onRowViewSelected(holder: RowPresenter.ViewHolder, selected: Boolean) {
		super.onRowViewSelected(holder, selected)
		if (topPadding != null && holder is ListRowPresenter.ViewHolder) {
			holder.gridView.setPadding(0, 0, 0, 0)
		}
	}

	override fun onRowViewExpanded(holder: RowPresenter.ViewHolder, expanded: Boolean) {
		super.onRowViewExpanded(holder, expanded)
		if (topPadding != null && holder is ListRowPresenter.ViewHolder) {
			holder.gridView.setPadding(0, 0, 0, 0)
		}
	}

	override fun onBindRowViewHolder(holder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(holder, item)

		// KFIN: visible gap between cards in a row, like in the browse grid
		if (holder is ListRowPresenter.ViewHolder) {
			holder.gridView.horizontalSpacing = Utils.convertDpToPixel(holder.gridView.context, 12)
			// KFIN: the tight cast-grid styling applies ONLY to the details page
			// (the one built with an explicit topPadding) - home and other rows
			// keep their stock padding/height
			if (topPadding != null) {
				// KFIN: zero once at bind - a layout listener would re-layout on
				// every scroll and make the rows visibly jump
				holder.gridView.setPadding(0, 0, 0, 0)
			} else {
				holder.gridView.setPadding(0, holder.gridView.paddingTop, 0, holder.gridView.paddingBottom)
			}
		}

		val view = holder.view?.parent as? View
		if (topPadding != null && view != null) view.setPadding(view.paddingLeft, topPadding, view.paddingRight, view.paddingBottom)

		// KFIN: collapse empty headers too - an empty ("") header still reserves
		// a full text line and creates a huge gap between the cast rows
		val headerName = (item as? ListRow)?.headerItem?.name
		holder.headerViewHolder?.view?.isVisible = !headerName.isNullOrEmpty()
	}
}
