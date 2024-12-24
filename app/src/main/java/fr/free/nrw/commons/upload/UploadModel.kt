package fr.free.nrw.commons.upload

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.FileUtils.getSHA1
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class UploadModel @Inject internal constructor(
    @param:Named("licenses") val licenses: List<String>,
    @param:Named("default_preferences") val store: JsonKvStore,
    @param:Named("licenses_by_name") val licensesByName: Map<String, String>,
    val context: Context,
    val sessionManager: SessionManager,
    val fileProcessor: FileProcessor,
    val imageProcessingService: ImageProcessingService
) {
    var license: String? = store.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3)
    val items: MutableList<UploadItem> = mutableListOf()
    val compositeDisposable: CompositeDisposable = CompositeDisposable()
    val selectedCategories: MutableList<String> = mutableListOf()
    val selectedDepictions: MutableList<DepictedItem> = mutableListOf()

    /**
     * Existing depicts which are selected
     */
    var selectedExistingDepictions: MutableList<String> = mutableListOf()
    val count: Int
        get() = items.size

    val uploads: List<UploadItem>
        get() = items

    var selectedLicense: String?
        get() = license
        set(licenseName) {
            license = licensesByName[licenseName]
            if (license == null) {
                store.remove(Prefs.DEFAULT_LICENSE)
            } else {
                store.putString(Prefs.DEFAULT_LICENSE, license!!)
            }
        }

    /**
     * cleanup the resources, I am Singleton, preparing for fresh upload
     */
    fun cleanUp() {
        compositeDisposable.clear()
        fileProcessor.cleanup()
        items.clear()
        selectedCategories.clear()
        selectedDepictions.clear()
        selectedExistingDepictions.clear()
    }

    fun setSelectedCategories(categories: List<String>) {
        selectedCategories.clear()
        selectedCategories.addAll(categories)
    }

    /**
     * pre process a one item at a time
     */
    fun preProcessImage(
        uploadableFile: UploadableFile?,
        place: Place?,
        similarImageInterface: SimilarImageInterface?,
        inAppPictureLocation: LatLng?
    ): Observable<UploadItem> = Observable.just(
        createAndAddUploadItem(uploadableFile, place, similarImageInterface, inAppPictureLocation)
    )

    /**
     * Calls validateImage() of ImageProcessingService to check quality of image
     *
     * @param uploadItem UploadItem whose quality is to be checked
     * @param inAppPictureLocation In app picture location (if any)
     * @return Quality of UploadItem
     */
    fun getImageQuality(uploadItem: UploadItem, inAppPictureLocation: LatLng?): Single<Int> =
        imageProcessingService.validateImage(uploadItem, inAppPictureLocation)

    /**
     * Calls checkDuplicateImage() of ImageProcessingService to check if image is duplicate
     *
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    fun checkDuplicateImage(filePath: String?): Single<Int> =
        imageProcessingService.checkDuplicateImage(filePath)

    /**
     * Calls validateCaption() of ImageProcessingService to check caption of image
     *
     * @param uploadItem UploadItem whose caption is to be checked
     * @return Quality of caption of the UploadItem
     */
    fun getCaptionQuality(uploadItem: UploadItem): Single<Int> =
        imageProcessingService.validateCaption(uploadItem)

    private fun createAndAddUploadItem(
        uploadableFile: UploadableFile?,
        place: Place?,
        similarImageInterface: SimilarImageInterface?,
        inAppPictureLocation: LatLng?
    ): UploadItem {
        val dateTimeWithSource = uploadableFile?.getFileCreatedDate(context)
        var fileCreatedDate: Long = -1
        var createdTimestampSource = ""
        var fileCreatedDateString: String? = ""
        if (dateTimeWithSource != null) {
            fileCreatedDate = dateTimeWithSource.epochDate
            fileCreatedDateString = dateTimeWithSource.dateString
            createdTimestampSource = dateTimeWithSource.source
        }
        Timber.d("File created date is %d", fileCreatedDate)
        val imageCoordinates = fileProcessor
            .processFileCoordinates(
                similarImageInterface, uploadableFile?.getFilePath(),
                inAppPictureLocation
            )
        val uploadItem = UploadItem(
            Uri.parse(uploadableFile?.getFilePath()),
            uploadableFile?.getMimeType(context), imageCoordinates, place, fileCreatedDate,
            createdTimestampSource,
            uploadableFile?.contentUri,
            fileCreatedDateString
        )

        // If an uploadItem of the same uploadableFile has been created before, we return that.
        // This is to avoid multiple instances of uploadItem of same file passed around.
        if (items.contains(uploadItem)) {
            return items[items.indexOf(uploadItem)]
        }

        uploadItem.uploadMediaDetails[0] = UploadMediaDetail(place)
        if (!items.contains(uploadItem)) {
            items.add(uploadItem)
        }
        return uploadItem
    }

    fun buildContributions(): Observable<Contribution> {
        return Observable.fromIterable(items).map { item: UploadItem ->
            val imageSHA1 = getSHA1(
                context.contentResolver.openInputStream(item.contentUri!!)!!
            )
            val contribution = Contribution(
                item,
                sessionManager,
                buildList { addAll(selectedDepictions) },
                buildList { addAll(selectedCategories) },
                imageSHA1
            )

            contribution.setHasInvalidLocation(item.hasInvalidLocation())

            Timber.d(
                "Created timestamp while building contribution is %s, %s",
                item.createdTimestamp,
                Date(item.createdTimestamp!!)
            )

            if (item.createdTimestamp != -1L) {
                contribution.dateCreated = Date(item.createdTimestamp)
                contribution.dateCreatedSource = item.createdTimestampSource
                //Set the date only if you have it, else the upload service is gonna try it the other way
            }

            if (contribution.wikidataPlace != null) {
                contribution.wikidataPlace!!.isMonumentUpload = item.isWLMUpload
            }
            contribution.countryCode = item.countryCode
            contribution
        }
    }

    fun deletePicture(filePath: String) {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().mediaUri.toString().contains(filePath)) {
                iterator.remove()
                break
            }
        }
        if (items.isEmpty()) {
            cleanUp()
        }
    }

    fun onDepictItemClicked(depictedItem: DepictedItem, media: Media?) {
        if (media == null) {
            if (depictedItem.isSelected) {
                selectedDepictions.add(depictedItem)
            } else {
                selectedDepictions.remove(depictedItem)
            }
        } else {
            if (depictedItem.isSelected) {
                if (media.depictionIds.contains(depictedItem.id)) {
                    selectedExistingDepictions.add(depictedItem.id)
                } else {
                    selectedDepictions.add(depictedItem)
                }
            } else {
                if (media.depictionIds.contains(depictedItem.id)) {
                    selectedExistingDepictions.remove(depictedItem.id)
                    if (!media.depictionIds.contains(depictedItem.id)) {
                        media.depictionIds = mutableListOf<String>().apply {
                            add(depictedItem.id)
                            addAll(media.depictionIds)
                        }
                    }
                } else {
                    selectedDepictions.remove(depictedItem)
                }
            }
        }
    }

    fun useSimilarPictureCoordinates(imageCoordinates: ImageCoordinates, uploadItemIndex: Int) {
        fileProcessor.prePopulateCategoriesAndDepictionsBy(imageCoordinates)
        items[uploadItemIndex].gpsCoords = imageCoordinates
    }
}
