package org.jellyfin.androidtv.ui.presentation

import android.view.View
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

		// KFIN: the photo slot defaults (right-top) for non-person pages
		private val photoFrame = binding.mainImage.parent as? android.widget.FrameLayout
		private val defaultFrameLp = photoFrame?.layoutParams?.let { androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(it) }
		private val defaultImageLp = binding.mainImage.layoutParams?.let { android.widget.FrameLayout.LayoutParams(it.width, it.height, (it as? android.widget.FrameLayout.LayoutParams)?.gravity ?: android.view.Gravity.TOP or android.view.Gravity.END) }
		private val defaultTitleLp = binding.fdTitle.layoutParams?.let { androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(it) }

		init {
			// KFIN: the rows grid already insets rows 28dp from the screen edge -
			// drop the duplicate offset so the text starts on the common edge
			(binding.guideMainStart.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let {
				it.guideBegin = 0
				binding.guideMainStart.layoutParams = it
			}
		}

		// KFIN: person pages move the photo to the left edge (the rows grid
		// provides the 28dp screen inset) at a fixed 172x258dp size
		// KFIN: person pages anchor the name at the photo's right edge +10dp,
		// vertically centered with the photo
		private fun layoutPersonName(isPerson: Boolean) {
			val frameId = photoFrame?.id ?: return
			val titleLp = binding.fdTitle.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams ?: return
			val density = detailRowView.resources.displayMetrics.density
			if (isPerson) {
				titleLp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
				titleLp.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
				titleLp.startToEnd = frameId
				titleLp.topToTop = frameId
				titleLp.bottomToBottom = frameId
				titleLp.verticalBias = 0.5f
				titleLp.marginStart = (10 * density).toInt()
			} else {
				defaultTitleLp?.let {
					titleLp.startToStart = it.startToStart
					titleLp.startToEnd = it.startToEnd
					titleLp.topToTop = it.topToTop
					titleLp.bottomToTop = it.bottomToTop
					titleLp.bottomToBottom = it.bottomToBottom
					titleLp.verticalBias = it.verticalBias
					titleLp.marginStart = it.marginStart
				}
			}
			binding.fdTitle.translationX = 0f
			binding.fdTitle.translationY = 0f
			binding.fdTitle.layoutParams = titleLp
		}

		private fun layoutPersonPhoto(isPerson: Boolean) {
			// KFIN: person photo corners rounded 3x (36dp)
			binding.mainImage.outlineProvider = if (isPerson) object : android.view.ViewOutlineProvider() {
				override fun getOutline(view: View, outline: android.graphics.Outline) {
					outline.setRoundRect(0, 0, view.width, view.height, 36 * view.resources.displayMetrics.density)
				}
			} else android.view.ViewOutlineProvider.BACKGROUND
			val frameLp = (photoFrame?.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) ?: return
			if (isPerson) {
				frameLp.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
				frameLp.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
				frameLp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
				frameLp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
				frameLp.endToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
				frameLp.marginStart = 0
				frameLp.marginEnd = 0
			} else {
				defaultFrameLp?.let {
					frameLp.width = it.width
					frameLp.height = it.height
					frameLp.startToStart = it.startToStart
					frameLp.endToEnd = it.endToEnd
					frameLp.endToStart = it.endToStart
					frameLp.marginStart = it.marginStart
					frameLp.marginEnd = it.marginEnd
				}
			}
			photoFrame?.layoutParams = frameLp

			val imageLp = binding.mainImage.layoutParams as? android.widget.FrameLayout.LayoutParams ?: return
			if (isPerson) {
				imageLp.width = (172 * detailRowView.resources.displayMetrics.density).toInt()
				imageLp.height = (258 * detailRowView.resources.displayMetrics.density).toInt()
				imageLp.gravity = android.view.Gravity.TOP or android.view.Gravity.START
				binding.mainImage.maxWidth = imageLp.width
				binding.mainImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
			} else {
				defaultImageLp?.let {
					imageLp.width = it.width
					imageLp.height = it.height
					imageLp.gravity = it.gravity
					binding.mainImage.maxWidth = (320 * detailRowView.resources.displayMetrics.density).toInt()
				}
				binding.mainImage.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
			}
			binding.mainImage.layoutParams = imageLp
		}
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
			layoutPersonPhoto(row.item.type == BaseItemKind.PERSON)
			layoutPersonName(row.item.type == BaseItemKind.PERSON)

			binding.fdButtonRow.removeAllViews()
			// KFIN: let the focus ring draw slightly outside the button bounds
			binding.fdButtonRow.clipChildren = false
			binding.fdButtonRow.clipToPadding = false
			var ancestor = binding.fdButtonRow.parent
			var levels = 0
			while (ancestor is android.view.ViewGroup && levels < 3) {
				ancestor.clipChildren = false
				ancestor.clipToPadding = false
				ancestor = ancestor.parent
				levels++
			}
			for (button in row.actions) {
				val parent = button.parent
				if (parent is ViewGroup) parent.removeView(button)

				// KFIN: UP from any action rises to the description (then home),
				// DOWN goes to the leftmost card of the first row
				button.setOnKeyListener { v, keyCode, event ->
					if (event?.action != android.view.KeyEvent.ACTION_DOWN) return@setOnKeyListener false
					when (keyCode) {
						android.view.KeyEvent.KEYCODE_DPAD_UP -> {
							binding.fdSummaryText.requestFocus()
							true
						}
						android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
							// only consume when the hook is registered - otherwise
							// let the default focus search work
							val action = org.jellyfin.androidtv.ui.shared.KfinNav.focusFirstRowAction
							if (action != null) {
								action.invoke()
								true
							} else {
								false
							}
						}
						else -> false
					}
				}
				binding.fdButtonRow.addView(button)
			}

			// KFIN: the description is a focus stop on the way to the status bar
			binding.fdSummaryText.defaultFocusHighlightEnabled = false
			binding.fdSummaryText.setOnFocusChangeListener { _, hasFocus ->
				binding.fdSummaryText.setBackgroundColor(
					if (hasFocus) 0x26FFFFFF else android.graphics.Color.TRANSPARENT
				)
			}
			binding.fdSummaryText.setOnKeyListener { _, keyCode, event ->
				if (event?.action == android.view.KeyEvent.ACTION_DOWN
					&& keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP
				) {
					org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.focusHome()
					true
				} else {
					false
				}
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
