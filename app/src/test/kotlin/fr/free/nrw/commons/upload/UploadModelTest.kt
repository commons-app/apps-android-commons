package fr.free.nrw.commons.upload

import android.net.Uri
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.filepicker.UploadableFile.DateTimeWithSource.EXIF_SOURCE
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.settings.Licenses
import fr.free.nrw.commons.settings.Licenses.Constants.CC0_ID
import fr.free.nrw.commons.settings.Licenses.Constants.CC_BY_SA_3_ID
import fr.free.nrw.commons.settings.Prefs.DEFAULT_LICENSE
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.ImageUtils
import io.reactivex.Single
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.AdditionalAnswers.returnsSecondArg
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UploadModelTest {

    @Mock
    lateinit var store: JsonKvStore

    @Mock
    lateinit var session: SessionManager

    @Mock
    lateinit var fileProcessor: FileProcessor

    @Mock
    lateinit var imageProcessingService: ImageProcessingService

    @Mock
    lateinit var uploadableFile: UploadableFile

    @Mock
    lateinit var similarImageInterface: SimilarImageInterface

    @Mock
    lateinit var imageCoordinates: ImageCoordinates

    private val context = RuntimeEnvironment.application.applicationContext
    private val testPlace = Place(
        "en", "name", null, "desc", null, null,
        Sitelinks.Builder()
            .setWikidataLink("http://www.wikidata.org/entity/1234")
            .setWikipediaLink("")
            .build(),
        "", false
    )
    private val expectedMediaDetail = UploadMediaDetail(
        testPlace.language, testPlace.longDescription, testPlace.name
    )
    private val creationDate = UploadableFile.DateTimeWithSource(0, EXIF_SOURCE)
    private val depictedItem = DepictedItem(
        "name", "desc", null, listOf("a", "b"), listOf("1", "2", "3"), true, "id"
    )
    private lateinit var licenseNames: List<String>
    private lateinit var testObject: UploadModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        whenever(session.userName).thenReturn("the user")

        whenever(store.getString(eq(DEFAULT_LICENSE), anyString())).then(returnsSecondArg<String>())
        licenseNames = Licenses.names(context)

        whenever(uploadableFile.getFileCreatedDate(any())).thenReturn(creationDate)
        whenever(uploadableFile.filePath).thenReturn("/a/b/c")
        whenever(uploadableFile.getMimeType(any())).thenReturn("image/png")
        whenever(uploadableFile.contentUri).thenReturn(Uri.EMPTY)

        whenever(fileProcessor.processFileCoordinates(similarImageInterface, "/a/b/c"))
            .thenReturn(imageCoordinates)
        whenever(imageCoordinates.decimalCoords).thenReturn("0.000,0.000")

        testObject = UploadModel(
            licenseNames,
            store,
            Licenses.mapByName(context),
            context,
            session,
            fileProcessor,
            imageProcessingService
        )
    }

    @Test
    fun initialState() {
        assertEquals(0, testObject.count)
        assertTrue(testObject.items.isEmpty())
        assertTrue(testObject.uploads.isEmpty())
        assertTrue(testObject.selectedCategories.isEmpty())
        assertTrue(testObject.selectedDepictions.isEmpty())
        assertEquals(CC_BY_SA_3_ID, testObject.selectedLicense)
        assertEquals(licenseNames, testObject.licenses)
    }

    @Test
    fun settingSelectedLicenseStoresTheId() {
        val name = context.getString(Licenses.CC0.name)

        testObject.selectedLicense = name

        assertEquals(CC0_ID, testObject.selectedLicense)
        verify(store).putString(DEFAULT_LICENSE, CC0_ID)
    }

    @Test
    fun settingSelectedCategoriesClearsPreviousSelection() {
        val initialList = listOf("a", "b")
        testObject.selectedCategories = initialList
        val selectedCategoriesList = testObject.selectedCategories
        assertEquals(initialList, selectedCategoriesList)

        val updatedList = listOf("1", "2", "3")
        testObject.selectedCategories = updatedList

        // Underlying storage didn't change, just the content
        assertSame(selectedCategoriesList, testObject.selectedCategories)
        assertEquals(updatedList, testObject.selectedCategories)
    }

    @Test
    fun imageQualityDetectionIsDelegatedToTheService() {
        val item = mock<UploadItem>()
        val expectedResult = Single.just(99)
        whenever(imageProcessingService.validateImage(item)).thenReturn(expectedResult)

        val result = testObject.getImageQuality(item)

        verify(imageProcessingService).validateImage(item)
        assertSame(expectedResult, result)
    }

    @Test
    fun toggleDepictionSelection() {
        testObject.onDepictItemClicked(depictedItem)
        assertEquals(listOf(depictedItem), testObject.selectedDepictions)

        depictedItem.isSelected = false
        testObject.onDepictItemClicked(depictedItem)
        assertEquals(emptyList<DepictedItem>(), testObject.selectedDepictions)
    }

    @Test
    fun preProcessSingleImageWithNonExistentPlace() {
        val result = testObject.preProcessImage(
            uploadableFile, testPlace, similarImageInterface
        ).test()
        result.assertNoErrors()
        result.assertValueCount(1)
        with(result.values().first()) {
            assertEquals(listOf(this), testObject.items)
            assertEquals(Uri.EMPTY, contentUri)
            assertEquals(Uri.parse("/a/b/c"), mediaUri)
            assertEquals(0, createdTimestamp)
            assertEquals(EXIF_SOURCE, createdTimestampSource)
            assertEquals(imageCoordinates, gpsCoords)
            assertEquals(ImageUtils.IMAGE_WAIT, imageQuality)
            assertFalse(isWLMUpload)
            assertSame(testPlace, place)
            assertEquals(listOf(expectedMediaDetail), uploadMediaDetails)
            assertNull(countryCode)
        }
    }

    @Test
    fun onlyUploadsTheSameFileOnce() {
        val firstItem = testObject.preProcessImage(
            uploadableFile, testPlace, similarImageInterface
        ).test().values().first()
        assertEquals(listOf(firstItem), testObject.items)

        val secondFile: UploadableFile = mock()
        whenever(secondFile.getFileCreatedDate(any())).thenReturn(creationDate)
        whenever(secondFile.filePath).thenReturn("/a/b/c")
        whenever(secondFile.getMimeType(any())).thenReturn("image/png")
        whenever(secondFile.contentUri).thenReturn(Uri.EMPTY)
        testObject.preProcessImage(secondFile, testPlace, similarImageInterface)

        assertEquals(listOf(firstItem), testObject.items)
    }

    @Test
    fun deleteLastPictureCleansUp() {
        val firstItem = testObject.preProcessImage(
            uploadableFile, testPlace, similarImageInterface
        ).test().values().first()
        assertEquals(listOf(firstItem), testObject.items)
        testObject.selectedCategories = listOf("1", "2")
        testObject.onDepictItemClicked(depictedItem)

        testObject.deletePicture("/a/b/c")

        assertTrue(testObject.items.isEmpty())
        assertTrue(testObject.selectedCategories.isEmpty())
        assertTrue(testObject.selectedDepictions.isEmpty())
        verify(fileProcessor).cleanup()
    }

    @Test
    fun useSimilarCoordinatesUpdatesTheItem() {
        val item = testObject.preProcessImage(
            uploadableFile, testPlace, similarImageInterface
        ).test().values().first()

        val updatedCoordinates: ImageCoordinates = mock()
        testObject.useSimilarPictureCoordinates(updatedCoordinates, 0)

        verify(fileProcessor).prePopulateCategoriesAndDepictionsBy(updatedCoordinates)
        assertEquals(updatedCoordinates, item.gpsCoords)
    }

    @Test
    fun buildContributionForSingleUpload() {
        testObject.preProcessImage(uploadableFile, testPlace, similarImageInterface)

        val contribution = testObject.buildContributions().test()

        contribution.assertNoErrors()
        with(contribution.values().first()) {
            assertEquals(EXIF_SOURCE, dateCreatedSource)
            assertEquals(Date(0L), dateCreated)
            assertEquals("0.000,0.000", decimalCoords)
            assertEquals(Uri.parse("/a/b/c"), localUri)
            assertEquals("1234", wikidataPlace?.id)
            assertFalse(wikidataPlace?.isMonumentUpload!!)
            assertEquals(0, hasInvalidLocation)
        }
    }
}