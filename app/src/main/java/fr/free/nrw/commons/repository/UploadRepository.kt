package fr.free.nrw.commons.repository

import android.net.Uri
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoriesModel
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyPlaces
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.SimilarImageInterface
import fr.free.nrw.commons.upload.UploadController
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadModel
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface defining category operations during upload.
 */
interface UploadCategoryOperations {
    /**
     * Fetches and returns the selected categories for the current upload
     *
     * @return
     */
    fun getSelectedCategories(): List<CategoryItem>

    /**
     * All categories from MWApi
     *
     * @param query
     * @param imageTitleList
     * @param selectedDepictions
     * @return
     */
    fun searchAll(
        query: String,
        imageTitleList: List<String>,
        selectedDepictions: List<DepictedItem>,
    ): Observable<List<CategoryItem>>

    /**
     * Sets the list of selected categories for the current upload
     *
     * @param categoryStringList
     */
    fun setSelectedCategories(categoryStringList: List<String>)

    /**
     * Handles the category selection/deselection
     *
     * @param categoryItem
     */
    fun onCategoryClicked(
        categoryItem: CategoryItem,
        media: Media?,
    )

    /**
     * Prunes the category list for irrelevant categories see #750
     *
     * @param name
     * @return
     */
    fun isSpammyCategory(name: String): Boolean

    /**
     * Gets the category for each unique {@link Place} associated with an {@link UploadItem}
     * from {@link #getUploads()}
     *
     * @return a single that provides the categories
     */
    fun getPlaceCategories(): Single<List<CategoryItem>>

    /**
     * Provides selected existing categories
     *
     * @return selected existing categories
     */
    fun getSelectedExistingCategories(): List<String>

    /**
     * Initialize existing categories
     *
     * @param selectedExistingCategories existing categories
     */
    fun setSelectedExistingCategories(selectedExistingCategories: List<String>)

    /**
     * Takes category names and Gets CategoryItem from the server
     *
     * @param categories names of Category
     * @return Observable<List<CategoryItem>>
     */
    fun getCategories(categories: List<String>): Observable<List<CategoryItem>>
}

/**
 * Interface defining depiction operations during upload.
 */
interface UploadDepictionOperations {
    fun onDepictItemClicked(
        depictedItem: DepictedItem,
        media: Media?,
    )

    /**
     * Fetches and returns the selected depictions for the current upload
     *
     * @return
     */
    fun getSelectedDepictions(): List<DepictedItem>

    /**
     * Provides selected existing depicts
     *
     * @return selected existing depicts
     */
    fun getSelectedExistingDepictions(): List<String>

    /**
     * Initialize existing depicts
     *
     * @param selectedExistingDepictions existing depicts
     */
    fun setSelectedExistingDepictions(selectedExistingDepictions: List<String>)

    /**
     * Gets the depiction for each unique {@link Place} associated with an {@link UploadItem}
     * from {@link #getUploads()}
     *
     * @return a single that provides the depictions
     */
    fun getPlaceDepictions(): Single<List<DepictedItem>>

    /**
     * Takes depict IDs as a parameter, converts into a slash separated String and Gets DepictItem
     * from the server
     *
     * @param depictionsQIDs IDs of Depiction
     * @return Flowable<List<DepictedItem>>
     */
    fun getDepictions(depictionsQIDs: List<String>): Flowable<List<DepictedItem>>
}

/**
 * Interface defining license operations during upload.
 */
interface UploadLicenseOperations {
    /**
     * Returns the string list of available licenses from the LocalDataSource
     *
     * @return
     */
    fun getLicenses(): List<String>

    /**
     * Returns the selected license for the current upload
     *
     * @return
     */
    fun getSelectedLicense(): String?

    /**
     * set selected license for the current upload
     *
     * @param licenseName
     */
    fun setSelectedLicense(licenseName: String?)

    fun isWMLSupportedForThisPlace(): Boolean
}

/**
 * Interface defining image quality and coordinates operations during upload.
 */
interface UploadQualityOperations {
    /**
     * Query the RemoteDataSource for image quality
     *
     * @param uploadItem UploadItem whose caption is to be checked
     * @param location Location of the image
     * @return Quality of UploadItem
     */
    fun getImageQuality(
        uploadItem: UploadItem,
        location: LatLng?,
    ): Single<Int>

    /**
     * Queries the RemoteDataSource to check if the image is a duplicate.
     *
     * @param originalFilePath The original file to be checked.
     * @param modifiedFilePath The modified version of the file (if any).
     * @return IMAGE_DUPLICATE if the image already exists, otherwise IMAGE_OK.
     */
    fun checkDuplicateImage(
        originalFilePath: Uri?,
        modifiedFilePath: Uri?,
    ): Single<Int>

    /**
     * query the RemoteDataSource for caption quality
     *
     * @param uploadItem UploadItem whose caption is to be checked
     * @return Quality of caption of the UploadItem
     */
    fun getCaptionQuality(uploadItem: UploadItem): Single<Int>

    fun useSimilarPictureCoordinates(
        imageCoordinates: ImageCoordinates,
        uploadItemIndex: Int,
    )
}

/**
 * Interface defining upload item operations.
 */
interface UploadItemOperations {
    /**
     * Returns the number of Upload Items
     *
     * @return
     */
    fun getCount(): Int

    /**
     * asks the LocalDataSource to delete the file with the given file path
     *
     * @param filePath
     */
    fun deletePicture(filePath: String)

    /**
     * fetches and returns the upload item
     *
     * @param index
     * @return
     */
    fun getUploadItem(index: Int): UploadItem?
}

/**
 * Default implementation of [UploadCategoryOperations].
 */
class DefaultUploadCategoryOperations(
    private val categoriesModel: CategoriesModel,
    private val uploadModel: UploadModel,
) : UploadCategoryOperations {
    override fun getSelectedCategories(): List<CategoryItem> = categoriesModel.getSelectedCategories()

    override fun searchAll(
        query: String,
        imageTitleList: List<String>,
        selectedDepictions: List<DepictedItem>,
    ): Observable<List<CategoryItem>> = categoriesModel.searchAll(query, imageTitleList, selectedDepictions)

    override fun setSelectedCategories(categoryStringList: List<String>) {
        uploadModel.setSelectedCategories(categoryStringList)
    }

    override fun onCategoryClicked(
        categoryItem: CategoryItem,
        media: Media?,
    ) {
        categoriesModel.onCategoryItemClicked(categoryItem, media)
    }

    override fun isSpammyCategory(name: String): Boolean = categoriesModel.isSpammyCategory(name)

    override fun getPlaceCategories(): Single<List<CategoryItem>> {
        val qids = mutableSetOf<String>()
        uploadModel.uploads.forEach { item ->
            item.place?.category?.let { qids.add(it) }
        }
        return Single.fromObservable(categoriesModel.getCategoriesByName(qids.toList()))
    }

    override fun getSelectedExistingCategories(): List<String> = categoriesModel.getSelectedExistingCategories()

    override fun setSelectedExistingCategories(selectedExistingCategories: List<String>) {
        categoriesModel.setSelectedExistingCategories(
            selectedExistingCategories.toMutableList(),
        )
    }

    override fun getCategories(categories: List<String>): Observable<List<CategoryItem>> =
        categoriesModel
            .getCategoriesByName(categories)
            ?.map { it.toList() } ?: Observable.empty()
}

/**
 * Default implementation of [UploadDepictionOperations].
 */
class DefaultUploadDepictionOperations(
    private val depictModel: DepictModel,
    private val uploadModel: UploadModel,
) : UploadDepictionOperations {
    override fun onDepictItemClicked(
        depictedItem: DepictedItem,
        media: Media?,
    ) {
        uploadModel.onDepictItemClicked(depictedItem, media)
    }

    override fun getSelectedDepictions(): List<DepictedItem> = uploadModel.selectedDepictions

    override fun getSelectedExistingDepictions(): List<String> = uploadModel.selectedExistingDepictions

    override fun setSelectedExistingDepictions(selectedExistingDepictions: List<String>) {
        uploadModel.selectedExistingDepictions = selectedExistingDepictions.toMutableList()
    }

    override fun getPlaceDepictions(): Single<List<DepictedItem>> {
        val qids = mutableSetOf<String>()
        uploadModel.uploads.forEach { item ->
            item.place?.let {
                it.wikiDataEntityId?.let { it1 ->
                    qids.add(it1)
                }
            }
        }
        return depictModel.getPlaceDepictions(qids.toList())
    }

    override fun getDepictions(depictionsQIDs: List<String>): Flowable<List<DepictedItem>> {
        val ids = joinQIDs(depictionsQIDs) ?: ""
        return depictModel.getDepictions(ids).toFlowable()
    }

    /**
     * Builds a string by joining all IDs divided by "|"
     *
     * @param depictionsQIDs IDs of depiction ex. ["Q11023","Q1356"]
     * @return string ex. "Q11023|Q1356"
     */
    private fun joinQIDs(depictionsQIDs: List<String>?): String? =
        depictionsQIDs
            ?.takeIf {
                it.isNotEmpty()
            }?.joinToString("|")
}

/**
 * Default implementation of [UploadLicenseOperations].
 */
class DefaultUploadLicenseOperations(
    private val uploadModel: UploadModel,
) : UploadLicenseOperations {
    override fun getLicenses(): List<String> = uploadModel.licenses

    override fun getSelectedLicense(): String? = uploadModel.selectedLicense

    override fun setSelectedLicense(licenseName: String?) {
        uploadModel.selectedLicense = licenseName
    }

    override fun isWMLSupportedForThisPlace(): Boolean = uploadModel.items.firstOrNull()?.isWLMUpload == true
}

/**
 * Default implementation of [UploadQualityOperations].
 */
class DefaultUploadQualityOperations(
    private val uploadModel: UploadModel,
) : UploadQualityOperations {
    override fun getImageQuality(
        uploadItem: UploadItem,
        location: LatLng?,
    ): Single<Int> = uploadModel.imageProcessingService.validateImage(uploadItem, location)

    override fun checkDuplicateImage(
        originalFilePath: Uri?,
        modifiedFilePath: Uri?,
    ): Single<Int> =
        uploadModel.imageProcessingService.checkIfFileAlreadyExists(
            originalFilePath = originalFilePath!!,
            modifiedFilePath = modifiedFilePath!!,
        )

    override fun getCaptionQuality(uploadItem: UploadItem): Single<Int> =
        uploadModel.imageProcessingService.validateCaption(uploadItem)

    override fun useSimilarPictureCoordinates(
        imageCoordinates: ImageCoordinates,
        uploadItemIndex: Int,
    ) {
        uploadModel.useSimilarPictureCoordinates(
            imageCoordinates,
            uploadItemIndex,
        )
    }
}

/**
 * Default implementation of [UploadItemOperations].
 */
class DefaultUploadItemOperations(
    private val uploadModel: UploadModel,
) : UploadItemOperations {
    override fun getCount(): Int = uploadModel.count

    override fun deletePicture(filePath: String) {
        uploadModel.deletePicture(filePath)
    }

    override fun getUploadItem(index: Int): UploadItem? =
        if (index >= 0) {
            uploadModel.items.getOrNull(index)
        } else {
            null // There is no item to copy details
        }
}

/**
 * The repository class for UploadActivity
 */
@Singleton
class UploadRepository
    @Inject
    constructor(
        private val uploadModel: UploadModel,
        private val uploadController: UploadController,
        private val categoriesModel: CategoriesModel,
        private val nearbyPlaces: NearbyPlaces,
        private val depictModel: DepictModel,
        private val contributionDao: ContributionDao,
    ) : UploadCategoryOperations by DefaultUploadCategoryOperations(categoriesModel, uploadModel),
        UploadDepictionOperations by DefaultUploadDepictionOperations(depictModel, uploadModel),
        UploadLicenseOperations by DefaultUploadLicenseOperations(uploadModel),
        UploadQualityOperations by DefaultUploadQualityOperations(uploadModel),
        UploadItemOperations by DefaultUploadItemOperations(uploadModel) {
        companion object {
            private const val NEARBY_RADIUS_IN_KILO_METERS = 0.1 // 100 meters
        }

        /**
         * Asks the RemoteDataSource to build contributions
         *
         * @return
         */
        fun buildContributions(): Observable<Contribution> = uploadModel.buildContributions()

        /**
         * Asks the RemoteDataSource to start upload for the contribution
         *
         * @param contribution
         */
        fun prepareMedia(contribution: Contribution) {
            uploadController.prepareMedia(contribution)
        }

        fun saveContribution(contribution: Contribution) {
            contributionDao.save(contribution).blockingAwait()
        }

        /**
         * Fetches and returns all the Upload Items
         *
         * @return
         */
        fun getUploads(): List<UploadItem> = uploadModel.uploads

        /**
         * Prepare for a fresh upload
         */
        fun cleanup() {
            uploadModel.cleanUp()
            // This needs further refactoring, this should not be here, right now the structure
            // won't support this
            categoriesModel.cleanUp()
            depictModel.cleanUp()
        }

        /**
         * Ask the RemoteDataSource to preprocess the image
         *
         * @param uploadableFile
         * @param place
         * @param similarImageInterface
         * @param inAppPictureLocation
         * @return
         */
        fun preProcessImage(
            uploadableFile: UploadableFile?,
            place: Place?,
            similarImageInterface: SimilarImageInterface?,
            inAppPictureLocation: LatLng?,
        ): Observable<UploadItem> =
            uploadModel.preProcessImage(
                uploadableFile,
                place,
                similarImageInterface,
                inAppPictureLocation,
            )

        /**
         * Search all depictions from
         *
         * @param query
         * @return
         */
        fun searchAllEntities(query: String): Flowable<List<DepictedItem>> = depictModel.searchAllEntities(query, this)

        /**
         * Returns nearest place matching the passed latitude and longitude
         *
         * @param decLatitude
         * @param decLongitude
         * @return
         */
        fun checkNearbyPlaces(
            decLatitude: Double,
            decLongitude: Double,
        ): Place? =
            try {
                val fromWikidataQuery =
                    nearbyPlaces.getFromWikidataQuery(
                        LatLng(decLatitude, decLongitude, 0.0f),
                        Locale.getDefault().language,
                        NEARBY_RADIUS_IN_KILO_METERS,
                        null,
                    )
                fromWikidataQuery?.firstOrNull()
            } catch (e: Exception) {
                Timber.e("Error fetching nearby places: %s", e.message)
                null
            }

        /**
         * Builds a string by joining all IDs divided by "|"
         *
         * @param depictionsQIDs IDs of depiction ex. ["Q11023","Q1356"]
         * @return string ex. "Q11023|Q1356"
         */
        private fun joinQIDs(depictionsQIDs: List<String>?): String? =
            depictionsQIDs
                ?.takeIf {
                    it.isNotEmpty()
                }?.joinToString("|")
    }
