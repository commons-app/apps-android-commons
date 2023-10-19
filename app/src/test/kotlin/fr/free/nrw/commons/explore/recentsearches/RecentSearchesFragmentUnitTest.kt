package fr.free.nrw.commons.explore.recentsearches

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RecentSearchesFragmentUnitTest {

    private lateinit var fragment: RecentSearchesFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var recentSearchesDao: RecentSearchesDao

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var listView: ListView

    @Mock
    private lateinit var adapter: ArrayAdapter<*>

    @Mock
    private lateinit var dialog: DialogInterface

    @Mock
    private lateinit var recentSearches: List<String>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        AppAdapter.set(TestAppAdapter())

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        fragment = RecentSearchesFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)
        view = LayoutInflater.from(activity)
            .inflate(R.layout.fragment_leaderboard, null) as View

        Whitebox.setInternalState(fragment, "recentSearchesDao", recentSearchesDao)
        Whitebox.setInternalState(fragment, "recent_searches_delete_button", imageView)
        Whitebox.setInternalState(fragment, "recent_searches_text_view", textView)
        Whitebox.setInternalState(fragment, "adapter", adapter)
        Whitebox.setInternalState(fragment, "recentSearchesList", listView)
        Whitebox.setInternalState(fragment, "recentSearches", listOf("string"))
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        fragment.onCreateView(layoutInflater, null, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        whenever(recentSearchesDao.recentSearches(10)).thenReturn(mutableListOf("search1"))
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testShowDeleteRecentAlertDialog() {
        val method: Method = RecentSearchesFragment::class.java.getDeclaredMethod(
            "showDeleteRecentAlertDialog",
            Context::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, context)
    }

    @Test
    @Throws(Exception::class)
    fun testSetDeleteRecentPositiveButton() {
        val method: Method = RecentSearchesFragment::class.java.getDeclaredMethod(
            "setDeleteRecentPositiveButton",
            Context::class.java,
            DialogInterface::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, context, dialog)
    }

    @Test
    @Throws(Exception::class)
    fun testShowDeleteAlertDialog() {
        val method: Method = RecentSearchesFragment::class.java.getDeclaredMethod(
            "showDeleteAlertDialog",
            Context::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, context, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testSetDeletePositiveButton() {
        val method: Method = RecentSearchesFragment::class.java.getDeclaredMethod(
            "setDeletePositiveButton",
            Context::class.java,
            DialogInterface::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, context, dialog, 0)
    }

}