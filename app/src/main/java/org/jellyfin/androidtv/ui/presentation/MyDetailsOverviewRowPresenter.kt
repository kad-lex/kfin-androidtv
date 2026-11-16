package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.DetailRowView
import org.jellyfin.androidtv.ui.itemdetail.MyDetailsOverviewRow
import org.jellyfin.androidtv.util.InfoLayoutHelper
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.sdk.model.api.BaseItemKind

class MyDetailsOverviewRowPresenter(
	private val markdownRenderer: MarkdownRenderer,
) : RowPresenter() {
	class ViewHolder(
		private val detailRowView: DetailRowView,
		private val markdownRenderer: MarkdownRenderer,
	) : RowPresenter.ViewHolder(detailRowView) {
		private val binding get() = detailRowView.binding
		private var boundInfoItems: List<org.jellyfin.androidtv.data.model.InfoItem>? = null
		private var boundGenreText: String? = null

		// KFIN: genre row also carries the duration (and "ends" when available);
		// the director field is not shown
		private fun buildGenreLine(): String {
			val meta = boundInfoItems
				?.filter { it.label.isNotEmpty() && it.value.isNotEmpty() }
				?.joinToString("    ") { "${it.label}: ${it.value}" }
				.orEmpty()
			return listOfNotNull(boundGenreText, meta.takeIf { it.isNotEmpty() })
				.joinToString("    ")
		}

		fun setItem(row: MyDetailsOverviewRow) {
			setTitle(row.item.name)

			InfoLayoutHelper.addInfoRow(view.context, row.item, row.item.mediaSources?.getOrNull(row.selectedMediaSourceIndex), binding.fdMainInfoRow, false)
			boundGenreText = row.item.genres?.joinToString(" / ")
			// KFIN: only duration/ends are kept - the director field is not shown
			boundInfoItems = listOfNotNull(row.infoItem2, row.infoItem3)
			binding.fdGenreRow.text = buildGenreLine()

			binding.mainImage.load(row.imageDrawable, null, null, 1.0, 0)

			setSummary(row.summary)

			if (row.item.type == BaseItemKind.PERSON) {
				binding.fdSummaryText.maxLines = 9
				binding.fdGenreRow.isVisible = false
			}

			binding.fdButtonRow.removeAllViews()
			for (button in row.actions) {
				val parent = button.parent
				if (parent is ViewGroup) parent.removeView(button)

				binding.fdButtonRow.addView(button)
			}
		}

		fun setTitle(title: String?) {
			binding.fdTitle.text = title
		}

		fun setSummary(summary: String?) {
			binding.fdSummaryText.text = summary?.let { markdownRenderer.toMarkdownSpanned(it) }
		}

		fun setInfoValue3(text: String?) {
			// KFIN: "ends" value lives in the genre line; rebuild it with the new value
			val items = boundInfoItems?.mapIndexed { index, item ->
				if (index == 1) item.copy(value = text ?: "") else item
			} ?: return
			boundInfoItems = items
			binding.fdGenreRow.text = buildGenreLine()
		}
	}

	var viewHolder: ViewHolder? = null
		private set

	init {
		syncActivatePolicy = SYNC_ACTIVATED_CUSTOM
	}

	override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
		val view = DetailRowView(parent.context)
		viewHolder = ViewHolder(view, markdownRenderer)
		return viewHolder!!
	}

	override fun onBindRowViewHolder(viewHolder: RowPresenter.ViewHolder, item: Any) {
		super.onBindRowViewHolder(viewHolder, item)
		if (item !is MyDetailsOverviewRow) return
		if (viewHolder !is ViewHolder) return

		viewHolder.setItem(item)
	}

	override fun onSelectLevelChanged(holder: RowPresenter.ViewHolder) = Unit
}
