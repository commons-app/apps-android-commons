package fr.free.nrw.commons.upload

import android.content.Context
import android.widget.GridLayout
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.data.models.upload.UploadMediaDetail
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UploadMediaDetailAdapterUnitTest {

    private lateinit var adapter: UploadMediaDetailAdapter
    private lateinit var context: Context
    private lateinit var viewHolder: UploadMediaDetailAdapter.ViewHolder
    private lateinit var activity: UploadActivity
    private lateinit var uploadMediaDetails: List<UploadMediaDetail>

    @Mock
    private lateinit var uploadMediaDetail: UploadMediaDetail

    @Mock
    private lateinit var eventListener: UploadMediaDetailAdapter.EventListener

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        uploadMediaDetails = mutableListOf(uploadMediaDetail, uploadMediaDetail)
        activity = Robolectric.buildActivity(UploadActivity::class.java).get()
        adapter = UploadMediaDetailAdapter("")
        context = RuntimeEnvironment.application.applicationContext
        Whitebox.setInternalState(adapter, "uploadMediaDetails", uploadMediaDetails)
        Whitebox.setInternalState(adapter, "eventListener", eventListener)
        viewHolder = adapter.onCreateViewHolder(GridLayout(activity), 0)
    }

    @Test
    @Throws(Exception::class)
    fun checkAdapterNotNull() {
        Assert.assertNotNull(adapter)
    }

    @Test
    @Throws(Exception::class)
    fun testSetItems() {
        val list = mutableListOf(uploadMediaDetail)
        val uploadMediaDetails: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("uploadMediaDetails")
        uploadMediaDetails.isAccessible = true
        val selectedLanguages: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("selectedLanguages")
        selectedLanguages.isAccessible = true
        adapter.items = list
        Assert.assertEquals(uploadMediaDetails.get(adapter), list)
        val map: HashMap<Int, String> = selectedLanguages.get(adapter) as HashMap<Int, String>
        Assert.assertEquals(map.size, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetItems() {
        val list = mutableListOf(uploadMediaDetail)
        val uploadMediaDetails: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("uploadMediaDetails")
        uploadMediaDetails.isAccessible = true
        uploadMediaDetails.set(adapter, list)
        Assert.assertEquals(adapter.items, list)
    }

    @Test
    @Throws(Exception::class)
    fun testGetItemCount() {
        val list = mutableListOf(uploadMediaDetail)
        val uploadMediaDetails: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("uploadMediaDetails")
        uploadMediaDetails.isAccessible = true
        uploadMediaDetails.set(adapter, list)
        Assert.assertEquals(adapter.itemCount, list.size)
    }

    @Test
    @Throws(Exception::class)
    fun testAddDescription() {
        val list = mutableListOf(uploadMediaDetail)
        val uploadMediaDetails: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("uploadMediaDetails")
        uploadMediaDetails.isAccessible = true
        uploadMediaDetails.set(adapter, list)
        val selectedLanguages: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("selectedLanguages")
        selectedLanguages.isAccessible = true
        selectedLanguages.set(adapter, hashMapOf<Int, String>())
        adapter.addDescription(uploadMediaDetail)
        val map: HashMap<Int, String> = selectedLanguages.get(adapter) as HashMap<Int, String>
        Assert.assertEquals(map[list.size], null)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveDescription() {
        val list = mutableListOf(uploadMediaDetail)
        val uploadMediaDetails: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("uploadMediaDetails")
        uploadMediaDetails.isAccessible = true
        uploadMediaDetails.set(adapter, list)
        val selectedLanguages: Field =
            UploadMediaDetailAdapter::class.java.getDeclaredField("selectedLanguages")
        selectedLanguages.isAccessible = true
        selectedLanguages.set(adapter, hashMapOf<Int, String>())
        adapter.removeDescription(uploadMediaDetail, list.size)
        val map: HashMap<Int, String> = selectedLanguages.get(adapter) as HashMap<Int, String>
        Assert.assertEquals(map[list.size], null)
    }

    @Test
    fun testOnBindViewHolderPosition0() {
        Whitebox.setInternalState(adapter, "savedLanguageValue", "")
        whenever(uploadMediaDetail.isManuallyAdded).thenReturn(false)
        whenever(uploadMediaDetail.selectedLanguageIndex).thenReturn(-1)
        adapter.onBindViewHolder(viewHolder, 0)
        verify(eventListener).onPrimaryCaptionTextChange(false)
        verify(uploadMediaDetail).isManuallyAdded
    }

    @Test
    fun testOnBindViewHolderPosition1() {
        Whitebox.setInternalState(adapter, "savedLanguageValue", "en")
        whenever(uploadMediaDetail.isManuallyAdded).thenReturn(true)
        whenever(uploadMediaDetail.selectedLanguageIndex).thenReturn(1)
        adapter.onBindViewHolder(viewHolder, 1)
        verify(uploadMediaDetail).isManuallyAdded
    }

}