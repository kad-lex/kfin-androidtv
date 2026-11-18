package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.Text
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Badge
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemCardBaseItemOverlay
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.composable.item.rememberPlayedPercentage
import org.jellyfin.androidtv.ui.shared.extractAccentColor
import org.jellyfin.androidtv.ui.shared.loadSmallPosterBitmap
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseItemPersonBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.sdk.compat.canResume
import org.jellyfin.sdk.model.api.ImageType as SdkImageType
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject

class CardPresenter @JvmOverloads constructor(
	val showInfo: Boolean,
	val imageType: ImageType,
	val staticHeight: Int,
	val uniformAspect: Boolean,
	// KFIN: only the home "continue watching" row renders resumable movies
	// as wide 16:9 backdrop cards; everywhere else they stay 2:3 posters
	val resumeWide: Boolean = false,
) : Presenter() {
	constructor(showInfo: Boolean, imageType: ImageType, staticHeight: Int) : this(showInfo, imageType, staticHeight, false)
	constructor(showInfo: Boolean, staticHeight: Int) : this(showInfo, ImageType.POSTER, staticHeight)
	constructor(showInfo: Boolean) : this(showInfo, 150)
	constructor() : this(true)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true

			setOnLongClickListener {
				context.getActivity()?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) ?: false
			}
		}

		return CardViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CardViewHolder) return
		if (item !is BaseRowItem) return

		viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder !is CardViewHolder) return

		viewHolder.unbind()
	}

	private inner class CardViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
		private val _item = MutableStateFlow<BaseRowItem?>(null)
		private val _focused = MutableStateFlow(false)

		init {
			composeView.setContent {
				val item by _item.collectAsState()
				val focused by _focused.collectAsState()

				CardViewHolderContent(
					item = item,
					focused = focused,
					showInfo = showInfo,
					imageType = imageType,
					staticHeight = staticHeight,
					uniformAspect = uniformAspect,
					resumeWide = resumeWide,
				)
			}

			_focused.value = view.isFocused
			composeView.onFocusChangeListener = { _, focused -> _focused.value = focused }

			// KFIN: vertical moves between rows always land on the leftmost card;
			// library grids are excluded (KfinNav checks the parent chain)
			composeView.setOnKeyListener { v, keyCode, event ->
				if (event?.action != android.view.KeyEvent.ACTION_DOWN) return@setOnKeyListener false
				when (keyCode) {
					android.view.KeyEvent.KEYCODE_DPAD_UP ->
						org.jellyfin.androidtv.ui.shared.KfinNav.moveVertical(v, false)
					android.view.KeyEvent.KEYCODE_DPAD_DOWN ->
						org.jellyfin.androidtv.ui.shared.KfinNav.moveVertical(v, true)
					else -> false
				}
			}
		}

		fun bind(item: BaseRowItem) {
			_item.value = item
			_focused.value = view.isFocused
		}

		fun unbind() {
			_item.value = null
			_focused.value = false
		}
	}
}

private data class BaseRowItemDisplayConfig(
	val image: JellyfinImage?,
	val iconRes: Int,
	val aspectRatio: Float,
	val overrideShowInfo: Boolean? = null,
	val scaleType: ImageView.ScaleType? = null,
)

private fun BaseRowItem.getDisplayConfig(imageType: ImageType, uniformAspect: Boolean, resumeWide: Boolean = false): BaseRowItemDisplayConfig = when (baseRowType) {
	BaseRowType.BaseItem -> {
		val preferSeriesPoster = this is BaseItemDtoBaseRowItem && preferSeriesPoster
		val primaryAspectRatio = baseItem?.primaryImageAspectRatio?.toFloat()
		val defaultAspectRatio = when {
			preferParentThumb && (baseItem?.parentThumbItemId != null || baseItem?.seriesThumbImageTag != null) -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			baseItem?.type == BaseItemKind.EPISODE && primaryAspectRatio != null -> primaryAspectRatio
			baseItem?.type == BaseItemKind.EPISODE && (baseItem.parentThumbItemId != null || baseItem.seriesThumbImageTag != null) -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			baseItem?.type == BaseItemKind.USER_VIEW -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> primaryAspectRatio ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		}

		val base = BaseRowItemDisplayConfig(
			aspectRatio = when (imageType) {
				ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
				ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
				// KFIN: the browse grid passes uniformAspect=true; force one poster
				// shape for all items so leanback lays the grid out uniformly
				else -> if (uniformAspect) ImageHelper.ASPECT_RATIO_2_3.toFloat() else defaultAspectRatio
			},
			image = getImage(imageType),
			iconRes = R.drawable.ic_clapperboard,
		)

		when (baseItem?.type) {
			BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM -> base.copy(
				iconRes = R.drawable.ic_music_album,
				aspectRatio = if (uniformAspect || base.aspectRatio < 0.8f) 1f else base.aspectRatio,
			)

			BaseItemKind.PERSON,
			BaseItemKind.MUSIC_ARTIST -> base.copy(
				iconRes = R.drawable.ic_user,
				aspectRatio = if (uniformAspect || base.aspectRatio < 0.8f) 1f else base.aspectRatio,
			)

			BaseItemKind.SEASON, BaseItemKind.SERIES -> base.copy(
				aspectRatio = if (imageType == ImageType.POSTER) ImageHelper.ASPECT_RATIO_2_3.toFloat() else base.aspectRatio,
				iconRes = R.drawable.ic_tv
			)

			BaseItemKind.EPISODE -> when (preferSeriesPoster) {
				true -> base.copy(
					aspectRatio = ImageHelper.ASPECT_RATIO_2_3.toFloat(),
					iconRes = R.drawable.ic_tv
				)

				false -> base.copy(
					aspectRatio = if (uniformAspect && imageType == ImageType.POSTER) ImageHelper.ASPECT_RATIO_2_3.toFloat() else ImageHelper.ASPECT_RATIO_16_9.toFloat(),
					iconRes = R.drawable.ic_tv,
					overrideShowInfo = true,
				)
			}

			BaseItemKind.COLLECTION_FOLDER, BaseItemKind.USER_VIEW -> base.copy(
				aspectRatio = if (uniformAspect && imageType == ImageType.POSTER) ImageHelper.ASPECT_RATIO_2_3.toFloat() else ImageHelper.ASPECT_RATIO_16_9.toFloat(),
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.FOLDER, BaseItemKind.GENRE, BaseItemKind.MUSIC_GENRE -> base.copy(
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.PHOTO -> base.copy(
				iconRes = R.drawable.ic_photo
			)

			BaseItemKind.PHOTO_ALBUM, BaseItemKind.PLAYLIST -> base.copy(
				iconRes = R.drawable.ic_folder
			)

			BaseItemKind.MOVIE, BaseItemKind.VIDEO -> {
				// KFIN: resumable items (continue watching) use a 16:9 backdrop card
				val resuming = baseItem?.canResume == true && resumeWide
				base.copy(
					aspectRatio = when (imageType) {
						ImageType.POSTER -> if (resuming) ImageHelper.ASPECT_RATIO_16_9.toFloat() else ImageHelper.ASPECT_RATIO_2_3.toFloat()
						else -> base.aspectRatio
					},
					image = if (resuming) {
						baseItem?.itemImages[SdkImageType.BACKDROP]
							?: baseItem?.itemImages[SdkImageType.THUMB]
							?: base.image
					} else base.image,
					iconRes = R.drawable.ic_clapperboard,
				)
			}

			else -> base
		}
	}

	BaseRowType.LiveTvChannel -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: 1f
		},
		image = getImage(imageType),
		scaleType = ImageView.ScaleType.FIT_CENTER,
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.LiveTvProgram -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
		overrideShowInfo = true,
	)

	BaseRowType.LiveTvRecording -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.SeriesTimer -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		iconRes = R.drawable.ic_tv_timer,
		image = getImage(imageType),
		overrideShowInfo = true,
	)

	BaseRowType.Person -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_user,
	)

	BaseRowType.Chapter -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)

	BaseRowType.GridButton -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)
}

@Composable
@Stable
private fun CardViewHolderContent(
	item: BaseRowItem?,
	focused: Boolean,
	showInfo: Boolean,
	imageType: ImageType,
	staticHeight: Int,
	uniformAspect: Boolean,
	resumeWide: Boolean = false,
) {
	val context = LocalContext.current
	val localDensity = LocalDensity.current

	val title = remember(item, context) { item?.getCardName(context) }
	val displayConfig = remember(item, imageType, uniformAspect, resumeWide) { item?.getDisplayConfig(imageType, uniformAspect, resumeWide) }

	// KFIN: in the browse grid every card must occupy exactly the same cell size,
	// otherwise leanback lays children out with overlaps and focus search jumps
	// between cards. Never let the view collapse while a holder is being recycled.
	val uniformSize = DpSize(staticHeight.dp * ImageHelper.ASPECT_RATIO_2_3.toFloat(), staticHeight.dp)
	if (item == null || displayConfig == null) {
		if (uniformAspect) Box(modifier = Modifier.size(uniformSize))
		return
	}

	val image = displayConfig.image
	val aspectRatio = displayConfig.aspectRatio.takeIf { it >= 0.1f }
		?: image?.aspectRatio?.takeIf { it >= 0.1f } ?: 1f

	val size = when {
		uniformAspect -> uniformSize
		item.staticHeight -> DpSize(staticHeight.dp * aspectRatio, staticHeight.dp)
		aspectRatio > 1f -> DpSize(130.dp * aspectRatio, 130.dp)
		else -> DpSize(150.dp * aspectRatio, 150.dp)
	}

	val usePreview = displayConfig.overrideShowInfo ?: showInfo

	// KFIN: colors extracted from the poster: accent (lower half) tints the
	// focus border, the bottom-strip color fills the caption lip
	val api = koinInject<ApiClient>()
	val glowUrl = remember(image, api) {
		image?.getUrl(api, maxWidth = 96, maxHeight = 96)
	}
	// KFIN: one accent color (lower half of the poster) drives both the
	// focus border and the bottom lip
	var cardAccent by remember(glowUrl) { mutableStateOf<Color?>(null) }
	LaunchedEffect(glowUrl) {
		val small = loadSmallPosterBitmap(context, glowUrl) ?: return@LaunchedEffect
		cardAccent = extractAccentColor(small.asImageBitmap())
	}
	// KFIN: the library gradient follows the focused card; publish its accent
	// whenever focus lands on it (extraction may have finished long ago)
	LaunchedEffect(focused, cardAccent) {
		if (focused) cardAccent?.let { org.jellyfin.androidtv.ui.shared.KfinAccentHolder.color.value = it }
	}

	val card = @Composable {
		ItemCard(
			focused = focused,
			accentColor = cardAccent,
			showPlayBubble = item.baseRowType != BaseRowType.Person && item.baseItem?.type != BaseItemKind.SEASON,
			// KFIN: the lip only exists where cards have captions (home rows) -
			// library grids have no captions and no lip
			image = {
				if (image != null) {
					AsyncImage(
						url = image.getUrl(
							api,
							maxWidth = with(localDensity) { size.width.roundToPx() },
							maxHeight = with(localDensity) { size.height.roundToPx() },
						),
						blurHash = image.blurHash,
						aspectRatio = aspectRatio,
						scaleType = displayConfig.scaleType ?: ImageView.ScaleType.CENTER_CROP,
						modifier = Modifier
							.fillMaxSize()
					)
				} else if (item is GridButtonBaseRowItem && item.gridButton.imageRes != null) {
					Image(
						painter = painterResource(item.gridButton.imageRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize()
					)
				} else {
					Image(
						painter = painterResource(displayConfig.iconRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize(0.4f)
							.align(Alignment.Center)
					)
				}
			},
			overlay = {
				val base = item.baseItem
				when {
					// KFIN: wide (16:9) cards and season posters carry their title as a badge
					base != null -> {
						val isWide = kotlin.math.abs(aspectRatio - ImageHelper.ASPECT_RATIO_16_9.toFloat()) < 0.2f
						val isSeason = base.type == BaseItemKind.SEASON
						ItemCardBaseItemOverlay(
							item = base,
							footer = null,
							titleBadge = title?.takeIf { isWide || isSeason },
						)
					}
					// KFIN: person cards show the name as a bottom badge
					item is BaseItemPersonBaseRowItem && title != null -> {
						Badge(
							modifier = Modifier
								.align(Alignment.BottomStart)
								.padding(horizontal = 6.dp, vertical = 6.dp),
							minSize = 19.dp,
							contentPadding = 2.dp,
							containerColor = Color.Black, // KFIN: unified opaque black
							contentColor = Color.White,
							textStyle = JellyfinTheme.typography.badge.copy(fontSize = 10.sp),
						) {
							Text(text = title.uppercase(), maxLines = 1)
						}
					}
				}
			},
			posterHeight = size.height,
			modifier = Modifier
				.width(size.width)
		)
	}

	if (usePreview) {
		val focusModifier = if (focused) Modifier.basicMarquee(
			iterations = Int.MAX_VALUE,
			initialDelayMillis = 0,
		) else Modifier

		// KFIN: title/subtitle moved into the card's bottom lip
		ItemPreview(
			card = { card() },
			title = null,
			subtitle = null,
		)
	} else {
		card()
	}
}
