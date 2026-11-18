package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R

/**
 * KFIN: fully custom player menu - our glass capsule, no appcompat/system
 * background. The menu fades in above the panel, horizontally centered on the
 * screen, bottom edge fixed at button center + 50px. Long menus render as a
 * grid (columns > 1) so everything fits without scrolling.
 */
class KfinMenuPopup private constructor(
	private val window: PopupWindow,
) {
	fun dismiss() = window.dismiss()

	companion object {
		private const val TEXT_FOCUSED = 0xE6000000.toInt()
		private const val TEXT_NORMAL = 0xD9FFFFFF.toInt()
		private const val FILL_FOCUSED = 0xFFFFFFFF.toInt()

		fun show(
			context: Context,
			anchor: View,
			items: List<String>,
			checkedIndex: Int,
			onSelect: (Int) -> Unit,
			onDismiss: () -> Unit,
			columns: Int = 1,
			// KFIN: when set, the menu's bottom edge floats this many px above
			// the transport panel instead of the fixed button-center offset
			abovePanelGap: Int? = null,
		): KfinMenuPopup {
			val density = context.resources.displayMetrics.density
			fun dp(v: Int) = (v * density).toInt()

			val grid = columns > 1
			val container = LinearLayout(context).apply {
				orientation = LinearLayout.VERTICAL
				// KFIN: 60% glass in the player - mutate so the shared drawable keeps its opacity
				background = ContextCompat.getDrawable(context, R.drawable.kfin_menu_background)?.mutate()?.apply { alpha = 153 }
				val pad = dp(8)
				setPadding(pad, pad, pad, pad)
			}
			lateinit var self: KfinMenuPopup
			val rowViews = mutableListOf<View>()
			val checkColor = 0xFFFF0033.toInt()

			val groups = if (grid) items.chunked(columns) else items.map { listOf(it) }

			// KFIN: fixed cell width in every menu (widest label incl. the
			// check prefix) - pills never resize with focus or content
			val cellWidth = run {
				val paint = android.text.TextPaint().apply {
					textSize = android.util.TypedValue.applyDimension(
						android.util.TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics
					)
				}
				val widest = items.maxOf { paint.measureText("\u2713  $it") }
				(widest + dp(16) * 2).toInt()
			}

			groups.forEachIndexed { groupIndex, groupItems ->
				val groupLayout = if (grid) {
					LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
				} else container

				groupItems.forEachIndexed { itemIndex, label ->
					val index = if (grid) groupIndex * columns + itemIndex else groupIndex

					val text = if (index == checkedIndex) {
						SpannableString("✓  $label").apply {
							setSpan(ForegroundColorSpan(checkColor), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
						}
					} else label

					// KFIN: per-row pill, driven manually - reliable focus outline
					val pill = GradientDrawable().apply {
						shape = GradientDrawable.RECTANGLE
						cornerRadius = 999f
						setColor(0x00000000)
					}

					val row = TextView(context).apply {
						this.text = text
						textSize = 16f
						gravity = Gravity.START or Gravity.CENTER_VERTICAL
						val h = dp(16)
						val v = dp(12)
						setPadding(h, v, h, v)
						background = pill
						isFocusable = true
						if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
							defaultFocusHighlightEnabled = false
						}

						val applyState = { focused: Boolean ->
							pill.setColor(if (focused) FILL_FOCUSED else 0x00000000)
							setTextColor(if (focused) TEXT_FOCUSED else TEXT_NORMAL)
						}
						setOnFocusChangeListener { _, hasFocus -> applyState(hasFocus) }
						applyState(isFocused)
						setOnClickListener {
							onSelect(index)
							self.dismiss()
						}
					}
					rowViews.add(row)

					groupLayout.addView(
						row,
						LinearLayout.LayoutParams(
							cellWidth,
							ViewGroup.LayoutParams.WRAP_CONTENT
						).apply {
							leftMargin = dp(4)
							rightMargin = dp(4)
							topMargin = dp(3)
							bottomMargin = dp(3)
						}
					)
				}

				if (grid) container.addView(groupLayout)
			}

			// KFIN: clamp the height to the free space above the panel
			container.measure(
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
			)
			val anchorTopOnScreen = IntArray(2).also { anchor.getLocationOnScreen(it) }[1]
			val maxAbove = (anchorTopOnScreen - dp(24)).coerceAtLeast(dp(120))
			val contentHeight = container.measuredHeight
			val needsScroll = !grid && contentHeight > maxAbove

			val content: View
			val popupHeight: Int
			if (needsScroll) {
				val scroll = android.widget.ScrollView(context).apply {
					isVerticalScrollBarEnabled = false
					addView(
						container,
						// ScrollView children must carry FrameLayout.LayoutParams
						android.widget.FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT
						)
					)
				}
				content = scroll
				popupHeight = maxAbove
			} else {
				content = container
				popupHeight = contentHeight
			}

			val popup = PopupWindow(
				content,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				popupHeight,
				true
			).apply {
				setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
				elevation = 0f
				animationStyle = R.style.WindowAnimation_Fade
			}

			val holder = KfinMenuPopup(popup)
			self = holder
			popup.setOnDismissListener { onDismiss() }

			// KFIN: horizontal center of the SCREEN; bottom edge either at
			// button center + 50px or floating above the transport panel
			val anchorLeftOnScreen = IntArray(2).also { anchor.getLocationOnScreen(it) }[0]
			val rootWidth = anchor.rootView?.width ?: container.measuredWidth
			val xoff = rootWidth / 2 - anchorLeftOnScreen - container.measuredWidth / 2
			val anchorBottomOnScreen = anchorTopOnScreen + anchor.height
			val yoff = if (abovePanelGap != null) {
				val panelTop = findTransportRowTop(anchor)
				if (panelTop != null) {
					// menu bottom = panel top - gap
					(panelTop - popupHeight - abovePanelGap) - anchorBottomOnScreen
				} else {
					-popupHeight - anchor.height / 2 + dp(50)
				}
			} else {
				-popupHeight - anchor.height / 2 + dp(50)
			}
			popup.showAsDropDown(anchor, xoff, yoff)

			// KFIN: simple fade
			popup.contentView.post {
				val anim = android.view.animation.AlphaAnimation(0f, 1f)
				anim.duration = 150
				anim.interpolator = android.view.animation.DecelerateInterpolator()
				popup.contentView.startAnimation(anim)
			}

			// focus the checked (or first) row once the window is ready
			container.post {
				checkedIndex.coerceIn(0, rowViews.size - 1)
					.takeIf { rowViews.isNotEmpty() }
					?.let { rowViews[it] }?.requestFocus()
			}

			return holder
		}

		private fun findTransportRowTop(anchor: View): Int? {
			var parent: android.view.ViewParent? = anchor.parent
			while (parent is View) {
				if (parent.javaClass.name == "androidx.leanback.widget.PlaybackTransportRowView") {
					return IntArray(2).also { parent.getLocationOnScreen(it) }[1]
				}
				parent = parent.parent
			}
			return null
		}
	}
}
