package fr.free.nrw.commons.explore.map

import android.location.Location
import android.view.View
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import io.reactivex.Observable
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Callable

class ExploreMapPresenter private constructor(
    var bookmarkLocationDao: BookmarkLocationsDao,
    private val markerHandler: ExploreMapMarkerHandler,
) : ExploreMapContract.UserActions,
    ExploreMapMarkerActions by markerHandler {
    /** Keeps presenter construction unchanged while giving marker callbacks a separate owner. */
    constructor(bookmarkLocationDao: BookmarkLocationsDao) : this(bookmarkLocationDao, ExploreMapMarkerHandler())

    private var isNearbyLocked = false
    private val currentLatLng: LatLng? = null
    private var exploreMapController: ExploreMapController? = null
    private var exploreMapFragmentView: ExploreMapContract.View? = DUMMY

    init {
        markerHandler.bind({ exploreMapFragmentView }, ::lockUnlockNearby)
    }

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

            // If "Show in Explore" was selected in Nearby, use the previous LatLng
            if (exploreMapFragmentView is ExploreMapFragment) {
                val exploreMapFragment = exploreMapFragmentView as ExploreMapFragment
                if (exploreMapFragment.recentlyCameFromNearbyMap()) {
                    // Ensure this LatLng will not be used again if user searches their GPS location
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

    override fun backButtonClicked(): Boolean = exploreMapFragmentView!!.backButtonClicked()

    /** Starts the initial map search after registering the controller and search-area action. */
    fun onMapReady(exploreMapController: ExploreMapController?) {
        this.exploreMapController = exploreMapController
        if (null != exploreMapFragmentView) {
            exploreMapFragmentView!!.addSearchThisAreaButtonAction()
            lockUnlockNearby(false)
            updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        }
    }

    fun loadAttractionsFromLocation(
        currentLatLng: LatLng?,
        searchLatLng: LatLng?,
        checkingAroundCurrent: Boolean,
    ): Observable<ExplorePlacesInfo?> =
        Observable.fromCallable(
            Callable {
                exploreMapController!!.loadAttractionsFromLocation(
                    currentLatLng,
                    searchLatLng,
                    checkingAroundCurrent,
                )
            },
        )

    fun onSearchThisAreaClicked(): View.OnClickListener =
        View.OnClickListener {
            // Lock map operations during search this area operation
            exploreMapFragmentView!!.setSearchThisAreaButtonVisibility(false)
            updateMap(
                if (searchCloseToCurrentLocation()) {
                    LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
                } else {
                    LocationChangeType.SEARCH_CUSTOM_AREA
                },
            )
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

        val mylocation =
            Location("").apply {
                latitude = exploreMapFragmentView!!.getLastMapFocus()!!.latitude
                longitude = exploreMapFragmentView!!.getLastMapFocus()!!.longitude
            }
        val destLocation =
            Location("").apply {
                latitude = exploreMapFragmentView!!.getMapFocus()!!.latitude
                longitude = exploreMapFragmentView!!.getMapFocus()!!.longitude
            }

        val distance = mylocation.distanceTo(destLocation)

        return !(distance > 2000.0 * 3 / 4)
    }

    companion object {
        private val DUMMY =
            Proxy.newProxyInstance(
                ExploreMapContract.View::class.java.classLoader,
                arrayOf<Class<*>>(ExploreMapContract.View::class.java),
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
