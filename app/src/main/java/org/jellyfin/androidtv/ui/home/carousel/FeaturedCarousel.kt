package org.jellyfin.androidtv.ui.home.carousel

import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.home.HomeFocusCoordinator
import org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.shared.innerGlow
import org.jellyfin.androidtv.ui.shared.extractAccentColorFromUrl
import org.jellyfin.androidtv.ui.shared.whiteGlow
import org.jellyfin.androidtv.util.coil.BackgroundBlurTransformation
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun FeaturedCarousel(
	modifier: Modifier = Modifier,
	backgroundModifier: Modifier = Modifier,
	foregroundModifier: Modifier = Modifier,
	blurBoundary: Dp,
	focusRequester: FocusRequester,
	onMoveDown: () -> Unit,
	content: @Composable () -> Unit = {},
) {
	val api = koinInject<ApiClient>()
	val navigationRepository = koinInject<NavigationRepository>()
	var items by remember { mutableStateOf(emptyList<FeaturedCarouselItem>()) }

	LaunchedEffect(api) {
		items = withContext(Dispatchers.IO) { loadFeaturedItems(api) }
	}
	if (items.isEmpty()) {
		Box(modifier = modifier) {
			Box(modifier = Modifier.zIndex(1f)) {
				content()
			}
		}
		return
	}

	var currentIndex by remember(items) { mutableIntStateOf(0) }

	// KFIN: the status bar recolors itself against the current backdrop
	val carouselContext = androidx.compose.ui.platform.LocalContext.current
	LaunchedEffect(items, currentIndex) {
		val item = items.getOrNull(currentIndex) ?: return@LaunchedEffect
		org.jellyfin.androidtv.ui.shared.KfinBarStyle.isLight.value =
			org.jellyfin.androidtv.ui.shared.isImageLightAtTop(carouselContext, item.imageUrl) ?: false
	}
	var isWatchNowFocused by remember { mutableStateOf(false) }

	// KFIN: no auto-focus - the carousel only takes focus via the explicit
	// DOWN path from the status bar (HomeFocusCoordinator.moveToCarousel)

	LaunchedEffect(currentIndex, isWatchNowFocused, items.size) {
		if (items.size > 1 && !isWatchNowFocused) {
			delay(8_000)
			currentIndex = (currentIndex + 1) % items.size
		}
	}

	val currentItem = items[currentIndex.coerceIn(items.indices)]

	Box(modifier = modifier) {
		Box(modifier = backgroundModifier) {
			Crossfade(
				targetState = currentItem,
				animationSpec = tween(durationMillis = 500),
				label = "featuredCarousel"
			) { item ->
				CarouselSlide(
					item = item,
					blurBoundary = blurBoundary
				)
			}
		}

		Box(modifier = Modifier.zIndex(1f)) {
			content()
		}

		Box(modifier = foregroundModifier.zIndex(2f)) {
			CarouselForeground(
				item = currentItem,
				onWatchNow = { navigationRepository.navigate(Destinations.itemDetails(currentItem.id)) },
				onPrevious = { currentIndex = if (currentIndex == 0) items.size - 1 else currentIndex - 1 },
				onNext = { currentIndex = (currentIndex + 1) % items.size },
				watchNowFocused = isWatchNowFocused,
				onWatchNowFocusChanged = { focused ->
					isWatchNowFocused = focused
					if (focused) HomeFocusCoordinator.setRowsFocused(false)
				},
				focusRequester = focusRequester,
				onMoveDown = onMoveDown,
				modifier = Modifier.fillMaxSize()
			)

			CarouselIndicator(
				itemCount = items.size,
				activeIndex = currentIndex,
				modifier = Modifier.align(Alignment.BottomEnd)
			)
		}
	}
}

private suspend fun loadFeaturedItems(api: ApiClient): List<FeaturedCarouselItem> = try {
	val response = api.itemsApi.getItems(
		request = GetItemsRequest(
			includeItemTypes = setOf(BaseItemKind.MOVIE),
			filters = setOf(ItemFilter.IS_UNPLAYED),
			sortBy = setOf(ItemSortBy.PREMIERE_DATE, ItemSortBy.SORT_NAME),
			sortOrder = setOf(SortOrder.DESCENDING),
			limit = 20,
			recursive = true,
			enableImageTypes = setOf(
				org.jellyfin.sdk.model.api.ImageType.BACKDROP,
				org.jellyfin.sdk.model.api.ImageType.THUMB,
				org.jellyfin.sdk.model.api.ImageType.LOGO,
				org.jellyfin.sdk.model.api.ImageType.PRIMARY,
			),
			fields = setOf(ItemFields.OVERVIEW),
			enableTotalRecordCount = false
		)
	)

	response.content.items.orEmpty().mapNotNull { item ->
		FeaturedCarouselItem.fromBaseItemDto(item, api)
	}
} catch (error: Exception) {
	Timber.e(error, "Failed to load featured carousel items")
	emptyList()
}

@Composable
private fun CarouselSlide(
	item: FeaturedCarouselItem,
	blurBoundary: Dp,
) {
	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val density = LocalDensity.current
		val boundaryFraction = with(density) { blurBoundary.toPx() / maxHeight.toPx() }.coerceIn(0f, 1f)
		// KFIN: fade band twice as wide as before
		val fadeFraction = with(density) { 200.toDp().toPx() / maxHeight.toPx() }

		AsyncImage(
			url = item.imageUrl,
			scaleType = ImageView.ScaleType.CENTER_CROP,
			modifier = Modifier.fillMaxSize()
		)

		// KFIN: accent fill instead of blur - opacity goes from 100% at the
		// bottom edge to 80% at the boundary, over the widened fade band
		val context = LocalContext.current
		var accent by remember(item.imageUrl) { mutableStateOf<Color?>(null) }
		LaunchedEffect(item.imageUrl) {
			accent = extractAccentColorFromUrl(context, item.imageUrl)
		}
		val accentFill = accent ?: Color(0xCC0B0D14)
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						0f to accentFill.copy(alpha = 0f),
						boundaryFraction to accentFill.copy(alpha = 0.7f),
						(boundaryFraction + fadeFraction).coerceAtMost(1f) to accentFill.copy(alpha = 1f),
						// KFIN: the accent gradually turns black towards the bottom edge
						0.85f to androidx.compose.ui.graphics.lerp(accentFill, Color.Black, 0.4f),
						1f to androidx.compose.ui.graphics.lerp(accentFill, Color.Black, 0.8f)
					)
				)
		)

		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						0f to Color.Transparent,
						boundaryFraction to Color.Transparent,
						(boundaryFraction + fadeFraction).coerceAtMost(1f) to Color.Black.copy(alpha = 0.2f),
						1f to Color.Black.copy(alpha = 0.2f)
					)
				)
		)
	}
}

@Composable
private fun CarouselForeground(
	item: FeaturedCarouselItem,
	onWatchNow: () -> Unit,
	onPrevious: () -> Unit,
	onNext: () -> Unit,
	watchNowFocused: Boolean,
	onWatchNowFocusChanged: (Boolean) -> Unit,
	focusRequester: FocusRequester,
	onMoveDown: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier.padding(start = 28.dp, top = 22.dp, end = 28.dp, bottom = 25.dp)
	) {
		if (item.logoUrl != null) {
			// KFIN: same slot and sizing as the details/grid/player logo -
			// 53dp from the top, 28dp from the end, height-bound 86dp.
			// adjustViewBounds is disabled so the view can never outgrow the box.
			androidx.compose.ui.viewinterop.AndroidView(
				factory = { ctx ->
					org.jellyfin.androidtv.ui.AsyncImageView(ctx).apply {
						adjustViewBounds = false
						scaleType = ImageView.ScaleType.FIT_CENTER
					}
				},
				update = { view -> view.load(url = item.logoUrl, aspectRatio = 2.5) },
				modifier = Modifier
					// KFIN: the logo hovers above the watch button
					.align(Alignment.BottomStart)
					.padding(bottom = 92.dp)
					.height(86.dp)
					.widthIn(max = 320.dp)
			)
		} else {
			Text(
				text = item.title,
				color = Color.White,
				fontSize = 30.sp,
				fontWeight = FontWeight.Bold,
				lineHeight = 34.sp,
				overflow = TextOverflow.Ellipsis,
				maxLines = 2,
				textAlign = TextAlign.End,
				modifier = Modifier
					.align(Alignment.TopEnd)
					.fillMaxWidth(0.5f)
			)
		}

		val watchNowShape = RoundedCornerShape(40.dp)
		// KFIN: matte glass - idle: 80% dark glass, white content at 90%;
		// focused: +5% scale, 95% white fill, white contour, black content at 90%
		val watchScale by animateFloatAsState(
			targetValue = if (watchNowFocused) 1.05f else 1f,
			animationSpec = tween(180),
			label = "watchScale"
		)
		Box(
			modifier = Modifier
				.align(Alignment.BottomStart)
				.padding(
					top = 4.dp,
					bottom = with(LocalDensity.current) { 11.toDp() }
				)
				.focusRequester(focusRequester)
				// KFIN: UP from the watch button rises to the status bar home tab
				.onPreviewKeyEvent { keyEvent ->
					if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown
						&& keyEvent.key == androidx.compose.ui.input.key.Key.DirectionUp
					) {
						org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.focusHome()
						true
					} else {
						false
					}
				}
				.graphicsLayer {
					scaleX = watchScale
					scaleY = watchScale
				}
				// KFIN: outer ring drawn before clip - clip() would cut anything
				// drawn outside the bounds, so the contour must come first
				.drawBehind {
					if (watchNowFocused) {
						val gap = 2.5.dp.toPx()
						drawRoundRect(
							color = Color.White,
							topLeft = Offset(-gap, -gap),
							size = Size(size.width + gap * 2, size.height + gap * 2),
							cornerRadius = CornerRadius(44.dp.toPx()),
							style = Stroke(width = 2.dp.toPx())
						)
					}
				}
				.clip(watchNowShape)
				// KFIN: status-bar style - opaque gray substrate, white selection
				.background(
					if (watchNowFocused) Color.White else Color.Black
				)
				
				.onFocusChanged { focusState -> onWatchNowFocusChanged(focusState.hasFocus) }
				.onKeyEvent { keyEvent ->
					if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false

					when (keyEvent.key) {
						Key.DirectionLeft -> {
							onPrevious()
							true
						}

						Key.DirectionRight -> {
							onNext()
							true
						}

						Key.DirectionUp -> {
							// KFIN: rise to the status bar (last focused tab) - works from
							// the first launch, not only after visiting the rows
							CatalogBarFocus.focusSaved()
							true
						}

						Key.DirectionDown -> {
							onMoveDown()
							true
						}

						else -> false
					}
				}
				.clickable(onClick = onWatchNow)
				.padding(
					horizontal = 16.dp - with(LocalDensity.current) { 2.5f.toDp() },
					vertical = 8.dp
				)
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = ImageVector.vectorResource(R.drawable.ic_play),
					contentDescription = null,
					tint = if (watchNowFocused) Color(0xF2000000) else Color.White
				)
				Spacer(Modifier.width(6.dp))
				Text(
					// KFIN: uppercase like the rest of the UI labels
					text = stringResource(R.string.watch).uppercase(),
					color = if (watchNowFocused) Color(0xE6000000) else Color.White.copy(alpha = 0.85f),
					fontSize = 14.sp,
					fontWeight = FontWeight.SemiBold
				)
			}
		}
	}
}

@Composable
private fun CarouselIndicator(
	itemCount: Int,
	activeIndex: Int,
	modifier: Modifier = Modifier,
) {
	// KFIN: dots only - no background pill; all dots are the same size, the
	// active one is highlighted exactly like the buttons: outline only
	Row(
		modifier = modifier
			.padding(16.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		repeat(itemCount) { index ->
			val isActive = index == activeIndex
			// KFIN: idle dots opaque black; active pure white with a white ring at a gap
			Box(
				modifier = Modifier
					.width(6.dp)
					.height(6.dp)
					.drawBehind {
						if (isActive) {
							val gap = 2.5.dp.toPx()
							drawCircle(
								color = Color.White,
								radius = (size.minDimension / 2f) + gap,
								style = Stroke(width = 2.dp.toPx())
							)
						}
					}
					.clip(CircleShape)
					.background(
						if (isActive) Color.White else Color.Black
					)
			)
		}
	}
}
