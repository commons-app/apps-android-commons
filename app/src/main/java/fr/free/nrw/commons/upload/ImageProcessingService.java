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

import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_TITLE;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

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

    public Single<Integer> validateImage(UploadModel.UploadItem uploadItem, boolean checkTitle) {
        Single<Integer> imageQuality = checkImageQuality(uploadItem.place, uploadItem.getMediaUri().getPath());
        Single<Integer> itemTitle = checkTitle ? validateItemTitle(uploadItem) : Single.just(ImageUtils.IMAGE_OK);
        return Single.zip(imageQuality, itemTitle, (quality, title) -> quality | title);
    }

    /**
     * Check image quality before upload
     * - checks duplicate image
     * - checks dark image
     * - checks geolocation for image
     */
    private Single<Integer> checkImageQuality(Place place, String filePath) {
        return Single.zip(
                checkDuplicateImage(filePath),
                checkImageGeoLocation(place, filePath),
                checkDarkImage(filePath), //Returns IMAGE_DARK or IMAGE_OK
                (dupe, wrongGeo, dark) -> dupe | wrongGeo | dark);
    }

    private Single<Integer> validateItemTitle(UploadModel.UploadItem uploadItem) {
        Title title = uploadItem.title;
        if (title.isEmpty()) {
            return Single.just(EMPTY_TITLE);
        }

        return Single.fromCallable(() -> mwApi.fileExistsWithName(uploadItem.getFileName()))
                .map(doesFileExist -> doesFileExist ? FILE_NAME_EXISTS : IMAGE_OK);
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
