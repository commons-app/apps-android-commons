package fr.free.nrw.commons.nearby.model

import fr.free.nrw.commons.location.LatLng

/**
 * The area to search for nearby places, and whether to only find places that need a photo
 */
sealed class NearbyQueryParams(
    val onlyNeedsPhoto: Boolean,
) {
    class Rectangular
        @JvmOverloads
        constructor(
            val screenTopRight: LatLng,
            val screenBottomLeft: LatLng,
            onlyNeedsPhoto: Boolean = false,
        ) : NearbyQueryParams(onlyNeedsPhoto)

    class Radial
        @JvmOverloads
        constructor(
            val center: LatLng,
            val radiusInKm: Float,
            onlyNeedsPhoto: Boolean = false,
        ) : NearbyQueryParams(onlyNeedsPhoto)

    /**
     * Fills the picture filter of a nearby query: skips places that already have a picture
     * (P18) when [onlyNeedsPhoto] is set, so they are never fetched.
     */
    fun withPictureFilter(query: String): String =
        query.replace(PICTURE_FILTER, if (onlyNeedsPhoto) "MINUS {?item wdt:P18 ?image}" else "")

    companion object {
        const val PICTURE_FILTER = "\${PICTURE_FILTER}"
    }
}
