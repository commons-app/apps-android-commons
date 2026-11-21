package fr.free.nrw.commons.nearby.fragments

import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import java.util.ArrayList

private const val FALLBACK_BOUNDS_DELTA_DEGREES = 0.009
private const val LAST_LOCATION_KEY = "LastLocation"

/**
 * Encapsulates dependencies required by [NearbyPlacesQueryHelper].
 */
data class NearbyPlacesDependencies(
    val nearbyControllerProvider: () -> NearbyController? = { null },
    val placesRepositoryProvider: () -> PlacesRepository? = { null },
    val compositeDisposableProvider: () -> CompositeDisposable? = { null },
    val presenterProvider: () -> NearbyParentFragmentPresenter? = { null },
    val applicationKvStoreProvider: () -> JsonKvStore? = { null },
    val scopeProvider: () -> LifecycleCoroutineScope? = { null },
)

/**
 * Holds viewport coordinates used for querying nearby places.
 */
data class PlacesQueryViewport(
    val screenTopRight: LatLng? = null,
    val screenBottomLeft: LatLng? = null,
    val lastMapFocus: LatLng? = null,
    val mapFocus: LatLng? = null,
    val lastKnownLocation: LatLng? = null,
)

/**
 * Handles place searching, loading attractions from location, and cache clearing
 * for NearbyParentFragment.
 */
class NearbyPlacesQueryHelper : NearbyParentFragmentContract.PlacesQuery {
    /** Interface defining external actions needed for place queries. */
    interface PlacesQueryActions {
        fun getQueryViewport(): PlacesQueryViewport

        fun setLastMapFocus(geoPoint: GeoPoint?)

        fun isMonumentsEnabled(): Boolean

        fun isRecenterToUserLocation(): Boolean

        fun setRecenterToUserLocation(recenter: Boolean)

        fun showNoNearbyPlacesMessage()

        fun showErrorFetchingPlacesMessage(errorMessage: String)

        fun setProgressBarVisibility(isVisible: Boolean)

        fun setFilterState()
    }

    private var dependencies: NearbyPlacesDependencies = NearbyPlacesDependencies()
    private var actions: PlacesQueryActions? = null

    var lastFocusLocation: LatLng? = null
    var stopQuery: Boolean = false

    /**
     * Initializes the helper with required dependencies and action callbacks.
     */
    fun init(
        dependencies: NearbyPlacesDependencies,
        actions: PlacesQueryActions,
    ) {
        this.dependencies = dependencies
        this.actions = actions
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     *
     * @param currentLatLng Coordinates around which to search for places
     */
    override fun populatePlaces(currentLatLng: LatLng) {
        val queryActions = actions ?: return
        val viewport = queryActions.getQueryViewport()
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        val rawTopRight = viewport.screenBottomLeft
        val rawBottomLeft = viewport.screenTopRight

        // When the nearby fragment is opened immediately upon app launch, the {screenTopRightLatLng}
        // and {screenBottomLeftLatLng} variables return {LatLng(0.0,0.0)} as output.
        // To address this issue, A small delta value {delta = 0.02} is used to adjust the latitude
        // and longitude values for {ZOOM_LEVEL = 15f}.
        // This adjustment helps in calculating the east and west corner LatLng accurately.
        // Note: This only happens when the nearby fragment is opened immediately upon app launch,
        // otherwise {screenTopRightLatLng} and {screenBottomLeftLatLng} are used to determine
        // the east and west corner LatLng.
        val isInitialZeroBounds =
            rawTopRight?.latitude == 0.0 &&
                rawTopRight.longitude == 0.0 &&
                rawBottomLeft?.latitude == 0.0 &&
                rawBottomLeft.longitude == 0.0

        val bounds =
            if (isInitialZeroBounds) {
                calculateFallbackBounds(currentLatLng)
            } else {
                Pair(rawTopRight, rawBottomLeft)
            }

        val mapFocus = viewport.mapFocus
        if (currentLatLng.equals(viewport.lastMapFocus)) {
            // Means we are checking around current location
            populatePlacesForCurrentLocation(mapFocus, bounds.first, bounds.second, currentLatLng, null)
        } else {
            populatePlacesForAnotherLocation(mapFocus, bounds.first, bounds.second, currentLatLng, null)
        }

        if (queryActions.isRecenterToUserLocation()) {
            queryActions.setRecenterToUserLocation(false)
        }
    }

    /**
     * Populates places for your location with a custom query filter.
     *
     * @param currentLatLng Coordinates around which to search for places
     * @param customQuery Custom query string (or null/empty for standard search)
     */
    override fun populatePlaces(
        currentLatLng: LatLng,
        customQuery: String?,
    ) {
        if (customQuery == null || customQuery.isEmpty()) {
            populatePlaces(currentLatLng)
            return
        }
        val queryActions = actions ?: return
        val viewport = queryActions.getQueryViewport()
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        val screenTopRightLatLng = viewport.screenBottomLeft
        val screenBottomLeftLatLng = viewport.screenTopRight
        val isRecenter = queryActions.isRecenterToUserLocation()

        if (currentLatLng.equals(lastFocusLocation) || lastFocusLocation == null || isRecenter) {
            // Means we are checking around current location
            populatePlacesForCurrentLocation(
                viewport.lastKnownLocation,
                screenTopRightLatLng,
                screenBottomLeftLatLng,
                currentLatLng,
                customQuery,
            )
        } else {
            populatePlacesForAnotherLocation(
                viewport.lastKnownLocation,
                screenTopRightLatLng,
                screenBottomLeftLatLng,
                currentLatLng,
                customQuery,
            )
        }
        if (isRecenter) {
            queryActions.setRecenterToUserLocation(false)
        }
    }

    /**
     * Stops any ongoing queries and clears all disposables.
     * This method sets the stopQuery flag to true and clears the compositeDisposable
     * to prevent any further processing.
     */
    override fun stopQuery() {
        stopQuery = true
        dependencies.compositeDisposableProvider()?.clear()
    }

    /**
     * Clears the Nearby local cache and then calls for pin details to be fetched afresh.
     */
    override fun emptyCache() {
        val placesRepo = dependencies.placesRepositoryProvider()
        val presenter = dependencies.presenterProvider()
        val disposable = dependencies.compositeDisposableProvider()
        if (placesRepo == null || presenter == null || disposable == null) return
        val scope = dependencies.scopeProvider()

        // reload the map once the cache is cleared
        disposable.add(
            placesRepo
                .clearCache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(
                    Completable.fromAction {
                        // reload only the pin details, by making all loaded pins gray:
                        val newPlaceGroups = ArrayList<MarkerPlaceGroup>(NearbyController.markerLabelList.size)
                        for (placeGroup in NearbyController.markerLabelList) {
                            val place =
                                Place(
                                    "",
                                    "",
                                    placeGroup.place.label,
                                    "",
                                    placeGroup.place.getLocation(),
                                    "",
                                    placeGroup.place.siteLinks,
                                    "",
                                    placeGroup.place.exists,
                                    placeGroup.place.entityID,
                                )
                            place.setDistance(placeGroup.place.distance)
                            place.isMonument = placeGroup.place.isMonument
                            newPlaceGroups.add(MarkerPlaceGroup(placeGroup.isBookmarked, place))
                        }
                        presenter.loadPlacesDataAsync(newPlaceGroups, scope)
                    },
                ).subscribe(
                    { Timber.d("Nearby Cache cleared successfully.") },
                    { throwable: Throwable? -> Timber.e(throwable, "Failed to clear the Nearby Cache") },
                ),
        )
    }

    /**
     * Persists place to database asynchronously.
     */
    fun savePlaceToDatabase(place: Place?) {
        val placesRepo = dependencies.placesRepositoryProvider() ?: return
        val disposable = dependencies.compositeDisposableProvider() ?: return
        placesRepo.save(place)?.subscribeOn(Schedulers.io())?.subscribe()?.let {
            disposable.add(it)
        }
    }

    private fun calculateFallbackBounds(currentLatLng: LatLng): Pair<LatLng, LatLng> {
        val delta = FALLBACK_BOUNDS_DELTA_DEGREES
        val westCornerLat = currentLatLng.latitude - delta
        val westCornerLong = currentLatLng.longitude - delta
        val eastCornerLat = currentLatLng.latitude + delta
        val eastCornerLong = currentLatLng.longitude + delta
        val topRight = LatLng(westCornerLat, westCornerLong, 0f)
        val bottomLeft = LatLng(eastCornerLat, eastCornerLong, 0f)
        return Pair(topRight, bottomLeft)
    }

    private fun populatePlacesForCurrentLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng?,
        screenBottomLeft: LatLng?,
        searchLatLng: LatLng,
        customQuery: String?,
    ) {
        val controller = dependencies.nearbyControllerProvider() ?: return
        val isMonuments = actions?.isMonumentsEnabled() ?: false
        val observable =
            Observable.fromCallable {
                controller.loadAttractionsFromLocation(
                    currentLatLng,
                    screenTopRight,
                    screenBottomLeft,
                    searchLatLng,
                    false,
                    true,
                    isMonuments,
                    customQuery,
                )
            }
        subscribePlacesObservable(observable, searchLatLng, isCurrentLocation = true)
    }

    private fun populatePlacesForAnotherLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng?,
        screenBottomLeft: LatLng?,
        searchLatLng: LatLng,
        customQuery: String?,
    ) {
        val controller = dependencies.nearbyControllerProvider() ?: return
        val isMonuments = actions?.isMonumentsEnabled() ?: false
        val observable =
            Observable.fromCallable {
                controller.loadAttractionsFromLocation(
                    currentLatLng,
                    screenTopRight,
                    screenBottomLeft,
                    searchLatLng,
                    false,
                    true,
                    isMonuments,
                    customQuery,
                )
            }
        subscribePlacesObservable(observable, searchLatLng, isCurrentLocation = false)
    }

    private fun subscribePlacesObservable(
        observable: Observable<NearbyPlacesInfo>,
        searchLatLng: LatLng,
        isCurrentLocation: Boolean,
    ) {
        val disposable = dependencies.compositeDisposableProvider() ?: return
        val queryActions = actions ?: return
        val presenter = dependencies.presenterProvider()

        disposable.add(
            observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo ->
                        if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                            queryActions.showNoNearbyPlacesMessage()
                            queryActions.setProgressBarVisibility(false)
                            presenter?.lockUnlockNearby(false)
                        } else {
                            // Updating last searched location
                            if (!isCurrentLocation) {
                                dependencies.applicationKvStoreProvider()?.putString(
                                    LAST_LOCATION_KEY,
                                    "${searchLatLng.latitude},${searchLatLng.longitude}",
                                )
                            }
                            // curLatLng is used to calculate distance from the current location to the place
                            // and distance is later on populated to the place
                            presenter?.updateMapMarkers(
                                nearbyPlacesInfo.placeList,
                                searchLatLng,
                                dependencies.scopeProvider(),
                            )
                            lastFocusLocation = searchLatLng
                            queryActions.setLastMapFocus(GeoPoint(searchLatLng.latitude, searchLatLng.longitude))
                            if (!isCurrentLocation) {
                                stopQuery()
                            }
                        }
                    },
                    { throwable ->
                        Timber.d(throwable)
                        queryActions.showErrorFetchingPlacesMessage(throwable.localizedMessage ?: "")
                        queryActions.setProgressBarVisibility(false)
                        presenter?.lockUnlockNearby(false)
                        queryActions.setFilterState()
                    },
                ),
        )
    }
}
