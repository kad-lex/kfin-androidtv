package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.View
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import org.jellyfin.androidtv.ui.playback.setSubtitleIndex
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

class ClosedCaptionsAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private var popup: KfinMenuPopup? = null

	init {
		initializeWithIcon(R.drawable.ic_select_subtitle)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		if (playbackController.currentStreamInfo == null) {
			Timber.w("StreamInfo null trying to obtain subtitles")
			Toast.makeText(context, "Unable to obtain subtitle info", Toast.LENGTH_LONG).show()
			return
		}

		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		removePopup()

		val ids = mutableListOf<Int>()
		val labels = mutableListOf<String>()
		ids.add(-1)
		labels.add(context.getString(R.string.lbl_none))
		for (sub in playbackController.currentMediaSource.mediaStreams.orEmpty()) {
			if (sub.type != MediaStreamType.SUBTITLE) continue
			ids.add(sub.index)
			labels.add(sub.displayTitle ?: "")
		}

		popup = KfinMenuPopup.show(
			context = context,
			anchor = view,
			items = labels,
			checkedIndex = ids.indexOf(playbackController.subtitleStreamIndex),
			onSelect = { position -> playbackController.setSubtitleIndex(ids[position]) },
			onDismiss = {
				videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
				popup = null
			},
			// KFIN: same placement as the audio menu - centered, 10px above
			// the transport panel; many tracks become a 3-column grid
			columns = if (labels.size > 6) 3 else 1,
			abovePanelGap = 10
		)
	}

	fun removePopup() {
		popup?.dismiss()
	}
}
