package fr.free.nrw.commons.upload;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ImageUtilsWrapper;
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
    private final ImageUtilsWrapper imageUtilsWrapper;
    private final ReadFBMD readFBMD;
    private final EXIFReader EXIFReader;
    private final MediaClient mediaClient;

    @Inject
    public ImageProcessingService(FileUtilsWrapper fileUtilsWrapper,
                                  ImageUtilsWrapper imageUtilsWrapper,
                                  ReadFBMD readFBMD, EXIFReader EXIFReader,
                                  MediaClient mediaClient, Context context) {
        this.fileUtilsWrapper = fileUtilsWrapper;
        this.imageUtilsWrapper = imageUtilsWrapper;
        this.readFBMD = readFBMD;
        this.EXIFReader = EXIFReader;
        this.mediaClient = mediaClient;
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
        Single<Integer> checkFBMD = checkFBMD(filePath);
        Single<Integer> checkEXIF = checkEXIF(filePath);

        Single<Integer> zipResult = Single.zip(duplicateImage, wrongGeoLocation, darkImage, itemTitle,
                (duplicate, wrongGeo, dark, title) -> {
                    Timber.d("Result for duplicate: %d, geo: %d, dark: %d, title: %d", duplicate, wrongGeo, dark, title);
                    return duplicate | wrongGeo | dark | title;
                });
        return Single.zip(zipResult, checkFBMD , checkEXIF , (zip , fbmd , exif)->{
            Timber.d("zip:" + zip + "fbmd:" + fbmd + "exif:" + exif);
            return zip | fbmd | exif;
                });
    }

    /**
     * We want to discourage users from uploading images to Commons that were taken from Facebook.
     * This attempts to detect whether an image was downloaded from Facebook by heuristically
     * searching for metadata that is specific to images that come from Facebook.
     */
    private Single<Integer> checkFBMD(String filepath) {
        return readFBMD.processMetadata(filepath);
    }

    /**
     * We try to minimize uploads from the Commons app that might be copyright violations.
     * If an image does not have any Exif metadata, then it was likely downloaded from the internet,
     * and is probably not an original work by the user. We detect these kinds of images by looking
     * for the presence of some basic Exif metadata.
     */
    private Single<Integer> checkEXIF(String filepath) {
        return EXIFReader.processMetadata(filepath);
    }


    /**
     * Checks item title
     * - empty title
     * - existing title
     *
     * @param uploadItem
     * @return
     */
    private Single<Integer> validateItemTitle(UploadModel.UploadItem uploadItem) {
        Timber.d("Checking for image title %s", uploadItem.getTitle());
        Title title = uploadItem.getTitle();
        if (title.isEmpty()) {
            return Single.just(EMPTY_TITLE);
        }

        return mediaClient.checkPageExistsUsingTitle("File:" + uploadItem.getFileName())
                .map(doesFileExist -> {
                    Timber.d("Result for valid title is %s", doesFileExist);
                    return doesFileExist ? FILE_NAME_EXISTS : IMAGE_OK;
                });
    }

    /**
     * Checks for duplicate image
     *
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    private Single<Integer> checkDuplicateImage(String filePath) {
        Timber.d("Checking for duplicate image %s", filePath);
        return Single.fromCallable(() ->
                fileUtilsWrapper.getFileInputStream(filePath))
                .map(fileUtilsWrapper::getSHA1)
                .flatMap(mediaClient::checkFileExistsUsingSha)
                .map(b -> {
                    Timber.d("Result for duplicate image %s", b);
                    return b ? ImageUtils.IMAGE_DUPLICATE : ImageUtils.IMAGE_OK;
                });
    }

    /**
     * Checks for dark image
     *
     * @param filePath file to be checked
     * @return IMAGE_DARK or IMAGE_OK
     */
    private Single<Integer> checkDarkImage(String filePath) {
        Timber.d("Checking for dark image %s", filePath);
        return imageUtilsWrapper.checkIfImageIsTooDark(filePath);
    }

    /**
     * Checks for image geolocation
     * returns IMAGE_OK if the place is null or if the file doesn't contain a geolocation
     *
     * @param filePath file to be checked
     * @return IMAGE_GEOLOCATION_DIFFERENT or IMAGE_OK
     */
    private Single<Integer> checkImageGeoLocation(Place place, String filePath) {
        Timber.d("Checking for image geolocation %s", filePath);
        if (place == null || StringUtils.isBlank(place.getWikiDataEntityId())) {
            return Single.just(ImageUtils.IMAGE_OK);
        }
        return Single.fromCallable(() -> filePath)
                .map(fileUtilsWrapper::getGeolocationOfFile)
                .flatMap(geoLocation -> {
                    if (StringUtils.isBlank(geoLocation)) {
                        return Single.just(ImageUtils.IMAGE_OK);
                    }
                    return imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, place.getLocation());
                });
    }
}

