package org.jellyfin.androidtv.ui.browsing

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideLocalInteractionTracker
import org.jellyfin.androidtv.ui.composable.compat.AppNavigationHost
import org.jellyfin.androidtv.ui.home.HomeFocusCoordinator
import org.jellyfin.androidtv.ui.shared.toolbar.ContentPrimaryAction
import org.jellyfin.androidtv.ui.shared.toolbar.ProfileClockCapsule
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.CustomPlaybackOverlayFragment
import org.jellyfin.androidtv.ui.player.video.VideoPlayerFragment
import org.jellyfin.androidtv.ui.screensaver.InAppScreensaver
import org.jellyfin.androidtv.ui.settings.compat.MainActivitySettings
import org.jellyfin.androidtv.ui.shared.toolbar.HomeStatusBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class MainActivity : FragmentActivity() {
	private val navigationRepository by inject<NavigationRepository>()
	private val sessionRepository by inject<SessionRepository>()
	private val userRepository by inject<UserRepository>()
	private val interactionTrackerViewModel by viewModel<InteractionTrackerViewModel>()
	private val workManager by inject<WorkManager>()
	private val playbackManager by inject<PlaybackManager>()

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		if (!validateAuthentication()) return

		interactionTrackerViewModel.keepScreenOn.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
			.onEach { keepScreenOn ->
				if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
				else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
			}.launchIn(lifecycleScope)

		if (savedInstanceState == null && navigationRepository.canGoBack) navigationRepository.reset(clearHistory = true)

		navigationRepository.currentAction
			.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
			.onEach {
				interactionTrackerViewModel.notifyInteraction(canCancel = false, userInitiated = false)
			}.launchIn(lifecycleScope)

		setContent {
			JellyfinTheme {
				ProvideLocalInteractionTracker(
					interactionTracker = { interactionTrackerViewModel.notifyInteraction(false, userInitiated = true) }
				) {
					Box(modifier = Modifier.fillMaxSize()) {
						AppBackground()
						AppNavigationHost(
							navigationRepository = navigationRepository,
						)

						val playState by playbackManager.state.playState.collectAsState()
						val isPlayerShown = remember { mutableStateOf(false) }
						// KFIN: the details page manages its own navigation - no status bar
						val isDetailsShown = remember { mutableStateOf(false) }
						DisposableEffect(Unit) {
							val callback = object : FragmentManager.FragmentLifecycleCallbacks() {
								override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
									isPlayerShown.value = fragment is CustomPlaybackOverlayFragment || fragment is VideoPlayerFragment
									isDetailsShown.value = fragment is org.jellyfin.androidtv.ui.itemdetail.FullDetailsFragment
										|| fragment is org.jellyfin.androidtv.ui.browsing.CollectionFragment
								}

								override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
									if (fragment is CustomPlaybackOverlayFragment || fragment is VideoPlayerFragment) {
										isPlayerShown.value = false
									}
									if (fragment is org.jellyfin.androidtv.ui.itemdetail.FullDetailsFragment) {
										org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.returnFromDetails = true
										org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.skipHomeAutofocusOnce = true
									}
								}
							}
							supportFragmentManager.registerFragmentLifecycleCallbacks(callback, true)
							onDispose { supportFragmentManager.unregisterFragmentLifecycleCallbacks(callback) }
						}
						AnimatedVisibility(
							visible = !isPlayerShown.value && !isDetailsShown.value && playState != PlayState.PLAYING && playState != PlayState.PAUSED,
							modifier = Modifier.align(Alignment.TopStart),
							enter = fadeIn(tween(300)),
							exit = fadeOut(tween(300))
						) {
							val statusBarMargin = with(LocalDensity.current) { 20.toDp() }
							Box(
								modifier = Modifier
									.zIndex(1f)
									.padding(start = 28.dp + with(LocalDensity.current) { 2.5f.toDp() }, top = statusBarMargin + with(LocalDensity.current) { 2.5f.toDp() })
									.onKeyEvent { keyEvent ->
										if (keyEvent.type == KeyEventType.KeyDown) {
											when (keyEvent.key) {
												// KFIN: moving up from the status bar is blocked
												Key.DirectionUp -> true
												Key.DirectionDown -> when {
													HomeFocusCoordinator.isActive -> {
														HomeFocusCoordinator.moveToCarousel()
														true
													}
													// KFIN: on content screens DOWN from any status bar
													// button jumps straight to the primary action
													ContentPrimaryAction.hasPrimary -> {
														ContentPrimaryAction.requestPrimary()
														true
													}
													else -> false
												}
												else -> false
											}
										} else {
											false
										}
									}
							) {
								HomeStatusBar()

								// KFIN: default focus on the home button at app start
								var autoFocusedHome by remember { androidx.compose.runtime.mutableStateOf(false) }
								androidx.compose.runtime.LaunchedEffect(Unit) {
									if (!autoFocusedHome) {
										autoFocusedHome = true
										kotlinx.coroutines.delay(600)
										if (supportFragmentManager.backStackEntryCount == 0 && !org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.skipHomeAutofocusOnce) {
											org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.focusHome()
										} else {
										org.jellyfin.androidtv.ui.shared.toolbar.CatalogBarFocus.skipHomeAutofocusOnce = false
										}
									}
								}
							}

						}


						// KFIN: clock + profile capsule, top-right, home screen only (hidden on details)
						val statusBarMarginGlobal = with(LocalDensity.current) { 20.toDp() }
						if (!isPlayerShown.value && !isDetailsShown.value && playState != PlayState.PLAYING && playState != PlayState.PAUSED) {
							Box(
								modifier = Modifier
									.align(Alignment.TopEnd)
									.zIndex(1f)
									.padding(
										top = statusBarMarginGlobal + with(LocalDensity.current) { 2.5f.toDp() },
										end = 28.dp + with(LocalDensity.current) { 2.5f.toDp() }
									)
									// KFIN: the profile capsule is one layer with the status bar
									.onKeyEvent { keyEvent ->
										if (keyEvent.type == KeyEventType.KeyDown) {
											when (keyEvent.key) {
												Key.DirectionUp -> true
												Key.DirectionDown -> {
													HomeFocusCoordinator.moveToCarousel()
													true
												}
												else -> false
											}
										} else {
											false
										}
									}
							) {
								ProfileClockCapsule()
							}
						}

						InAppScreensaver()
						MainActivitySettings()
					}
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()

		if (!validateAuthentication()) return

		applyTheme()

		interactionTrackerViewModel.activityPaused = false
	}

	private fun validateAuthentication(): Boolean {
		if (sessionRepository.currentSession.value == null || userRepository.currentUser.value == null) {
			Timber.w("Activity ${this::class.qualifiedName} started without a session, bouncing to StartupActivity")
			startActivity(Intent(this, StartupActivity::class.java))
			finish()
			return false
		}

		return true
	}

	override fun onPause() {
		super.onPause()

		interactionTrackerViewModel.activityPaused = true
	}

	override fun onStop() {
		super.onStop()

		workManager.enqueue(OneTimeWorkRequestBuilder<LeanbackChannelWorker>().build())

		lifecycleScope.launch(Dispatchers.IO) {
			Timber.i("MainActivity stopped")
			sessionRepository.restoreSession(destroyOnly = true)
		}
	}

	// Forward key events to fragments

	override fun dispatchKeyEvent(event: KeyEvent): Boolean {
		if (event.action == KeyEvent.ACTION_DOWN && HomeFocusCoordinator.isRowsFocused) {
			when (event.keyCode) {
				KeyEvent.KEYCODE_DPAD_UP -> if (HomeFocusCoordinator.moveUp()) return true
				KeyEvent.KEYCODE_DPAD_DOWN -> if (HomeFocusCoordinator.moveDown()) return true
			}
		}
		return try {
			super.dispatchKeyEvent(event)
		} catch (e: IllegalArgumentException) {
			// KFIN: Compose's FocusFinder crashes when a view is detached mid
			// focus search (rows rebuilt/title reparented) - swallow and ignore
			Timber.w(e, "focus search raced a detached view")
			true
		}
	}

	private fun Fragment.onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean {
		var result = childFragmentManager.fragments.any { it.onKeyEvent(keyCode, event) }
		if (!result && this is View.OnKeyListener) result = onKey(currentFocus, keyCode, event)
		return result
	}

	private fun onKeyEvent(keyCode: Int, event: KeyEvent?): Boolean = supportFragmentManager.fragments
		.any { it.onKeyEvent(keyCode, event) }

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyDown(keyCode, event)

	override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)

	override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean =
		onKeyEvent(keyCode, event) || super.onKeyUp(keyCode, event)
}
