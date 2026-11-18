package fr.free.nrw.commons.nearby.fragments

import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.isMonumentsEnabled
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import timber.log.Timber

/**
 * Initializes bottom sheet helper with view providers, bookmark actions, and upload callbacks.
 */
internal fun NearbyParentFragment.initBottomSheetHelper() {
    bottomSheetHelper.init(
        bindingProvider = { binding },
        contextProvider = { context },
        activityProvider = { activity },
        bookmarkDaoProvider = { bookmarkLocationDao },
        actions =
            object : NearbyBottomSheetHelper.NearbyBottomSheetActions {
                override fun onCameraCenterRequested(place: Place) {
                    moveCameraToPosition(GeoPoint(place.location.latitude, place.location.longitude))
                }

                override fun onMediaClicked(imageName: String) {
                    mediaDetailHelper.handleMediaClick(imageName)
                }

                override fun onBookmarkToggled(place: Place) {
                    presenter?.toggleBookmarkedStatus(place, scope)
                }

                override fun onSheetStateChanged(newState: Int) {
                    prepareViewsForSheetPosition(newState)
                }

                override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                override fun onUploadActionInitiated(
                    place: Place,
                    uploadType: NearbyBottomSheetHelper.UploadType,
                ) {
                    storeSharedPrefs(place)
                    val act = activity ?: return
                    when (uploadType) {
                        NearbyBottomSheetHelper.UploadType.CAMERA -> {
                            Timber.d("Camera button tapped. Place: %s", place.toString())
                            controller?.initiateCameraPick(
                                act,
                                inAppCameraLocationPermissionLauncher,
                                cameraPickLauncherForResult,
                            )
                        }
                        NearbyBottomSheetHelper.UploadType.GALLERY -> {
                            Timber.d("Gallery button tapped. Place: %s", place.toString())
                            controller?.initiateGalleryPick(
                                act,
                                galleryPickLauncherForResult,
                                false,
                            )
                        }
                        NearbyBottomSheetHelper.UploadType.CUSTOM_GALLERY -> {
                            Timber.d("Gallery button tapped. Place: %s", place.toString())
                            // Pass singleSelection = true for Nearby flow
                            controller?.initiateCustomGalleryPickWithPermission(
                                act,
                                customSelectorLauncherForResult,
                                singleSelection = true,
                            )
                        }
                    }
                }

                override fun getCoroutineScope(): LifecycleCoroutineScope? = scope

                override fun getPlaceAdapter(): PlaceAdapter? = adapter
            },
    )
}

/**
 * Initializes FAB helper with view binding and photo marker check actions.
 */
internal fun NearbyParentFragment.initFabHelper() {
    fabHelper.init(
        bindingProvider = { binding },
        contextProvider = { context },
        activityProvider = { activity },
        applicationKvStoreProvider = { applicationKvStore },
        actions =
            object : NearbyFabHelper.NearbyFabActions {
                override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                override fun onPhotoMarkerCheckRequested() {
                    addPhotoMarkerIfDepictsMode()
                }
            },
    )
}

/**
 * Initializes filter helper with place type search adapters, debounce listeners, and theme preferences.
 */
internal fun NearbyParentFragment.initFilterHelper() {
    filterHelper.init(
        bindingProvider = { binding },
        contextProvider = { context },
        activityProvider = { activity },
        dependencies =
            NearbyFilterHelper.NearbyFilterDependencies(
                placeTypeLabelsClientProvider = { placeTypeLabelsClient },
                compositeDisposableProvider = { disposables },
                presenterProvider = { presenter },
            ),
        actions =
            object : NearbyFilterHelper.NearbyFilterActions {
                override fun hideBottomSheet() {
                    bottomSheetHelper.hideBottomSheet()
                }

                override fun isDarkTheme(): Boolean = isDarkTheme

                override fun setHasOptionsMenu(hasMenu: Boolean) {
                    this@initFilterHelper.setHasOptionsMenu(hasMenu)
                }
            },
    )
}

/**
 * Initializes places query helper with repository, controller, and viewport callbacks.
 */
internal fun NearbyParentFragment.initPlacesQueryHelper() {
    placesQueryHelper.init(
        dependencies =
            NearbyPlacesDependencies(
                nearbyControllerProvider = { nearbyController },
                placesRepositoryProvider = { placesRepository },
                compositeDisposableProvider = { disposables },
                presenterProvider = { presenter },
                applicationKvStoreProvider = { applicationKvStore },
                scopeProvider = { scope },
            ),
        actions =
            object : NearbyPlacesQueryHelper.PlacesQueryActions {
                override fun getQueryViewport(): PlacesQueryViewport =
                    PlacesQueryViewport(
                        screenTopRight = viewportHelper.getScreenTopRight(),
                        screenBottomLeft = viewportHelper.getScreenBottomLeft(),
                        lastMapFocus = viewportHelper.lastMapFocus?.let { LatLng(it.latitude, it.longitude, 0.0f) },
                        mapFocus = viewportHelper.getMapFocus(),
                        lastKnownLocation = locationHelper.lastKnownLocation,
                    )

                override fun setLastMapFocus(geoPoint: GeoPoint?) {
                    viewportHelper.setLastMapFocus(geoPoint)
                }

                override fun isMonumentsEnabled(): Boolean = isMonumentsEnabled

                override fun isRecenterToUserLocation(): Boolean = locationHelper.recenterToUserLocation

                override fun setRecenterToUserLocation(recenter: Boolean) {
                    locationHelper.setRecenterToUserLocation(recenter)
                }

                override fun showNoNearbyPlacesMessage() {
                    context?.let {
                        this@initPlacesQueryHelper.showErrorMessage(
                            getString(fr.free.nrw.commons.R.string.no_nearby_places_around),
                        )
                    }
                }

                override fun showErrorFetchingPlacesMessage(errorMessage: String) {
                    context?.let {
                        this@initPlacesQueryHelper.showErrorMessage(
                            getString(fr.free.nrw.commons.R.string.error_fetching_nearby_places) + errorMessage,
                        )
                    }
                }

                override fun setProgressBarVisibility(isVisible: Boolean) {
                    this@initPlacesQueryHelper.setProgressBarVisibility(isVisible)
                }

                override fun setFilterState() {
                    this@initPlacesQueryHelper.setFilterState()
                }
            },
    )
}

/**
 * Initializes media detail helper for handling media preview and pager fragment transactions.
 */
internal fun NearbyParentFragment.initMediaDetailHelper() {
    mediaDetailHelper.init(
        mediaClientProvider = { mediaClient },
        childFragmentManagerProvider = { childFragmentManager },
        bindingProvider = { binding },
        activityProvider = { activity },
        compositeDisposableProvider = { disposables },
    )
}

/**
 * Initializes status helper with network broadcast receivers, snackbar handlers, and Wikidata edit callbacks.
 */
internal fun NearbyParentFragment.initStatusHelper() {
    statusHelper.init(
        NearbyStatusDependencies(
            activityProvider = { activity },
            contextProvider = { context },
            bindingProvider = { binding },
            presenterProvider = { presenter },
            locationManagerProvider = { locationManager },
            readyCallbackConsumer = { nearbyParentFragmentInstanceReadyCallback = it },
            mediaDetailHelperProvider = { mediaDetailHelper },
        ),
    )
    wikidataEditListener.authenticationStateListener = this
}

/**
 * Initialise background based on theme, this should be doe ideally via styles, that would need
 * another refactor
 */
internal fun NearbyParentFragment.initThemePreferences() {
    filterHelper.applyTheme(isDarkTheme)
    if (isDarkTheme) {
        binding?.bottomSheetNearby?.rvNearbyList?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), fr.free.nrw.commons.R.color.contributionListDarkBackground),
        )
        binding
            ?.map
            ?.overlayManager
            ?.tilesOverlay
            ?.setColorFilter(TilesOverlay.INVERT_COLORS)
    } else {
        binding?.bottomSheetNearby?.rvNearbyList?.setBackgroundColor(
            ContextCompat.getColor(requireContext(), fr.free.nrw.commons.R.color.white),
        )
    }
}

/**
 * Initializes the places RecyclerView and its adapter.
 */
internal fun NearbyParentFragment.initRvNearbyList() {
    binding?.bottomSheetNearby?.rvNearbyList?.layoutManager = LinearLayoutManager(context)
    adapter =
        PlaceAdapter(
            bookmarkLocationsDao = bookmarkLocationDao,
            scope = scope,
            onPlaceClicked = { place: Place ->
                moveCameraToPosition(
                    GeoPoint(
                        place.location.latitude,
                        place.location.longitude,
                    ),
                )
            },
            onBookmarkClicked = { place: Place?, _: Boolean? ->
                presenter?.toggleBookmarkedStatus(place, scope)
            },
            commonPlaceClickActions = commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher = inAppCameraLocationPermissionLauncher,
            galleryPickLauncherForResult = galleryPickLauncherForResult,
            cameraPickLauncherForResult = cameraPickLauncherForResult,
        )
    binding?.bottomSheetNearby?.rvNearbyList?.adapter = adapter
}

/**
 * Initializes view hierarchy components, bottom sheet and FAB helpers.
 */
internal fun NearbyParentFragment.initViews() {
    Timber.d("init views called")
    if (!isMonumentsEnabled) {
        NearbyFilterState.setWlmSelected(false)
    }
}

/**
 * Fetches place details for a given place marker and updates bottom sheet.
 */
internal fun NearbyParentFragment.getPlaceData(
    place: Place,
    marker: Marker,
    isBookmarked: Boolean,
) {
    val getPlaceObservable =
        Observable.fromCallable {
            nearbyController?.getPlaces(java.util.List.of(place))
        }
    disposables.add(
        getPlaceObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { placeList: List<Place>? ->
                    if (!placeList.isNullOrEmpty()) {
                        val updatedPlace = placeList[0]
                        updatedPlace.distance = place.distance
                        updatedPlace.location = place.location
                        marker.title = updatedPlace.name
                        val desc = updatedPlace.longDescription
                        marker.snippet =
                            if (NearbyParentFragment.containsParentheses(desc)) {
                                NearbyParentFragment.getTextBetweenParentheses(desc)
                            } else {
                                desc
                            }
                        marker.showInfoWindow()
                        presenter?.handlePinClicked(updatedPlace)
                        placesQueryHelper.savePlaceToDatabase(updatedPlace)
                        val icon =
                            getCachedDrawable(
                                requireContext(),
                                getIconFor(updatedPlace, isBookmarked, viewportHelper.nearestPlace),
                                markerOverlayHelper.drawableCache,
                            )
                        marker.icon = icon
                        binding?.map?.invalidate()
                        binding?.bottomSheetDetails?.dataCircularProgress?.visibility = View.GONE
                        binding?.bottomSheetDetails?.icon?.visibility = View.VISIBLE
                        binding?.bottomSheetDetails?.wikiDataLl?.visibility = View.VISIBLE
                        bottomSheetHelper.passInfoToSheet(updatedPlace)
                        bottomSheetHelper.hideBottomSheet()
                    }
                },
                { throwable: Throwable ->
                    Timber.d(throwable)
                    showErrorMessage(
                        getString(fr.free.nrw.commons.R.string.could_not_load_place_data) +
                            throwable.localizedMessage,
                    )
                },
            ),
    )
}
