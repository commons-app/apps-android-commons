package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_WAIT
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.net.UnknownHostException

/**
 * Unit tests for [UploadMediaQualityHelper].
 */
@RunWith(RobolectricTestRunner::class)
class UploadMediaQualityHelperTest {
    @Mock
    private lateinit var repository: UploadRepository

    @Mock
    private lateinit var view: UploadMediaDetailsContract.View

    @Mock
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var imageCoordinates: ImageCoordinates

    private lateinit var activity: Activity
    private lateinit var basicKvStore: BasicKvStore
    private lateinit var testScheduler: TestScheduler
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var helper: UploadMediaQualityHelper

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val context = ApplicationProvider.getApplicationContext<Context>()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        basicKvStore = BasicKvStore(context, UploadActivity.STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE)
        basicKvStore.clearAll()

        testScheduler = TestScheduler()
        compositeDisposable = CompositeDisposable()
        helper =
            UploadMediaQualityHelper(
                repository = repository,
                mainThreadScheduler = testScheduler,
                compositeDisposable = compositeDisposable,
                viewProvider = { view },
            )
        helper.setupBasicKvStoreFactory { basicKvStore }
    }

    @After
    fun tearDown() {
        compositeDisposable.clear()
        UploadMediaPresenter.isBatteryDialogShowing = false
        UploadMediaPresenter.isCategoriesDialogShowing = false
    }

    @Test
    fun testGetImageQualityWithEmptyUploadsReturnsFalse() {
        whenever(repository.getUploads()).thenReturn(emptyList())

        val result = helper.getImageQuality(0, null, activity)

        assertFalse(result)
        verify(view).showProgress(true)
        verify(view).showProgress(false)
        val errorMessage =
            "Internal error: Zero upload items received by the Upload Media Detail Fragment. " +
                "Sorry, please upload again."
        verify(view).showMessage(
            errorMessage,
            R.color.color_error,
        )
    }

    @Test
    fun testGetImageQualitySuccess() {
        val location = LatLng(10.0, 20.0, 1.0f)
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location)).thenReturn(Single.just(IMAGE_OK))
        whenever(uploadItem.imageQuality).thenReturn(IMAGE_WAIT)
        whenever(uploadItem.gpsCoords).thenReturn(imageCoordinates)

        val result = helper.getImageQuality(0, location, activity)

        assertTrue(result)
        verify(view).showProgress(true)
        testScheduler.triggerActions()
        val storedJson = basicKvStore.getString(UploadMediaQualityHelper.UPLOAD_QUALITIES_KEY, null)
        assertTrue(storedJson?.contains("\"UploadItem0\":0") == true)
    }

    @Test
    fun testGetImageQualityUnknownHostException() {
        val location = LatLng(10.0, 20.0, 1.0f)
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(Single.error(UnknownHostException("network down")))

        val result = helper.getImageQuality(0, location, activity)

        assertTrue(result)
        testScheduler.triggerActions()
        verify(view).showProgress(false)
        verify(view).showConnectionErrorPopup()
    }

    @Test
    fun testGetImageQualityGenericError() {
        val location = LatLng(10.0, 20.0, 1.0f)
        whenever(repository.getUploads()).thenReturn(listOf(uploadItem))
        whenever(repository.getImageQuality(uploadItem, location))
            .thenReturn(Single.error(RuntimeException("unexpected error")))

        val result = helper.getImageQuality(0, location, activity)

        assertTrue(result)
        testScheduler.triggerActions()
        verify(view).showMessage("unexpected error", R.color.color_error)
    }

    @Test
    fun testCheckImageQualityWhenQualityIsOk() {
        whenever(uploadItem.imageQuality).thenReturn(IMAGE_WAIT)
        basicKvStore.putString(UploadMediaQualityHelper.UPLOAD_QUALITIES_KEY, "{\"UploadItem0\":0}")

        helper.checkImageQuality(uploadItem, 0)

        verify(view).showProgress(false)
        verify(uploadItem).hasInvalidLocation = false
        verify(uploadItem).imageQuality = IMAGE_OK
    }

    @Test
    fun testCheckImageQualityWhenQualityIsBad() {
        whenever(uploadItem.imageQuality).thenReturn(IMAGE_WAIT)
        basicKvStore.putString(UploadMediaQualityHelper.UPLOAD_QUALITIES_KEY, "{\"UploadItem0\":8}")

        helper.checkImageQuality(uploadItem, 0)

        verify(view).showProgress(false)
        verify(uploadItem).hasInvalidLocation = true
        verify(view).showBadImagePopup(8, 0, uploadItem)
    }

    @Test
    fun testUpdateImageQualitiesJSON() {
        basicKvStore.putString(
            UploadMediaQualityHelper.UPLOAD_QUALITIES_KEY,
            "{\"UploadItem0\":1,\"UploadItem1\":2}",
        )

        helper.updateImageQualitiesJSON(2, 0)

        val updatedJson = basicKvStore.getString(UploadMediaQualityHelper.UPLOAD_QUALITIES_KEY, null)
        assertTrue(updatedJson?.contains("\"UploadItem0\":2") == true)
        assertFalse(updatedJson?.contains("UploadItem1") == true)
    }
}
