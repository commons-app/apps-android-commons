package fr.free.nrw.commons.upload

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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
        repository =
            UploadRepository(
                uploadModel,
                uploadController,
                categoriesModel,
                nearbyPlaces,
                depictModel,
                contributionDao,
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
            uploadController.prepareMedia(contribution),
        )
    }

    @Test
    fun testSaveContribution() {
        assertEquals(
            repository.saveContribution(contribution),
            contributionDao.save(contribution).blockingAwait(),
        )
    }

    @Test
    fun testGetUploads() {
        val result = listOf(uploadItem)
        whenever(uploadModel.uploads).thenReturn(result)
        assertSame(result, repository.getUploads())
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
        assertEquals(repository.getSelectedCategories(), categoriesModel.getSelectedCategories())
    }

    @Test
    fun testSearchAll() {
        val empty = Observable.empty<List<CategoryItem>>()
        whenever(categoriesModel.searchAll(any(), any(), any())).thenReturn(empty)
        assertSame(empty, repository.searchAll("", listOf(), listOf()))

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
            repository.isSpammyCategory(""),
            categoriesModel.isSpammyCategory(""),
        )
    }

    @Test
    fun testGetLicenses() {
        whenever(uploadModel.licenses).thenReturn(listOf())
        repository.getLicenses()
        verify(uploadModel).licenses
    }

    @Test
    fun testGetSelectedLicense() {
        assertEquals(repository.getSelectedLicense(), uploadModel.selectedLicense)
    }

    @Test
    fun testGetCount() {
        assertEquals(repository.getCount(), uploadModel.count)
    }

    @Test
    fun testPreProcessImage() {
        assertEquals(
            repository.preProcessImage(uploadableFile, place, similarImageInterface, location),
            uploadModel.preProcessImage(uploadableFile, place, similarImageInterface, location),
        )
    }

    @Test
    fun testGetImageQuality() {
        assertEquals(
            repository.getImageQuality(uploadItem, location),
            uploadModel.getImageQuality(uploadItem, location),
        )
    }

    @Test
    fun testGetCaptionQuality() {
        assertEquals(
            repository.getCaptionQuality(uploadItem),
            uploadModel.getCaptionQuality(uploadItem)
        )
    }

    @Test
    fun testDeletePicture() {
        assertEquals(repository.deletePicture(""), uploadModel.deletePicture(""))
    }

    @Test
    fun testGetUploadItemCaseNonNull() {
        `when`(uploadModel.items).thenReturn(mutableListOf(uploadItem))
        assertEquals(
            repository.getUploadItem(0),
            uploadItem,
        )
    }

    @Test
    fun testGetUploadItemCaseNull() {
        assertEquals(repository.getUploadItem(-1), null)
    }

    @Test
    fun testOnDepictItemClicked() {
        assertEquals(
            repository.onDepictItemClicked(depictedItem, mock()),
            uploadModel.onDepictItemClicked(depictedItem, mock()),
        )
    }

    @Test
    fun testGetSelectedDepictions() {
        repository.getSelectedDepictions()
        verify(uploadModel).selectedDepictions
    }

    @Test
    fun testGetSelectedExistingDepictions() {
        repository.getSelectedExistingDepictions()
        verify(uploadModel).selectedExistingDepictions
    }

    @Test
    fun testSearchAllEntities() {
        assertEquals(
            repository.searchAllEntities(""),
            depictModel.searchAllEntities("", repository),
        )
    }

    @Test
    fun testGetPlaceDepictions() {
        `when`(uploadModel.uploads).thenReturn(listOf(uploadItem))
        `when`(uploadItem.place).thenReturn(place)
        `when`(place.wikiDataEntityId).thenReturn("1")
        assertEquals(
            repository.getPlaceDepictions(),
            depictModel.getPlaceDepictions(listOf("1")),
        )
    }

    @Test
    fun testCheckNearbyPlacesWithoutExceptionCaseNonNull() {
        `when`(
            nearbyPlaces.getFromWikidataQuery(
                LatLng(0.0, 0.0, 0.0f),
                java.util.Locale
                    .getDefault()
                    .language,
                0.1,
                null,
            ),
        ).thenReturn(listOf(place))
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            place,
        )
    }

    @Test
    fun testCheckNearbyPlacesWithoutExceptionCaseNull() {
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            null,
        )
    }

    @Test
    fun testCheckNearbyPlacesWithException() {
        `when`(
            nearbyPlaces.getFromWikidataQuery(
                LatLng(0.0, 0.0, 0.0f),
                java.util.Locale
                    .getDefault()
                    .language,
                0.1,
                null,
            ),
        ).thenThrow(Exception())
        assertEquals(
            repository.checkNearbyPlaces(0.0, 0.0),
            null,
        )
    }

    @Test
    fun testUseSimilarPictureCoordinates() {
        assertEquals(
            repository.useSimilarPictureCoordinates(imageCoordinates, 0),
            uploadModel.useSimilarPictureCoordinates(imageCoordinates, 0),
        )
    }

    @Test
    fun testIsWMLSupportedForThisPlace() {
        whenever(uploadModel.items).thenReturn(mutableListOf(uploadItem))
        whenever(uploadItem.isWLMUpload).thenReturn(true)
        assertEquals(
            repository.isWMLSupportedForThisPlace(),
            true,
        )
    }

    @Test
    fun testGetDepictions() {
        `when`(depictModel.getDepictions("Q12"))
            .thenReturn(Single.just(listOf(mock(DepictedItem::class.java))))
        val method: Method =
            UploadRepository::class.java.getDeclaredMethod(
                "getDepictions",
                List::class.java,
            )
        method.isAccessible = true
        method.invoke(repository, listOf("Q12"))
    }

    @Test
    fun testJoinIDs() {
        val method: Method =
            UploadRepository::class.java.getDeclaredMethod(
                "joinQIDs",
                List::class.java,
            )
        method.isAccessible = true
        method.invoke(repository, listOf("Q12", "Q23"))
    }

    @Test
    fun `test joinIDs when depictIDs is null`() {
        val method: Method =
            UploadRepository::class.java.getDeclaredMethod(
                "joinQIDs",
                List::class.java,
            )
        method.isAccessible = true
        method.invoke(repository, null)
    }

    @Test
    fun testGetSelectedExistingCategories() {
        assertEquals(
            repository.getSelectedExistingCategories(),
            categoriesModel.getSelectedExistingCategories(),
        )
    }

    @Test
    fun testSetSelectedExistingCategories() {
        assertEquals(
            repository.setSelectedExistingCategories(listOf("Test")),
            categoriesModel.setSelectedExistingCategories(mutableListOf("Test")),
        )
    }

    @Test
    fun testGetCategories() {
        assertEquals(
            repository.getCategories(listOf("Test")),
            categoriesModel.getCategoriesByName(mutableListOf("Test"))
                ?: Observable.empty<List<CategoryItem>>()
        )
    }
}
