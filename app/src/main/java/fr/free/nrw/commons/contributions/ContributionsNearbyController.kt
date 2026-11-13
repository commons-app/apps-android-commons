package fr.free.nrw.commons.contributions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.View
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.LengthUtils.computeBearing
import fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

/** Owns nearby place loading and compass updates. */
class ContributionsNearbyController(
    private val fragment: ContributionsFragment,
) : LocationUpdateListener {
    private var currentLatLng: LatLng? = null
    private var mSensorManager: SensorManager? = null
    private var mLight: Sensor? = null
    private var direction = 0f
    private val compassListener =
        object : SensorEventListener {
            /**
             * When the device rotates, rotate the Nearby banner's compass arrow in tandem.
             */
            override fun onSensorChanged(event: SensorEvent) {
                val rotateDegree = Math.round(event.values[0]).toFloat()
                fragment.binding!!.cardViewNearby.rotateCompass(rotateDegree, direction)
            }

            /** Receives sensor accuracy updates; no adjustment is needed. */
            override fun onAccuracyChanged(
                sensor: Sensor,
                accuracy: Int,
            ) {
                // Nothing to do.
            }
        }

    /** Loads the closest nearby places using the last known location. */
    fun updateClosestNearbyCardViewInfo() {
        currentLatLng = fragment.locationManager!!.getLastLocation()
        fragment.subscriptions.add(
            Observable
                .fromCallable {
                    fragment.nearbyController?.loadAttractionsFromLocation(
                        currentLatLng,
                        currentLatLng,
                        true,
                        false,
                    )
                } // thanks to boolean, it will only return closest result
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo? ->
                        this.updateNearbyNotification(
                            nearbyPlacesInfo,
                        )
                    },
                    { throwable: Throwable? ->
                        Timber.d(throwable)
                        updateNearbyNotification(null)
                    },
                ),
        )
    }

    /** Shows the first nearby place without an image and updates its bearing. */
    fun updateNearbyNotification(nearbyPlacesInfo: NearbyPlacesInfo?) {
        if (nearbyPlacesInfo?.placeList != null && nearbyPlacesInfo.placeList.size > 0) {
            var closestNearbyPlace: Place? = null
            // Find the first nearby place that has no image and exists
            for (place in nearbyPlacesInfo.placeList) {
                if (place.pic == "" && place.exists) {
                    closestNearbyPlace = place
                    break
                }
            }

            if (closestNearbyPlace == null) {
                fragment.binding!!.cardViewNearby.visibility = View.GONE
            } else {
                val distance = formatDistanceBetween(currentLatLng, closestNearbyPlace.location)
                closestNearbyPlace.setDistance(distance)
                direction = computeBearing(currentLatLng!!, closestNearbyPlace.location).toFloat()
                fragment.binding!!.cardViewNearby.updateContent(closestNearbyPlace)
            }
        } else {
            // Means that no close nearby place is found
            fragment.binding!!.cardViewNearby.visibility = View.GONE
        }

        // Prevent Nearby banner from appearing in Media Details, fixing bug https://github.com/commons-app/apps-android-commons/issues/4731
        if (fragment.mediaDetailPagerFragment != null && !fragment.contributionsListFragment!!.isVisible) {
            fragment.binding!!.cardViewNearby.visibility = View.GONE
        }
    }

    /** Refreshes nearby places after a significant location change. */
    override fun onLocationChangedSignificantly(latLng: LatLng) {
        // Will be called if location changed more than 1000 meter
        updateClosestNearbyCardViewInfo()
    }

    /** Refreshes nearby places, logging failures after a slight location change. */
    override fun onLocationChangedSlightly(latLng: LatLng) {
        /* Update closest nearby notification card onLocationChangedSlightly
         */
        runCatching { updateClosestNearbyCardViewInfo() }.onFailure { error ->
            if (error is Exception) {
                Timber.e(error)
            } else {
                throw error
            }
        }
    }

    /** Refreshes nearby places after a medium location change. */
    override fun onLocationChangedMedium(latLng: LatLng) {
        // Update closest nearby card view if location changed more than 500 meters
        updateClosestNearbyCardViewInfo()
    }

    /** Obtains the orientation sensor used by the nearby compass. */
    fun create() {
        mSensorManager = fragment.requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
    }

    /** Starts listening for location changes. */
    fun resumeLocation() {
        fragment.locationManager!!.addLocationListener(this)
    }

    /** Starts the nearby compass sensor. */
    fun resumeSensor() {
        mSensorManager!!.registerListener(compassListener, mLight, SensorManager.SENSOR_DELAY_UI)
    }

    /** Stops location and compass updates while the screen is paused. */
    fun pause() {
        fragment.locationManager!!.removeLocationListener(this)
        fragment.locationManager!!.unregisterLocationManager()
        mSensorManager!!.unregisterListener(compassListener)
    }
}
