package fr.free.nrw.commons.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.location.LatLng;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

@Singleton
public class ImageUtilsWrapper {

    @Inject
    public ImageUtilsWrapper() {

    }

    public Single<Integer> checkIfImageIsTooDark(String bitmapPath) {
        return Single.just(ImageUtils.checkIfImageIsTooDark(bitmapPath))
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation());
    }

    public Single<Integer> checkImageGeolocationIsDifferent(String geolocationOfFileString, LatLng latLng) {
        boolean isImageGeoLocationDifferent = ImageUtils.checkImageGeolocationIsDifferent(geolocationOfFileString, latLng);
        return Single.just(isImageGeoLocationDifferent)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .map(isDifferent -> isDifferent ? ImageUtils.IMAGE_GEOLOCATION_DIFFERENT : ImageUtils.IMAGE_OK);
    }
}
