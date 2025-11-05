package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import org.junit.Assert.assertEquals
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
import java.lang.reflect.Method
import kotlin.random.Random.Default.nextBoolean

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [21],
    application = TestCommonsApplication::class,
    shadows = [ShadowFileProvider::class],
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

    @Mock
    private lateinit var mockResultLauncher: ActivityResultLauncher<Intent>

    private val intentCaptor: KArgumentCaptor<Intent> = argumentCaptor()

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
        val openDocumentPreferred = nextBoolean()

        FilePicker.openGallery(activity, mockResultLauncher, 0, openDocumentPreferred)

        verify(mockResultLauncher).launch(intentCaptor.capture())

        val capturedIntent = intentCaptor.firstValue

        if (openDocumentPreferred) {
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, capturedIntent.action)
        } else {
            assertEquals(Intent.ACTION_GET_CONTENT, capturedIntent.action)
        }
    }

    @Test
    fun testOpenCameraForImageCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        val mockApplication = mock(Application::class.java)
        `when`(activity.applicationContext).thenReturn(mockApplication)
        FilePicker.openCameraForImage(activity, mockResultLauncher, 0)

        verify(mockResultLauncher).launch(intentCaptor.capture())

        val capturedIntent = intentCaptor.firstValue
        
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, capturedIntent.action)
    }

    @Test
    fun testCreateCameraPictureFile() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "createCameraPictureFile",
                Context::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testCreateCameraForImageIntent() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "createCameraForImageIntent",
                Context::class.java,
                Int::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, context, 0)
    }

    @Test
    fun testRevokeWritePermission() {
        val mockFilePicker = mock(FilePicker::class.java)
        val mockUri = mock(Uri::class.java)
        val mockContext = mock(Context::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "revokeWritePermission",
                Context::class.java,
                Uri::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, mockContext, mockUri)
    }

    @Test
    fun testRestoreType() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "restoreType",
                Context::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testTakenCameraPicture() {
        val mockFilePicker = mock(FilePicker::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "takenCameraPicture",
                Context::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, context)
    }

    @Test
    fun testTakenCameraPictureCaseTrue() {
        val mockFilePicker = mock(FilePicker::class.java)
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.getString("last_photo", null)).thenReturn("")
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "takenCameraPicture",
                Context::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, activity)
    }

    @Test
    fun testIsPhoto() {
        val mockFilePicker = mock(FilePicker::class.java)
        val mockIntent = mock(Intent::class.java)
        val method: Method =
            FilePicker::class.java.getDeclaredMethod(
                "isPhoto",
                Intent::class.java,
            )
        method.isAccessible = true
        method.invoke(mockFilePicker, mockIntent)
    }

    @Test
    fun testOpenCustomSelectorRequestCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        FilePicker.openCustomSelector(activity, mockResultLauncher, 0)

        verify(mockResultLauncher).launch(intentCaptor.capture())

        val capturedIntent = intentCaptor.firstValue

        assertEquals(
            CustomSelectorActivity.Companion::class.java.declaringClass.name,
            capturedIntent.component?.className
        )
    }
}
