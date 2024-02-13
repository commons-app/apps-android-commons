package fr.free.nrw.commons.upload

import android.os.Parcelable
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import kotlinx.parcelize.Parcelize

@Parcelize
data class WikidataPlace(
    override val id: String,
    override val name: String,
    val imageValue: String?,
    val wikipediaArticle: String?,
    val location: LatLng? = null,
    var isMonumentUpload : Boolean =false
) :
    WikidataItem, Parcelable {
    constructor(place: Place) : this(
        place.wikiDataEntityId!!,
        place.name,
        place.pic.takeIf { it.isNotBlank() },
        place.siteLinks.wikipediaLink?.toString() ?: "",
        place.location,
        isMonumentUpload=place.isMonument
    )

    companion object {
        @JvmStatic
        fun from(place: Place?): WikidataPlace? {
            return place?.let { WikidataPlace(it) }
        }
    }

    fun getWikipediaPageTitle(): String? {
        return wikipediaArticle?.substringAfterLast("/")
    }
}
