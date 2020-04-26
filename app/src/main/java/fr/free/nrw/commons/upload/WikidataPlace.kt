package fr.free.nrw.commons.upload

import android.os.Parcelable
import fr.free.nrw.commons.nearby.Place
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class WikidataPlace(override val id: String, override val name: String, val imageValue: String?) :
    WikidataItem,Parcelable {
    constructor(place: Place) : this(
        place.wikiDataEntityId!!,
        place.name,
        place.pic.takeIf { it.isNotBlank() })

    companion object {
        @JvmStatic
        fun from(place: Place?): WikidataPlace? {
            return place?.let { WikidataPlace(it) }
        }
    }
}
