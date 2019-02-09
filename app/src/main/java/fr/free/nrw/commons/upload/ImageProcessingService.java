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
import timber.log.Timber;

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

    /**
     * Check image quality before upload
     * - checks duplicate image
     * - checks dark image
     * - checks geolocation for image
     * - check for valid title
     */
    Single<Integer> validateImage(UploadModel.UploadItem uploadItem, boolean checkTitle) {
        int currentImageQuality = uploadItem.getImageQuality();
        Timber.d("Current image quality is %d", currentImageQuality);
        if (currentImageQuality == ImageUtils.IMAGE_KEEP) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        Timber.d("Checking the validity of image");
        String filePath = uploadItem.getMediaUri().getPath();
        Single<Integer> duplicateImage = checkDuplicateImage(filePath);
        Single<Integer> wrongGeoLocation = checkImageGeoLocation(uploadItem.getPlace(), filePath);
        Single<Integer> darkImage = checkDarkImage(filePath);
        Single<Integer> itemTitle = checkTitle ? validateItemTitle(uploadItem) : Single.just(ImageUtils.IMAGE_OK);

        return Single.zip(duplicateImage, wrongGeoLocation, darkImage, itemTitle,
                (duplicate, wrongGeo, dark, title) -> {
                    Timber.d("Result for duplicate: %d, geo: %d, dark: %d, title: %d", duplicate, wrongGeo, dark, title);
                    return duplicate | wrongGeo | dark | title;
                });
    }

    /**
     * Checks item title
     * - empty title
     * - existing title
     * @param uploadItem
     * @return
     */
    private Single<Integer> validateItemTitle(UploadModel.UploadItem uploadItem) {
        Timber.d("Checking for image title %s", uploadItem.getTitle());
        Title title = uploadItem.getTitle();
        if (title.isEmpty()) {
            return Single.just(EMPTY_TITLE);
        }

        return Single.fromCallable(() -> mwApi.fileExistsWithName(uploadItem.getFileName()))
                .map(doesFileExist -> {
                    Timber.d("Result for valid title is %s", doesFileExist);
                    return doesFileExist ? FILE_NAME_EXISTS : IMAGE_OK;
                });
    }

    /**
     * Checks for duplicate image
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    private Single<Integer> checkDuplicateImage(String filePath) {
        Timber.d("Checking for duplicate image %s", filePath);
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(fileUtilsWrapper::getSHA1)
                .map(mwApi::existingFile)
                .map(b -> {
                    Timber.d("Result for duplicate image %s", b);
                    return b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK;
                });
    }

    /**
     * Checks for dark image
     * @param filePath file to be checked
     * @return IMAGE_DARK or IMAGE_OK
     */
    private Single<Integer> checkDarkImage(String filePath) {
        Timber.d("Checking for dark image %s", filePath);
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
        Timber.d("Checking for image geolocation %s", filePath);
        if (place == null || StringUtils.isNullOrWhiteSpace(place.getWikiDataEntityId())) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        return Single.fromCallable(() -> filePath)
                .map(fileUtilsWrapper::getGeolocationOfFile)
                .flatMap(geoLocation -> imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, place.getLocation()));
    }
}
