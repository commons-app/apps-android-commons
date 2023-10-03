package fr.free.nrw.commons.description

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.ActivityDescriptionEditBinding
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowProgressDialog
import java.lang.reflect.Method
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DescriptionEditActivityUnitTest {

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var uploadMediaDetails: ArrayList<UploadMediaDetail>
    private lateinit var binding: ActivityDescriptionEditBinding

    @Mock
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter

    @Mock
    private lateinit var rvDescriptions: RecyclerView

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        uploadMediaDetails = mutableListOf(UploadMediaDetail("en", "desc"))
                as ArrayList<UploadMediaDetail>
        val intent = Intent().putExtra("title", "read")
        val bundle = Bundle()
        bundle.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, uploadMediaDetails)
        bundle.putString(WIKITEXT, "desc")
        bundle.putString(Prefs.DESCRIPTION_LANGUAGE, "bn")
        intent.putExtras(bundle)
        activity =
            Robolectric.buildActivity(DescriptionEditActivity::class.java, intent).create().get()
        binding = ActivityDescriptionEditBinding.inflate(LayoutInflater.from(activity))
        activity.setContentView(R.layout.activity_description_edit)

        Whitebox.setInternalState(activity, "wikiText", "Description=")
        Whitebox.setInternalState(activity, "uploadMediaDetailAdapter", uploadMediaDetailAdapter)
        Whitebox.setInternalState(activity, "rvDescriptions", rvDescriptions)
        Whitebox.setInternalState(activity, "binding", binding)
        Whitebox.setInternalState(activity, "savedLanguageValue", "bn")
        `when`(uploadMediaDetailAdapter.items).thenReturn(uploadMediaDetails)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testShowLoggingProgressBar() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "showLoggingProgressBar"
        )
        method.isAccessible = true
        method.invoke(activity)
        val dialog: ProgressDialog = ShadowProgressDialog.getLatestDialog() as ProgressDialog
        assertEquals(dialog.isShowing, true)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateDescription() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "updateDescription", List::class.java
        )
        method.isAccessible = true
        method.invoke(activity, mutableListOf(UploadMediaDetail("en", "desc")))
        assertEquals(activity.isFinishing, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSubmitButtonClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onSubmitButtonClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
        assertEquals(activity.isFinishing, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnButtonAddDescriptionClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onButtonAddDescriptionClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
        verify(uploadMediaDetailAdapter).addDescription(UploadMediaDetail())
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackButtonClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onBackButtonClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
        assertEquals(activity.isFinishing, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPrimaryCaptionTextChange() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onPrimaryCaptionTextChange", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowInfoAlert() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "showInfoAlert", Int::class.java, Int::class.java
        )
        method.isAccessible = true
        method.invoke(
            activity,
            android.R.string.ok,
            android.R.string.ok
        )
        val dialog: AlertDialog = ShadowAlertDialog.getLatestDialog() as AlertDialog
        assertEquals(dialog.isShowing, true)
    }

}