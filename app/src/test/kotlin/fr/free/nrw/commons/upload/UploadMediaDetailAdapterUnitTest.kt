package fr.free.nrw.commons.upload

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.GridLayout
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.recentlanguages.Language
import fr.free.nrw.commons.recentlanguages.RecentLanguagesAdapter
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.SettingsFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field
import java.lang.reflect.Method

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

    @Mock
    private lateinit var recentLanguagesDao: RecentLanguagesDao

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var listView: ListView

    @Mock
    private lateinit var adapterView: AdapterView<RecentLanguagesAdapter>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        uploadMediaDetails = mutableListOf(uploadMediaDetail, uploadMediaDetail)
        activity = Robolectric.buildActivity(UploadActivity::class.java).get()
        adapter = UploadMediaDetailAdapter("", recentLanguagesDao)
        context = ApplicationProvider.getApplicationContext()
        Whitebox.setInternalState(adapter, "uploadMediaDetails", uploadMediaDetails)
        Whitebox.setInternalState(adapter, "eventListener", eventListener)
        Whitebox.setInternalState(adapter, "recentLanguagesTextView", textView)
        Whitebox.setInternalState(adapter, "separator", view)
        Whitebox.setInternalState(adapter, "languageHistoryListView", listView)
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

    @Test
    fun testHideRecentLanguagesSection() {
        val method: Method = UploadMediaDetailAdapter.ViewHolder::class.java.getDeclaredMethod(
            "hideRecentLanguagesSection"
        )
        method.isAccessible = true
        method.invoke(viewHolder)
        verify(listView, times(1)).visibility = View.GONE
        verify(view, times(1)).visibility = View.GONE
        verify(textView, times(1)).visibility = View.GONE
    }

    @Test
    fun `Test setUpRecentLanguagesSection when list is empty`() {
        val method: Method = UploadMediaDetailAdapter.ViewHolder::class.java.getDeclaredMethod(
            "setUpRecentLanguagesSection",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(viewHolder, emptyList<Language>())
        verify(listView, times(1)).visibility = View.GONE
        verify(view, times(1)).visibility = View.GONE
        verify(textView, times(1)).visibility = View.GONE
    }

    @Test
    fun `Test setUpRecentLanguagesSection when list is not empty`() {
        val method: Method = UploadMediaDetailAdapter.ViewHolder::class.java.getDeclaredMethod(
            "setUpRecentLanguagesSection",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(viewHolder, listOf(
            Language("Bengali", "bn"),
            Language("Bengali", "bn"),
            Language("Bengali", "bn"),
            Language("Bengali", "bn"),
            Language("Bengali", "bn"),
            Language("Bengali", "bn")
        ))
        verify(listView, times(1)).visibility = View.VISIBLE
        verify(view, times(1)).visibility = View.VISIBLE
        verify(textView, times(1)).visibility = View.VISIBLE
    }

    @Test
    @Throws(Exception::class)
    fun testOnRecentLanguageClicked() {
        whenever(recentLanguagesDao.findRecentLanguage(any()))
            .thenReturn(true)
        whenever(adapterView.adapter)
            .thenReturn(
                RecentLanguagesAdapter(context,
                listOf(Language("English", "en")),
                hashMapOf<String,String>()
                )
            )
        val method: Method = UploadMediaDetailAdapter.ViewHolder::class.java.getDeclaredMethod(
            "onRecentLanguageClicked",
            Dialog::class.java,
            AdapterView::class.java,
            Int::class.java,
            UploadMediaDetail::class.java
        )
        method.isAccessible = true
        method.invoke(viewHolder, Mockito.mock(Dialog::class.java), adapterView, 0,
            Mockito.mock(UploadMediaDetail::class.java))
        verify(recentLanguagesDao, times(1)).findRecentLanguage(any())
        verify(adapterView, times(3)).adapter
    }

    @Test
    fun testRemoveLeadingAndTrailingWhitespace() {
        // empty space
        val test1 = "  test  "
        val expected1 = "test"
        Assert.assertEquals(expected1, viewHolder.removeLeadingAndTrailingWhitespace(test1))

        val test2 = "  test test "
        val expected2 = "test test"
        Assert.assertEquals(expected2, viewHolder.removeLeadingAndTrailingWhitespace(test2))

        // No whitespace
        val test3 = "No trailing space";
        val expected3 = "No trailing space";
        Assert.assertEquals(expected3, viewHolder.removeLeadingAndTrailingWhitespace(test3))

        // blank string
        val test4 = " \r \t  "
        val expected4 = "";
        Assert.assertEquals(expected4, viewHolder.removeLeadingAndTrailingWhitespace(test4))
    }

    @Test
    fun testRemoveLeadingAndTrailingInstanceTab() {
        val test = "\ttest\t"
        val expected = "test"
        Assert.assertEquals(expected, viewHolder.removeLeadingAndTrailingWhitespace(test))
    }

    @Test
    fun testRemoveLeadingAndTrailingCarriageReturn() {
        val test = "\rtest\r"
        val expected = "test"
        Assert.assertEquals(expected, viewHolder.removeLeadingAndTrailingWhitespace(test))
    }

    @Test
    fun testCaptionJapaneseCharacters() {
        val test1 = "テスト　テスト"
        val expected1 = "テスト テスト"
        Assert.assertEquals(expected1, viewHolder.convertIdeographicSpaceToLatinSpace(test1));

        val test2 = "　\r　\t　テスト　\r　\t　"
        val expected2 = "テスト"
        Assert.assertEquals(expected2, viewHolder.removeLeadingAndTrailingWhitespace(test2))
    }
}