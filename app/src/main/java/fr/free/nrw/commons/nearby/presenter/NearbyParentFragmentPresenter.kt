package fr.free.nrw.commons.nearby.presenter

import android.location.Location
import android.view.View
import androidx.annotation.MainThread
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.LocationUtils
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import timber.log.Timber
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class NearbyParentFragmentPresenter
    (var bookmarkLocationDao: BookmarkLocationsDao) : NearbyParentFragmentContract.UserActions,
    WikidataP18EditListener, LocationUpdateListener {
    private var isNearbyLocked = false
    private var currentLatLng: LatLng? = null

    private var placesLoadedOnce = false

    private var customQuery: String? = null

    private var nearbyParentFragmentView: NearbyParentFragmentContract.View = DUMMY

    override fun attachView(view: NearbyParentFragmentContract.View) {
        this.nearbyParentFragmentView = view
    }

    override fun detachView() {
        this.nearbyParentFragmentView = DUMMY
    }

    override fun removeNearbyPreferences(applicationKvStore: JsonKvStore) {
        Timber.d("Remove place objects")
        applicationKvStore.remove(PLACE_OBJECT)
    }

    fun initializeMapOperations() {
        lockUnlockNearby(false)
        updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        nearbyParentFragmentView.setCheckBoxAction()
    }

    /**
     * Sets click listeners of FABs, and 2 bottom sheets
     */
    override fun setActionListeners(applicationKvStore: JsonKvStore) {
        nearbyParentFragmentView.setFABPlusAction(View.OnClickListener { v: View? ->
            if (applicationKvStore.getBoolean("login_skipped", false)) {
                // prompt the user to login
                nearbyParentFragmentView.displayLoginSkippedWarning()
            } else {
                nearbyParentFragmentView.animateFABs()
            }
        })

        nearbyParentFragmentView.setFABRecenterAction(View.OnClickListener { v: View? ->
            nearbyParentFragmentView.recenterMap(currentLatLng)
        })
    }

    override fun backButtonClicked(): Boolean {
        if (nearbyParentFragmentView.isAdvancedQueryFragmentVisible()) {
            nearbyParentFragmentView.showHideAdvancedQueryFragment(false)
            return true
        } else if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.listOptionMenuItemClicked()
            return true
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.setBottomSheetDetailsSmaller()
            return true
        }
        return false
    }

    fun markerUnselected() {
        nearbyParentFragmentView.hideBottomSheet()
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
            nearbyParentFragmentView.disableFABRecenter()
        } else {
            nearbyParentFragmentView.enableFABRecenter()
        }
    }

    /**
     * This method should be the single point to update Map and List. Triggered by location changes
     *
     * @param locationChangeType defines if location changed significantly or slightly
     */
    override fun updateMapAndList(locationChangeType: LocationChangeType) {
        Timber.d("Presenter updates map and list")
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns")
            return
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established")
            return
        }

        val lastLocation = nearbyParentFragmentView.getLastMapFocus()
        currentLatLng = if (nearbyParentFragmentView.getMapCenter() != null) {
            nearbyParentFragmentView.getMapCenter()
        } else {
            lastLocation
        }
        if (currentLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable")
            return
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType == LocationChangeType.CUSTOM_QUERY) {
            Timber.d("ADVANCED_QUERY_SEARCH")
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            var updatedLocationByUser = LocationUtils.deriveUpdatedLocationFromSearchQuery(
                customQuery!!
            )
            if (updatedLocationByUser == null) {
                updatedLocationByUser = lastLocation
            }
            nearbyParentFragmentView.populatePlaces(updatedLocationByUser, customQuery)
        } else if (locationChangeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
            || locationChangeType == LocationChangeType.MAP_UPDATED
        ) {
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapCenter())
        } else if (locationChangeType == LocationChangeType.SEARCH_CUSTOM_AREA) {
            Timber.d("SEARCH_CUSTOM_AREA")
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapFocus())
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly")
        }
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param nearbyPlaces This variable has placeToCenter list information and distances.
     */
    fun updateMapMarkers(
        nearbyPlaces: MutableList<Place?>?, currentLatLng: LatLng?,
        shouldTrackPosition: Boolean
    ) {

//            nearbyParentFragmentView.clearAllMarkers();
        val baseMarkers = NearbyController
            .loadAttractionsFromLocationToBaseMarkerOptions(
                currentLatLng,  // Curlatlang will be used to calculate distances
                nearbyPlaces
            )
        nearbyParentFragmentView.updateMapMarkers(baseMarkers)
        lockUnlockNearby(false) // So that new location updates wont come
        nearbyParentFragmentView.setProgressBarVisibility(false)
        nearbyParentFragmentView.updateListFragment(nearbyPlaces)

    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before map is
     * ready. So we will remember it when the map is ready
     */
    private fun handleCenteringTaskIfAny() {
        if (!placesLoadedOnce) {
            placesLoadedOnce = true
            nearbyParentFragmentView.centerMapToPlace(null)
        }
    }

    override fun onWikidataEditSuccessful() {
        updateMapAndList(LocationChangeType.MAP_UPDATED)
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) {
        Timber.d("Location significantly changed")
        updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    override fun onLocationChangedSlightly(latLng: LatLng) {
        Timber.d("Location significantly changed")
        updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
    }

    override fun onLocationChangedMedium(latLng: LatLng) {
        Timber.d("Location changed medium")
    }

    override fun filterByMarkerType(
        selectedLabels: MutableList<Label?>?, state: Int,
        filterForPlaceState: Boolean, filterForAllNoneType: Boolean
    ) {
        if (filterForAllNoneType) { // Means we will set labels based on states
            when (state) {
                CheckBoxTriStates.UNKNOWN -> {}
                CheckBoxTriStates.UNCHECKED -> {
                    //TODO
                    nearbyParentFragmentView.filterOutAllMarkers()
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut()
                }

                CheckBoxTriStates.CHECKED -> {
                    // Despite showing all labels NearbyFilterState still should be applied
                    nearbyParentFragmentView.filterMarkersByLabels(
                        selectedLabels,
                        filterForPlaceState, false
                    )
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected()
                }
            }
        } else {
            nearbyParentFragmentView.filterMarkersByLabels(
                selectedLabels,
                filterForPlaceState, false
            )
        }
    }

    @MainThread
    override fun updateMapMarkersToController(baseMarkers: MutableList<BaseMarker>) {
        NearbyController.markerLabelList.clear()
        for (i in baseMarkers.indices) {
            val nearbyBaseMarker = baseMarkers.get(i)
            NearbyController.markerLabelList.add(
                MarkerPlaceGroup(
                    bookmarkLocationDao.findBookmarkLocation(nearbyBaseMarker.place),
                    nearbyBaseMarker.place
                )
            )
        }
    }

    override fun setCheckboxUnknown() {
        nearbyParentFragmentView.setCheckBoxState(CheckBoxTriStates.UNKNOWN)
    }

    override fun setAdvancedQuery(query: String) {
        this.customQuery = query
    }

    override fun searchViewGainedFocus() {
        if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.hideBottomSheet()
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.hideBottomDetailsSheet()
        }
    }

    /**
     * Initiates a search for places within the area. Depending on whether the search
     * is close to the current location, the map and list are updated
     * accordingly.
     */
    fun searchInTheArea() {
        if (searchCloseToCurrentLocation()) {
            updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        } else {
            updateMapAndList(LocationChangeType.SEARCH_CUSTOM_AREA)
        }
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    fun searchCloseToCurrentLocation(): Boolean {
        if (null == nearbyParentFragmentView.getLastMapFocus()) {
            return true
        }
        //TODO
        val mylocation = Location("")
        val dest_location = Location("")
        dest_location.setLatitude(nearbyParentFragmentView.getMapFocus().latitude)
        dest_location.setLongitude(nearbyParentFragmentView.getMapFocus().longitude)
        mylocation.setLatitude(nearbyParentFragmentView.getLastMapFocus().latitude)
        mylocation.setLongitude(nearbyParentFragmentView.getLastMapFocus().longitude)
        val distance = mylocation.distanceTo(dest_location)

        return if (distance > 2000.0 * 3 / 4) {
            false
        } else {
            true
        }
    }

    fun onMapReady() {
        initializeMapOperations()
    }

    companion object {
        private val DUMMY = Proxy.newProxyInstance(
            NearbyParentFragmentContract.View::class.java.getClassLoader(),
            arrayOf<Class<*>>(NearbyParentFragmentContract.View::class.java),
            InvocationHandler { proxy: Any?, method: Method?, args: Array<Any?>? ->
                if (method!!.getName() == "onMyEvent") {
                    return@InvocationHandler null
                } else if (String::class.java == method.getReturnType()) {
                    return@InvocationHandler ""
                } else if (Int::class.java == method.getReturnType()) {
                    return@InvocationHandler 0
                } else if (Int::class.javaPrimitiveType == method.getReturnType()) {
                    return@InvocationHandler 0
                } else if (Boolean::class.java == method.getReturnType()) {
                    return@InvocationHandler java.lang.Boolean.FALSE
                } else if (Boolean::class.javaPrimitiveType == method.getReturnType()) {
                    return@InvocationHandler false
                } else {
                    return@InvocationHandler null
                }
            }
        ) as NearbyParentFragmentContract.View
    }
}
