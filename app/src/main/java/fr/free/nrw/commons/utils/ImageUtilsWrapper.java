package fr.free.nrw.commons.utils;

import android.graphics.BitmapRegionDecoder;

import javax.inject.Inject;
import javax.inject.Singleton;

import static fr.free.nrw.commons.utils.ImageUtils.*;

@Singleton
public class ImageUtilsWrapper {

    @Inject
    public ImageUtilsWrapper() {

    }

    public @Result int checkIfImageIsTooDark(BitmapRegionDecoder bitmapRegionDecoder) {
        return ImageUtils.checkIfImageIsTooDark(bitmapRegionDecoder);
    }

    public boolean checkImageGeolocationIsDifferent(String geolocationOfFileString, String wikidataItemLocationString) {
        return ImageUtils.checkImageGeolocationIsDifferent(geolocationOfFileString, wikidataItemLocationString);
    }
}
