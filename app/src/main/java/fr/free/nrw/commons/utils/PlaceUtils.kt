package fr.free.nrw.commons.utils

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks

object PlaceUtils {

    @JvmStatic
    fun latLngFromPointString(pointString: String): LatLng? {
        val matcher = Regex("Point\\(([^ ]+) ([^ ]+)\\)").find(pointString) ?: return null
        return try {
            val longitude = matcher.groupValues[1].toDouble()
            val latitude = matcher.groupValues[2].toDouble()
            LatLng(latitude, longitude, 0.0F)
        } catch (e: NumberFormatException) {
            null
        }
    }

    @JvmStatic
    fun mediaToExplorePlace(mediaList: List<Media>): List<Place> {
        val explorePlaceList = mutableListOf<Place>()
        for (media in mediaList) {
            val place = Place(
                media.filename ?: "",
                media.fallbackDescription ?: "",
                media.coordinates,
                media.categories.toString(),
                Sitelinks.Builder()
                    .setCommonsLink(media.pageTitle?.canonicalUri ?: "")
                    .setWikipediaLink("")
                    .setWikidataLink("")
                    .build(),
                media.imageUrl ?: "",
                media.thumbUrl ?: "",
                ""
            )
            // Set caption, with fallback
            place.caption = media.captions?.values?.firstOrNull()
                ?: media.filename?.removePrefix("File:")?.replace('_', ' ')
                ?: "Unknown"

            explorePlaceList.add(place)
        }
        return explorePlaceList
    }
}
