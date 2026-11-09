package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.R
import fr.free.nrw.commons.locationpicker.LocationPicker
import fr.free.nrw.commons.locationpicker.LocationPicker.getCameraPosition
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.RandomAccessFileExifWriter
import timber.log.Timber
import java.io.File
import java.util.Locale

/** Handles location picker results and location metadata for an upload image. */
internal class UploadMediaLocationController : UploadMediaDetailsContract.Location {
    lateinit var view: UploadMediaDetailView
    private val fragment get() = view.fragment
    private val currentBinding get() = view.bindingProvider()
    private val binding get() = currentBinding!!

    /**
     * True if location is added via the "missing location" popup dialog (which appears after
     * tapping "Next" if the picture has no geographical coordinates).
     */
    private var isMissingLocationDialog = false

    /**
     * Start Location picker activity. Show the location first then user can modify it by clicking
     * modify location button.
     * @param uploadItem current upload item
     */
    override fun showExternalMap(uploadItem: UploadItem) {
        var defaultLatitude = DEFAULT_LATITUDE
        var defaultLongitude = DEFAULT_LONGITUDE
        var defaultZoom = DEFAULT_ZOOM

        /* Retrieve image location from EXIF if present or
           check if user has provided location while using the in-app camera.
           Use location of last UploadItem if none of them is available */
        val locationPickerIntent: Intent
        if (uploadItem.gpsCoords != null &&
            uploadItem.gpsCoords!!
                .decLatitude != 0.0 &&
            uploadItem.gpsCoords!!.decLongitude != 0.0
        ) {
            defaultLatitude = uploadItem.gpsCoords!!.decLatitude
            defaultLongitude = uploadItem.gpsCoords!!.decLongitude
            defaultZoom = uploadItem.gpsCoords!!.zoomLevel

            locationPickerIntent =
                LocationPicker
                    .IntentBuilder()
                    .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, defaultZoom))
                    .activityKey("UploadActivity")
                    .build(fragment.requireActivity())
        } else {
            if (fragment.defaultKvStore.getString(UploadMediaDetailFragment.LAST_LOCATION) != null) {
                val locationLatLng =
                    fragment.defaultKvStore
                        .getString(UploadMediaDetailFragment.LAST_LOCATION)!!
                        .split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                defaultLatitude = locationLatLng[0].toDouble()
                defaultLongitude = locationLatLng[1].toDouble()
            }
            if (fragment.defaultKvStore.getString(UploadMediaDetailFragment.LAST_ZOOM) != null) {
                defaultZoom = fragment.defaultKvStore.getString(UploadMediaDetailFragment.LAST_ZOOM)!!.toDouble()
            }

            locationPickerIntent =
                LocationPicker
                    .IntentBuilder()
                    .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, defaultZoom))
                    .activityKey("NoLocationUploadActivity")
                    .build(fragment.requireActivity())
        }
        view.locationLauncher(locationPickerIntent)
    }

    /** Handles the result of the location picker. */
    fun onCameraPosition(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            return
        }
        checkNotNull(result.data)
        val cameraPosition =
            getCameraPosition(
                result.data!!,
            )

        if (cameraPosition != null) {
            val latitude = cameraPosition.latitude.toString()
            val longitude = cameraPosition.longitude.toString()
            val zoom = cameraPosition.zoom

            this@UploadMediaLocationController.editLocation(latitude, longitude, zoom)
            // If isMissingLocationDialog is true, it means that the user has already tapped the
            // "Next" button, so go directly to the next step.
            if (isMissingLocationDialog) {
                isMissingLocationDialog = false
                fragment.presenter.displayLocDialog(
                    fragment.indexOfFragment,
                    fragment.inAppPictureLocation,
                    fragment.hasUserRemovedLocation,
                )
            }
        } else {
            // If camera position is null means location is removed by the user
            removeLocation()
        }
    }

    /**
     * Removes the location data from the image, by setting them to null
     */
    private fun removeLocation() {
        val gpsCoords = view.currentUploadItem()?.gpsCoords ?: return
        gpsCoords.decimalCoords = null
        fragment.handleUploadExceptions(
            action = {
                val filePath = fragment.uploadableFile!!.getFilePath()
                RandomAccessFileExifWriter.removeLocation(File(filePath))

                val mapQuestion =
                    ContextCompat.getDrawable(fragment.requireContext(), R.drawable.ic_map_not_available_20dp)

                if (currentBinding != null) {
                    binding.locationImageView.setImageDrawable(mapQuestion)
                    binding.locationTextView.setText(R.string.add_location)
                }

                gpsCoords.decLatitude = 0.0
                gpsCoords.decLongitude = 0.0
                gpsCoords.imageCoordsExists = false
                fragment.hasUserRemovedLocation = true

                Toast
                    .makeText(fragment.context, fragment.getString(R.string.location_removed), Toast.LENGTH_LONG)
                    .show()
            },
            onError = { e ->
                Timber.d(e)
                Toast
                    .makeText(
                        fragment.context,
                        "Location could not be removed due to internal error",
                        Toast.LENGTH_LONG,
                    ).show()
            },
        )
    }

    /**
     * Update the old coordinates with new one
     * @param latitude new latitude
     * @param longitude new longitude
     */
    fun editLocation(
        latitude: String,
        longitude: String,
        zoom: Double,
    ) {
        val gpsCoords = view.currentUploadItem()?.gpsCoords ?: return
        val roundedLat = roundCoordinate(latitude.toDouble())
        val roundedLon = roundCoordinate(longitude.toDouble())
        gpsCoords.decLatitude = roundedLat.toDouble()
        gpsCoords.decLongitude = roundedLon.toDouble()
        gpsCoords.decimalCoords = "$roundedLat|$roundedLon"
        gpsCoords.imageCoordsExists = true
        gpsCoords.zoomLevel = zoom

        // Replace the map icon using the one with a green tick
        val mapTick = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.ic_map_available_20dp)

        if (currentBinding != null) {
            binding.locationImageView.setImageDrawable(mapTick)
            binding.locationTextView.setText(R.string.edit_location)
        }

        Toast.makeText(fragment.context, fragment.getString(R.string.location_updated), Toast.LENGTH_LONG).show()
    }

    /**
     * Rounds a coordinate to 6 decimal places (precision of ~0.11 meters).
     * This avoids false precision from GPS/map-picker which aren't that accurate.
     *
     * @param coordinate the coordinate value to round
     * @return coordinate string rounded to 6 decimal places
     */
    private fun roundCoordinate(coordinate: Double): String = String.format(Locale.US, "%.6f", coordinate)

    /**
     * Showing dialog for adding location
     *
     * @param runnable proceed for verifying image quality
     */
    override fun displayAddLocationDialog(runnable: Runnable) {
        isMissingLocationDialog = true
        showAlertDialog(
            fragment.requireActivity(),
            fragment.getString(R.string.no_location_found_title),
            fragment.getString(R.string.no_location_found_message),
            fragment.getString(R.string.add_location),
            fragment.getString(R.string.skip_login),
            {
                fragment.presenter.onMapIconClicked(fragment.indexOfFragment)
            },
            runnable,
        )
    }

    companion object {
        private const val DEFAULT_LATITUDE = 37.773972
        private const val DEFAULT_LONGITUDE = -122.431297
        private const val DEFAULT_ZOOM = 16.0
    }
}
