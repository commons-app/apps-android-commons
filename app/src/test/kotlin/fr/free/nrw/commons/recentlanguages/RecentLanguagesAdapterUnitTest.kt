package fr.free.nrw.commons.recentlanguages

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import kotlinx.android.synthetic.main.row_item_languages_spinner.view.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RecentLanguagesAdapterUnitTest {

    private lateinit var adapter: RecentLanguagesAdapter
    private lateinit var languages: List<Language>

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var viewGroup: ViewGroup

    @Mock
    private lateinit var convertView: View

    @Mock
    private lateinit var textView: TextView

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        languages = listOf(
            Language("English", "en"),
            Language("Bengali", "bn")
        )
        adapter = RecentLanguagesAdapter(context, languages, hashMapOf(1 to "en"))
    }

    @Test
    @Throws(Exception::class)
    fun checkAdapterNotNull() {
        Assert.assertNotNull(adapter)
    }

    @Test
    @Throws(Exception::class)
    fun testIsEnabled() {
        val list = languages
        val recentLanguagesAdapter: Field =
            RecentLanguagesAdapter::class.java.getDeclaredField("recentLanguages")
        recentLanguagesAdapter.isAccessible = true
        recentLanguagesAdapter.set(adapter, list)
        Assert.assertEquals(adapter.isEnabled(0), false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCount() {
        val list = languages
        val recentLanguagesAdapter: Field =
            RecentLanguagesAdapter::class.java.getDeclaredField("recentLanguages")
        recentLanguagesAdapter.isAccessible = true
        recentLanguagesAdapter.set(adapter, list)
        Assert.assertEquals(adapter.count, list.size)
    }

    @Test
    @Throws(Exception::class)
    fun testGetLanguageName() {
        val list = languages
        val recentLanguagesAdapter: Field =
            RecentLanguagesAdapter::class.java.getDeclaredField("recentLanguages")
        recentLanguagesAdapter.isAccessible = true
        recentLanguagesAdapter.set(adapter, list)
        val languageName = list[0].languageName
        Assert.assertEquals(adapter.getLanguageName(0), languageName)
    }

    @Test
    @Throws(Exception::class)
    fun testGetView() {
        val list = languages
        whenever(convertView.tv_language).thenReturn(textView)
        val recentLanguagesAdapter: Field =
            RecentLanguagesAdapter::class.java.getDeclaredField("recentLanguages")
        recentLanguagesAdapter.isAccessible = true
        recentLanguagesAdapter.set(adapter, list)
        Assert.assertEquals(adapter.getView(0, convertView, viewGroup), convertView)
    }

    @Test
    @Throws(Exception::class)
    fun testGetLanguageCode() {
        val list = languages
        val recentLanguagesAdapter: Field =
            RecentLanguagesAdapter::class.java.getDeclaredField("recentLanguages")
        recentLanguagesAdapter.isAccessible = true
        recentLanguagesAdapter.set(adapter, list)
        val languageCode = list[0].languageCode
        Assert.assertEquals(adapter.getLanguageCode(0), languageCode)
    }
}