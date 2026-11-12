package org.jellyfin.androidtv.ui.home.carousel

import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemBackdropImages
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ImageType
import java.util.UUID

data class FeaturedCarouselItem(
	val id: UUID,
	val title: String,
	val overview: String,
	val imageUrl: String?,
	val logoUrl: String?,
	val productionYear: Int?,
	val runtimeTicks: Long?,
	val communityRating: Float?,
	val parentalRating: String?,
) {
	val runtimeText: String
		get() {
			val runtimeTicks = runtimeTicks ?: return ""
			val totalMinutes = runtimeTicks / TICKS_PER_MINUTE
			val hours = totalMinutes / MINUTES_PER_HOUR
			val remainingMinutes = totalMinutes % MINUTES_PER_HOUR

			return when {
				hours > 0 -> "$hours ч $remainingMinutes мин"
				totalMinutes > 0 -> "$totalMinutes мин"
				else -> ""
			}
		}

	companion object {
		private const val TICKS_PER_MINUTE = 600_000_000L
		private const val MINUTES_PER_HOUR = 60L

		fun fromBaseItemDto(item: BaseItemDto, api: ApiClient): FeaturedCarouselItem? {
			val image = item.itemBackdropImages.firstOrNull()
				?: item.itemImages[ImageType.THUMB]
				?: item.parentImages[ImageType.THUMB]
				?: item.itemImages[ImageType.PRIMARY]
			val logo = item.itemImages[ImageType.LOGO] ?: item.parentImages[ImageType.LOGO]

			return FeaturedCarouselItem(
				id = item.id,
				title = item.name.orEmpty(),
				overview = item.overview.orEmpty(),
				imageUrl = image?.getUrl(api, maxWidth = 1920, maxHeight = 1080),
				logoUrl = logo?.getUrl(api, maxWidth = 600),
				productionYear = item.productionYear,
				runtimeTicks = item.runTimeTicks,
				communityRating = item.communityRating,
				parentalRating = item.officialRating,
			)
		}
	}
}
