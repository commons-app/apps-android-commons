package fr.free.nrw.commons.nearby.presenter

import android.view.View
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import timber.log.Timber
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Base presenter handling MVP view lifecycle, action listeners, and UI state
 * for NearbyParentFragment.
 */
abstract class BaseNearbyViewPresenter(
    val bookmarkLocationDao: BookmarkLocationsDao,
    val placesRepository: PlacesRepository,
    val nearbyController: NearbyController,
) : NearbyParentFragmentContract.UserActions,
    WikidataP18EditListener,
    LocationUpdateListener {
    var nearbyParentFragmentView: NearbyParentFragmentContract.View = DUMMY
        protected set

    protected var currentLatLng: LatLng? = null

    override fun attachView(view: NearbyParentFragmentContract.View) {
        nearbyParentFragmentView = view
    }

    override fun detachView() {
        nearbyParentFragmentView = DUMMY
    }

    override fun removeNearbyPreferences(applicationKvStore: JsonKvStore) {
        Timber.d("Remove place objects")
        applicationKvStore.remove(PLACE_OBJECT)
    }

    /**
     * Sets click listeners of FABs, and 2 bottom sheets
     */
    override fun setActionListeners(applicationKvStore: JsonKvStore?) {
        nearbyParentFragmentView.setFABPlusAction(
            View.OnClickListener { v: View? ->
                if (applicationKvStore != null &&
                    applicationKvStore.getBoolean(
                        "login_skipped",
                        false,
                    )
                ) {
                    // prompt the user to login
                    nearbyParentFragmentView.displayLoginSkippedWarning()
                } else {
                    nearbyParentFragmentView.animateFABs()
                }
            },
        )

        nearbyParentFragmentView.setFABRecenterAction(
            View.OnClickListener { v: View? ->
                nearbyParentFragmentView.recenterMap(currentLatLng)
            },
        )
    }

    override fun backButtonClicked(): Boolean =
        when {
            nearbyParentFragmentView.isAdvancedQueryFragmentVisible() -> {
                nearbyParentFragmentView.showHideAdvancedQueryFragment(false)
                true
            }
            nearbyParentFragmentView.isListBottomSheetExpanded() -> {
                // Back should first hide the bottom sheet if it is expanded
                nearbyParentFragmentView.listOptionMenuItemClicked()
                true
            }
            nearbyParentFragmentView.isDetailsBottomSheetVisible() -> {
                nearbyParentFragmentView.setBottomSheetDetailsSmaller()
                true
            }
            nearbyParentFragmentView.isNearbyFilterListVisible() -> {
                nearbyParentFragmentView.hideNearbyFilterList()
                true
            }
            else -> false
        }

    override fun searchViewGainedFocus() {
        if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.hideBottomSheet()
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.hideBottomDetailsSheet()
        }
    }

    override fun setCheckboxUnknown() {
        nearbyParentFragmentView.setCheckBoxState(CheckBoxTriStates.UNKNOWN)
    }

    override fun filterByMarkerType(
        selectedLabels: List<Label?>?,
        state: Int,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean,
    ) {
        if (filterForAllNoneType) { // Means we will set labels based on states
            when (state) {
                CheckBoxTriStates.UNKNOWN -> {}
                CheckBoxTriStates.UNCHECKED -> {
                    // TODO
                    nearbyParentFragmentView.filterOutAllMarkers()
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut()
                }

                CheckBoxTriStates.CHECKED -> {
                    // Despite showing all labels NearbyFilterState still should be applied
                    nearbyParentFragmentView.filterMarkersByLabels(
                        selectedLabels,
                        filterForPlaceState,
                        false,
                    )
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected()
                }
            }
        } else {
            nearbyParentFragmentView.filterMarkersByLabels(
                selectedLabels,
                filterForPlaceState,
                false,
            )
        }
    }

    companion object {
        val DUMMY: NearbyParentFragmentContract.View =
            Proxy.newProxyInstance(
                NearbyParentFragmentContract.View::class.java.classLoader,
                arrayOf<Class<*>>(NearbyParentFragmentContract.View::class.java),
                InvocationHandler { proxy: Any?, method: Method?, args: Array<Any?>? ->
                    if (method!!.name == "onMyEvent") {
                        return@InvocationHandler null
                    } else if (String::class.java == method.returnType) {
                        return@InvocationHandler ""
                    } else if (Int::class.java == method.returnType) {
                        return@InvocationHandler 0
                    } else if (Int::class.javaPrimitiveType == method.returnType) {
                        return@InvocationHandler 0
                    } else if (Boolean::class.java == method.returnType) {
                        return@InvocationHandler java.lang.Boolean.FALSE
                    } else if (Boolean::class.javaPrimitiveType == method.returnType) {
                        return@InvocationHandler false
                    } else {
                        return@InvocationHandler null
                    }
                },
            ) as NearbyParentFragmentContract.View
    }
}
