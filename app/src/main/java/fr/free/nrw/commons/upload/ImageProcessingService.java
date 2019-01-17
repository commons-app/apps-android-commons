package fr.free.nrw.commons.upload;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.BitmapRegionDecoderWrapper;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ImageUtilsWrapper;
import fr.free.nrw.commons.utils.StringUtils;
import io.reactivex.Single;

/**
 * Methods for pre-processing images to be uploaded
 */
@Singleton
public class ImageProcessingService {
    private final FileUtilsWrapper fileUtilsWrapper;
    private final BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper;
    private final ImageUtilsWrapper imageUtilsWrapper;
    private final MediaWikiApi mwApi;

    @Inject
    public ImageProcessingService(FileUtilsWrapper fileUtilsWrapper,
                                  BitmapRegionDecoderWrapper bitmapRegionDecoderWrapper,
                                  ImageUtilsWrapper imageUtilsWrapper,
                                  MediaWikiApi mwApi) {
        this.fileUtilsWrapper = fileUtilsWrapper;
        this.bitmapRegionDecoderWrapper = bitmapRegionDecoderWrapper;

        this.imageUtilsWrapper = imageUtilsWrapper;
        this.mwApi = mwApi;
    }

    /**
     * Check image quality before upload
     * - checks duplicate image
     * - checks dark image
     */
    public Single<Integer> checkImageQuality(String filePath) {
        return checkImageQuality(null, filePath);
    }

    /**
     * Check image quality before upload
     * - checks duplicate image
     * - checks dark image
     * - checks geolocation for image
     */
    public Single<Integer> checkImageQuality(Place place, String filePath) {
        return Single.zip(
                checkDuplicateImage(filePath),
                checkImageGeoLocation(place, filePath),
                checkDarkImage(filePath), //Returns IMAGE_DARK or IMAGE_OK
                (dupe, wrongGeo, dark) -> dupe | wrongGeo | dark);
    }

    /**
     * Checks for duplicate image
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    private Single<Integer> checkDuplicateImage(String filePath) {
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(fileUtilsWrapper::getSHA1)
                .map(mwApi::existingFile)
                .map(b -> b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK);
    }

    /**
     * Checks for dark image
     * @param filePath file to be checked
     * @return IMAGE_DARK or IMAGE_OK
     */
    private Single<Integer> checkDarkImage(String filePath) {
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(file -> bitmapRegionDecoderWrapper.newInstance(file, false))
                .flatMap(imageUtilsWrapper::checkIfImageIsTooDark);
    }

    /**
     * Checks for image geolocation
     * @param filePath file to be checked
     * @return IMAGE_GEOLOCATION_DIFFERENT or IMAGE_OK
     */
    private Single<Integer> checkImageGeoLocation(Place place, String filePath) {
        if (place == null || StringUtils.isNullOrWhiteSpace(place.getWikiDataEntityId())) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        return Single.fromCallable(() -> filePath)
                .map(fileUtilsWrapper::getGeolocationOfFile)
                .flatMap(geoLocation -> imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, place.getLocation()));
    }
}
