package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.filepicker.Constants.RequestCodes
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method
import kotlin.random.Random.Default.nextBoolean

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [21],
    application = TestCommonsApplication::class,
    shadows = [ShadowFileProvider::class]
)
@LooperMode(LooperMode.Mode.PAUSED)
class FilePickerTest {

    @Mock
    internal lateinit var activity: Activity

    @Mock
    internal lateinit var sharedPref: SharedPreferences

    @Mock
    var sharedPreferencesEditor: SharedPreferences.Editor? = null

    @Mock
    var unit: Unit? = null

    @Captor
    var requestCodeCaptor: ArgumentCaptor<Integer>? = null

    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testOpenGalleryRequestCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        FilePicker.openGallery(activity, 0, nextBoolean())
        verify(activity).startActivityForResult(
            ArgumentMatchers.any(),
            requestCodeCaptor?.capture()?.toInt()!!
        )
        assertEquals(requestCodeCaptor?.value, RequestCodes.PICK_PICTURE_FROM_GALLERY)
    }

    @Test
    fun testOpenCameraForImageCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        val mockApplication = mock(Application::class.java)
        `when`(activity.applicationContext).thenReturn(mockApplication)
        FilePicker.openCameraForImage(activity, 0)
        verify(activity).startActivityForResult(
            ArgumentMatchers.any(),
            requestCodeCaptor?.capture()?.toInt()!!
        )
        assertEquals(requestCodeCaptor?.value, RequestCodes.TAKE_PICTURE)
    }

    @Test
    fun testCreateCameraPictureFile() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "createCameraPictureFile",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testCreateCameraForImageIntent() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "createCameraForImageIntent",
            Context::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, context, 0)
    }

    @Test
    fun testRevokeWritePermission() {
        val mockFilePicker = mock(FilePicker::class.java)
        val mockUri = mock(Uri::class.java)
        val mockContext = mock(Context::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "revokeWritePermission",
            Context::class.java,
            Uri::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, mockContext, mockUri)
    }

    @Test
    fun testRestoreType() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "restoreType",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testTakenCameraPicture() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "takenCameraPicture",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testTakenCameraPictureCaseTrue() {
        val mockFilePicker = mock(FilePicker::class.java)
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.getString("last_photo", null)).thenReturn("")
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "takenCameraPicture",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, activity)
    }

    @Test
    fun testTakenCameraVideo() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "takenCameraVideo",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testTakenCameraVideoCaseTrue() {
        val mockFilePicker = mock(FilePicker::class.java)
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.getString("last_video", null)).thenReturn("")
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "takenCameraVideo",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, activity)
    }

    @Test
    fun testIsPhoto() {
        val mockFilePicker = mock(FilePicker::class.java)
        val mockIntent = mock(Intent::class.java)
        val method: Method = FilePicker::class.java.getDeclaredMethod(
            "isPhoto",
            Intent::class.java
        )
        method.isAccessible = true
        method.invoke(mockFilePicker, mockIntent)
    }

    @Test
    fun testHandleActivityResultCaseOne() {
        val mockIntent = mock(Intent::class.java)
        FilePicker.handleActivityResult(
            RequestCodes.FILE_PICKER_IMAGE_IDENTIFICATOR,
            Activity.RESULT_OK,
            mockIntent,
            activity,
            object : DefaultCallback() {
                override fun onCanceled(source: FilePicker.ImageSource, type: Int) {
                    super.onCanceled(source, type)
                }

                override fun onImagePickerError(
                    e: Exception,
                    source: FilePicker.ImageSource,
                    type: Int
                ) {
                }

                override fun onImagesPicked(
                    imagesFiles: List<UploadableFile>,
                    source: FilePicker.ImageSource,
                    type: Int
                ) {
                }
            })
    }

    @Test
    fun testOpenCustomSelectorRequestCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        FilePicker.openCustomSelector(activity, 0)
        verify(activity).startActivityForResult(ArgumentMatchers.any(), requestCodeCaptor?.capture()?.toInt()!!)
        assertEquals(requestCodeCaptor?.value, RequestCodes.PICK_PICTURE_FROM_CUSTOM_SELECTOR)
    }
}