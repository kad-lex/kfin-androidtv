package org.jellyfin.androidtv.ui.presentation

import android.graphics.Typeface
import android.widget.TextView
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter

class CustomRowHeaderPresenter : RowHeaderPresenter() {
	override fun onSelectLevelChanged(holder: ViewHolder) = Unit

	override fun onBindViewHolder(holder: Presenter.ViewHolder, item: Any?) {
		super.onBindViewHolder(holder, item)
		// KFIN: row titles are bold uppercase like the rest of the headers
		(holder.view as? TextView)?.let { view ->
			val family = view.typeface
			view.setTypeface(family ?: Typeface.DEFAULT, Typeface.BOLD)
			view.text = view.text?.toString()?.uppercase()
		}
		// KFIN: raised 5px from the previous position
		val density = holder.view.resources.displayMetrics.density
		holder.view.translationY = 12f * density - 9f
	}
}
