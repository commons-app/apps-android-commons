package fr.free.nrw.commons.upload

import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_DUPLICATE
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtilsWrapper
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Methods for pre-processing images to be uploaded
 */
@Singleton
class ImageProcessingService @Inject constructor(
    private val fileUtilsWrapper: FileUtilsWrapper,
    private val imageUtilsWrapper: ImageUtilsWrapper,
    private val readFBMD: ReadFBMD,
    private val EXIFReader: EXIFReader,
    private val mediaClient: MediaClient
) {
    /**
     * Check image quality before upload - checks duplicate image - checks dark image - checks
     * geolocation for image
     *
     * @param uploadItem UploadItem whose quality is to be checked
     * @param inAppPictureLocation In app picture location (if any)
     * @return Quality of UploadItem
     */
    fun validateImage(uploadItem: UploadItem, inAppPictureLocation: LatLng?): Single<Int> {
        val currentImageQuality = uploadItem.imageQuality
        Timber.d("Current image quality is %d", currentImageQuality)
        if (currentImageQuality == IMAGE_KEEP || currentImageQuality == IMAGE_OK) {
            return Single.just(IMAGE_OK)
        }

        Timber.d("Checking the validity of image")
        val filePath = uploadItem.mediaUri.path

        return Single.zip(
            checkDuplicateImage(filePath),
            checkImageGeoLocation(uploadItem.place, filePath, inAppPictureLocation),
            checkDarkImage(filePath!!),
            checkFBMD(filePath),
            checkEXIF(filePath)
        ) { duplicateImage: Int, wrongGeoLocation: Int, darkImage: Int, fbmd: Int, exif: Int ->
            Timber.d(
                "duplicate: %d, geo: %d, dark: %d, fbmd: %d, exif: %d",
                duplicateImage, wrongGeoLocation, darkImage, fbmd, exif
            )
            return@zip duplicateImage or wrongGeoLocation or darkImage or fbmd or exif
        }
    }

    /**
     * Checks caption of the given UploadItem
     *
     * @param uploadItem UploadItem whose caption is to be verified
     * @return Quality of caption of the UploadItem
     */
    fun validateCaption(uploadItem: UploadItem): Single<Int> {
        val currentImageQuality = uploadItem.imageQuality
        Timber.d("Current image quality is %d", currentImageQuality)
        if (currentImageQuality == IMAGE_KEEP) {
            return Single.just(IMAGE_OK)
        }
        Timber.d("Checking the validity of caption")

        return validateItemTitle(uploadItem)
    }

    /**
     * We want to discourage users from uploading images to Commons that were taken from Facebook.
     * This attempts to detect whether an image was downloaded from Facebook by heuristically
     * searching for metadata that is specific to images that come from Facebook.
     */
    private fun checkFBMD(filepath: String?): Single<Int> =
        readFBMD.processMetadata(filepath)

    /**
     * We try to minimize uploads from the Commons app that might be copyright violations. If an
     * image does not have any Exif metadata, then it was likely downloaded from the internet, and
     * is probably not an original work by the user. We detect these kinds of images by looking for
     * the presence of some basic Exif metadata.
     */
    private fun checkEXIF(filepath: String): Single<Int> =
        EXIFReader.processMetadata(filepath)


    /**
     * Checks item caption - empty caption - existing caption
     */
    private fun validateItemTitle(uploadItem: UploadItem): Single<Int> {
        Timber.d("Checking for image title %s", uploadItem.uploadMediaDetails)
        val captions = uploadItem.uploadMediaDetails
        if (captions.isEmpty()) {
            return Single.just(EMPTY_CAPTION)
        }

        return mediaClient.checkPageExistsUsingTitle("File:" + uploadItem.filename)
            .map { doesFileExist: Boolean ->
                Timber.d("Result for valid title is %s", doesFileExist)
                if (doesFileExist) FILE_NAME_EXISTS else IMAGE_OK
            }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Checks for duplicate image
     *
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    fun checkDuplicateImage(filePath: String?): Single<Int> {
        Timber.d("Checking for duplicate image %s", filePath)
        return Single.fromCallable { fileUtilsWrapper.getFileInputStream(filePath) }
            .map { stream: FileInputStream? ->
                fileUtilsWrapper.getSHA1(stream)
            }
            .flatMap { fileSha: String? ->
                mediaClient.checkFileExistsUsingSha(fileSha)
            }
            .map {
                Timber.d("Result for duplicate image %s", it)
                if (it) IMAGE_DUPLICATE else IMAGE_OK
            }
            .subscribeOn(Schedulers.io())
    }

    /**
     * Checks for dark image
     *
     * @param filePath file to be checked
     * @return IMAGE_DARK or IMAGE_OK
     */
    private fun checkDarkImage(filePath: String): Single<Int> {
        Timber.d("Checking for dark image %s", filePath)
        return imageUtilsWrapper.checkIfImageIsTooDark(filePath)
    }

    /**
     * Checks for image geolocation returns IMAGE_OK if the place is null or if the file doesn't
     * contain a geolocation
     *
     * @param filePath file to be checked
     * @return IMAGE_GEOLOCATION_DIFFERENT or IMAGE_OK
     */
    private fun checkImageGeoLocation(
        place: Place?,
        filePath: String?,
        inAppPictureLocation: LatLng?
    ): Single<Int> {
        Timber.d("Checking for image geolocation %s", filePath)
        if (place == null || StringUtils.isBlank(place.wikiDataEntityId)) {
            return Single.just(IMAGE_OK)
        }

        return Single.fromCallable<String?> { filePath }
            .flatMap { path: String? ->
                Single.just<String?>(
                    fileUtilsWrapper.getGeolocationOfFile(path!!, inAppPictureLocation)
                )
            }
            .flatMap { geoLocation: String? ->
                if (geoLocation.isNullOrBlank()) {
                    return@flatMap Single.just<Int>(IMAGE_OK)
                }
                imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, place.getLocation())
            }
            .subscribeOn(Schedulers.io())
    }
}

