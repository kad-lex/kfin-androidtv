package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterNotNull
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.androidtv.ui.home.HomeFocusCoordinator
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.settings.compat.SettingsViewModel
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

// KFIN: the bar has no substrate - its content is pinned white with a 2px
// shadow; the selection is static white fill with dark content
@Composable
fun barColors(): Triple<Color, Color, Color> = Triple(
	Color.White,
	Color.White,
	Color(0xE6000000),
)

// KFIN: plain icon - no shadow; capsules are black with white content
@Composable
private fun KfinShadowIcon(
	imageVector: ImageVector,
	contentDescription: String?,
	tint: Color,
	modifier: Modifier = Modifier,
) {
	Icon(
		imageVector = imageVector,
		contentDescription = contentDescription,
		tint = tint,
		modifier = modifier
	)
}

@Composable
fun GlassCatalogBar(
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit,
) {
	val barShape = RoundedCornerShape(40.dp)
	val sizeIncrease = with(LocalDensity.current) { 8.toDp() }
	val halfSizeIncrease = with(LocalDensity.current) { 2.toDp() }

	Row(
		modifier = modifier
			.widthIn(max = 1240.dp + sizeIncrease)
			.horizontalScroll(rememberScrollState())
			.clip(barShape)
			// KFIN: +sizeIncrease on all sides
			.padding(
				horizontal = 10.dp + halfSizeIncrease,
				vertical = 2.dp + halfSizeIncrease
			)
			.focusGroup(),
		horizontalArrangement = Arrangement.spacedBy(2.dp),
		verticalAlignment = Alignment.CenterVertically,
		content = content
	)
}

@Composable
private fun GlassCatalogTab(
	title: String?,
	icon: ImageVector,
	selected: Boolean,
	enabled: Boolean = true,
	focusRequester: FocusRequester = FocusRequester(),
	onFocus: () -> Unit = {},
	onClick: () -> Unit,
) {
	val barShape = RoundedCornerShape(40.dp)
	val contentAlpha = if (enabled) 1f else 0.45f
	var isFocused by remember { mutableStateOf(false) }
	val isActive = selected || isFocused

	// KFIN: selection style - 80% white fill, dark content
	// KFIN: black on light backdrops, white on dark ones
	val (base, fill, onFill) = barColors()
	val tabColor by animateColorAsState(
		if (isActive) onFill else base.copy(alpha = contentAlpha),
		tween(180),
		label = "tabColor"
	)
	Box(
		modifier = Modifier
			.focusRequester(focusRequester)			.clip(barShape)
			.background(if (isActive) fill else Color.Black, barShape)
			.onFocusChanged { focusState ->
				isFocused = focusState.hasFocus
				if (focusState.hasFocus) {
					HomeFocusCoordinator.setRowsFocused(false)
					onFocus()
				}
			}
			.clickable(enabled = enabled, onClick = onClick)
			.padding(horizontal = 7.dp, vertical = 5.dp)
	) {
		val contentColor = tabColor
		if (title == null) {
			KfinShadowIcon(
				imageVector = icon,
				contentDescription = null,
				tint = contentColor,
				modifier = Modifier.size(16.dp))
		} else {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(6.dp)
			) {
				KfinShadowIcon(
					imageVector = icon,
					contentDescription = null,
					tint = contentColor,
					modifier = Modifier.size(16.dp)				)
				Text(
					// KFIN: status bar labels are uppercase
					text = title.uppercase(),
					color = contentColor,
					fontSize = 14.sp,
					fontWeight = FontWeight.Bold,
					maxLines = 1,
					softWrap = false
				)
			}
		}
	}
}

@Composable
fun GlassIconButton(
	icon: ImageVector,
	contentDescription: String,
	modifier: Modifier = Modifier,
	activated: Boolean = false,
	onClick: () -> Unit,
) {
	val (base, fill, onFill) = barColors()
	val shape = RoundedCornerShape(40.dp)
	var isFocused by remember { mutableStateOf(false) }

	Box(
		modifier = modifier
			.clip(shape)
			.background(if (isFocused) fill else Color.Black, shape)
			.onFocusChanged { focusState ->
				isFocused = focusState.hasFocus
				if (focusState.hasFocus) HomeFocusCoordinator.setRowsFocused(false)
			}
			.clickable(onClick = onClick)
			.padding(7.dp)
	) {
		KfinShadowIcon(
			imageVector = icon,
			contentDescription = contentDescription,
			tint = if (activated && !isFocused) Color(0xFFFF0033) else if (isFocused) onFill else base,
			modifier = Modifier.size(16.dp))
	}
}

@Composable
fun HomeStatusBar() {
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val userViewsRepository = koinInject<UserViewsRepository>()
	val navigationRepository = koinInject<NavigationRepository>()
	val itemLauncher = koinInject<ItemLauncher>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()

	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userViews by userViewsRepository.views.collectAsState(initial = emptyList())

	fun openLibrary(collectionType: CollectionType) {
		val library = userViews.firstOrNull { it.collectionType == collectionType }
		if (library != null) itemLauncher.launchUserView(library)
	}

	// KFIN: rising onto the status bar lands on the home button by default
	// or on the tab of the library currently on screen
	val homeFocusRequester = remember { FocusRequester() }
	val moviesFocusRequester = remember { FocusRequester() }
	val seriesFocusRequester = remember { FocusRequester() }
	val boxsetsFocusRequester = remember { FocusRequester() }
	var currentTabRequester by remember { androidx.compose.runtime.mutableStateOf(homeFocusRequester) }

	fun openLibraryTab(collectionType: CollectionType, requester: FocusRequester) {
		currentTabRequester = requester
		openLibrary(collectionType)
	}

	DisposableEffect(Unit) {
		CatalogBarFocus.setActions(
			focusSaved = { currentTabRequester.requestFocus() },
			focusHome = { homeFocusRequester.requestFocus() }
		)
		CatalogBarFocus.setTabAction { currentTabRequester.requestFocus() }
		onDispose {
			CatalogBarFocus.setActions(null, null)
			CatalogBarFocus.setTabAction(null)
		}
	}

	GlassCatalogBar {
		GlassCatalogTab(
			title = null,
			icon = ImageVector.vectorResource(R.drawable.ic_house),
			selected = false,
			focusRequester = homeFocusRequester,
			onClick = { navigationRepository.navigate(Destinations.home, replace = true) }
		)
		GlassCatalogTab(
			title = "Фильмы",
			icon = ImageVector.vectorResource(R.drawable.ic_clapperboard),
			selected = false,
			enabled = userViews.any { it.collectionType == CollectionType.MOVIES },
			focusRequester = moviesFocusRequester,
			onClick = { openLibraryTab(CollectionType.MOVIES, moviesFocusRequester) }
		)
		GlassCatalogTab(
			title = "Сериалы",
			icon = ImageVector.vectorResource(R.drawable.ic_tv),
			selected = false,
			enabled = userViews.any { it.collectionType == CollectionType.TVSHOWS },
			focusRequester = seriesFocusRequester,
			onClick = { openLibraryTab(CollectionType.TVSHOWS, seriesFocusRequester) }
		)
		GlassCatalogTab(
			title = "Коллекции",
			icon = ImageVector.vectorResource(R.drawable.ic_folder),
			selected = false,
			enabled = userViews.any { it.collectionType == CollectionType.BOXSETS },
			focusRequester = boxsetsFocusRequester,
			onClick = { openLibraryTab(CollectionType.BOXSETS, boxsetsFocusRequester) }
		)
		Spacer(Modifier.width(4.dp))

		GlassIconButton(
			icon = ImageVector.vectorResource(R.drawable.ic_search),
			contentDescription = stringResource(R.string.lbl_search),
			onClick = { navigationRepository.navigate(Destinations.search()) }
		)
		GlassIconButton(
			icon = ImageVector.vectorResource(R.drawable.ic_settings),
			contentDescription = stringResource(R.string.lbl_settings),
			modifier = Modifier.onGloballyPositioned { coordinates ->
				// KFIN: the settings panel opens on this button's axis, 20px below the bar
				val bounds = coordinates.boundsInWindow()
				CatalogBarFocus.settingsButtonCenterX = bounds.center.x
				CatalogBarFocus.settingsButtonBottomY = bounds.bottom
			},
			onClick = { settingsViewModel.show() }
		)

		// KFIN: clock removed from the bar

		// Screen-specific tools (e.g. library sort/filter buttons)
		CatalogBarTools.content.value?.invoke(this)
	}
}

/**
 * KFIN: standalone profile button for the top-right corner - same behaviour
 * as the old in-bar avatar (switch user).
 */
@Composable
fun ProfileButton() {
	val sessionRepository = koinInject<SessionRepository>()
	val mediaManager = koinInject<MediaManager>()
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val activity = LocalActivity.current

	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	var isProfileFocused by remember { mutableStateOf(false) }
	val profileScale by animateFloatAsState(if (isProfileFocused) 1.025f else 1f, tween(180), label = "profileScale")
	val (base, fill, onFill) = barColors()

	Box(
		modifier = Modifier
			.size(31.dp)
			.graphicsLayer { scaleX = profileScale; scaleY = profileScale }
			.clip(RoundedCornerShape(40.dp))
			// KFIN: plain opaque gray - no material effects
			.then(if (isProfileFocused) Modifier.background(fill, RoundedCornerShape(40.dp)) else Modifier)
			.then(if (isProfileFocused) Modifier.border(2.dp, fill, RoundedCornerShape(40.dp)) else Modifier)
			.onFocusChanged { focusState ->
				isProfileFocused = focusState.hasFocus
				if (focusState.hasFocus) HomeFocusCoordinator.setRowsFocused(false)
			}
			.clickable {
				mediaManager.clearAudioQueue()
				sessionRepository.destroyCurrentSession()
				activity?.startActivity(ActivityDestinations.startup(activity))
				activity?.finishAfterTransition()
			}
			.padding(2.dp)
	) {
		ProfilePicture(
			url = userImage,
			contentDescription = stringResource(R.string.lbl_switch_user),
			modifier = Modifier
				.size(27.dp)
				.clip(IconButtonDefaults.Shape)
		)
	}
}

/**
 * KFIN: clock + profile capsule for the top-right corner (home screen only).
 */
@Composable
fun ProfileClockCapsule() {
	val sessionRepository = koinInject<SessionRepository>()
	val mediaManager = koinInject<MediaManager>()
	val api = koinInject<ApiClient>()
	val userRepository = koinInject<UserRepository>()
	val activity = LocalActivity.current

	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	var isProfileFocused by remember { mutableStateOf(false) }

	var now by remember { mutableStateOf(LocalDateTime.now()) }
	LaunchedEffect(Unit) {
		while (true) {
			now = LocalDateTime.now()
			kotlinx.coroutines.delay(1000L)
		}
	}
	val time = DateTimeFormatter.ofPattern("HH:mm").format(now)

	val (base, fill, onFill) = barColors()
	val capsuleShape = RoundedCornerShape(40.dp)
	Row(
		modifier = Modifier
			.clip(capsuleShape)
			.background(Color.Black, capsuleShape)
			.padding(horizontal = 14.dp, vertical = 4.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(10.dp)
	) {
		// KFIN: system font, colored against the backdrop
		Text(
			text = time,
			color = base,
			fontSize = 14.sp
		)

		Box(
			modifier = Modifier
				.size(24.dp)
				.clip(RoundedCornerShape(40.dp))
				.then(if (isProfileFocused) Modifier.background(fill, RoundedCornerShape(40.dp)) else Modifier)
				.then(if (isProfileFocused) Modifier.border(2.dp, fill, RoundedCornerShape(40.dp)) else Modifier)
				.onFocusChanged { focusState ->
					isProfileFocused = focusState.hasFocus
					if (focusState.hasFocus) HomeFocusCoordinator.setRowsFocused(false)
				}
				.clickable {
					mediaManager.clearAudioQueue()
					sessionRepository.destroyCurrentSession()
					activity?.startActivity(ActivityDestinations.startup(activity))
					activity?.finishAfterTransition()
				}
		) {
			ProfilePicture(
				url = userImage,
				contentDescription = stringResource(R.string.lbl_switch_user),
				modifier = Modifier
					.fillMaxSize()
					.clip(IconButtonDefaults.Shape)
			)
		}
	}
}
