package fr.free.nrw.commons.explore.map

import android.location.Location
import android.view.View
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.explore.map.ExploreMapController.Companion.loadAttractionsFromLocationToBaseMarkerOptions
import fr.free.nrw.commons.explore.map.ExploreMapController.NearbyBaseMarkerThumbCallback
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.Place
import io.reactivex.Observable
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Callable

class ExploreMapPresenter(
    var bookmarkLocationDao: BookmarkLocationsDao
) : ExploreMapContract.UserActions, NearbyBaseMarkerThumbCallback {

    private var isNearbyLocked = false
    private val currentLatLng: LatLng? = null
    private var exploreMapController: ExploreMapController? = null
    private var exploreMapFragmentView: ExploreMapContract.View? = DUMMY

    override fun updateMap(locationChangeType: LocationChangeType) {
        Timber.d("Presenter updates map and list$locationChangeType")
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns")
            return
        }

        if (!exploreMapFragmentView!!.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established")
            return
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED) {
            Timber.d("LOCATION_SIGNIFICANTLY_CHANGED")
            var populateLatLng = exploreMapFragmentView!!.getMapCenter()

            //If "Show in Explore" was selected in Nearby, use the previous LatLng
            if (exploreMapFragmentView is ExploreMapFragment) {
                val exploreMapFragment = exploreMapFragmentView as ExploreMapFragment
                if (exploreMapFragment.recentlyCameFromNearbyMap()) {
                    //Ensure this LatLng will not be used again if user searches their GPS location
                    exploreMapFragment.setRecentlyCameFromNearbyMap(false)

                    populateLatLng = exploreMapFragment.previousLatLng
                }
            }

            lockUnlockNearby(true)
            exploreMapFragmentView!!.setProgressBarVisibility(true)
            exploreMapFragmentView!!.populatePlaces(populateLatLng)
        } else if (locationChangeType == LocationChangeType.SEARCH_CUSTOM_AREA) {
            Timber.d("SEARCH_CUSTOM_AREA")
            lockUnlockNearby(true)
            exploreMapFragmentView!!.setProgressBarVisibility(true)
            exploreMapFragmentView!!.populatePlaces(exploreMapFragmentView!!.getMapFocus())
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly")
        }
    }

    /**
     * Nearby updates takes time, since they are network operations. During update time, we don't
     * want to get any other calls from user. So locking nearby.
     *
     * @param isNearbyLocked true means lock, false means unlock
     */
    override fun lockUnlockNearby(isNearbyLocked: Boolean) {
        this.isNearbyLocked = isNearbyLocked
        if (isNearbyLocked) {
            exploreMapFragmentView!!.disableFABRecenter()
        } else {
            exploreMapFragmentView!!.enableFABRecenter()
        }
    }

    override fun attachView(view: ExploreMapContract.View?) {
        exploreMapFragmentView = view
    }

    override fun detachView() {
        exploreMapFragmentView = DUMMY
    }

    /**
     * Sets click listener of FAB
     */
    override fun setActionListeners(applicationKvStore: JsonKvStore?) {
        exploreMapFragmentView!!.setFABRecenterAction {
            exploreMapFragmentView!!.recenterMap(currentLatLng)
        }
    }

    override fun backButtonClicked(): Boolean =
        exploreMapFragmentView!!.backButtonClicked()

    fun onMapReady(exploreMapController: ExploreMapController?) {
        this.exploreMapController = exploreMapController
        if (null != exploreMapFragmentView) {
            exploreMapFragmentView!!.addSearchThisAreaButtonAction()
            initializeMapOperations()
        }
    }

    fun initializeMapOperations() {
        lockUnlockNearby(false)
        updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    fun loadAttractionsFromLocation(
        currentLatLng: LatLng?,
        searchLatLng: LatLng?, checkingAroundCurrent: Boolean
    ): Observable<ExplorePlacesInfo?> = Observable.fromCallable(Callable {
        exploreMapController!!.loadAttractionsFromLocation(
            currentLatLng,
            searchLatLng,
            checkingAroundCurrent
        )
    })

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param explorePlacesInfo This variable has placeToCenter list information and distances.
     */
    fun updateMapMarkers(
        explorePlacesInfo: ExplorePlacesInfo
    ) {
        if (explorePlacesInfo.mediaList != null) {
            prepareNearbyBaseMarkers(explorePlacesInfo)
        } else {
            lockUnlockNearby(false) // So that new location updates wont come
            exploreMapFragmentView!!.setProgressBarVisibility(false)
        }
    }

    private fun prepareNearbyBaseMarkers(explorePlacesInfo: ExplorePlacesInfo) {
        loadAttractionsFromLocationToBaseMarkerOptions(
            explorePlacesInfo.currentLatLng,  // Curlatlang will be used to calculate distances
            explorePlacesInfo.explorePlaceList,
            exploreMapFragmentView!!.getContext()!!,
            this,
            explorePlacesInfo
        )
    }

    override fun onNearbyBaseMarkerThumbsReady(
        baseMarkers: List<BaseMarker>?,
        explorePlacesInfo: ExplorePlacesInfo?
    ) {
        if (null != exploreMapFragmentView) {
            exploreMapFragmentView!!.addMarkersToMap(baseMarkers)
            lockUnlockNearby(false) // So that new location updates wont come
            exploreMapFragmentView!!.setProgressBarVisibility(false)
        }
    }

    fun onSearchThisAreaClicked(): View.OnClickListener {
        return View.OnClickListener {
            // Lock map operations during search this area operation
            exploreMapFragmentView!!.setSearchThisAreaButtonVisibility(false)
            updateMap(if (searchCloseToCurrentLocation()) {
                LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
            } else {
                LocationChangeType.SEARCH_CUSTOM_AREA
            })
        }
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    private fun searchCloseToCurrentLocation(): Boolean {
        if (null == exploreMapFragmentView!!.getLastMapFocus()) {
            return true
        }

        val mylocation = Location("").apply {
            latitude = exploreMapFragmentView!!.getLastMapFocus()!!.latitude
            longitude = exploreMapFragmentView!!.getLastMapFocus()!!.longitude
        }
        val dest_location = Location("").apply {
            latitude = exploreMapFragmentView!!.getMapFocus()!!.latitude
            longitude = exploreMapFragmentView!!.getMapFocus()!!.longitude
        }

        val distance = mylocation.distanceTo(dest_location)

        return !(distance > 2000.0 * 3 / 4)
    }

    companion object {
        private val DUMMY = Proxy.newProxyInstance(
            ExploreMapContract.View::class.java.classLoader,
            arrayOf<Class<*>>(ExploreMapContract.View::class.java)
        ) { _: Any?, method: Method, _: Array<Any?>? ->
            when {
                method.name == "onMyEvent" -> null
                String::class.java == method.returnType -> ""
                Int::class.java == method.returnType -> 0
                Int::class.javaPrimitiveType == method.returnType -> 0
                Boolean::class.java == method.returnType -> java.lang.Boolean.FALSE
                Boolean::class.javaPrimitiveType == method.returnType -> false
                else -> null
            }
        } as ExploreMapContract.View
    }
}
