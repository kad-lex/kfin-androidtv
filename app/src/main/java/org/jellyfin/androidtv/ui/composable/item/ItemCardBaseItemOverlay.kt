package org.jellyfin.androidtv.ui.composable.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.base.Badge
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Seekbar
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.design.Tokens
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.VideoRangeType
import org.koin.compose.koinInject

@Composable
@Stable
fun ItemCardBaseItemOverlay(
	item: BaseItemDto,
	footer: (@Composable () -> Unit)? = null,
) = Box(
	modifier = Modifier
		.fillMaxSize()
		.padding(Tokens.Space.spaceXs)
) {
	StateIndicator(
		item = item,
		modifier = Modifier.align(Alignment.TopStart),
	)

	Column(
		modifier = Modifier.align(Alignment.TopEnd),
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs),
		horizontalAlignment = Alignment.End
	) {
		WatchIndicator(item = item)

		QualityBadges(item = item)
	}

	Column(
		modifier = Modifier.align(Alignment.BottomCenter),
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		ProgressIndicator(
			item = item,
		)

		if (footer != null) footer()
	}
}

@Composable
@Stable
private fun StateIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val isFavorited = item.userData?.isFavorite == true
	val isRecording = item.timerId?.takeIf { item.type == BaseItemKind.LIVE_TV_PROGRAM || item.type == BaseItemKind.PROGRAM } != null
	val isRecordingActive = item.seriesTimerId != null && isRecording

	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)
	) {
		if (isRecording) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_record_series),
				contentDescription = null,
				tint = if (isRecordingActive) Tokens.Color.colorRed600 else Tokens.Color.colorGrey100,
				modifier = modifier
					.size(24.dp)
			)
		}

		if (isFavorited) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_heart),
				contentDescription = null,
				tint = Tokens.Color.colorRed500,
				modifier = modifier
					.size(24.dp)
			)
		}
	}
}

@Composable
@Stable
private fun WatchIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val userPreferences = koinInject<UserPreferences>()
	val watchedIndicatorBehavior = userPreferences[UserPreferences.watchedIndicatorBehavior]

	if (watchedIndicatorBehavior == WatchedIndicatorBehavior.NEVER) return
	if (watchedIndicatorBehavior == WatchedIndicatorBehavior.EPISODES_ONLY && item.type != BaseItemKind.EPISODE) return

	val isPlayed = item.userData?.played == true
	val unplayedItems = item.userData?.unplayedItemCount?.takeIf { it > 0 }

	if (isPlayed) {
		Badge(
			modifier = modifier
				.size(24.dp),
		) {
			Icon(
				imageVector = ImageVector.vectorResource(R.drawable.ic_watch),
				contentDescription = null,
				modifier = Modifier.size(12.dp)
			)
		}
	} else if (unplayedItems != null) {
		if (watchedIndicatorBehavior == WatchedIndicatorBehavior.HIDE_UNWATCHED) return

		Badge(
			modifier = modifier
				.sizeIn(minWidth = 24.dp, minHeight = 24.dp),
		) {
			Text(
				text = unplayedItems.toString(),
			)
		}
	}
}

@Composable
@Stable
private fun QualityBadges(item: BaseItemDto) {
	val videoStream = item.mediaStreams?.firstOrNull { it.type == MediaStreamType.VIDEO }
	val height = videoStream?.height
	val resolution = when {
		height == null -> null
		height >= 2160 -> "4K"
		height >= 1080 -> "1080p"
		height >= 720 -> "720p"
		else -> null
	}
	val isHdr = when (videoStream?.videoRangeType) {
		VideoRangeType.HDR10,
		VideoRangeType.HDR10_PLUS,
		VideoRangeType.HLG,
		VideoRangeType.DOVI,
		VideoRangeType.DOVI_WITH_HDR10,
		VideoRangeType.DOVI_WITH_HLG,
		VideoRangeType.DOVI_WITH_EL,
		VideoRangeType.DOVI_WITH_HDR10_PLUS,
		VideoRangeType.DOVI_WITH_ELHDR10_PLUS -> true

		else -> false
	}

	if (resolution == null && !isHdr) return

	Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.spaceXs)) {
		if (resolution != null) {
			Badge {
				Text(text = resolution)
			}
		}

		if (isHdr) {
			Badge {
				Text(text = "HDR")
			}
		}
	}
}

// Media types that need to show a user progress bar (based on playedPercentage in userData)
private val progressMediaTypes = setOf(MediaType.VIDEO, MediaType.BOOK)

@Composable
private fun ProgressIndicator(
	item: BaseItemDto,
	modifier: Modifier = Modifier,
) {
	val playbackManager = koinInject<PlaybackManager>()
	val playState by playbackManager.state.playState.collectAsState()
	val currentQueueEntry by rememberQueueEntry(playbackManager)

	val playedPercentage = when {
		playState == PlayState.PLAYING && currentQueueEntry?.baseItem?.id == item.id -> rememberPlayerProgress(playbackManager).value
		item.mediaType in progressMediaTypes -> item.userData?.playedPercentage?.toFloat()?.div(100f)?.coerceIn(0f, 1f)

		else -> null
	}

	if (playedPercentage != null) {
		Box(modifier = modifier.padding(Tokens.Space.spaceXs)) {
			Seekbar(
				progress = playedPercentage,
				enabled = false,
				modifier = Modifier
					.fillMaxWidth()
					.height(4.dp)
			)
		}
	}
}
