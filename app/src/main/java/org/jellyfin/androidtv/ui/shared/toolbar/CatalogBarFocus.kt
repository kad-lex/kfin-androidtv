package org.jellyfin.androidtv.ui.shared.toolbar

/**
 * KFIN: cross-UI focus hooks for deterministic status-bar navigation.
 * focusSaved restores the last focused status bar item (or the home tab),
 * focusHome focuses the home tab directly.
 */
object CatalogBarFocus {
	// KFIN: set when a details page is paused - home then restores the card
	// the user dove into instead of jumping to the home tab
	var returnFromDetails = false

	// KFIN: the status bar is recreated on return - its 600ms home autofocus
	// would otherwise steal the restored card focus
	var skipHomeAutofocusOnce = false

	private var focusSavedAction: Runnable? = null
	private var focusHomeAction: Runnable? = null

	fun setActions(focusSaved: Runnable?, focusHome: Runnable?) {
		focusSavedAction = focusSaved
		focusHomeAction = focusHome
	}

	fun focusSaved() {
		focusSavedAction?.run()
	}

	fun focusHome() {
		focusHomeAction?.run()
	}

	// KFIN: status bar geometry for drop-down panels (settings, sort, ...)
	var settingsButtonCenterX = -1f
	var settingsButtonBottomY = -1f

	// KFIN: focuses the tab of the library currently on screen (falls back to home)
	private var focusTabAction: Runnable? = null

	fun setTabAction(action: Runnable?) {
		focusTabAction = action
	}

	fun focusTab() {
		if (focusTabAction != null) focusTabAction?.run() else focusHome()
	}
}

/**
 * KFIN: screens register their primary action (play button, grid, ...) so
 * pressing DOWN on any status bar button lands on it directly.
 */
object ContentPrimaryAction {
	private var focusPrimaryAction: Runnable? = null

	val hasPrimary get() = focusPrimaryAction != null

	fun setPrimary(action: Runnable?) {
		focusPrimaryAction = action
	}

	fun requestPrimary() {
		focusPrimaryAction?.run()
	}
}
