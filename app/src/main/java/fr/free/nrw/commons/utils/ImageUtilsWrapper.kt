package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageUtilsWrapper @Inject constructor() {
    fun checkIfImageIsTooDark(bitmapPath: String?) =
        Single.fromCallable { ImageUtils.checkIfImageIsTooDark(bitmapPath) }
            .subscribeOn(Schedulers.computation())

    fun checkImageGeolocationIsDifferent(geolocationOfFileString: String?, latLng: LatLng?) =
        Single.just(ImageUtils.checkImageGeolocationIsDifferent(geolocationOfFileString, latLng))
            .subscribeOn(Schedulers.computation())
            .map { isDifferent: Boolean -> if (isDifferent) ImageUtils.IMAGE_GEOLOCATION_DIFFERENT else ImageUtils.IMAGE_OK }
}
