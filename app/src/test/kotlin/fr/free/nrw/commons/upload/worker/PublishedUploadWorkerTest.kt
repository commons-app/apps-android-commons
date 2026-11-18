package fr.free.nrw.commons.upload.worker

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.db.Converters
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.upload.StashUploadResult
import fr.free.nrw.commons.upload.StashUploadState
import fr.free.nrw.commons.upload.UploadClient
import fr.free.nrw.commons.upload.UploadResponse
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.wikidata.WikidataEditService
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.IOException

/** Exercises the upload and retry paths with real contribution and picker records. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class PublishedUploadWorkerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var worker: UploadWorker
    private lateinit var contribution: Contribution
    private lateinit var uploadedSha1: String
    private val uploadClient = mock<UploadClient>()
    private val wikidataEditService = mock<WikidataEditService>()
    private val publishedMedia = Media(pageId = "1234", filename = "File:Published.jpg")

    /** Seeds a local image and simulates Commons accepting the publish request. */
    @Before
    fun setUp() {
        mockkObject(Converters)
        every { Converters.getGson() } returns Gson()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        worker = mock<UploadWorker>(defaultAnswer = CALLS_REAL_METHODS)
        ReflectionHelpers.setField(worker, "appContext", context)
        ReflectionHelpers.setField(worker, "stashErrorCodes", emptyList<String>())
        worker.uploadClient = uploadClient
        worker.wikidataEditService = wikidataEditService
        worker.contributionDao = database.contributionDao()
        worker.uploadedStatusDao = database.UploadedStatusDao()
        worker.fileUtilsWrapper = FileUtilsWrapper(context)
        worker.notificationHelper = mock()
        worker.mediaClient = mock()

        val file = temp.newFile("edited.jpg").apply { writeText("edited copy") }
        uploadedSha1 = FileUtils.getSHA1(file.inputStream())
        contribution =
            Contribution(
                media = Media(filename = REQUESTED_FILENAME),
                state = Contribution.STATE_IN_PROGRESS,
                localUri = Uri.fromFile(file),
                contentUri = Uri.parse("content://gallery/original.jpg"),
                imageSHA1 = SOURCE_SHA1,
            )
        worker.contributionDao.saveSynchronous(contribution)
        whenever(uploadClient.uploadFileToStash(eq(REQUESTED_FILENAME), any(), any()))
            .thenReturn(Observable.just(StashUploadResult(StashUploadState.SUCCESS, FILE_KEY, null)))
        whenever(uploadClient.uploadFileFromStash(contribution, REQUESTED_FILENAME, FILE_KEY))
            .thenReturn(Observable.just(UploadResult("Success", FILE_KEY, 0, PUBLISHED_FILENAME)))
        whenever(worker.mediaClient.checkPageExistsUsingTitle("File:$REQUESTED_FILENAME"))
            .thenReturn(Single.just(false))
        whenever(worker.mediaClient.getMedia("File:$PUBLISHED_FILENAME")).thenReturn(Single.just(publishedMedia))
        whenever(wikidataEditService.addDepictionsAndCaptions(eq(PUBLISHED_FILENAME), any(), any()))
            .thenReturn(Observable.just(true))
    }

    /** Closes the in-memory database after each test. */
    @After
    fun closeDatabase() {
        database.close()
        unmockkObject(Converters)
    }

    /** Confirms that a file stays actioned while its metadata is pending and retries only metadata. */
    @Test
    fun publishedFileStaysActionedAcrossMetadataFailureAndRetry() =
        runTest {
            whenever(wikidataEditService.addDepictionsAndCaptions(PUBLISHED_FILENAME, contribution, false))
                .thenReturn(Observable.error(IOException("Metadata unavailable")))

            worker.uploadContribution(contribution)

            val pending = worker.contributionDao.getContribution(contribution.pageId)
            assertEquals(Contribution.STATE_METADATA_PENDING, pending.state)
            assertEquals(PUBLISHED_FILENAME, pending.uploadedFilename)
            assertEquals(1, worker.uploadedStatusDao.findByModifiedImageSHA1(uploadedSha1, true))

            worker.uploadContribution(pending)

            val completed = worker.contributionDao.getContribution(publishedMedia.pageId)
            assertEquals(Contribution.STATE_COMPLETED, completed.state)
            assertEquals(publishedMedia.filename, completed.media.filename)
            assertNull(completed.uploadedFilename)
            assertEquals(1, worker.uploadedStatusDao.countUploadedBySourceSHA1(SOURCE_SHA1))
            verify(uploadClient).uploadFileToStash(eq(REQUESTED_FILENAME), eq(contribution), any())
            verify(uploadClient).uploadFileFromStash(contribution, REQUESTED_FILENAME, FILE_KEY)
            verify(wikidataEditService).addDepictionsAndCaptions(PUBLISHED_FILENAME, pending, true)
        }

    /** Confirms that a failed media lookup cannot erase the remote publication from the picker. */
    @Test
    fun publishedFileStaysActionedWhenContributionLookupFails() =
        runTest {
            whenever(worker.mediaClient.getMedia("File:$PUBLISHED_FILENAME"))
                .thenReturn(Single.error(IOException("Media unavailable")))

            worker.uploadContribution(contribution)

            val pending = worker.contributionDao.getContribution(contribution.pageId)
            assertEquals(Contribution.STATE_METADATA_PENDING, pending.state)
            assertEquals(1, worker.uploadedStatusDao.countUploadedBySourceSHA1(SOURCE_SHA1))
        }

    /** Uses Commons' checksum even when the local upload copy is no longer readable. */
    @Test
    fun publishedChecksumDoesNotRequireLocalFile() =
        runTest {
            val missingCopy = contribution.copy(localUri = Uri.fromFile(temp.root.resolve("missing.jpg")))
            val responseJson =
                """
                {"upload":{"result":"Success","filename":"$PUBLISHED_FILENAME","imageinfo":{"sha1":"$uploadedSha1"}}}
                """.trimIndent()
            val response = Gson().fromJson(responseJson, UploadResponse::class.java)
            whenever(uploadClient.uploadFileFromStash(missingCopy, REQUESTED_FILENAME, FILE_KEY))
                .thenReturn(Observable.just(response.upload))

            worker.uploadContribution(missingCopy)

            assertEquals(1, worker.uploadedStatusDao.countUploadedBySourceSHA1(SOURCE_SHA1))
            assertEquals(1, worker.uploadedStatusDao.findByModifiedImageSHA1(uploadedSha1, true))
            val completed = worker.contributionDao.getContribution(publishedMedia.pageId)
            assertEquals(Contribution.STATE_COMPLETED, completed.state)
        }

    /** Keeps a confirmed edited upload when an older picker lookup returns a negative result. */
    @Test
    fun staleNegativePickerLookupDoesNotOverwritePublication() =
        runTest {
            worker.uploadContribution(contribution)

            worker.uploadedStatusDao.insertUploaded(UploadedStatus(SOURCE_SHA1, uploadedSha1, false, false))

            assertEquals(1, worker.uploadedStatusDao.countUploadedBySourceSHA1(SOURCE_SHA1))
            assertEquals(1, worker.uploadedStatusDao.findByModifiedImageSHA1(uploadedSha1, true))
        }

    /** Confirms that a rejected publication never becomes actioned in the picker. */
    @Test
    fun rejectedPublicationRemainsActionable() =
        runTest {
            whenever(uploadClient.uploadFileFromStash(contribution, REQUESTED_FILENAME, FILE_KEY))
                .thenReturn(Observable.just(UploadResult("Warning", FILE_KEY, 0, PUBLISHED_FILENAME)))

            worker.uploadContribution(contribution)

            assertEquals(Contribution.STATE_FAILED, worker.contributionDao.getContribution(contribution.pageId).state)
            assertNull(worker.uploadedStatusDao.getFromImageSHA1(SOURCE_SHA1))
        }

    companion object {
        private const val REQUESTED_FILENAME = "Draft.jpg"
        private const val PUBLISHED_FILENAME = "Published.jpg"
        private const val FILE_KEY = "file-key"
        private const val SOURCE_SHA1 = "original-file-sha1"
    }
}
