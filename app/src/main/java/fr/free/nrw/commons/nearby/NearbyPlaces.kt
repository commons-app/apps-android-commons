package fr.free.nrw.commons.nearby

import android.location.Location
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.nearby.model.NearbyQueryParams.Radial
import fr.free.nrw.commons.nearby.model.NearbyQueryParams.Rectangular
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Handles the Wikidata query to obtain Places around search location.
 * Reads Wikidata query to check nearby wikidata items which needs picture, with a circular
 * search. As a point is center of a circle with a radius will be set later.
 */
@Singleton
class NearbyPlaces @Inject constructor(private val okHttpJsonApiClient: OkHttpJsonApiClient) {
    var radius: Double = INITIAL_RADIUS

    /**
     * Expands the radius as needed for the Wikidata query
     */
    @Throws(Exception::class)
    fun radiusExpander(
        currentLatLng: LatLng, lang: String, returnClosestResult: Boolean, customQuery: String?
    ): List<Place>? {
        val minResults: Int
        val maxRadius: Double

        var places: List<Place>? = emptyList()

        // If returnClosestResult is true, then this means that we are trying to get closest point
        // to use in cardView in Contributions fragment
        if (returnClosestResult) {
            minResults = 1 // Return closest nearby place
            maxRadius = 5.0 // Return places only in 5 km area
            radius = INITIAL_RADIUS // refresh radius again, otherwise increased radius is grater than MAX_RADIUS, thus returns null
        } else {
            minResults = 20
            maxRadius = 300.0 // in kilometers
            radius = INITIAL_RADIUS
        }

        // Increase the radius gradually to find a satisfactory number of nearby places
        while (radius <= maxRadius) {
            places = getFromWikidataQuery(currentLatLng, lang, radius, customQuery)
            val size = places?.size ?: 0
            Timber.d("%d results at radius: %f", size, radius)
            if (size >= minResults) {
                break
            }
            radius *= RADIUS_MULTIPLIER
        }
        // make sure we will be able to send at least one request next time
        if (radius > maxRadius) {
            radius = maxRadius
        }
        return places
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     */
    @Throws(Exception::class)
    fun getFromWikidataQuery(
        cur: LatLng, lang: String, radius: Double, customQuery: String?
    ): List<Place>? = okHttpJsonApiClient.getNearbyPlaces(cur, lang, radius, customQuery)

    /**
     * Retrieves a list of places from a Wikidata query based on screen coordinates and optional
     * parameters.
     */
    @Throws(Exception::class)
    fun getFromWikidataQuery(
        centerPoint: LatLng,
        screenTopRight: LatLng,
        screenBottomLeft: LatLng,
        lang: String,
        shouldQueryForMonuments: Boolean,
        customQuery: String?
    ): List<Place>? {
        if (customQuery != null) {
            val nearbyPlaces: List<Place>? = okHttpJsonApiClient.getNearbyPlaces(
                Rectangular(screenTopRight, screenBottomLeft),
                lang,
                shouldQueryForMonuments,
                customQuery
            )
            return nearbyPlaces ?: listOf()
        }

        val lowerLimit = 1000
        val upperLimit = 1500

        val results = FloatArray(1)
        Location.distanceBetween(
            centerPoint.latitude,
            screenTopRight.longitude,
            centerPoint.latitude,
            screenBottomLeft.longitude,
            results
        )
        val longGap = results[0] / 1000f
        Location.distanceBetween(
            screenTopRight.latitude,
            centerPoint.longitude,
            screenBottomLeft.latitude,
            centerPoint.longitude,
            results
        )
        val latGap = results[0] / 1000f

        if (max(longGap, latGap) < 100f) {
            val itemCount = okHttpJsonApiClient.getNearbyItemCount(
                Rectangular(screenTopRight, screenBottomLeft)
            )
            if (itemCount < upperLimit) {
                val nearbyPlaces: List<Place>? = okHttpJsonApiClient.getNearbyPlaces(
                    Rectangular(screenTopRight, screenBottomLeft),
                    lang,
                    shouldQueryForMonuments,
                    null
                )
                return nearbyPlaces ?: emptyList()
            }
        }

        // minRadius, targetRadius and maxRadius are radii in decameters
        // unlike other radii here, which are in kilometers, to avoid looping over
        // floating point values
        var minRadius = 0
        var maxRadius = Math.round(min(300f, min(longGap, latGap))) * 100
        var targetRadius = maxRadius / 2
        while (minRadius < maxRadius) {
            targetRadius = minRadius + (maxRadius - minRadius + 1) / 2
            val itemCount = okHttpJsonApiClient.getNearbyItemCount(
                Radial(centerPoint, targetRadius / 100f)
            )
            if (itemCount >= lowerLimit && itemCount < upperLimit) {
                break
            }
            if (targetRadius > maxRadius / 2 && itemCount < lowerLimit / 5) { // fast forward
                minRadius = targetRadius
                targetRadius = minRadius + (maxRadius - minRadius + 1) / 2
                minRadius = targetRadius
                if (itemCount < lowerLimit / 10 && minRadius < maxRadius) { // fast forward again
                    targetRadius = minRadius + (maxRadius - minRadius + 1) / 2
                    minRadius = targetRadius
                }
                continue
            }
            if (itemCount < upperLimit) {
                minRadius = targetRadius
            } else {
                maxRadius = targetRadius - 1
            }
        }
        val nearbyPlaces: List<Place>? = okHttpJsonApiClient.getNearbyPlaces(
            Radial(centerPoint, targetRadius / 100f), lang, shouldQueryForMonuments, null
        )
        return nearbyPlaces ?: emptyList()
    }

    /**
     * Retrieves a list of places based on the provided list of places and language.
     * This method fetches place information from a Wikidata query using the specified language.
     */
    @Throws(Exception::class)
    fun getPlaces(
        placeList: List<Place>?, lang: String
    ): List<Place>? = okHttpJsonApiClient.getPlaces(placeList ?: emptyList(), lang)

    /**
     * Runs the Wikidata query to retrieve the KML String
     */
    @Throws(Exception::class)
    fun getPlacesAsKML(leftLatLng: LatLng, rightLatLng: LatLng): String? =
        okHttpJsonApiClient.getPlacesAsKML(leftLatLng, rightLatLng)

    /**
     * Runs the Wikidata query to retrieve the GPX String
     */
    @Throws(Exception::class)
    fun getPlacesAsGPX(leftLatLng: LatLng, rightLatLng: LatLng): String? =
        okHttpJsonApiClient.getPlacesAsGPX(leftLatLng, rightLatLng)

    companion object {
        private const val INITIAL_RADIUS = 0.3 // in kilometers
        private const val RADIUS_MULTIPLIER = 2.0
    }
}
