package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import android.view.View
import androidx.leanback.app.RowsSupportFragment

open class AutoFocusRowsFragment : RowsSupportFragment() {
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// KFIN: rows span edge to edge; freed width becomes the gap between cards
		verticalGridView.setPadding(0, verticalGridView.paddingTop, 0, verticalGridView.paddingBottom)

		// KFIN: the container (EdgeFadeFrameLayout) erases content alpha at the edges

		verticalGridView.requestFocus()
	}
}
