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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.filterNotNull
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.button.IconButton
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
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

	Row(
		modifier = modifier
			.widthIn(max = 840.dp)
			.horizontalScroll(rememberScrollState())
			.shadow(elevation = 24.dp, shape = barShape)
			.clip(barShape)
			.background(Color(0x660B0D14))
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
			.padding(horizontal = 10.dp, vertical = 2.dp)
			.focusGroup(),
		horizontalArrangement = Arrangement.spacedBy(2.dp),
		verticalAlignment = Alignment.CenterVertically,
		content = content
	)
}

@Composable
private fun GlassCatalogTab(
	title: String,
	icon: ImageVector,
	selected: Boolean,
	enabled: Boolean = true,
	onClick: () -> Unit,
) {
	val barShape = RoundedCornerShape(40.dp)
	val contentAlpha = if (enabled) 1f else 0.45f
	var isFocused by remember { mutableStateOf(false) }
	val isActive = selected || isFocused

	Box(
		modifier = Modifier
			.clip(barShape)
			.background(
				if (isActive) Color.White.copy(alpha = 0.20f)
				else Color.Transparent
			)
			.then(
				if (isActive) Modifier.border(
					2.dp,
					Color(0xFF3B82F6),
					barShape
				) else Modifier
			)
			.onFocusChanged { focusState -> isFocused = focusState.hasFocus }
			.clickable(enabled = enabled, onClick = onClick)
			.padding(horizontal = 8.dp, vertical = 6.dp)
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(6.dp)
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = Color.White.copy(alpha = contentAlpha),
				modifier = Modifier.size(16.dp)
			)
			Text(
				text = title,
				color = Color.White.copy(alpha = contentAlpha),
				fontSize = 13.sp,
				fontWeight = FontWeight.Bold,
				maxLines = 1,
				softWrap = false
			)
		}
	}
}

@Composable
private fun GlassIconButton(
	icon: ImageVector,
	contentDescription: String,
	onClick: () -> Unit,
) {
	IconButton(
		onClick = onClick,
		colors = ButtonDefaults.colors(
			containerColor = Color.Transparent,
			contentColor = Color.White,
			focusedContainerColor = Color.White.copy(alpha = 0.18f),
			focusedContentColor = Color.White
		),
		contentPadding = PaddingValues(8.dp),
	) {
		Icon(
			imageVector = icon,
			contentDescription = contentDescription,
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

	fun openLibrary(collectionType: CollectionType) {
		val library = userViews.firstOrNull { it.collectionType == collectionType }
		if (library != null) itemLauncher.launchUserView(library)
	}

	GlassCatalogBar {
		val userImagePainter = rememberAsyncImagePainter(userImage)
		val userImageState by userImagePainter.state.collectAsState()
		val userImageVisible = userImageState is AsyncImagePainter.State.Success

		IconButton(
			onClick = {
				mediaManager.clearAudioQueue()
				sessionRepository.destroyCurrentSession()
				activity?.startActivity(ActivityDestinations.startup(activity))
				activity?.finishAfterTransition()
			},
			modifier = Modifier.size(36.dp),
			colors = ButtonDefaults.colors(
				containerColor = Color.Transparent,
				contentColor = Color.White,
				focusedContainerColor = Color.White.copy(alpha = 0.18f),
				focusedContentColor = Color.White
			),
			contentPadding = if (userImageVisible) PaddingValues(2.dp) else IconButtonDefaults.ContentPadding,
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
			title = stringResource(R.string.lbl_home),
			icon = ImageVector.vectorResource(R.drawable.ic_house),
			selected = true,
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
	}
}
