package fr.free.nrw.commons.nearby.presenter

import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import timber.log.Timber
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Handles UI interactions, view attachment lifecycle, preferences, and bottom sheet navigation
 * for NearbyParentFragment.
 */
class NearbyUiInteractionHandler : NearbyUiOperations {
    var nearbyParentFragmentView: NearbyParentFragmentContract.View = DUMMY
        private set

    override fun attachView(view: NearbyParentFragmentContract.View) {
        nearbyParentFragmentView = view
    }

    override fun detachView() {
        nearbyParentFragmentView = DUMMY
    }

    var currentLatLngProvider: (() -> LatLng?)? = null

    override fun setActionListeners(applicationKvStore: JsonKvStore?) {
        nearbyParentFragmentView.setFABPlusAction {
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
        }
        nearbyParentFragmentView.setFABRecenterAction {
            nearbyParentFragmentView.recenterMap(currentLatLngProvider?.invoke())
        }
    }

    override fun removeNearbyPreferences(applicationKvStore: JsonKvStore) {
        Timber.d("Remove place objects")
        applicationKvStore.remove(PLACE_OBJECT)
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

    override fun markerUnselected() {
        nearbyParentFragmentView.hideBottomSheet()
    }

    companion object {
        val DUMMY: NearbyParentFragmentContract.View =
            Proxy.newProxyInstance(
                NearbyParentFragmentContract.View::class.java.classLoader,
                arrayOf<Class<*>>(NearbyParentFragmentContract.View::class.java),
                InvocationHandler { _: Any?, method: Method?, _: Array<Any?>? ->
                    when {
                        method!!.name == "onMyEvent" -> null
                        String::class.java == method.returnType -> ""
                        Int::class.java == method.returnType || Integer.TYPE == method.returnType -> 0
                        Boolean::class.java == method.returnType || java.lang.Boolean.TYPE == method.returnType -> false
                        else -> null
                    }
                },
            ) as NearbyParentFragmentContract.View
    }
}
