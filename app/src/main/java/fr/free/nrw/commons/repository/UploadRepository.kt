package fr.free.nrw.commons.repository

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
 * The repository class for UploadActivity
 */
@Singleton
class UploadRepository @Inject constructor(
    private val uploadModel: UploadModel,
    private val uploadController: UploadController,
    private val categoriesModel: CategoriesModel,
    private val nearbyPlaces: NearbyPlaces,
    private val depictModel: DepictModel,
    private val contributionDao: ContributionDao
) {

    companion object {
        private const val NEARBY_RADIUS_IN_KILO_METERS = 0.1 // 100 meters
    }

    /**
     * Asks the RemoteDataSource to build contributions
     *
     * @return
     */
    fun buildContributions(): Observable<Contribution>? {
        return uploadModel.buildContributions()
    }

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
    fun getUploads(): List<UploadItem> {
        return uploadModel.uploads
    }

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
     * Fetches and returns the selected categories for the current upload
     *
     * @return
     */
    fun getSelectedCategories(): List<CategoryItem> {
        return categoriesModel.getSelectedCategories()
    }

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
        selectedDepictions: List<DepictedItem>
    ): Observable<List<CategoryItem>> {
        return categoriesModel.searchAll(query, imageTitleList, selectedDepictions)
    }

    /**
     * Sets the list of selected categories for the current upload
     *
     * @param categoryStringList
     */
    fun setSelectedCategories(categoryStringList: List<String>) {
        uploadModel.setSelectedCategories(categoryStringList)
    }

    /**
     * Handles the category selection/deselection
     *
     * @param categoryItem
     */
    fun onCategoryClicked(categoryItem: CategoryItem, media: Media?) {
        categoriesModel.onCategoryItemClicked(categoryItem, media)
    }

    /**
     * Prunes the category list for irrelevant categories see #750
     *
     * @param name
     * @return
     */
    fun isSpammyCategory(name: String): Boolean {
        return categoriesModel.isSpammyCategory(name)
    }

    /**
     * Returns the string list of available licenses from the LocalDataSource
     *
     * @return
     */
    fun getLicenses(): List<String> {
        return uploadModel.licenses
    }

    /**
     * Returns the selected license for the current upload
     *
     * @return
     */
    fun getSelectedLicense(): String? {
        return uploadModel.selectedLicense
    }

    /**
     * Returns the number of Upload Items
     *
     * @return
     */
    fun getCount(): Int {
        return uploadModel.count
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
        inAppPictureLocation: LatLng?
    ): Observable<UploadItem>? {
        return uploadModel.preProcessImage(
            uploadableFile,
            place,
            similarImageInterface,
            inAppPictureLocation
        )
    }

    /**
     * Query the RemoteDataSource for image quality
     *
     * @param uploadItem UploadItem whose caption is to be checked
     * @param location Location of the image
     * @return Quality of UploadItem
     */
    fun getImageQuality(uploadItem: UploadItem, location: LatLng?): Single<Int>? {
        return uploadModel.getImageQuality(uploadItem, location)
    }

    /**
     * Query the RemoteDataSource for image duplicity check
     *
     * @param filePath file to be checked
     * @return IMAGE_DUPLICATE or IMAGE_OK
     */
    fun checkDuplicateImage(filePath: String): Single<Int> {
        return uploadModel.checkDuplicateImage(filePath)
    }

    /**
     * query the RemoteDataSource for caption quality
     *
     * @param uploadItem UploadItem whose caption is to be checked
     * @return Quality of caption of the UploadItem
     */
    fun getCaptionQuality(uploadItem: UploadItem): Single<Int>? {
        return uploadModel.getCaptionQuality(uploadItem)
    }

    /**
     * asks the LocalDataSource to delete the file with the given file path
     *
     * @param filePath
     */
    fun deletePicture(filePath: String) {
        uploadModel.deletePicture(filePath)
    }

    /**
     * fetches and returns the upload item
     *
     * @param index
     * @return
     */
    fun getUploadItem(index: Int): UploadItem? {
        return if (index >= 0) {
            uploadModel.items.getOrNull(index)
        } else null //There is no item to copy details
    }

    /**
     * set selected license for the current upload
     *
     * @param licenseName
     */
    fun setSelectedLicense(licenseName: String?) {
        uploadModel.selectedLicense = licenseName
    }

    fun onDepictItemClicked(depictedItem: DepictedItem, media: Media?) {
        uploadModel.onDepictItemClicked(depictedItem, media)
    }

    /**
     * Fetches and returns the selected depictions for the current upload
     *
     * @return
     */
    fun getSelectedDepictions(): List<DepictedItem> {
        return uploadModel.selectedDepictions
    }

    /**
     * Provides selected existing depicts
     *
     * @return selected existing depicts
     */
    fun getSelectedExistingDepictions(): List<String> {
        return uploadModel.selectedExistingDepictions
    }

    /**
     * Initialize existing depicts
     *
     * @param selectedExistingDepictions existing depicts
     */
    fun setSelectedExistingDepictions(selectedExistingDepictions: List<String>) {
        uploadModel.selectedExistingDepictions = selectedExistingDepictions.toMutableList()
    }

    /**
     * Search all depictions from
     *
     * @param query
     * @return
     */
    fun searchAllEntities(query: String): Flowable<List<DepictedItem>> {
        return depictModel.searchAllEntities(query, this)
    }

    /**
     * Gets the depiction for each unique {@link Place} associated with an {@link UploadItem}
     * from {@link #getUploads()}
     *
     * @return a single that provides the depictions
     */
    fun getPlaceDepictions(): Single<List<DepictedItem>> {
        val qids = mutableSetOf<String>()
        getUploads().forEach { item ->
            item.place?.let {
                it.wikiDataEntityId?.let { it1 ->
                    qids.add(it1)
                }
            }
        }
        return depictModel.getPlaceDepictions(qids.toList())
    }

    /**
     * Gets the category for each unique {@link Place} associated with an {@link UploadItem}
     * from {@link #getUploads()}
     *
     * @return a single that provides the categories
     */
    fun getPlaceCategories(): Single<List<CategoryItem>> {
        val qids = mutableSetOf<String>()
        getUploads().forEach { item ->
            item.place?.category?.let { qids.add(it) }
        }
        return Single.fromObservable(categoriesModel.getCategoriesByName(qids.toList()))
    }

    /**
     * Takes depict IDs as a parameter, converts into a slash separated String and Gets DepictItem
     * from the server
     *
     * @param depictionsQIDs IDs of Depiction
     * @return Flowable<List<DepictedItem>>
     */
    fun getDepictions(depictionsQIDs: List<String>): Flowable<List<DepictedItem>> {
        val ids = joinQIDs(depictionsQIDs) ?: ""
        return depictModel.getDepictions(ids).toFlowable()
    }

    /**
     * Builds a string by joining all IDs divided by "|"
     *
     * @param depictionsQIDs IDs of depiction ex. ["Q11023","Q1356"]
     * @return string ex. "Q11023|Q1356"
     */
    private fun joinQIDs(depictionsQIDs: List<String>?): String? {
        return depictionsQIDs?.takeIf {
            it.isNotEmpty()
        }?.joinToString("|")
    }

    /**
     * Returns nearest place matching the passed latitude and longitude
     *
     * @param decLatitude
     * @param decLongitude
     * @return
     */
    fun checkNearbyPlaces(decLatitude: Double, decLongitude: Double): Place? {
        return try {
            val fromWikidataQuery = nearbyPlaces.getFromWikidataQuery(
                LatLng(decLatitude, decLongitude, 0.0f),
                Locale.getDefault().language,
                NEARBY_RADIUS_IN_KILO_METERS,
                null
            )
            fromWikidataQuery?.firstOrNull()
        } catch (e: Exception) {
            Timber.e("Error fetching nearby places: %s", e.message)
            null
        }
    }

    fun useSimilarPictureCoordinates(imageCoordinates: ImageCoordinates, uploadItemIndex: Int) {
        uploadModel.useSimilarPictureCoordinates(
            imageCoordinates,
            uploadItemIndex
        )
    }

    fun isWMLSupportedForThisPlace(): Boolean {
        return uploadModel.items.firstOrNull()?.isWLMUpload == true
    }

    /**
     * Provides selected existing categories
     *
     * @return selected existing categories
     */
    fun getSelectedExistingCategories(): List<String> {
        return categoriesModel.getSelectedExistingCategories()
    }

    /**
     * Initialize existing categories
     *
     * @param selectedExistingCategories existing categories
     */
    fun setSelectedExistingCategories(selectedExistingCategories: List<String>) {
        categoriesModel.setSelectedExistingCategories(
            selectedExistingCategories.toMutableList()
        )
    }

    /**
     * Takes category names and Gets CategoryItem from the server
     *
     * @param categories names of Category
     * @return Observable<List<CategoryItem>>
     */
    fun getCategories(categories: List<String>): Observable<List<CategoryItem>> {
        return categoriesModel.getCategoriesByName(categories)
            ?.map { it.toList() } ?: Observable.empty()
    }
}
