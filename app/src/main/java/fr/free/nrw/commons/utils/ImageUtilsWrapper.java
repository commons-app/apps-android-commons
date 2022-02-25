package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.data.models.location.LatLng;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageUtilsWrapper {

    @Inject
    public ImageUtilsWrapper() {

    }

    public Single<Integer> checkIfImageIsTooDark(String bitmapPath) {
        return Single.fromCallable(() -> ImageUtils.checkIfImageIsTooDark(bitmapPath))
            .subscribeOn(Schedulers.computation());
    }

    public Single<Integer> checkImageGeolocationIsDifferent(String geolocationOfFileString,
        LatLng latLng) {
        return Single.fromCallable(
            () -> ImageUtils.checkImageGeolocationIsDifferent(geolocationOfFileString, latLng))
            .subscribeOn(Schedulers.computation())
            .map(isDifferent -> isDifferent ? ImageUtils.IMAGE_GEOLOCATION_DIFFERENT
                : ImageUtils.IMAGE_OK);
    }
}
