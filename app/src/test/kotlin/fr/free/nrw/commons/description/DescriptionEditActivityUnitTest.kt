package fr.free.nrw.commons.description

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.ActivityDescriptionEditBinding
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method
import java.util.ArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DescriptionEditActivityUnitTest {

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var uploadMediaDetails: ArrayList<UploadMediaDetail>
    private lateinit var binding: ActivityDescriptionEditBinding

    @Mock
    private lateinit var bundle: Bundle

    @Mock
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter

    @Mock
    private lateinit var rvDescriptions: RecyclerView

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        uploadMediaDetails = mutableListOf(UploadMediaDetail("en", "desc"))
                as ArrayList<UploadMediaDetail>
        val intent = Intent().putExtra("title", "read")
        val bundle = Bundle()
        bundle.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, uploadMediaDetails)
        bundle.putString(WIKITEXT, "desc")
        intent.putExtras(bundle)
        activity =
            Robolectric.buildActivity(DescriptionEditActivity::class.java, intent).get()
        binding = ActivityDescriptionEditBinding.inflate(LayoutInflater.from(activity))
        activity.setContentView(R.layout.activity_description_edit)

        Whitebox.setInternalState(activity, "wikiText", "Description=")
        Whitebox.setInternalState(activity, "uploadMediaDetailAdapter", uploadMediaDetailAdapter)
        Whitebox.setInternalState(activity, "rvDescriptions", rvDescriptions)
        Whitebox.setInternalState(activity, "binding", binding)
        `when`(uploadMediaDetailAdapter.items).thenReturn(uploadMediaDetails)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreate() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onCreate", Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }


    @Test
    @Throws(Exception::class)
    fun testShowLoggingProgressBar() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "showLoggingProgressBar"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateDescription() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "updateDescription", List::class.java
        )
        method.isAccessible = true
        method.invoke(activity, mutableListOf(UploadMediaDetail("en", "desc")))
    }

    @Test
    @Throws(Exception::class)
    fun testOnSubmitButtonClicked() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onSubmitButtonClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnButtonAddDescriptionClicked() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onButtonAddDescriptionClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackButtonClicked() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onBackButtonClicked", View::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPrimaryCaptionTextChange() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "onPrimaryCaptionTextChange", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowInfoAlert() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "showInfoAlert", Int::class.java, Int::class.java
        )
        method.isAccessible = true
        method.invoke(
            activity,
            android.R.string.ok,
            android.R.string.ok
        )
    }

    @Test
    @Throws(Exception::class)
    fun testInitRecyclerView() {
        val method: Method = DescriptionEditActivity::class.java.getDeclaredMethod(
            "initRecyclerView", ArrayList::class.java
        )
        method.isAccessible = true
        method.invoke(
            activity,
            uploadMediaDetails
        )
    }

}