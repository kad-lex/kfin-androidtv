package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.LibraryPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import java.util.UUID

@Composable
fun SettingsLibrariesDisplayScreen(itemId: UUID, displayPreferencesId: String) {
	val router = LocalRouter.current
	val preferencesRepository = koinInject<PreferencesRepository>()
	val userView = rememberUserView(itemId)
	val libraryPreferences = remember(displayPreferencesId) { preferencesRepository.getLibraryPreferences(displayPreferencesId) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_libraries).uppercase()) },
				headingContent = { Text(userView?.name.orEmpty()) },
			)
		}

		item {
			var posterSize by rememberPreference(libraryPreferences, LibraryPreferences.posterSize)

			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_image_size)) },
				captionContent = { Text(stringResource(posterSize.nameRes)) },
				onClick = {
					router.push(
						Routes.LIBRARIES_DISPLAY_IMAGE_SIZE,
						mapOf("itemId" to itemId.toString(), "displayPreferencesId" to displayPreferencesId)
					)
				},
				modifier = Modifier.focusKey(Routes.LIBRARIES_DISPLAY_IMAGE_SIZE)
			)
		}
	}
}
