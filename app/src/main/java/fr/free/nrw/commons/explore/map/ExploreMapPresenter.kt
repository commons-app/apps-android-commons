package fr.free.nrw.commons.explore.map

import android.location.Location
import android.view.View
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import io.reactivex.Observable
import timber.log.Timber
import java.lang.reflect.Proxy

class ExploreMapPresenter(
    private val bookmarkLocationDao: BookmarkLocationsDao
) : ExploreMapContract.UserActions, ExploreMapController.NearbyBaseMarkerThumbCallback {

    private var isNearbyLocked: Boolean = false
    private var currentLatLng: LatLng? = null
    private var exploreMapController: ExploreMapController? = null

    companion object {
        private val DUMMY: ExploreMapContract.View = Proxy.newProxyInstance(
            ExploreMapContract.View::class.java.classLoader,
            arrayOf(ExploreMapContract.View::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                String::class.java -> ""
                Integer::class.java -> 0
                Int::class.java -> 0
                Boolean::class.java -> false
                Boolean::class.javaPrimitiveType -> false
                else -> null
            }
        } as ExploreMapContract.View
    }

    private var exploreMapFragmentView: ExploreMapContract.View = DUMMY

    override fun updateMap(locationChangeType: LocationChangeType) {
        Timber.d("Presenter updates map and list $locationChangeType")
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns")
            return
        }

        if (!exploreMapFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established")
            return
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        when (locationChangeType) {
            LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED -> {
                Timber.d("LOCATION_SIGNIFICANTLY_CHANGED")
                lockUnlockNearby(true)
                exploreMapFragmentView.setProgressBarVisibility(true)
                exploreMapFragmentView.populatePlaces(exploreMapFragmentView.getMapCenter())
            }

            LocationChangeType.SEARCH_CUSTOM_AREA -> {
                Timber.d("SEARCH_CUSTOM_AREA")
                lockUnlockNearby(true)
                exploreMapFragmentView.setProgressBarVisibility(true)
                exploreMapFragmentView.populatePlaces(exploreMapFragmentView.getMapFocus())
            }

            else -> {
                Timber.d("Means location changed slightly")
            }
        }
    }

    /**
     * Nearby updates take time since they are network operations. During update time, we don't
     * want to get any other calls from the user. So locking nearby.
     *
     * @param isNearbyLocked true means lock, false means unlock
     */
    override fun lockUnlockNearby(isNearbyLocked: Boolean) {
        this.isNearbyLocked = isNearbyLocked
        if (isNearbyLocked) {
            exploreMapFragmentView.disableFABRecenter()
        } else {
            exploreMapFragmentView.enableFABRecenter()
        }
    }

    override fun attachView(view: ExploreMapContract.View) {
        exploreMapFragmentView = view
    }

    override fun detachView() {
        exploreMapFragmentView = DUMMY
    }

    /**
     * Sets click listener of FAB
     */
    override fun setActionListeners(applicationKvStore: JsonKvStore) {
        exploreMapFragmentView.setFABRecenterAction {
            currentLatLng?.let { it1 -> exploreMapFragmentView.recenterMap(it1) }
        }
    }

    override fun backButtonClicked(): Boolean {
        return exploreMapFragmentView.backButtonClicked()
    }

    fun onMapReady(exploreMapController: ExploreMapController) {
        this.exploreMapController = exploreMapController
        exploreMapFragmentView.addSearchThisAreaButtonAction()
        initializeMapOperations()
    }

    fun initializeMapOperations() {
        lockUnlockNearby(false)
        updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    fun loadAttractionsFromLocation(
        currentLatLng: LatLng,
        searchLatLng: LatLng?,
        checkingAroundCurrent: Boolean
    ): Observable<ExplorePlacesInfo> {
        return Observable.fromCallable {
            exploreMapController?.loadAttractionsFromLocation(
                currentLatLng, searchLatLng, checkingAroundCurrent
            )
        }
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param explorePlacesInfo This variable has placeToCenter list information and distances.
     */
    fun updateMapMarkers(explorePlacesInfo: MapController.ExplorePlacesInfo) {
        if (explorePlacesInfo.mediaList != null) {
            prepareNearbyBaseMarkers(explorePlacesInfo)
        } else {
            lockUnlockNearby(false) // So that new location updates won't come
            exploreMapFragmentView.setProgressBarVisibility(false)
        }
    }

    private fun prepareNearbyBaseMarkers(explorePlacesInfo: MapController.ExplorePlacesInfo) {
        ExploreMapController.loadAttractionsFromLocationToBaseMarkerOptions(
            explorePlacesInfo.currentLatLng,
            explorePlacesInfo.explorePlaceList,
            exploreMapFragmentView.getContext(),
            this,
            explorePlacesInfo
        )
    }

    override fun onNearbyBaseMarkerThumbsReady(
        baseMarkers: List<BaseMarker>,
        explorePlacesInfo: ExplorePlacesInfo
    ) {
        exploreMapFragmentView.addMarkersToMap(baseMarkers)
        lockUnlockNearby(false) // So that new location updates won't come
        exploreMapFragmentView.setProgressBarVisibility(false)
    }

    fun onSearchThisAreaClicked(): View.OnClickListener {
        return View.OnClickListener {
            // Lock map operations during search this area operation
            exploreMapFragmentView.setSearchThisAreaButtonVisibility(false)

            if (searchCloseToCurrentLocation()) {
                updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
            } else {
                updateMap(LocationChangeType.SEARCH_CUSTOM_AREA)
            }
        }
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    fun searchCloseToCurrentLocation(): Boolean {
        val lastMapFocus = exploreMapFragmentView.getLastMapFocus() ?: return true

        val myLocation = Location("").apply {
            latitude = lastMapFocus.latitude
            longitude = lastMapFocus.longitude
        }

        val destLocation = Location("").apply {
            latitude = exploreMapFragmentView.getMapFocus().latitude
            longitude = exploreMapFragmentView.getMapFocus().longitude
        }

        return myLocation.distanceTo(destLocation) <= 2000.0 * 3 / 4
    }
}
