package org.jellyfin.androidtv.ui.home

object HomeFocusCoordinator {
	var isActive = false
		private set

	var isRowsFocused = false
		private set

	fun setRowsFocused(value: Boolean) {
		isRowsFocused = value
	}

	var focusCarousel: (() -> Unit)? = null
		private set
	var focusProfile: (() -> Unit)? = null
		private set
	var focusRows: ((Int) -> Unit)? = null
		private set

	private var currentRowProvider: () -> Int = { 0 }
	private var rowCountProvider: () -> Int = { 0 }

	fun activate(
		focusCarousel: () -> Unit,
		focusRows: (Int) -> Unit,
		currentRow: () -> Int,
		rowCount: () -> Int,
	) {
		isActive = true
		this.focusCarousel = focusCarousel
		this.focusRows = focusRows
		this.currentRowProvider = currentRow
		this.rowCountProvider = rowCount
	}

	fun setProfileFocusAction(focusProfile: () -> Unit) {
		this.focusProfile = focusProfile
	}

	fun clearProfileFocusAction() {
		focusProfile = null
	}

	fun clear() {
		isActive = false
		isRowsFocused = false
		focusCarousel = null
		focusRows = null
		currentRowProvider = { 0 }
		rowCountProvider = { 0 }
	}

	fun moveToCarousel() {
		if (isActive) focusCarousel?.invoke()
	}

	fun moveToProfile() {
		if (isActive) focusProfile?.invoke()
	}

	fun moveUp(): Boolean {
		if (!isActive) return false

		val row = currentRowProvider()
		return if (row > 0) {
			focusRows?.invoke(row - 1)
			true
		} else {
			focusCarousel?.invoke()
			true
		}
	}

	fun moveDown(): Boolean {
		if (!isActive) return false

		val row = currentRowProvider()
		val rowCount = rowCountProvider()
		return if (row < rowCount - 1) {
			focusRows?.invoke(row + 1)
			true
		} else {
			false
		}
	}
}
