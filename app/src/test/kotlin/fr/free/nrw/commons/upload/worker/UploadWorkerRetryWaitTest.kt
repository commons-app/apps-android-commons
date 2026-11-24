package fr.free.nrw.commons.upload.worker

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.UnsuccessfulResponseException
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.di.CommonsApplicationComponent
import fr.free.nrw.commons.upload.StashUploadResult
import fr.free.nrw.commons.upload.StashUploadState
import fr.free.nrw.commons.upload.UploadClient
import fr.free.nrw.commons.wikidata.WikidataEditService
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwServiceError
import io.reactivex.Observable
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * The worker saves when a failed upload may be retried automatically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = Application::class)
class UploadWorkerRetryWaitTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var dao: ContributionDao
    private val uploadClient = mock<UploadClient>()
    private val wikidataEditService = mock<WikidataEditService>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.contributionDao()
        val notifications = mock<UploadNotificationHelper>()
        whenever(notifications.getNotificationBuilder(any())).thenAnswer {
            NotificationCompat.Builder(context, "test").setSmallIcon(android.R.drawable.stat_notify_error)
        }
        val component = mock<CommonsApplicationComponent>()
        // The database converters read Gson through the application
        whenever(component.gson()).thenReturn(Gson())
        Whitebox.setInternalState(CommonsApplication::class.java, "instance", mock<CommonsApplication>())
        doAnswer { call ->
            val worker = call.getArgument<UploadWorker>(0)
            worker.contributionDao = dao
            worker.uploadClient = uploadClient
            worker.wikidataEditService = wikidataEditService
            worker.notificationHelper = notifications
            worker.sessionManager = mock()
            worker.uploadedStatusDao = mock()
            worker.mediaClient = mock()
            worker.fileUtilsWrapper = mock()
            worker.placesRepository = mock()
            null
        }.whenever(component).inject(any<UploadWorker>())
        val injection = mock<ApplicationlessInjection>()
        whenever(injection.commonsApplicationComponent).thenReturn(component)
        Whitebox.setInternalState(ApplicationlessInjection::class.java, "_instance", injection)
    }

    @After
    fun tearDown() {
        Whitebox.setInternalState(ApplicationlessInjection::class.java, "_instance", null as ApplicationlessInjection?)
        WorkRequestHelper.markUploadWorkerAsStopped()
        database.close()
    }

    @Test
    fun stashFailureSavesTheWaitFromRetryAfter() {
        whenever(uploadClient.uploadFileToStash(any(), any(), any())).thenReturn(
            Observable.just(
                StashUploadResult(StashUploadState.FAILED, null, "Unsuccessful response: HTTP 503", 120_000L),
            ),
        )
        dao.saveSynchronous(queued(uploadedFilename = null))
        val before = System.currentTimeMillis()

        runWorker()

        val saved = dao.getContribution(PAGE_ID)
        assertEquals(Contribution.STATE_FAILED, saved.state)
        assertWaitAbout(120_000L, before, saved.retryNotBefore)
    }

    @Test
    fun stashErrorSavesTheWaitFromRetryAfter() {
        whenever(uploadClient.uploadFileToStash(any(), any(), any())).thenReturn(
            Observable.error(UnsuccessfulResponseException(503, 120_000L)),
        )
        dao.saveSynchronous(queued(uploadedFilename = null))
        val before = System.currentTimeMillis()

        runWorker()

        val saved = dao.getContribution(PAGE_ID)
        assertEquals(Contribution.STATE_FAILED, saved.state)
        assertWaitAbout(120_000L, before, saved.retryNotBefore)
    }

    @Test
    fun rateLimitedCaptionsSaveAWait() {
        val error =
            Gson().fromJson(
                """{"code":"ratelimited","text":"As an anti-abuse measure, you are limited..."}""",
                MwServiceError::class.java,
            )
        whenever(wikidataEditService.addDepictionsAndCaptions(any(), any(), anyOrNull())).thenReturn(
            Observable.error(MwException(error, listOf(error))),
        )
        dao.saveSynchronous(queued(uploadedFilename = "Retry_wait.jpg"))
        val before = System.currentTimeMillis()

        runWorker()

        val saved = dao.getContribution(PAGE_ID)
        assertEquals(Contribution.STATE_METADATA_PENDING, saved.state)
        // The readable message is kept for the user
        assertEquals("As an anti-abuse measure, you are limited...", saved.errorInfo)
        assertWaitAbout(5_000L, before, saved.retryNotBefore)
    }

    private fun runWorker() {
        runBlocking { TestListenableWorkerBuilder<UploadWorker>(context).build().doWork() }
    }

    private fun assertWaitAbout(
        waitMillis: Long,
        before: Long,
        retryNotBefore: Date?,
    ) {
        val time = requireNotNull(retryNotBefore) { "no wait was saved" }.time
        assertTrue("wait ends at $time", time >= before + waitMillis && time <= System.currentTimeMillis() + waitMillis)
    }

    private fun queued(uploadedFilename: String?) =
        Contribution(
            media = Media(filename = "Retry wait.jpg"),
            pageId = PAGE_ID,
            state = Contribution.STATE_QUEUED,
            localUri = Uri.parse("file:///retry-wait.jpg"),
            dateUploadStarted = Date(),
        ).apply { this.uploadedFilename = uploadedFilename }

    private companion object {
        const val PAGE_ID = "retry-wait"
    }
}
