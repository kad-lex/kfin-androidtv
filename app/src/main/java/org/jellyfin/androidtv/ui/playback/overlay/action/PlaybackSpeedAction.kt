package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.SpeedSteps.entries.toTypedArray()
	private var popup: KfinMenuPopup? = null

	init {
		initializeWithIcon(R.drawable.ic_playback_speed)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = KfinMenuPopup.show(
			context = context,
			anchor = view,
			items = speeds.map { String.format(Locale.US, "%.2fx", it.speed) },
			checkedIndex = speeds.indexOf(speedController.currentSpeed),
			onSelect = { position -> speedController.currentSpeed = speeds[position] },
			onDismiss = {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			},
			// KFIN: all speeds in a single row, 10px above the transport panel
			columns = 8,
			abovePanelGap = 10
		)
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
