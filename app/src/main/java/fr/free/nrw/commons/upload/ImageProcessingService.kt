package fr.free.nrw.commons.upload

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_BANNED_EXTENSION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_BLACKLISTED
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_INVALID
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_TOO_LONG
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_DUPLICATE
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtilsWrapper
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

// MediaWiki rejects file names longer than 240 bytes (filename-toolong)
private const val MAX_FILE_NAME_BYTES = 240

// UploadWorker.findUniqueFileName adds " (12345)" when the file name already exists
private const val UNIQUE_SUFFIX_BYTES = 8

// MediaWiki rejects a file name if any part after a dot is one of these (filetype-banned), for
// example "Visit konga.com.jpg". No API returns this list. It is MediaWiki's default
// $wgProhibitedFileExtensions (includes/MainConfigSchema.php) plus the Wikimedia additions
// in $wmgProhibitedFileExtensions (operations/mediawiki-config, wmf-config/InitialiseSettings.php).
private val BANNED_FILE_EXTENSIONS =
    setOf(
        "html",
        "htm",
        "js",
        "jsb",
        "mhtml",
        "mht",
        "xhtml",
        "xht",
        "php",
        "phtml",
        "php3",
        "php4",
        "php5",
        "phps",
        "phar",
        "shtml",
        "jhtml",
        "pl",
        "py",
        "cgi",
        "exe",
        "scr",
        "dll",
        "msi",
        "vbs",
        "bat",
        "com",
        "pif",
        "cmd",
        "vxd",
        "cpl",
        "xml",
        "txt",
    )

/**
 * Methods for pre-processing images to be uploaded
 */
@Singleton
class ImageProcessingService
    @Suppress("LongParameterList")
    @Inject
    constructor(
        private val fileUtilsWrapper: FileUtilsWrapper,
        private val imageUtilsWrapper: ImageUtilsWrapper,
        private val readFBMD: ReadFBMD,
        private val exifReader: EXIFReader,
        private val mediaClient: MediaClient,
        private val uploadClient: UploadClient,
        private val appContext: Context,
    ) {
        /**
         * Check image quality before upload - checks duplicate image - checks dark image - checks
         * geolocation for image
         *
         * @param uploadItem UploadItem whose quality is to be checked
         * @param inAppPictureLocation In app picture location (if any)
         * @return Quality of UploadItem
         */
        fun validateImage(
            uploadItem: UploadItem,
            inAppPictureLocation: LatLng?,
        ): Single<Int> {
            val currentImageQuality = uploadItem.imageQuality
            Timber.d("Current image quality is %d", currentImageQuality)
            if (currentImageQuality == IMAGE_KEEP || currentImageQuality == IMAGE_OK) {
                return Single.just(IMAGE_OK)
            }

            Timber.d("Checking the validity of image")
            val filePath = uploadItem.mediaUri?.path
            // Geolocation, darkness, FBMD, and EXIF are image-specific checks,
            // so they do not apply to other media types, such as audio.
            val isImage = uploadItem.mimeType?.startsWith("image/") ?: true

            return if (isImage) {
                Single.zip(
                    checkIfFileAlreadyExists(
                        originalFilePath = uploadItem.contentUri!!,
                        modifiedFilePath = uploadItem.mediaUri!!,
                    ),
                    checkImageGeoLocation(uploadItem.place, filePath, inAppPictureLocation),
                    checkDarkImage(filePath!!),
                    checkFBMD(filePath),
                    checkEXIF(filePath),
                ) { duplicateImage: Int, wrongGeoLocation: Int, darkImage: Int, fbmd: Int, exif: Int ->
                    val unsupportedFormatResult =
                        if (uploadItem.hasUnsupportedFormat) {
                            fr.free.nrw.commons.utils.ImageUtils.IMAGE_FORMAT_UNSUPPORTED
                        } else {
                            0
                        }
                    Timber.d(
                        "duplicate: %d, geo: %d, dark: %d, fbmd: %d, exif: %d, unsupported: %d",
                        duplicateImage,
                        wrongGeoLocation,
                        darkImage,
                        fbmd,
                        exif,
                        unsupportedFormatResult,
                    )
                    return@zip duplicateImage or wrongGeoLocation or darkImage or fbmd or exif or
                        unsupportedFormatResult
                }
            } else {
                checkIfFileAlreadyExists(
                    originalFilePath = uploadItem.contentUri!!,
                    modifiedFilePath = uploadItem.mediaUri!!,
                )
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
        private fun checkFBMD(filepath: String?): Single<Int> = readFBMD.processMetadata(filepath)

        /**
         * We try to minimize uploads from the Commons app that might be copyright violations. If an
         * image does not have any Exif metadata, then it was likely downloaded from the internet, and
         * is probably not an original work by the user. We detect these kinds of images by looking for
         * the presence of some basic Exif metadata.
         */
        private fun checkEXIF(filepath: String): Single<Int> = exifReader.processMetadata(filepath)

        /**
         * Checks item caption - empty caption - too long, banned extension, blacklisted or invalid caption -
         * existing caption
         */
        private fun validateItemTitle(uploadItem: UploadItem): Single<Int> {
            Timber.d("Checking for image title %s", uploadItem.uploadMediaDetails)
            val captions = uploadItem.uploadMediaDetails
            val fileNameBytes = uploadItem.filename.toByteArray().size
            val hasBannedExtension =
                uploadItem.filename
                    .split(".")
                    .drop(1)
                    .any { it.lowercase() in BANNED_FILE_EXTENSIONS }
            return when {
                captions.isEmpty() -> Single.just(EMPTY_CAPTION)
                // Commons rejects these names anyway, so there is no need to ask it about the name
                fileNameBytes > MAX_FILE_NAME_BYTES -> Single.just(FILE_NAME_TOO_LONG)
                hasBannedExtension -> Single.just(FILE_NAME_BANNED_EXTENSION)
                else ->
                    Single
                        .zip(
                            uploadClient.checkFileName(uploadItem.filename),
                            mediaClient.checkPageExistsUsingTitle("File:" + uploadItem.filename),
                        ) { fileNameCheck: FileNameCheck, doesFileExist: Boolean ->
                            Timber.d("Result for valid title is %s, file name check %s", doesFileExist, fileNameCheck)
                            // A blacklisted, invalid or too long name can never be uploaded,
                            // so it is reported before an existing name
                            when {
                                fileNameCheck == FileNameCheck.BLACKLISTED -> FILE_NAME_BLACKLISTED
                                fileNameCheck == FileNameCheck.INVALID -> FILE_NAME_INVALID
                                doesFileExist && fileNameBytes > MAX_FILE_NAME_BYTES - UNIQUE_SUFFIX_BYTES ->
                                    FILE_NAME_TOO_LONG
                                doesFileExist -> FILE_NAME_EXISTS
                                else -> IMAGE_OK
                            }
                        }.subscribeOn(Schedulers.io())
            }
        }

        /**
         * Checks if file already exists using original & modified file's SHA
         *
         * @param originalFilePath original file to be checked
         * @param modifiedFilePath modified (after exif modifications) file to be checked
         * @return IMAGE_DUPLICATE or IMAGE_OK
         */
        fun checkIfFileAlreadyExists(
            originalFilePath: Uri,
            modifiedFilePath: Uri,
        ): Single<Int> {
            val originalStreamResult =
                Single
                    .fromCallable {
                        appContext.contentResolver.openInputStream(originalFilePath)!!
                    }.flatMap { inputStream ->
                        checkDuplicateImage(inputStream)
                    }

            val modifiedStreamResult =
                Single
                    .fromCallable {
                        val path = modifiedFilePath.path
                        if (path != null && java.io.File(path).exists()) {
                            fileUtilsWrapper.getFileInputStream(path)
                        } else {
                            throw java.io.FileNotFoundException("Modified file not found at $path")
                        }
                    }.flatMap { inputStream ->
                        checkDuplicateImage(inputStream)
                    }.onErrorResumeNext { throwable ->
                        if (throwable is java.io.FileNotFoundException) {
                            Timber.d("Modified file not found, treating as non-duplicate")
                            Single.just(IMAGE_OK)
                        } else {
                            Single.error(throwable)
                        }
                    }

            return Single.zip(
                originalStreamResult,
                modifiedStreamResult,
            ) { resultForOriginal, resultForDuplicate ->
                return@zip if (resultForOriginal == IMAGE_DUPLICATE || resultForDuplicate == IMAGE_DUPLICATE) {
                    IMAGE_DUPLICATE
                } else {
                    IMAGE_OK
                }
            }
        }

        /**
         * Checks for duplicate image by calculating its SHA1 hash and querying the media client.
         *
         * @param inputStream The input stream of the file to check.
         * @return IMAGE_DUPLICATE if the file exists, or IMAGE_OK otherwise.
         */
        private fun checkDuplicateImage(inputStream: InputStream): Single<Int> =
            Single
                .fromCallable { fileUtilsWrapper.getSHA1(inputStream) }
                .flatMap { fileSha: String? ->
                    mediaClient.checkFileExistsUsingSha(fileSha)
                }.map {
                    Timber.d("Result for duplicate image %s", it)
                    if (it) IMAGE_DUPLICATE else IMAGE_OK
                }.subscribeOn(Schedulers.io())

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
            inAppPictureLocation: LatLng?,
        ): Single<Int> {
            Timber.d("Checking for image geolocation %s", filePath)
            if (place == null || StringUtils.isBlank(place.wikiDataEntityId)) {
                return Single.just(IMAGE_OK)
            }

            return Single
                .fromCallable<String?> { filePath }
                .flatMap { path: String? ->
                    Single.just<String?>(
                        fileUtilsWrapper.getGeolocationOfFile(path!!, inAppPictureLocation),
                    )
                }.flatMap { geoLocation: String? ->
                    if (geoLocation.isNullOrBlank()) {
                        return@flatMap Single.just<Int>(IMAGE_OK)
                    }
                    imageUtilsWrapper.checkImageGeolocationIsDifferent(geoLocation, place.getLocation())
                }.subscribeOn(Schedulers.io())
        }
    }
