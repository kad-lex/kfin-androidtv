package org.jellyfin.androidtv.ui.itemdetail

import android.content.ActivityNotFoundException
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.ExternalAppRepository
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.VideoQueueManager
import org.jellyfin.androidtv.util.TimeUtils
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.androidtv.util.apiclient.getSeriesOverview
import org.jellyfin.androidtv.util.popupMenu
import org.jellyfin.androidtv.util.sdk.TrailerUtils.getExternalTrailerIntent
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.androidtv.util.sdk.compat.copyWithUserData
import org.jellyfin.androidtv.util.showIfNotEmpty
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.liveTvApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.jellyfin.sdk.model.extensions.ticks
import org.jellyfin.sdk.model.serializer.toUUID
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun FullDetailsFragment.deleteItem(
	api: ApiClient,
	item: BaseItemDto,
	dataRefreshService: DataRefreshService,
	navigationRepository: NavigationRepository,
) = lifecycleScope.launch {
	Timber.i("Deleting item ${item.name} (id=${item.id})")

	try {
		withContext(Dispatchers.IO) {
			api.libraryApi.deleteItem(item.id)
		}
	} catch (error: ApiClientException) {
		Timber.e(error, "Failed to delete item ${item.name} (id=${item.id})")
		Toast.makeText(
			context,
			getString(R.string.item_deletion_failed, item.name),
			Toast.LENGTH_LONG
		).show()
		return@launch
	}

	dataRefreshService.lastDeletedItemId = item.id

	if (navigationRepository.canGoBack) navigationRepository.goBack()
	else navigationRepository.navigate(Destinations.home)

	Toast.makeText(context, getString(R.string.item_deleted, item.name), Toast.LENGTH_LONG).show()
}

fun FullDetailsFragment.showDetailsMenu(
	view: View,
	baseItemDto: BaseItemDto,
) = popupMenu(requireContext(), view) {
	// for each button check if it exists (not-null) and is invisible (overflow prevention)
	if (queueButton?.isVisible == false) {
		item(getString(R.string.lbl_add_to_queue)) { addItemToQueue() }
	}

	if (shuffleButton?.isVisible == false) {
		item(getString(R.string.lbl_shuffle_all)) { shufflePlay() }
	}

	if (trailerButton?.isVisible == false) {
		item(getString(R.string.lbl_play_trailers)) { playTrailers() }
	}

	if (favButton?.isVisible == false) {
		val favoriteStringRes = when (baseItemDto.userData?.isFavorite) {
			true -> R.string.lbl_remove_favorite
			else -> R.string.lbl_add_favorite
		}

		item(getString(favoriteStringRes)) { toggleFavorite() }
	}

	if (goToSeriesButton?.isVisible == false) {
		item(getString(R.string.lbl_goto_series)) { gotoSeries() }
	}
}.showIfNotEmpty()

fun FullDetailsFragment.createFakeSeriesTimerBaseItemDto(timer: SeriesTimerInfoDto) = BaseItemDto(
	id = requireNotNull(timer.id).toUUID(),
	type = BaseItemKind.FOLDER,
	mediaType = MediaType.UNKNOWN,
	seriesTimerId = timer.id,
	name = timer.name,
	overview = timer.getSeriesOverview(requireContext()),
)

fun FullDetailsFragment.toggleFavorite() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setFavorite(
			item = mBaseItem.id,
			favorite = !(mBaseItem.userData?.isFavorite ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		favButton.isActivated = userData.isFavorite
		dataRefreshService.lastFavoriteUpdate = Instant.now()
	}
}

fun FullDetailsFragment.togglePlayed() {
	val itemMutationRepository by inject<ItemMutationRepository>()
	val dataRefreshService by inject<DataRefreshService>()

	lifecycleScope.launch {
		val userData = itemMutationRepository.setPlayed(
			item = mBaseItem.id,
			played = !(mBaseItem.userData?.played ?: false)
		)
		mBaseItem = mBaseItem.copyWithUserData(userData)
		mWatchedToggleButton.isActivated = userData.played

		// Adjust resume
		mResumeButton?.apply {
			isVisible = mBaseItem.canResume
		}

		// Force lists to re-fetch
		dataRefreshService.lastPlayback = Instant.now()
		when (mBaseItem.type) {
			BaseItemKind.MOVIE -> dataRefreshService.lastMoviePlayback = Instant.now()
			BaseItemKind.EPISODE -> dataRefreshService.lastTvPlayback = Instant.now()
			else -> Unit
		}

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.playTrailers() {
	val localTrailerCount = mBaseItem.localTrailerCount ?: 0

	// External trailer
	if (localTrailerCount < 1) try {
		val intent = getExternalTrailerIntent(requireContext(), mBaseItem)
		if (intent != null) startActivity(intent)
	} catch (exception: ActivityNotFoundException) {
		Timber.w(exception, "Unable to open external trailer")
		Toast.makeText(
			requireContext(),
			getString(R.string.no_player_message),
			Toast.LENGTH_LONG
		).show()
	} else lifecycleScope.launch {
		val api by inject<ApiClient>()

		try {
			val trailers = withContext(Dispatchers.IO) {
				api.userLibraryApi.getLocalTrailers(mBaseItem.id).content
			}
			play(trailers, 0, false)
		} catch (exception: ApiClientException) {
			Timber.e(exception, "Error retrieving trailers for playback")
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		}
	}
}

fun FullDetailsFragment.getItem(id: UUID, callback: (item: BaseItemDto?) -> Unit) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val response = try {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(id).content
			}
		} catch (err: ApiClientException) {
			Timber.w(err, "Failed to get item $id")
			null
		}

		callback(response)
	}
}

fun FullDetailsFragment.populatePreviousButton() {
	if (mBaseItem.type != BaseItemKind.EPISODE) return

	val api by inject<ApiClient>()

	lifecycleScope.launch {
		val siblings = withContext(Dispatchers.IO) {
			api.tvShowsApi.getEpisodes(
				seriesId = requireNotNull(mBaseItem.seriesId),
				adjacentTo = mBaseItem.id,
			).content
		}

		val currentIndex = siblings.items.indexOfFirst { it.id == mBaseItem.id }
		val previousItem = siblings.items.getOrNull(currentIndex - 1)?.id

		mPrevItemId = previousItem
		mPrevButton.isVisible = previousItem != null

		showMoreButtonIfNeeded()
	}
}

fun FullDetailsFragment.getNextUpEpisode(callback: (BaseItemDto?) -> Unit) {
	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		callback(nextUpEpisode)
	}
}

suspend fun FullDetailsFragment.getNextUpEpisode(): BaseItemDto? {
	val api by inject<ApiClient>()

	try {
		val episodes = withContext(Dispatchers.IO) {
			api.tvShowsApi.getNextUp(
				seriesId = mBaseItem.seriesId ?: mBaseItem.id,
				fields = ItemRepository.itemFields,
				limit = 1,
			).content
		}
		return episodes.items.firstOrNull()
	} catch (err: ApiClientException) {
		Timber.w(err, "Failed to get next up items")
		return null
	}
}

fun FullDetailsFragment.chooseExternalPlayer() {
	val externalAppRepository = KoinJavaComponent.get<ExternalAppRepository>(ExternalAppRepository::class.java)
	val apps = externalAppRepository.getExternalPlayerApps(requireContext())

	if (apps.isEmpty()) {
		Toast.makeText(context, getString(R.string.lbl_choose_external_player), Toast.LENGTH_LONG).show()
		return
	}

	// KFIN: styled like the settings/sort menus - glass capsule, focus outline,
	// crossfade animation
	val context = requireContext()
	val packageManager = context.packageManager
	val currentComponent = externalAppRepository.let { repo ->
		org.koin.java.KoinJavaComponent.get<org.jellyfin.androidtv.preference.UserPreferences>(
			org.jellyfin.androidtv.preference.UserPreferences::class.java
		)[org.jellyfin.androidtv.preference.UserPreferences.externalPlayerComponentName]
	}

	val density = context.resources.displayMetrics.density
	fun dp(v: Int) = (v * density).toInt()

	val container = android.widget.LinearLayout(context).apply {
		orientation = android.widget.LinearLayout.VERTICAL
		setPadding(dp(8), dp(8), dp(8), dp(8))
		val capsule = android.graphics.drawable.GradientDrawable().apply {
			shape = android.graphics.drawable.GradientDrawable.RECTANGLE
			setColor(0xCC0B0D14.toInt())
			cornerRadius = dp(24).toFloat()
			setStroke(dp(1), 0x1AFFFFFF.toInt())
		}
		background = capsule
	}

	var checkedView: android.widget.TextView? = null
	val popupWindow = android.widget.PopupWindow(
		container,
		android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
		android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
		true
	)

	apps.forEach { resolveInfo ->
		val isCurrent = android.content.ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name).flattenToShortString() == currentComponent
		val item = android.widget.TextView(context).apply {
			text = (if (isCurrent) "\u2713  " else "") + resolveInfo.loadLabel(packageManager).toString().uppercase()
			setTextColor(android.graphics.Color.WHITE)
			textSize = 14f
			setAllCaps(false)
			setPadding(dp(16), dp(10), dp(16), dp(10))
			isFocusable = true
			isFocusableInTouchMode = true

			val itemBg = android.graphics.drawable.GradientDrawable().apply {
				shape = android.graphics.drawable.GradientDrawable.RECTANGLE
				cornerRadius = dp(40).toFloat()
				setColor(0x00000000)
			}
			background = itemBg
			setOnFocusChangeListener { _, hasFocus ->
				// KFIN: selection - white pill slightly larger than the item
				itemBg.setColor(if (hasFocus) 0xF2FFFFFF.toInt() else 0x00000000)
				itemBg.setStroke(if (hasFocus) dp(1) else 0, if (hasFocus) 0xB3F8FAFC.toInt() else 0x00000000)
				setTextColor(if (hasFocus) 0xF2000000.toInt() else android.graphics.Color.WHITE)
				val lp = layoutParams as? android.widget.LinearLayout.LayoutParams
				if (lp != null) {
					val grow = if (hasFocus) dp(3) else 0
					lp.leftMargin = grow
					lp.rightMargin = grow
					lp.topMargin = grow / 2
					lp.bottomMargin = grow / 2
					layoutParams = lp
				}
			}
			setOnClickListener {
				externalAppRepository.setExternalPlayerapp(resolveInfo.activityInfo)
				playInExternalPlayer()
				popupWindow.dismiss()
			}
		}
		container.addView(item)
		if (isCurrent) checkedView = item
	}

	popupWindow.isOutsideTouchable = true
	popupWindow.animationStyle = org.jellyfin.androidtv.R.style.WindowAnimation_Fade
	// KFIN: appears in place, below the status bar, like the other menus
	popupWindow.showAtLocation(requireView(), android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL, 0, dp(76))
	checkedView?.requestFocus()
}

fun FullDetailsFragment.playInExternalPlayer() {
	val baseItem = mBaseItem ?: return
	val videoQueueManager = KoinJavaComponent.get<VideoQueueManager>(VideoQueueManager::class.java)

	playbackHelper.value.getItemsToPlay(
		requireContext(),
		baseItem,
		baseItem.type == BaseItemKind.MOVIE,
		false,
		object : Response<List<BaseItemDto>>(lifecycle) {
			override fun onResponse(response: List<BaseItemDto>) {
				if (!isActive) return
				if (response.isEmpty()) {
					Timber.e("No items to play - ignoring external play request.")
					return
				}

				videoQueueManager.setCurrentVideoQueue(response)
				videoQueueManager.setCurrentMediaPosition(0)
				startActivity(ActivityDestinations.externalPlayer(requireContext()))
			}
		}
	)
}

fun FullDetailsFragment.resumePlayback(v: View) {
	if (mBaseItem.type != BaseItemKind.SERIES) {
		val pos = (mBaseItem.userData?.playbackPositionTicks?.ticks
			?: Duration.ZERO) - resumePreroll.milliseconds
		play(mBaseItem, pos.inWholeMilliseconds.toInt(), false)
		return
	}

	lifecycleScope.launch {
		val nextUpEpisode = getNextUpEpisode()
		if (nextUpEpisode == null) {
			Toast.makeText(
				requireContext(),
				getString(R.string.msg_video_playback_error),
				Toast.LENGTH_LONG
			).show()
		} else if (nextUpEpisode.userData?.playbackPositionTicks == 0L) {
			play(nextUpEpisode, 0, false)
		} else {
			showResumeMenu(v, nextUpEpisode)
		}
	}
}

fun FullDetailsFragment.showResumeMenu(
	view: View,
	nextUpEpisode: BaseItemDto
) = popupMenu(requireContext(), view) {
	val pos = (nextUpEpisode.userData?.playbackPositionTicks?.ticks
		?: Duration.ZERO) - resumePreroll.milliseconds
	item(
		getString(
			R.string.lbl_resume_from,
			TimeUtils.formatMillis(pos.inWholeMilliseconds)
		)
	) {
		play(nextUpEpisode, pos.inWholeMilliseconds.toInt(), false)
	}
	item(getString(R.string.lbl_from_beginning)) {
		play(nextUpEpisode, 0, false)
	}
}.showIfNotEmpty()

fun FullDetailsFragment.getLiveTvSeriesTimer(
	id: String,
	callback: (timer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getSeriesTimer(id).content
			}
		}.onSuccess { timer ->
			callback(timer)
		}
	}
}

fun FullDetailsFragment.getLiveTvProgram(
	id: UUID,
	callback: (program: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getProgram(id.toString()).content
			}
		}.onSuccess { program ->
			callback(program)
		}
	}
}

fun FullDetailsFragment.createLiveTvSeriesTimer(
	seriesTimer: SeriesTimerInfoDto,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.createSeriesTimer(seriesTimer)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvDefaultTimer(
	id: UUID,
	callback: (seriesTimer: SeriesTimerInfoDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getDefaultTimer(id.toString()).content
			}
		}.onSuccess { seriesTimer ->
			callback(seriesTimer)
		}
	}
}

fun FullDetailsFragment.cancelLiveTvSeriesTimer(
	timerId: String,
	callback: () -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.cancelTimer(timerId)
			}
		}.onSuccess {
			callback()
		}
	}
}

fun FullDetailsFragment.getLiveTvChannel(
	id: UUID,
	callback: (channel: BaseItemDto) -> Unit,
) {
	val api by inject<ApiClient>()

	lifecycleScope.launch {
		runCatching {
			withContext(Dispatchers.IO) {
				api.liveTvApi.getChannel(id).content
			}
		}.onSuccess { channel ->
			callback(channel)
		}
	}
}
