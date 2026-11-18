package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class SelectAudioAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: KfinMenuPopup? = null

	init {
		initializeWithIcon(R.drawable.ic_select_audio)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		val audioTracks = playbackController.currentStreamInfo?.selectableAudioStreams ?: return
		val currentAudioIndex = playbackController.audioStreamIndex

		dismissPopup()
		popup = KfinMenuPopup.show(
			context = context,
			anchor = view,
			items = audioTracks.map { it.displayTitle ?: "" },
			checkedIndex = audioTracks.indexOfFirst { it.index == currentAudioIndex },
			onSelect = { position -> playbackController.switchAudioStream(audioTracks[position].index) },
			onDismiss = {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			},
			// KFIN: many tracks become a 3-column grid; the menu floats 10px
			// above the transport panel
			columns = if (audioTracks.size > 6) 3 else 1,
			abovePanelGap = 10
		)
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
