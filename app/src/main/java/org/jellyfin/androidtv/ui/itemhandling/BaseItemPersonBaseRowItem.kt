package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.util.apiclient.primaryImage
import org.jellyfin.sdk.model.api.BaseItemPerson

class BaseItemPersonBaseRowItem(
	val person: BaseItemPerson,
) : BaseRowItem(
	baseRowType = BaseRowType.Person,
	staticHeight = true,
) {
	override fun getImage(imageType: ImageType) = person.primaryImage
	override val itemId get() = person.id
	override fun getFullName(context: Context) = person.name
	// KFIN: cards show the character's name (role), not the actor's real name
	override fun getCardName(context: Context) = person.role ?: person.name
	override fun getName(context: Context) = person.name
	override fun getSubText(context: Context) = person.role
}
