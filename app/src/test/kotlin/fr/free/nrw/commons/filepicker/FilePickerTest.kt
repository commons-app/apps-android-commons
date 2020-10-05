package fr.free.nrw.commons.filepicker

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import fr.free.nrw.commons.filepicker.Constants.RequestCodes
import fr.free.nrw.commons.filepicker.FilePicker.handleActivityResult
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

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

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testOpenGalleryRequestCode() {
        `when`(PreferenceManager.getDefaultSharedPreferences(activity)).thenReturn(sharedPref)
        `when`(sharedPref.edit()).thenReturn(sharedPreferencesEditor)
        `when`(sharedPref.edit().putInt("type", 0)).thenReturn(sharedPreferencesEditor)
        FilePicker.openGallery(activity, 0)
        verify(activity).startActivityForResult(ArgumentMatchers.anyObject(), requestCodeCaptor?.capture()?.toInt()!!)
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
        verify(activity).startActivityForResult(ArgumentMatchers.anyObject(), requestCodeCaptor?.capture()?.toInt()!!)
        assertEquals(requestCodeCaptor?.value, RequestCodes.TAKE_PICTURE)
    }
}