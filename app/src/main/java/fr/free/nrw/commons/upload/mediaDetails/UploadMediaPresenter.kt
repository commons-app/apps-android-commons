package fr.free.nrw.commons.upload.mediaDetails

import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.IO_THREAD
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.MAIN_THREAD
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.UploadActivity.Companion.setUploadIsOfAPlace
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback
import fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_BANNED_EXTENSION
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_BLACKLISTED
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_INVALID
import fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_TOO_LONG
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.github.coordinates2country.Coordinates2Country
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.UnknownHostException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class UploadMediaPresenter
    @Inject
    constructor(
        repository: UploadRepository,
        @param:Named(IO_THREAD) private val ioScheduler: Scheduler,
        @param:Named(MAIN_THREAD) private val mainThreadScheduler: Scheduler,
    ) : BaseUploadMediaPresenter(repository) {
        private val compositeDisposable = CompositeDisposable()

        override val qualityHelper: UploadMediaQualityHelper =
            UploadMediaQualityHelper(repository, mainThreadScheduler, compositeDisposable) { view }

        private val countryNamesAndCodes: Map<String, String> by lazy {
            // Create a map containing all ISO countries 2-letter codes provided by
            // `Locale.getISOCountries()` and their english names
            buildMap {
                Locale.getISOCountries().forEach {
                    put(Locale("en", it).getDisplayCountry(Locale.ENGLISH), it)
                }
            }
        }

        override fun onAttachView(view: UploadMediaDetailsContract.View) {
            this.view = view
        }

        override fun onDetachView() {
            view = DUMMY
            compositeDisposable.clear()
        }

        /**
         * Sets the Upload Media Details for the corresponding upload item
         */
        override fun setUploadMediaDetails(
            uploadMediaDetails: List<UploadMediaDetail>,
            uploadItemIndex: Int,
        ) {
            val uploadItems = repository.getUploads()
            if (uploadItemIndex >= 0 && uploadItemIndex < uploadItems.size) {
                if (uploadMediaDetails.isNotEmpty()) {
                    uploadItems[uploadItemIndex].uploadMediaDetails =
                        uploadMediaDetails as? MutableList<UploadMediaDetail> ?: uploadMediaDetails.toMutableList()
                    Timber.d("Set uploadMediaDetails for index %d, size %d", uploadItemIndex, uploadMediaDetails.size)
                } else {
                    uploadItems[uploadItemIndex].uploadMediaDetails = mutableListOf(UploadMediaDetail())
                    Timber.w("Received empty uploadMediaDetails for index %d, initialized default", uploadItemIndex)
                }
            } else {
                Timber.e(
                    "Invalid index %d for uploadItems size %d, skipping setUploadMediaDetails",
                    uploadItemIndex,
                    uploadItems.size,
                )
            }
        }

        /**
         * Receives the corresponding uploadable file, processes it and return the view with and uplaod item
         */
        override fun receiveImage(
            uploadableFile: UploadableFile?,
            place: Place?,
            inAppPictureLocation: LatLng?,
        ) {
            view.showProgress(true)
            compositeDisposable.add(
                repository
                    .preProcessImage(
                        uploadableFile,
                        place,
                        this,
                        inAppPictureLocation,
                    ).map { uploadItem: UploadItem ->
                        if (place != null && place.isMonument && place.location != null) {
                            val countryCode =
                                countryNamesAndCodes[
                                    Coordinates2Country.country(
                                        place.location.latitude,
                                        place.location.longitude,
                                    ),
                                ]
                            if (countryCode != null && WLM_SUPPORTED_COUNTRIES.contains(countryCode.lowercase())) {
                                uploadItem.isWLMUpload = true
                                uploadItem.countryCode = countryCode.lowercase()
                            }
                        }
                        uploadItem
                    }.subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ uploadItem: UploadItem ->
                        view.onImageProcessed(uploadItem)
                        view.updateMediaDetails(uploadItem.uploadMediaDetails)
                        view.showProgress(false)
                        val gpsCoords = uploadItem.gpsCoords
                        val hasImageCoordinates = gpsCoords != null && gpsCoords.imageCoordsExists

                        // Only check for nearby places if image has coordinates AND no place was pre-selected
                        // This prevents the popup from appearing when uploading from Nearby feature
                        if (hasImageCoordinates && place == null && uploadItem.place == null) {
                            checkNearbyPlaces(uploadItem)
                        }
                    }, { throwable: Throwable? ->
                        Timber.e(throwable, "Error occurred in processing images")
                    }),
            )
        }

        /**
         * This method checks for the nearest location that needs images and suggests it to the user.
         */
        private fun checkNearbyPlaces(uploadItem: UploadItem) {
            compositeDisposable.add(
                Maybe
                    .fromCallable {
                        repository.checkNearbyPlaces(
                            uploadItem.gpsCoords!!.decLatitude,
                            uploadItem.gpsCoords!!.decLongitude,
                        )
                    }.subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe({
                        view.onNearbyPlaceFound(uploadItem, it)
                    }, { throwable: Throwable? ->
                        Timber.e(throwable, "Error occurred in processing images")
                    }),
            )
        }

        /**
         * Checks if the image has a location. Displays a dialog alerting user that no
         * location has been to added to the image and asking them to add one, if location was not
         * removed by the user
         *
         * @param uploadItemIndex Index of the uploadItem which has no location
         * @param inAppPictureLocation In app picture location (if any)
         * @param hasUserRemovedLocation True if user has removed location from the image
         */
        override fun displayLocDialog(
            uploadItemIndex: Int,
            inAppPictureLocation: LatLng?,
            hasUserRemovedLocation: Boolean,
        ) {
            val uploadItem = repository.getUploads()[uploadItemIndex]
            // The location prompt is only meant for images, not for other media types, such as audio.
            val isImage = uploadItem.mimeType?.startsWith("image/") ?: true
            val hasNoLocation =
                uploadItem.gpsCoords!!.decimalCoords == null &&
                    inAppPictureLocation == null &&
                    !hasUserRemovedLocation
            if (isImage && hasNoLocation) {
                view.displayAddLocationDialog { verifyCaptionQuality(uploadItem) }
            } else {
                verifyCaptionQuality(uploadItem)
            }
        }

        /**
         * Verifies the image's caption and calls function to handle the result
         *
         * @param uploadItem UploadItem whose caption is checked
         */
        private fun verifyCaptionQuality(uploadItem: UploadItem) {
            view.showProgress(true)
            compositeDisposable.add(
                repository
                    .getCaptionQuality(uploadItem)
                    .observeOn(mainThreadScheduler)
                    .subscribe({ capResult: Int ->
                        view.showProgress(false)
                        handleCaptionResult(capResult, uploadItem)
                    }, { throwable: Throwable ->
                        view.showProgress(false)
                        if (throwable is UnknownHostException) {
                            view.showConnectionErrorPopupForCaptionCheck()
                        } else {
                            view.showMessage(throwable.localizedMessage, R.color.color_error)
                        }
                        Timber.e(throwable, "Error occurred while handling image")
                    }),
            )
        }

        /**
         * Handles image's caption results and shows dialog if necessary
         *
         * @param errorCode Error code of the UploadItem
         * @param uploadItem UploadItem whose caption is checked
         */
        fun handleCaptionResult(
            errorCode: Int,
            uploadItem: UploadItem,
        ) {
            // If errorCode is empty caption show message
            if (errorCode == EMPTY_CAPTION) {
                Timber.d("Captions are empty. Showing toast")
                view.showMessage(R.string.add_caption_toast, R.color.color_error)
            }

            // Commons would only reject a blacklisted file name after the whole file is uploaded
            if (errorCode == FILE_NAME_BLACKLISTED) {
                Timber.d("File name is blacklisted. Showing toast")
                view.showMessage(R.string.file_name_blacklisted_toast, R.color.color_error)
            }

            // Commons would reject a file name that is too long, also after the whole file is uploaded
            if (errorCode == FILE_NAME_TOO_LONG) {
                Timber.d("File name is too long. Showing toast")
                view.showMessage(R.string.file_name_too_long_toast, R.color.color_error)
            }

            // Commons would reject a file name that is not a valid title when the upload starts
            if (errorCode == FILE_NAME_INVALID) {
                Timber.d("File name is not valid. Showing toast")
                view.showMessage(R.string.file_name_invalid_toast, R.color.color_error)
            }

            // Commons would reject a file name with a banned extension in it when the upload starts
            if (errorCode == FILE_NAME_BANNED_EXTENSION) {
                Timber.d("File name has a banned extension. Showing toast")
                view.showMessage(R.string.file_name_banned_extension_toast, R.color.color_error)
            }

            // If image with same file name exists check the bit in errorCode is set or not
            if ((errorCode and FILE_NAME_EXISTS) != 0) {
                Timber.d("Trying to show duplicate picture popup")
                view.showDuplicatePicturePopup(uploadItem)
            }

            // If caption is not duplicate or user still wants to upload it
            if (errorCode == IMAGE_OK) {
                Timber.d("Image captions are okay or user still wants to upload it")
                view.onImageValidationSuccess()
            }
        }

        override fun useSimilarPictureCoordinates(
            imageCoordinates: ImageCoordinates,
            uploadItemIndex: Int,
        ) = repository.useSimilarPictureCoordinates(imageCoordinates, uploadItemIndex)

        /**
         * Updates the information regarding the specified place for the specified upload item
         * when the user confirms the suggested nearby place.
         *
         * @param place The place to be associated with the uploads.
         * @param uploadItemIndex Index of the uploadItem whose detected place has been confirmed
         */
        override fun onUserConfirmedUploadIsOfPlace(
            place: Place?,
            uploadItemIndex: Int,
        ) {
            val uploadItem = repository.getUploads()[uploadItemIndex]

            uploadItem.place = place
            val uploadMediaDetails = uploadItem.uploadMediaDetails
            // Update UploadMediaDetail object for this UploadItem
            uploadMediaDetails[0] = UploadMediaDetail(place)

            // Now that the UploadItem and its associated UploadMediaDetail objects have been updated,
            // update the view with the modified media details of the first upload item
            view.updateMediaDetails(uploadMediaDetails)
            setUploadIsOfAPlace(true)
        }

        companion object {
            private val WLM_SUPPORTED_COUNTRIES =
                listOf(
                    "am",
                    "at",
                    "az",
                    "br",
                    "hr",
                    "sv",
                    "fi",
                    "fr",
                    "de",
                    "gh",
                    "in",
                    "ie",
                    "il",
                    "mk",
                    "my",
                    "mt",
                    "pk",
                    "pe",
                    "pl",
                    "ru",
                    "rw",
                    "si",
                    "es",
                    "se",
                    "tw",
                    "ug",
                    "ua",
                    "us",
                )

            val DUMMY: UploadMediaDetailsContract.View =
                Proxy.newProxyInstance(
                    UploadMediaDetailsContract.View::class.java.classLoader,
                    arrayOf<Class<*>>(UploadMediaDetailsContract.View::class.java),
                ) { _: Any?, _: Method?, _: Array<Any?>? -> null } as UploadMediaDetailsContract.View

            var presenterCallback: UploadMediaDetailFragmentCallback? = null

            /**
             * Variable used to determine if the battery-optimisation dialog is being shown or not
             */
            var isBatteryDialogShowing: Boolean = false

            var isCategoriesDialogShowing: Boolean = false
        }
    }
