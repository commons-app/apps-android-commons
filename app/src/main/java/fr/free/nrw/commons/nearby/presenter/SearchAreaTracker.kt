package fr.free.nrw.commons.nearby.presenter

import fr.free.nrw.commons.location.LatLng
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val LOADED_AREA_MAX_AGE_MINUTES = 5L
private const val SAME_AREA_TOLERANCE = 0.1 // share of the map width and height

/**
 * The map area a Nearby search was made for, as given by the corners of the visible map.
 */
internal data class SearchArea(
    val topRight: LatLng,
    val bottomLeft: LatLng,
) {
    /**
     * Returns true if both corners of [other] are within [SAME_AREA_TOLERANCE] of the map
     * width and height from the corners of this area, so a zoom or a real pan is not the same.
     */
    fun isSameAs(other: SearchArea): Boolean {
        val latTolerance = abs(topRight.latitude - bottomLeft.latitude) * SAME_AREA_TOLERANCE
        val lngTolerance = abs(topRight.longitude - bottomLeft.longitude) * SAME_AREA_TOLERANCE
        return abs(topRight.latitude - other.topRight.latitude) <= latTolerance &&
            abs(topRight.longitude - other.topRight.longitude) <= lngTolerance &&
            abs(bottomLeft.latitude - other.bottomLeft.latitude) <= latTolerance &&
            abs(bottomLeft.longitude - other.bottomLeft.longitude) <= lngTolerance
    }
}

/**
 * Remembers the area of the last Nearby search whose places are shown, so that the same area
 * is not searched again when the screen comes back or the map barely moved.
 *
 * Every new search forgets the previous result, so a remembered area always means the latest
 * search finished and showed its places. A search that failed, found nothing or was cancelled
 * is never reused.
 */
internal class SearchAreaTracker(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private var searchingArea: SearchArea? = null
    private var loadedArea: SearchArea? = null
    private var loadedTime = 0L

    /**
     * Called when a search starts.
     *
     * @param area The area searched, or null if the result must not be reused (custom query).
     */
    fun onSearchStarted(area: SearchArea?) {
        searchingArea = area
        loadedArea = null
    }

    /**
     * Called when the running search shows its places.
     */
    fun onPlacesShown() {
        loadedArea = searchingArea
        loadedTime = currentTimeMillis()
    }

    /**
     * Returns true if the places of [visibleArea] were shown less than
     * [LOADED_AREA_MAX_AGE_MINUTES] ago, so searching it again is not needed.
     */
    fun isLoaded(visibleArea: SearchArea?): Boolean =
        visibleArea != null &&
            loadedArea?.isSameAs(visibleArea) == true &&
            currentTimeMillis() - loadedTime <= TimeUnit.MINUTES.toMillis(LOADED_AREA_MAX_AGE_MINUTES)
}
