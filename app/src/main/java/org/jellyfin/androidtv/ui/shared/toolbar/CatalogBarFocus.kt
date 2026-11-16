package org.jellyfin.androidtv.ui.shared.toolbar

/**
 * KFIN: cross-UI focus hooks for deterministic status-bar navigation.
 * focusSaved restores the last focused status bar item (or the home tab),
 * focusHome focuses the home tab directly.
 */
object CatalogBarFocus {
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
