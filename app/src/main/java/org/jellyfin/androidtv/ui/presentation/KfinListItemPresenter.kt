package org.jellyfin.androidtv.ui.presentation

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.ui.AsyncImageView
import org.jellyfin.androidtv.ui.itemhandling.BaseItemPersonBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.java.KoinJavaComponent

// KFIN: vertical list rows - thumbnail left, title + description right.
// Episodes: name + overview; seasons: name + overview; people: role + real name
class KfinListItemPresenter : Presenter() {
	class ViewHolder(view: View) : Presenter.ViewHolder(view) {
		val title: TextView = ((view as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0) as TextView
		val description: TextView = ((view as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(1) as TextView
		val image: AsyncImageView = (view as LinearLayout).getChildAt(1) as AsyncImageView
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val context = parent.context
		val density = context.resources.displayMetrics.density
		fun dp(value: Int) = (value * density).toInt()

		val image = AsyncImageView(context)
		val title = TextView(context).apply {
			textSize = 16f
			setTypeface(typeface, android.graphics.Typeface.BOLD)
			setTextColor(0xFFFFFFFF.toInt())
			maxLines = 1
		}
		val description = TextView(context).apply {
			textSize = 13f
			setTextColor(0xB3FFFFFF.toInt())
			// KFIN: the pill is fixed to the description block's width - the text
			// can never stretch it, overflow ends with "..."
			maxLines = 2
			ellipsize = android.text.TextUtils.TruncateAt.END
			val screenW = context.resources.displayMetrics.widthPixels
			maxWidth = screenW - ((253 + 16 + 160 + 12) * context.resources.displayMetrics.density).toInt()
		}
		val texts = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(12), 0, 0, 0)
			addView(title)
			addView(description)
		}
		// KFIN: rounded poster corners
		image.clipToOutline = true
		image.outlineProvider = object : android.view.ViewOutlineProvider() {
			override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
				outline.setRoundRect(0, 0, view.width, view.height, 16 * density)
			}
		}
image.post { image.invalidateOutline() }
		val root = LinearLayout(context).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(dp(8), dp(6), dp(8), dp(6))
			isFocusable = true
			// KFIN: leanback ignores layoutParams - force the full row width
			minimumWidth = parent.width
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
			)
			// KFIN: title + description on the left, poster at the right edge
			addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
			addView(image, LinearLayout.LayoutParams(dp(160), dp(90)))
		}
		return ViewHolder(root)
	}

	override fun onBindViewHolder(holder: Presenter.ViewHolder, item: Any?) {
		val vh = holder as ViewHolder
		val rowItem = item as BaseRowItem
		val api = KoinJavaComponent.get<ApiClient>(ApiClient::class.java)
		val density = vh.view.resources.displayMetrics.density

		when (item) {
			is BaseItemPersonBaseRowItem -> {
				vh.title.text = item.person.role ?: item.person.name
				vh.description.text = item.person.name
				// KFIN: fixed photo slot, the image is cropped to fill it
				vh.image.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
				vh.image.load(item.person.primaryImage?.getUrl(api, maxWidth = 160), null, null, 0.67, 0)
				vh.image.layoutParams.width = (60 * density).toInt()
			}
			else -> {
				vh.title.text = rowItem.getName(vh.view.context)
				val overview = rowItem.getSummary(vh.view.context).orEmpty()
				vh.description.text = if (overview.isNotEmpty()) overview
					else rowItem.baseItem?.childCount?.let { "$it серий" }.orEmpty()
				// KFIN: seasons show a portrait poster (60x90), episodes a
				// landscape still (160x90) - both fill the slot (CENTER_CROP) so
				// the view's rounded outline actually clips the image corners
				val isSeason = rowItem.baseItem?.type == org.jellyfin.sdk.model.api.BaseItemKind.SEASON
				vh.image.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
				vh.image.load(rowItem.getImage(org.jellyfin.androidtv.constant.ImageType.POSTER)?.getUrl(api, maxWidth = 320), null, null, if (isSeason) 0.67 else 1.78, 0)
				vh.image.layoutParams.width = ((if (isSeason) 60 else 160) * density).toInt()
			}
		}
		vh.image.layoutParams.height = (90 * density).toInt()
		vh.image.requestLayout()
		vh.image.invalidateOutline()
		// KFIN: leanback overwrites the row's params - force the width of the
		// description block (screen minus the 28dp left + 225dp right guides),
		// which also ends exactly at the actors grid's right edge
		val widthPx = vh.view.resources.displayMetrics.widthPixels
		vh.view.minimumWidth = widthPx - (253 * density).toInt()
		vh.view.requestLayout()

		// KFIN: gray pill with the soft menu outline; white fill + dark text on focus
		val bg = android.graphics.drawable.GradientDrawable().apply {
			cornerRadius = 12 * density
			setColor(0xFF000000.toInt())
			setStroke((1 * density).toInt(), 0x24FFFFFF)
		}
		vh.view.background = bg
		vh.view.setOnFocusChangeListener { _, focused ->
			bg.setColor(if (focused) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
			val content = if (focused) 0xE6000000.toInt() else 0xFFFFFFFF.toInt()
			vh.title.setTextColor(content)
			vh.description.setTextColor(if (focused) 0xE6000000.toInt() else 0xB3FFFFFF.toInt())
		}
	}

	override fun onUnbindViewHolder(holder: Presenter.ViewHolder) = Unit
}
