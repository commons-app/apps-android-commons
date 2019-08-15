package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
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
 *//*if (dataInBytes[0] == 70 && dataInBytes[1] == 66 && dataInBytes[2] == 77 && dataInBytes[3] == 68) {
                Timber.d("Contains FBMD");
                return Single.just(ImageUtils.FILE_FBMD);
            }*/
@Singleton
public class ImageProcessingService {
    private final FileUtilsWrapper fileUtilsWrapper;
    private final ImageUtilsWrapper imageUtilsWrapper;
    private final MediaWikiApi mwApi;
    private final ReadFBMD readFBMD;
    private final EXIFReader EXIFReader;
    private final Context context;

    @Inject
    public ImageProcessingService(FileUtilsWrapper fileUtilsWrapper,
                                  ImageUtilsWrapper imageUtilsWrapper,
                                  MediaWikiApi mwApi, ReadFBMD readFBMD, EXIFReader EXIFReader,
                                  Context context) {
        this.fileUtilsWrapper = fileUtilsWrapper;
        this.imageUtilsWrapper = imageUtilsWrapper;
        this.mwApi = mwApi;
        this.readFBMD = readFBMD;
        this.EXIFReader = EXIFReader;
        this.context = context;
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
        Uri contentUri=uploadItem.getContentUri();
        Single<Integer> duplicateImage = checkDuplicateImage(filePath);
        Single<Integer> wrongGeoLocation = checkImageGeoLocation(uploadItem.getPlace(), filePath);
        Single<Integer> darkImage = checkDarkImage(filePath);
        Single<Integer> itemTitle = checkTitle ? validateItemTitle(uploadItem) : Single.just(ImageUtils.IMAGE_OK);
        Single<Integer> checkFBMD = checkFBMD(context,contentUri);
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
     * Other than the Image quality we need to check that using this Image doesn't violate's facebook's copyright's.
     * Whenever a user tries to upload an image that was downloaded from Facebook then we warn the user with a message to stop the upload
     * To know whether the Image is downloaded from facebook:
     * -We read the metadata of any Image and check for FBMD
     * -Facebook downloaded image's contains metadata of the type IPTC
     * - From this IPTC metadata we extract a byte array that contains FBMD as it's initials. If the image was downloaded from facebook
     * Thus we successfully protect common's from Facebook's copyright violation
     */

    public Single<Integer> checkFBMD(Context context,Uri contentUri) {
        try {
            return readFBMD.processMetadata(context,contentUri);
        } catch (IOException e) {
            return Single.just(ImageUtils.FILE_FBMD);
        }
    }

    /**
    * To avoid copyright we check for EXIF data in any image.
     * Images that are downloaded from internet generally don't have any EXIF data in them
     * while images taken via camera or screenshots in phone have EXIF data with them.
     * So we check if the image has no EXIF data then we display a warning to the user
     * * */

    public Single<Integer> checkEXIF(String filepath){
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

        return Single.fromCallable(() -> mwApi.fileExistsWithName(uploadItem.getFileName()))
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
                .map(mwApi::existingFile)
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

