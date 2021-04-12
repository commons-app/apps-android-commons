package fr.free.nrw.commons.upload

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.UploadService.ACTION_START_SERVICE
import fr.free.nrw.commons.wikidata.WikidataEditService
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import java.lang.reflect.Field
import java.lang.reflect.Method

class UploadServiceUnitTests {

    @Mock
    private lateinit var contribution: Contribution

    @Mock
    private lateinit var uploadClient: UploadClient

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var ioThreadScheduler: Scheduler

    @Mock
    private lateinit var completable: Completable

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var curNotification: Builder

    @Mock
    private lateinit var contributionDao: ContributionDao

    @Mock
    private lateinit var contributionsToUpload: PublishProcessor<Contribution>

    @Mock
    private lateinit var single: Single<Int>

    @Mock
    private lateinit var singleBool: Single<Boolean>

    @Mock
    private lateinit var intent: Intent

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var notificationManager: NotificationManagerCompat

    @Mock
    private lateinit var chunkInfo: ChunkInfo

    @Mock
    private lateinit var resources: Resources

    @InjectMocks
    private lateinit var wikidataEditService: WikidataEditService


    private lateinit var mockContext: Context
    private lateinit var uploadService: UploadService
    private lateinit var notificationUpdateProgressListener: UploadService.NotificationUpdateProgressListener

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        uploadService = UploadService()
        notificationUpdateProgressListener = uploadService.NotificationUpdateProgressListener(
            "",
            "",
            "",
            contribution
        )

        mockContext = PowerMockito.mock(Context::class.java)

        `when`(contributionDao.update(contribution)).thenReturn(completable)
        `when`(contributionDao.save(contribution)).thenReturn(completable)
        `when`(contributionDao.update(contribution).subscribeOn(ioThreadScheduler)).thenReturn(
            completable
        )
        `when`(contributionDao.save(contribution).subscribeOn(ioThreadScheduler)).thenReturn(
            completable
        )
        `when`(contributionDao.getPendingUploads(any())).thenReturn(single)
        `when`(contributionDao.updateStates(anyInt(), any())).thenReturn(single)
        `when`(contributionDao.updateStates(anyInt(), any()).observeOn(any())).thenReturn(single)
        `when`(
            contributionDao.updateStates(anyInt(), any()).observeOn(any()).subscribeOn(
                ioThreadScheduler
            )
        ).thenReturn(single)

        val compositeDisposableField: Field =
            UploadService::class.java.getDeclaredField("compositeDisposable")
        compositeDisposableField.isAccessible = true
        compositeDisposableField.set(uploadService, compositeDisposable)

        val uploadClientField: Field =
            UploadService::class.java.getDeclaredField("uploadClient")
        uploadClientField.isAccessible = true
        uploadClientField.set(uploadService, uploadClient)

        val contributionDaoField: Field =
            UploadService::class.java.getDeclaredField("contributionDao")
        contributionDaoField.isAccessible = true
        contributionDaoField.set(uploadService, contributionDao)

        val ioThreadSchedulerField: Field =
            UploadService::class.java.getDeclaredField("ioThreadScheduler")
        ioThreadSchedulerField.isAccessible = true
        ioThreadSchedulerField.set(uploadService, ioThreadScheduler)

        val curNotificationField: Field =
            UploadService::class.java.getDeclaredField("curNotification")
        curNotificationField.isAccessible = true
        curNotificationField.set(uploadService, curNotification)

        val defaultKvStoreField: Field =
            UploadService::class.java.getDeclaredField("defaultKvStore")
        defaultKvStoreField.isAccessible = true
        defaultKvStoreField.set(uploadService, defaultKvStore)

        val contributionsToUploadField: Field =
            UploadService::class.java.getDeclaredField("contributionsToUpload")
        contributionsToUploadField.isAccessible = true
        contributionsToUploadField.set(uploadService, contributionsToUpload)

        val notificationManagerdField: Field =
            UploadService::class.java.getDeclaredField("notificationManager")
        notificationManagerdField.isAccessible = true
        notificationManagerdField.set(uploadService, notificationManager)

        val wikidataEditServiceField: Field =
            UploadService::class.java.getDeclaredField("wikidataEditService")
        wikidataEditServiceField.isAccessible = true
        wikidataEditServiceField.set(uploadService, wikidataEditService)

        val mediaClientField: Field =
            UploadService::class.java.getDeclaredField("mediaClient")
        mediaClientField.isAccessible = true
        mediaClientField.set(uploadService, mediaClient)
    }

    @Test
    fun testPauseUpload() {
        uploadService.pauseUpload(contribution)
    }

    @Test
    fun testOnDestroy() {
        uploadService.onDestroy()
    }

    @Test
    fun testOnBind() {
        uploadService.onBind(null)
    }

    @Test
    fun testQueueCaseTrue() {
        `when`(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                false
            )
        ).thenReturn(true)
        uploadService.queue(contribution)
    }

    @Test
    fun testQueueCaseFalse() {
        `when`(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                false
            )
        ).thenReturn(false)
        uploadService.queue(contribution)
    }

    @Test
    fun testOnStartCommandCaseTrue() {
        `when`(intent.action).thenReturn(ACTION_START_SERVICE)
        uploadService.onStartCommand(intent, 0, 0)
    }

    @Test
    fun testSetServiceCallback() {
        uploadService.setServiceCallback(null)
    }

    @Test
    fun testGetNotificationBuilder() {
        `when`(mockContext.resources).thenReturn(resources)
        val method: Method = UploadService::class.java.getDeclaredMethod(
            "getNotificationBuilder", String::class.java
        )
        method.isAccessible = true
        method.invoke(uploadService, "")
    }

    @Test
    fun testHandleUploadCaseFalse() {
        `when`(mockContext.resources).thenReturn(resources)
        uploadService.handleUpload(contribution)
    }

    @Test
    fun testUploadContributionCaseNull() {
        val method: Method = UploadService::class.java.getDeclaredMethod(
            "uploadContribution", Contribution::class.java
        )
        method.isAccessible = true
        method.invoke(uploadService, contribution)
    }

    @Test
    fun testClearChunks() {
        val method: Method = UploadService::class.java.getDeclaredMethod(
            "clearChunks", Contribution::class.java
        )
        method.isAccessible = true
        method.invoke(uploadService, contribution)
    }

    @Test
    fun testFindUniqueFilename() {
        `when`(mediaClient.checkPageExistsUsingTitle(any())).thenReturn(singleBool)
        `when`(mediaClient.checkPageExistsUsingTitle(any()).blockingGet()).thenReturn(false)
        val method: Method = UploadService::class.java.getDeclaredMethod(
            "findUniqueFilename", String::class.java
        )
        method.isAccessible = true
        method.invoke(uploadService, ".")
    }

    @Test
    fun testOnChunkUploaded() {
        notificationUpdateProgressListener.onChunkUploaded(contribution, chunkInfo)
    }

    @Test
    fun testOnProgressCaseFalse() {
        `when`(curNotification.setProgress(anyInt(), anyInt(), anyBoolean())).thenReturn(
            curNotification
        )
        notificationUpdateProgressListener.onProgress(0, 1)
    }

    @Test
    fun testOnProgressCaseTrue() {
        `when`(curNotification.setContentTitle(anyString())).thenReturn(curNotification)
        `when`(curNotification.setContentTitle(anyString()).setTicker(anyString())).thenReturn(
            curNotification
        )
        `when`(curNotification.setProgress(anyInt(), anyInt(), anyBoolean())).thenReturn(
            curNotification
        )
        notificationUpdateProgressListener.onProgress(0, 0)
    }

    @Test
    fun testGetService() {
        uploadService.UploadServiceLocalBinder().service
    }

}