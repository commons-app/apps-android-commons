package fr.free.nrw.commons.utils;

import android.graphics.BitmapRegionDecoder;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.location.LatLng;

import static fr.free.nrw.commons.utils.ImageUtils.*;

@Singleton
public class ImageUtilsWrapper {

    @Inject
    public ImageUtilsWrapper() {

    }

    public @Result int checkIfImageIsTooDark(BitmapRegionDecoder bitmapRegionDecoder) {
        return ImageUtils.checkIfImageIsTooDark(bitmapRegionDecoder);
    }

    public boolean checkImageGeolocationIsDifferent(String geolocationOfFileString, LatLng latLng) {
        return ImageUtils.checkImageGeolocationIsDifferent(geolocationOfFileString, latLng);
    }
}
