package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.constant.ZoomMode
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class ZoomAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: KfinMenuPopup? = null

	init {
		initializeWithIcon(R.drawable.ic_aspect_ratio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()

		val modes = listOf(ZoomMode.FIT, ZoomMode.AUTO_CROP, ZoomMode.STRETCH)
		val labels = listOf(
			context.getString(R.string.lbl_fit),
			context.getString(R.string.lbl_auto_crop),
			context.getString(R.string.lbl_stretch),
		)

		popup = KfinMenuPopup.show(
			context = context,
			anchor = view,
			items = labels,
			checkedIndex = modes.indexOf(playbackController.zoomMode),
			onSelect = { position -> playbackController.setZoom(modes[position]) },
			onDismiss = {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			},
			// KFIN: one row, 10px above the transport panel
			columns = 3,
			abovePanelGap = 10
		)
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
