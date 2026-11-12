package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class AppTheme(
	override val nameRes: Int,
) : PreferenceEnum {
	/**
	 * The default dark theme
	 */
	DARK(R.string.pref_theme_dark),

	/**
	 * The emerald theme
	 */
	EMERALD(R.string.pref_theme_emerald),

	/**
	 * The muted purple theme
	 */
	MUTED_PURPLE(R.string.pref_theme_muted_purple),

	/**
	 * Тема №3: Obsidian Smoked Glass (OLED Theater + Rim Light + YouTube Red Seekbar)
	 */
	OBSIDIAN_SMOKED(R.string.pref_theme_obsidian_smoked),
}
