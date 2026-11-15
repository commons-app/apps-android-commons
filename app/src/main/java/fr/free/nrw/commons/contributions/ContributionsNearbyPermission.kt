package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.ViewUtil.showLongToast

/** Owns location permission requests for the nearby contributions card. */
class ContributionsNearbyPermission(
    private val fragment: ContributionsFragment,
) {
    private var checkBoxView: View? = null
    private var checkBox: CheckBox? = null
    private val nearbyLocationPermissionLauncher =
        fragment.registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            object : ActivityResultCallback<Map<String, Boolean>> {
                /** Updates the nearby card after the location permission request completes. */
                override fun onActivityResult(result: Map<String, Boolean>) {
                    var areAllGranted = true
                    for (b in result.values) {
                        areAllGranted = areAllGranted && b
                    }

                    if (areAllGranted) {
                        onLocationPermissionGranted()
                    } else {
                        if (canExplainPermission()) {
                            fragment.binding!!.cardViewNearby.permissionType =
                                NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION
                        } else {
                            displayYouWontSeeNearbyMessage()
                        }
                    }
                }
            },
        )

    /** Checks location access before displaying the nearby card. */
    fun checkPermissionsAndShowNearbyCardView() {
        if (hasPermission(
                fragment.requireActivity(),
                arrayOf(permission.ACCESS_FINE_LOCATION),
            )
        ) {
            onLocationPermissionGranted()
        } else if (canExplainPermission()) {
            fragment.binding!!.cardViewNearby.permissionType =
                NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION
            showNearbyCardPermissionRationale()
        }
    }

    /** Launches the nearby card location permission request. */
    private fun requestLocationPermission() {
        nearbyLocationPermissionLauncher.launch(arrayOf(permission.ACCESS_FINE_LOCATION))
    }

    /** Enables nearby location updates after permission is granted. */
    private fun onLocationPermissionGranted() {
        fragment.binding!!.cardViewNearby.permissionType =
            NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED
        fragment.locationManager!!.registerLocationManager()
    }

    /** Explains why the nearby card needs location access. */
    fun showNearbyCardPermissionRationale() {
        showAlertDialog(
            fragment.requireActivity(),
            fragment.getString(R.string.nearby_card_permission_title),
            fragment.getString(R.string.nearby_card_permission_explanation),
            { this.requestLocationPermission() },
            { this.displayYouWontSeeNearbyMessage() },
            checkBoxView,
        )
    }

    /** Records the opt-out and explains that nearby places cannot be shown. */
    private fun displayYouWontSeeNearbyMessage() {
        showLongToast(
            fragment.requireActivity(),
            fragment.resources.getString(R.string.unable_to_display_nearest_place),
        )
        // Set to true as the user doesn't want the app to ask for location permission anymore
        fragment.store!!.putBoolean("doNotAskForLocationPermission", true)
    }

    /** Builds the opt-out checkbox used in the location permission explanation. */
    fun createPermissionView() {
        checkBoxView = View.inflate(fragment.activity, R.layout.nearby_permission_dialog, null)
        checkBox = checkBoxView?.findViewById<View>(R.id.never_ask_again) as CheckBox
        checkBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                // Do not ask for permission on activity start again
                fragment.store!!.putBoolean("displayLocationPermissionForCardView", false)
            }
        }
    }

    /** Checks whether the nearby card may explain a location permission denial. */
    private fun canExplainPermission(): Boolean {
        val canAskAgain =
            fragment.shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION) &&
                fragment.store!!.getBoolean("displayLocationPermissionForCardView", true)
        return canAskAgain &&
            !fragment.store!!.getBoolean("doNotAskForLocationPermission", false) &&
            (fragment.activity as MainActivity).activeFragment == ActiveFragment.CONTRIBUTIONS
    }
}
