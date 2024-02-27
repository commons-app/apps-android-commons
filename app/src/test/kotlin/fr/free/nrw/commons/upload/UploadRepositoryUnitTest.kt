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
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
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
        assertThat(repository.buildContributions(), equalTo( uploadModel.buildContributions()))
    }

    @Test
    fun testPrepareMedia() {
        assertThat(
            repository.prepareMedia(contribution),
            equalTo(uploadController.prepareMedia(contribution))
        )
    }

    @Test
    fun testSaveContribution() {
        assertThat(
            repository.saveContribution(contribution),
            equalTo(contributionDao.save(contribution).blockingAwait())
        )
    }

    @Test
    fun testGetUploads() {
        assertThat(repository.uploads, equalTo( uploadModel.uploads))
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
        assertThat(repository.selectedCategories, equalTo( categoriesModel.getSelectedCategories()))
    }

    @Test
    fun testSearchAll() {
        assertThat(
            repository.searchAll("", listOf(), listOf()),
            equalTo(categoriesModel.searchAll("", listOf(), listOf()))
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
        assertThat(
            repository.containsYear(""), equalTo(categoriesModel.containsYear(""))
        )
    }

    @Test
    fun testGetLicenses() {
        assertThat(repository.licenses, equalTo( uploadModel.licenses))
    }

    @Test
    fun testGetSelectedLicense() {
        assertThat(repository.selectedLicense, equalTo( uploadModel.selectedLicense))
    }

    @Test
    fun testGetCount() {
        assertThat(repository.count, equalTo( uploadModel.count))
    }

    @Test
    fun testPreProcessImage() {
        assertThat(
            repository.preProcessImage(uploadableFile, place, similarImageInterface, location),
            equalTo(uploadModel.preProcessImage(uploadableFile, place, similarImageInterface, location))
        )
    }

    @Test
    fun testGetImageQuality() {
        assertThat(
            repository.getImageQuality(uploadItem, location),
            equalTo(uploadModel.getImageQuality(uploadItem, location))
        )
    }

    @Test
    fun testDeletePicture() {
        assertThat(repository.deletePicture(""), equalTo( uploadModel.deletePicture("")))
    }

    @Test
    fun testGetUploadItemCaseNonNull() {
        `when`(uploadModel.items).thenReturn(listOf(uploadItem))
        assertThat(
            repository.getUploadItem(0),
            equalTo(uploadModel.items[0])
        )
    }

    @Test
    fun testGetUploadItemCaseNull() {
        assertThat(repository.getUploadItem(-1), equalTo( null))
    }

    @Test
    fun testSetSelectedLicense() {
        assertThat(repository.setSelectedLicense(""), equalTo( uploadModel.setSelectedLicense("")))
    }

    @Test
    fun testSetSelectedExistingDepictions() {
        assertThat(repository.setSelectedExistingDepictions(listOf("")),
            equalTo(uploadModel.setSelectedExistingDepictions(listOf(""))))
    }

    @Test
    fun testOnDepictItemClicked() {
        assertThat(
            repository.onDepictItemClicked(depictedItem, mock()),
            equalTo(uploadModel.onDepictItemClicked(depictedItem, mock()))
        )
    }

    @Test
    fun testGetSelectedDepictions() {
        assertThat(repository.selectedDepictions, equalTo( uploadModel.selectedDepictions))
    }

    @Test
    fun testGetSelectedExistingDepictions() {
        assertThat(repository.selectedExistingDepictions, equalTo( uploadModel.selectedExistingDepictions))
    }

    @Test
    fun testSearchAllEntities() {
        assertThat(
            repository.searchAllEntities(""),
            equalTo(depictModel.searchAllEntities("", repository))
        )
    }

    @Test
    fun testGetPlaceDepictions() {
        `when`(uploadModel.uploads).thenReturn(listOf(uploadItem))
        `when`(uploadItem.place).thenReturn(place)
        `when`(place.wikiDataEntityId).thenReturn("1")
        assertThat(
            repository.placeDepictions,
            equalTo(depictModel.getPlaceDepictions(listOf("1")))
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
        assertThat(
            repository.checkNearbyPlaces(0.0, 0.0),
            equalTo(place)
        )
    }

    @Test
    fun testCheckNearbyPlacesWithoutExceptionCaseNull() {
        assertThat(
            repository.checkNearbyPlaces(0.0, 0.0),
            equalTo(null)
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
        assertThat(
            repository.checkNearbyPlaces(0.0, 0.0),
            equalTo(null)
        )
    }

    @Test
    fun testUseSimilarPictureCoordinates() {
        assertThat(
            repository.useSimilarPictureCoordinates(imageCoordinates, 0),
            equalTo(uploadModel.useSimilarPictureCoordinates(imageCoordinates, 0))
        )
    }

    @Test
    fun testIsWMLSupportedForThisPlace() {
        `when`(uploadModel.items).thenReturn(listOf(uploadItem))
        `when`(uploadItem.isWLMUpload).thenReturn(true)
        assertThat(
            repository.isWMLSupportedForThisPlace,
            equalTo(true)
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
        assertThat(repository.selectedExistingCategories,
            equalTo(categoriesModel.getSelectedExistingCategories()))
    }

    @Test
    fun testSetSelectedExistingCategories() {
        assertThat(repository.setSelectedExistingCategories(listOf("Test")),
            equalTo(categoriesModel.setSelectedExistingCategories(mutableListOf("Test"))))
    }

    @Test
    fun testGetCategories() {
        assertThat(repository.getCategories(listOf("Test")),
            equalTo(categoriesModel.getCategoriesByName(mutableListOf("Test"))))
    }
}