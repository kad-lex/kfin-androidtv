package org.jellyfin.androidtv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.ui.home.carousel.FeaturedCarousel
import org.koin.android.ext.android.inject

class HomeFragment : Fragment() {
	private val sessionRepository by inject<SessionRepository>()
	private val serverRepository by inject<ServerRepository>()
	private val notificationRepository by inject<NotificationsRepository>()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val carouselFocusRequester = remember { FocusRequester() }
		var focusCarousel by remember { mutableStateOf(false) }
		var rowsFragment by remember { mutableStateOf<HomeRowsFragment?>(null) }

		LaunchedEffect(focusCarousel) {
			if (focusCarousel) {
				carouselFocusRequester.requestFocus()
				focusCarousel = false
			}
		}

		DisposableEffect(rowsFragment) {
			rowsFragment?.let { fragment ->
				HomeFocusCoordinator.activate(
					focusCarousel = { focusCarousel = true },
					focusRows = { index -> fragment.focusRow(index) },
					currentRow = { fragment.currentRowIndex },
					rowCount = { fragment.rowCount }
				)
			}
			onDispose {
				HomeFocusCoordinator.clear()
			}
		}

		val carouselHeight = 280.dp
		val watchNowBottomPadding = 22.dp
		val rowsTopBoundary = carouselHeight - watchNowBottomPadding + with(LocalDensity.current) { 1.toDp() }

		FeaturedCarousel(
			modifier = Modifier.fillMaxSize(),
			backgroundModifier = Modifier.fillMaxSize(),
			foregroundModifier = Modifier
				.fillMaxWidth()
				.height(carouselHeight),
			fadeBoundary = rowsTopBoundary,
			focusRequester = carouselFocusRequester,
			onMoveDown = { HomeFocusCoordinator.focusRows?.invoke(0) }
		) {
			AndroidFragment<HomeRowsFragment>(
				modifier = Modifier
					.fillMaxSize()
					.padding(top = rowsTopBoundary)
					.focusGroup(),
				maxLifecycle = Lifecycle.State.RESUMED,
				onUpdate = { fragment ->
					rowsFragment = fragment
					fragment.onFocusCarousel = { focusCarousel = true }
				}
			)
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		sessionRepository.currentSession
			.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
			.map { session ->
				if (session == null) null
				else serverRepository.getServer(session.serverId)
			}
			.onEach { server ->
				notificationRepository.updateServerNotifications(server)
			}
			.launchIn(viewLifecycleOwner.lifecycleScope)
	}
}
