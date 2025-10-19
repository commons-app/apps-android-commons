package fr.free.nrw.commons.explore.map

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils.addRedBorder
import fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween
import fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween
import fr.free.nrw.commons.utils.LocationUtils.calculateDistance
import fr.free.nrw.commons.utils.PlaceUtils.mediaToExplorePlace
import timber.log.Timber
import javax.inject.Inject

class ExploreMapController @Inject constructor(
    private val exploreMapCalls: ExploreMapCalls
) : MapController() {
    // Can be current and camera target on search this area button is used
    private var latestSearchLocation: LatLng? = null

    // Any last search radius
    private var latestSearchRadius: Double = 0.0

    // Search radius of only searches around current location
    private var currentLocationSearchRadius: Double = 0.0

    @JvmField
    // current location of user
    var currentLocation: LatLng? = null

    /**
     * Takes location as parameter and returns ExplorePlaces info that holds currentLatLng, mediaList,
     * explorePlaceList and boundaryCoordinates
     *
     * @param currentLatLng                     is current geolocation
     * @param searchLatLng                  is the location that we want to search around
     * @param checkingAroundCurrentLocation is a boolean flag. True if we want to check around
     * current location, false if another location
     * @return explorePlacesInfo info that holds currentLatLng, mediaList, explorePlaceList and
     * boundaryCoordinates
     */
    fun loadAttractionsFromLocation(
        currentLatLng: LatLng?, searchLatLng: LatLng?,
        checkingAroundCurrentLocation: Boolean
    ): ExplorePlacesInfo? {
        if (searchLatLng == null) {
            Timber.d("Loading attractions explore map, but search is null")
            return null
        }

        val explorePlacesInfo = ExplorePlacesInfo()
        try {
            explorePlacesInfo.currentLatLng = currentLatLng
            latestSearchLocation = searchLatLng

            val mediaList = exploreMapCalls.callCommonsQuery(searchLatLng)
            val boundaryCoordinates = arrayOf(
                mediaList[0].coordinates!!,  // south
                mediaList[0].coordinates!!,  // north
                mediaList[0].coordinates!!,  // west
                mediaList[0].coordinates!!
            ) // east, init with a random location

            Timber.d("Sorting places by distance...")
            val distances: MutableMap<Media, Double> = HashMap()
            for (media in mediaList) {
                distances[media] = computeDistanceBetween(media.coordinates!!, searchLatLng)
                // Find boundaries with basic find max approach
                if (media.coordinates!!.latitude
                    < boundaryCoordinates[0]!!.latitude
                ) {
                    boundaryCoordinates[0] = media.coordinates!!
                }
                if (media.coordinates!!.latitude
                    > boundaryCoordinates[1]!!.latitude
                ) {
                    boundaryCoordinates[1] = media.coordinates!!
                }
                if (media.coordinates!!.longitude
                    < boundaryCoordinates[2]!!.longitude
                ) {
                    boundaryCoordinates[2] = media.coordinates!!
                }
                if (media.coordinates!!.longitude
                    > boundaryCoordinates[3]!!.longitude
                ) {
                    boundaryCoordinates[3] = media.coordinates!!
                }
            }
            explorePlacesInfo.mediaList = mediaList
            explorePlacesInfo.explorePlaceList = mediaToExplorePlace(mediaList)
            explorePlacesInfo.boundaryCoordinates = boundaryCoordinates

            // Sets latestSearchRadius to maximum distance among boundaries and search location
            for ((latitude, longitude) in boundaryCoordinates) {
                val distance = calculateDistance(
                    latitude,
                    longitude, searchLatLng.latitude, searchLatLng.longitude
                )
                if (distance > latestSearchRadius) {
                    latestSearchRadius = distance
                }
            }

            // Our radius searched around us, will be used to understand when user search their own location, we will follow them
            if (checkingAroundCurrentLocation) {
                currentLocationSearchRadius = latestSearchRadius
                currentLocation = currentLatLng
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return explorePlacesInfo
    }

    interface NearbyBaseMarkerThumbCallback {
        // Callback to notify thumbnails of explore markers are added as icons and ready
        fun onNearbyBaseMarkerThumbsReady(
            baseMarkers: List<BaseMarker>?,
            explorePlacesInfo: ExplorePlacesInfo?
        )
    }

    companion object {
        /**
         * Loads attractions from location for map view, we need to return places in Place data type
         *
         * @return baseMarkerOptions list that holds nearby places with their icons
         */
        fun loadAttractionsFromLocationToBaseMarkerOptions(
            currentLatLng: LatLng?,
            placeList: List<Place>?,
            context: Context,
            callback: NearbyBaseMarkerThumbCallback,
            explorePlacesInfo: ExplorePlacesInfo?
        ): List<BaseMarker> {
            val baseMarkerList: MutableList<BaseMarker> = ArrayList()

            if (placeList == null) {
                return baseMarkerList
            }

            var vectorDrawable: VectorDrawableCompat? = null
            try {
                vectorDrawable = VectorDrawableCompat.create(
                    context.resources, R.drawable.ic_custom_map_marker_dark, context.theme
                )
            } catch (e: Resources.NotFoundException) {
                // ignore when running tests.
            }
            if (vectorDrawable != null) {
                for (explorePlace in placeList) {
                    val baseMarker = BaseMarker()
                    val distance = formatDistanceBetween(currentLatLng, explorePlace.location)
                    explorePlace.setDistance(distance)

                    baseMarker.title =
                        explorePlace.name.substring(5, explorePlace.name.lastIndexOf("."))
                    baseMarker.position = LatLng(
                        explorePlace.location.latitude,
                        explorePlace.location.longitude, 0f
                    )
                    baseMarker.place = explorePlace

                    Glide.with(context)
                        .asBitmap()
                        .load(explorePlace.thumb)
                        .placeholder(R.drawable.image_placeholder_96)
                        .apply(RequestOptions().override(96, 96).centerCrop())
                        .into(object : CustomTarget<Bitmap>() {
                            // We add icons to markers when bitmaps are ready
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                baseMarker.icon = addRedBorder(resource, 6, context)
                                baseMarkerList.add(baseMarker)
                                if (baseMarkerList.size == placeList.size) {
                                    // if true, we added all markers to list and can trigger thumbs ready callback
                                    callback.onNearbyBaseMarkerThumbsReady(
                                        baseMarkerList,
                                        explorePlacesInfo
                                    )
                                }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) = Unit

                            // We add thumbnail icon for images that couldn't be loaded
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)
                                baseMarker.fromResource(context, R.drawable.image_placeholder_96)
                                baseMarkerList.add(baseMarker)
                                if (baseMarkerList.size == placeList.size) {
                                    // if true, we added all markers to list and can trigger thumbs ready callback
                                    callback.onNearbyBaseMarkerThumbsReady(
                                        baseMarkerList,
                                        explorePlacesInfo
                                    )
                                }
                            }
                        })
                }
            }
            return baseMarkerList
        }
    }
}
