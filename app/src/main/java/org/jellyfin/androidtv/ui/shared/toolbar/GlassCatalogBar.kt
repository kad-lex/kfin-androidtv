package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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

@Composable
fun GlassCatalogBar(
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit,
) {
	val barShape = RoundedCornerShape(40.dp)
	val sizeIncrease = with(LocalDensity.current) { 6.toDp() }
	val halfSizeIncrease = with(LocalDensity.current) { 2.toDp() }

	Row(
		modifier = modifier
			.widthIn(max = 1240.dp + sizeIncrease)
			.horizontalScroll(rememberScrollState())
			.clip(barShape)
			.background(Color(0xA30B0D14))
			.border(
				width = 1.dp,
				brush = Brush.verticalGradient(
					colors = listOf(
						Color.White.copy(alpha = 0.10f),
						Color.White.copy(alpha = 0.05f)
					)
				),
				shape = barShape
			)
			.padding(
				horizontal = 10.dp + halfSizeIncrease,
				vertical = 2.dp + halfSizeIncrease + with(LocalDensity.current) { 2.toDp() }
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

	// KFIN: selection is a double white contour - an outer ring larger than
	// the button plus the inner ring on its edge, no fill
	val density = LocalDensity.current
	Box(
		modifier = Modifier
			.focusRequester(focusRequester)
			.drawBehind {
				if (isActive) {
					val expand = with(density) { 4.dp.toPx() }
					val stroke = with(density) { 3.dp.toPx() }
					val radius = CornerRadius(with(density) { 44.dp.toPx() })
					drawRoundRect(
						color = Color.White,
						topLeft = Offset(-expand, -expand),
						size = Size(size.width + expand * 2, size.height + expand * 2),
						cornerRadius = radius,
						style = Stroke(width = stroke)
					)
				}
			}
			.then(
				if (isActive) Modifier.border(
					2.dp,
					Color.White,
					barShape
				) else Modifier
			)
			.clip(barShape)
			.onFocusChanged { focusState ->
				isFocused = focusState.hasFocus
				if (focusState.hasFocus) {
					HomeFocusCoordinator.setRowsFocused(false)
					onFocus()
				}
			}
			.clickable(enabled = enabled, onClick = onClick)
			.padding(horizontal = 8.dp, vertical = 6.dp)
	) {
		val contentColor = Color.White.copy(alpha = contentAlpha)
		if (title == null) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = contentColor,
				modifier = Modifier.size(18.dp)
			)
		} else {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(6.dp)
			) {
				Icon(
					imageVector = icon,
					contentDescription = null,
					tint = contentColor,
					modifier = Modifier.size(16.dp)
				)
				Text(
					// KFIN: status bar labels are uppercase
					text = title.uppercase(),
					color = contentColor,
					fontSize = 13.sp,
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
	val shape = RoundedCornerShape(40.dp)
	var isFocused by remember { mutableStateOf(false) }

	val density2 = LocalDensity.current
	Box(
		modifier = modifier
			.drawBehind {
				if (isFocused) {
					val expand = with(density2) { 4.dp.toPx() }
					val stroke = with(density2) { 3.dp.toPx() }
					val radius = CornerRadius(with(density2) { 44.dp.toPx() })
					drawRoundRect(
						color = Color.White,
						topLeft = Offset(-expand, -expand),
						size = Size(size.width + expand * 2, size.height + expand * 2),
						cornerRadius = radius,
						style = Stroke(width = stroke)
					)
				}
			}
			.then(
				if (isFocused) Modifier.border(
					2.dp,
					Color.White,
					shape
				) else Modifier
			)
			.clip(shape)
			.onFocusChanged { focusState ->
				isFocused = focusState.hasFocus
				if (focusState.hasFocus) HomeFocusCoordinator.setRowsFocused(false)
			}
			.clickable(onClick = onClick)
			.padding(8.dp)
	) {
		Icon(
			imageVector = icon,
			contentDescription = contentDescription,
			tint = if (activated && !isFocused) Color(0xFF00A4DC) else Color.White,
			modifier = Modifier.size(18.dp)
		)
	}
}

@Composable
fun HomeStatusBar() {
	val userRepository = koinInject<UserRepository>()
	val api = koinInject<ApiClient>()
	val userViewsRepository = koinInject<UserViewsRepository>()
	val navigationRepository = koinInject<NavigationRepository>()
	val itemLauncher = koinInject<ItemLauncher>()
	val mediaManager = koinInject<MediaManager>()
	val sessionRepository = koinInject<SessionRepository>()
	val settingsViewModel = koinActivityViewModel<SettingsViewModel>()
	val activity = LocalActivity.current

	val currentUser by remember { userRepository.currentUser.filterNotNull() }.collectAsState(null)
	val userViews by userViewsRepository.views.collectAsState(initial = emptyList())
	val userImage = remember(currentUser) { currentUser?.primaryImage?.getUrl(api) }
	val profileFocusRequester = remember { FocusRequester() }
	var isProfileFocused by remember { mutableStateOf(false) }

	DisposableEffect(Unit) {
		HomeFocusCoordinator.setProfileFocusAction {
			profileFocusRequester.requestFocus()
		}
		onDispose {
			HomeFocusCoordinator.clearProfileFocusAction()
		}
	}

	fun openLibrary(collectionType: CollectionType) {
		val library = userViews.firstOrNull { it.collectionType == collectionType }
		if (library != null) itemLauncher.launchUserView(library)
	}

	// KFIN: rising onto the status bar always lands on the home button
	val homeFocusRequester = remember { FocusRequester() }

	DisposableEffect(Unit) {
		CatalogBarFocus.setActions(
			focusSaved = { homeFocusRequester.requestFocus() },
			focusHome = { homeFocusRequester.requestFocus() }
		)
		onDispose {
			CatalogBarFocus.setActions(null, null)
		}
	}

	GlassCatalogBar {
		Box(
			modifier = Modifier
				.size(36.dp)
				.focusRequester(profileFocusRequester)
				.clip(RoundedCornerShape(40.dp))
				.then(
					if (isProfileFocused) Modifier.border(
						1.dp + with(LocalDensity.current) { 1.toDp() },
						Color(0xFFF8FAFC).copy(alpha = 0.7f),
						RoundedCornerShape(40.dp)
					) else Modifier
				)
				// KFIN: focus is outline + 10% white fill, no glow
				.then(if (isProfileFocused) Modifier.background(Color(0xF20B0D14), RoundedCornerShape(40.dp)) else Modifier)
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
					.size(32.dp)
					.clip(IconButtonDefaults.Shape)
			)
		}

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
			onClick = { openLibrary(CollectionType.MOVIES) }
		)
		GlassCatalogTab(
			title = "Сериалы",
			icon = ImageVector.vectorResource(R.drawable.ic_tv),
			selected = false,
			enabled = userViews.any { it.collectionType == CollectionType.TVSHOWS },
			onClick = { openLibrary(CollectionType.TVSHOWS) }
		)
		GlassCatalogTab(
			title = "Коллекции",
			icon = ImageVector.vectorResource(R.drawable.ic_folder),
			selected = false,
			enabled = userViews.any { it.collectionType == CollectionType.BOXSETS },
			onClick = { openLibrary(CollectionType.BOXSETS) }
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
			onClick = { settingsViewModel.show() }
		)

		ToolbarClock()

		// Screen-specific tools (e.g. library sort/filter buttons)
		CatalogBarTools.content.value?.invoke(this)
	}
}
