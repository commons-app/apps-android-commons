package fr.free.nrw.commons.upload

import android.os.Parcelable
import fr.free.nrw.commons.nearby.Place
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class WikidataPlace(
    override val id: String,
    override val name: String,
    val imageValue: String?,
    val wikipediaArticle: String?
) :
    WikidataItem, Parcelable {
    constructor(place: Place) : this(
        place.wikiDataEntityId!!,
        place.name,
        place.pic.takeIf { it.isNotBlank() },
        if (place.siteLinks.wikipediaLink == null) "" else place.siteLinks.wikipediaLink.toString()
    )

    companion object {
        @JvmStatic
        fun from(place: Place?): WikidataPlace? {
            return place?.let { WikidataPlace(it) }
        }
    }

    fun getWikipediaPageTitle(): String? {
        if (wikipediaArticle == null) {
            return null
        }
        val split: Array<String> = wikipediaArticle.split("/".toRegex()).toTypedArray()
        return split[split.size - 1]
    }
}
