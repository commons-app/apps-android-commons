package fr.free.nrw.commons.utils

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ImageUtilsTest {

    private lateinit var context: Context

    @Mock
    private lateinit var bitmap: Bitmap

    @Mock
    private lateinit var progressDialogWallpaper: ProgressDialog

    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCheckIfImageIsTooDarkCaseException() {
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("")
            , ImageUtils.IMAGE_OK)
    }

    // Refer: testCheckIfImageIsTooDarkCaseException()
    @Test
    fun testCheckIfProperImageIsTooDark() {
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/ok1.jpg")
            , ImageUtils.IMAGE_OK)
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/ok2.jpg")
            , ImageUtils.IMAGE_OK)
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/ok3.jpg")
            , ImageUtils.IMAGE_OK)
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/ok4.jpg")
            , ImageUtils.IMAGE_OK)
    }

    // Refer: testCheckIfImageIsTooDarkCaseException()
    @Test
    fun testCheckIfDarkImageIsTooDark() {
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/dark1.jpg")
            , ImageUtils.IMAGE_DARK)
        Assert.assertEquals(ImageUtils.checkIfImageIsTooDark("src/test/resources/ImageTest/dark2.jpg")
            , ImageUtils.IMAGE_DARK)
    }

    @Test
    fun testCheckIfImageIsTooDark() {
        val tempFile = File.createTempFile("prefix", "suffix")
        ImageUtils.checkIfImageIsTooDark(tempFile.absolutePath)
    }

    @Test
    fun testSetWallpaper() {
        val mockImageUtils = mock(ImageUtils::class.java)
        val method: Method = ImageUtils::class.java.getDeclaredMethod(
            "setWallpaper",
            Context::class.java,
            Bitmap::class.java
        )
        method.isAccessible = true

        `when`(progressDialogWallpaper.isShowing).thenReturn(true)

        val progressDialogWallpaperField: Field =
            ImageUtils::class.java.getDeclaredField("progressDialogWallpaper")
        progressDialogWallpaperField.isAccessible = true
        progressDialogWallpaperField.set(mockImageUtils, progressDialogWallpaper)

        method.invoke(mockImageUtils, context, bitmap)
    }

    @Test
    fun testShowSettingAvatarProgressBar() {
        val mockImageUtils = mock(ImageUtils::class.java)
        val method: Method = ImageUtils::class.java.getDeclaredMethod(
            "showSettingAvatarProgressBar",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockImageUtils, context)
    }

    @Test
    fun testGetErrorMessageForResultCase0() {
        ImageUtils.getErrorMessageForResult(context, 0)
    }

    @Test
    fun testGetErrorMessageForResultCaseIMAGE_DARK() {
        ImageUtils.getErrorMessageForResult(context, 1)
    }

    @Test
    fun testGetErrorMessageForResultCaseIMAGE_BLURRY() {
        ImageUtils.getErrorMessageForResult(context, 2)
    }

    @Test
    fun testGetErrorMessageForResultCaseIMAGE_DUPLICATE() {
        ImageUtils.getErrorMessageForResult(context, 4)
    }

    @Test
    fun testGetErrorMessageForResultCaseIMAGE_GEOLOCATION_DIFFERENT() {
        ImageUtils.getErrorMessageForResult(context, 8)
    }

    @Test
    fun testGetErrorMessageForResultCaseFILE_FBMD() {
        ImageUtils.getErrorMessageForResult(context, 16)
    }

    @Test
    fun testGetErrorMessageForResultCaseFILE_NO_EXIF() {
        ImageUtils.getErrorMessageForResult(context, 32)
    }

    @Test
    fun testSetAvatarFromImageUrl() {
        ImageUtils.setAvatarFromImageUrl(
            context,
            "",
            "",
            okHttpJsonApiClient,
            compositeDisposable
        )
    }

    @Test
    fun testCheckImageGeolocationIsDifferentCaseNull() {
        Assert.assertEquals(
            ImageUtils.checkImageGeolocationIsDifferent(
                "test",
                null
            ), false
        )
    }

    @Test
    fun testCheckImageGeolocationIsDifferent() {
        Assert.assertEquals(
            ImageUtils.checkImageGeolocationIsDifferent(
                "0.0|0.0",
                LatLng(0.0, 0.0, 0f)
            ), false
        )
    }

    @Test
    fun testCheckIfImageIsDark() {
        val mockImageUtils = mock(ImageUtils::class.java)
        val method: Method = ImageUtils::class.java.getDeclaredMethod(
            "checkIfImageIsDark",
            Bitmap::class.java
        )
        method.isAccessible = true
        method.invoke(mockImageUtils, null)
    }

}