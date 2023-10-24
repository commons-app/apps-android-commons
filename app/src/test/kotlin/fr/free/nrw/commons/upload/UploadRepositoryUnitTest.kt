package fr.free.nrw.commons.upload

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoriesModel
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyPlaces
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method

class UploadRepositoryUnitTest {

    private lateinit var repository: UploadRepository

    @Mock
    private lateinit var uploadModel: UploadModel

    @Mock
    private lateinit var uploadController: UploadController

    @Mock
    private lateinit var categoriesModel: CategoriesModel

    @Mock
    private lateinit var nearbyPlaces: NearbyPlaces

    @Mock
    private lateinit var depictModel: DepictModel

    @Mock
    private lateinit var contributionDao: ContributionDao

    @Mock
    private lateinit var contribution: Contribution

    @Mock
    private lateinit var completable: Completable

    @Mock
    private lateinit var categoryItem: CategoryItem

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var place: Place

    @Mock
    private var location: LatLng? = null

    @Mock
    private lateinit var similarImageInterface: SimilarImageInterface

    @Mock
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var depictedItem: DepictedItem

    @Mock
    private lateinit var imageCoordinates: ImageCoordinates

    @Mock
    private lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repository = UploadRepository(
            uploadModel,
            uploadController,
            categoriesModel,
            nearbyPlaces,
            depictModel,
            contributionDao
        )
        `when`(contributionDao.save(contribution)).thenReturn(completable)
    }

    @Test
    fun testBuildContributions() {
        assertEquals(repository.buildContributions(), uploadModel.buildContributions())
    }

    @Test
    fun testPrepareMedia() {
        assertEquals(
            repository.prepareMedia(contribution),
            uploadController.prepareMedia(contribution)
        )
    }

    @Test
    fun testSaveContribution() {
        assertEquals(
            repository.saveContribution(contribution),
            contributionDao.save(contribution).blockingAwait()
        )
    }

    @Test
    fun testGetUploads() {
        assertEquals(repository.uploads, uploadModel.uploads)
    }

    @Test
    fun testCleanup() {
        repository.cleanup()
        verify(uploadModel).cleanUp()
        verify(categoriesModel).cleanUp()
        verify(depictModel).cleanUp()
    }

    @Test
    fun testGetSelectedCategories() {
        assertEquals(repository.selectedCategories, categoriesModel.getSelectedCategories())
    }

    @Test
    fun testSearchAll() {
        assertEquals(
            repository.searchAll("", listOf(), listOf()),
            categoriesModel.searchAll("", listOf(), listOf())
        )
    }

    @Test
    fun testSetSelectedCategories() {
        repository.setSelectedCategories(listOf())
        verify(uploadModel).setSelectedCategories(listOf())
    }

    @Test
    fun testOnCategoryClicked() {
        repository.onCategoryClicked(categoryItem, media)
        verify(categoriesModel).onCategoryItemClicked(categoryItem, media)
    }

    @Test
    fun testContainsYear() {
        assertEquals(
            repository.containsYear(""), categoriesModel.containsYear("")
        )
    }

    @Test
    fun testGetLicenses() {
        assertEquals(repository.licenses, uploadModel.licenses)
    }

    @Test
    fun testGetSelectedLicense() {
        assertEquals(repository.selectedLicense, uploadModel.selectedLicense)
    }

    @Test
    fun testGetCount() {
        assertEquals(repository.count, uploadModel.count)
    }

    @Test
    fun testPreProcessImage() {
        assertEquals(
            repository.preProcessImage(uploadableFile, place, similarImageInterface, location),
            uploadModel.preProcessImage(uploadableFile, place, similarImageInterface, location)
        )
    }

    @Test
    fun testGetImageQuality() {
        assertEquals(
            repository.getImageQuality(uploadItem, location),
            uploadModel.getImageQuality(uploadItem, location)
        )
    }

    @Test
    fun testDeletePicture() {
        assertEquals(repository.deletePicture(""), uploadModel.deletePicture(""))
    }

    @Test
    fun testGetUploadItemCaseNonNull() {
        `when`(uploadModel.items).thenReturn(listOf(uploadItem))
        assertEquals(
            repository.getUploadItem(0),
            uploadModel.items[0]
        )
    }

    @Test
    fun testGetUploadItemCaseNull() {
        assertEquals(repository.getUploadItem(-1), null)
    }

    @Test
    fun testSetSelectedLicense() {
        assertEquals(repository.setSelectedLicense(""), uploadModel.setSelectedLicense(""))
    }

    @Test
    fun testSetSelectedExistingDepictions() {
        assertEquals(repository.setSelectedExistingDepictions(listOf("")),
            uploadModel.setSelectedExistingDepictions(listOf("")))
    }

    @Test
    fun testOnDepictItemClicked() {
        assertEquals(
            repository.onDepictItemClicked(depictedItem, mock()),
            uploadModel.onDepictItemClicked(depictedItem, mock())
        )
    }

    @Test
    fun testGetSelectedDepictions() {
        assertEquals(repository.selectedDepictions, uploadModel.selectedDepictions)
    }

    @Test
    fun testGetSelectedExistingDepictions() {
        assertEquals(repository.selectedExistingDepictions, uploadModel.selectedExistingDepictions)
    }

    @Test
    fun testSearchAllEntities() {
        assertEquals(
            repository.searchAllEntities(""),
            depictModel.searchAllEntities("", repository)
        )
    }

    @Test
    fun testGetPlaceDepictions() {
        `when`(uploadModel.uploads).thenReturn(listOf(uploadItem))
        `when`(uploadItem.place).thenReturn(place)
        `when`(place.wikiDataEntityId).thenReturn("1")
        assertEquals(
            repository.placeDepictions,
            depictModel.getPlaceDepictions(listOf("1"))
        )
    }

    @Test
    fun testCheckNearbyPlacesWithoutExceptionCaseNonNull() {
        `when`(
            nearbyPlaces.getFromWikidataQuery(
                LatLng(0.0, 0.0, 0.0f),
                java.util.Locale.getDefault().language, 0.1,
                false, null
            )
        ).thenReturn(listOf(place))
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            place
        )
    }

    @Test
    fun testCheckNearbyPlacesWithoutExceptionCaseNull() {
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            null
        )
    }

    @Test
    fun testCheckNearbyPlacesWithException() {
        `when`(
            nearbyPlaces.getFromWikidataQuery(
                LatLng(0.0, 0.0, 0.0f),
                java.util.Locale.getDefault().language, 0.1,
                false, null
            )
        ).thenThrow(Exception())
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            null
        )
    }

    @Test
    fun testUseSimilarPictureCoordinates() {
        assertEquals(
            repository.useSimilarPictureCoordinates(imageCoordinates, 0),
            uploadModel.useSimilarPictureCoordinates(imageCoordinates, 0)
        )
    }

    @Test
    fun testIsWMLSupportedForThisPlace() {
        `when`(uploadModel.items).thenReturn(listOf(uploadItem))
        `when`(uploadItem.isWLMUpload).thenReturn(true)
        assertEquals(
            repository.isWMLSupportedForThisPlace,
            true
        )
    }

    @Test
    fun testGetDepictions() {
        `when`(depictModel.getDepictions("Q12"))
            .thenReturn(Single.just(listOf(mock(DepictedItem::class.java))))
        val method: Method = UploadRepository::class.java.getDeclaredMethod(
            "getDepictions",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(repository, listOf("Q12"))
    }

    @Test
    fun testJoinIDs() {
        val method: Method = UploadRepository::class.java.getDeclaredMethod(
            "joinQIDs",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(repository, listOf("Q12", "Q23"))
    }

    @Test
    fun `test joinIDs when depictIDs is null`() {
        val method: Method = UploadRepository::class.java.getDeclaredMethod(
            "joinQIDs",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(repository, null)
    }

    @Test
    fun testGetSelectedExistingCategories() {
        assertEquals(repository.selectedExistingCategories,
            categoriesModel.getSelectedExistingCategories())
    }

    @Test
    fun testSetSelectedExistingCategories() {
        assertEquals(repository.setSelectedExistingCategories(listOf("Test")),
            categoriesModel.setSelectedExistingCategories(mutableListOf("Test")))
    }

    @Test
    fun testGetCategories() {
        assertEquals(repository.getCategories(listOf("Test")),
            categoriesModel.getCategoriesByName(mutableListOf("Test")))
    }
}