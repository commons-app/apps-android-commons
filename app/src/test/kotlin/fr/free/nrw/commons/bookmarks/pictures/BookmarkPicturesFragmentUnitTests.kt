package fr.free.nrw.commons.bookmarks.pictures

import android.content.ContentProviderClient
import android.content.Context
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.GridView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import media
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkPicturesFragmentUnitTests {

    private lateinit var fragment: BookmarkPicturesFragment

    private lateinit var context: Context

    private lateinit var view: View

    @Mock
    lateinit var statusTextView: TextView

    @Mock
    lateinit var progressBar: ProgressBar

    @Mock
    lateinit var gridView: GridView

    @Mock
    private lateinit var parentLayout: RelativeLayout

    @Mock
    private lateinit var client: ContentProviderClient

    @Mock
    private lateinit var savedInstanceState: Bundle

    private lateinit var controller: BookmarkPicturesController

    @Mock
    private lateinit var gridAdapter: GridViewAdapter

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var throwable: Throwable

    @Mock
    private lateinit var mediaList: List<Media>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = BookmarkPicturesFragment.newInstance()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        view = LayoutInflater.from(activity)
            .inflate(R.layout.fragment_bookmarks_pictures, null) as View

        fragment.statusTextView = statusTextView
        fragment.progressBar = progressBar
        fragment.gridView = gridView
        fragment.parentLayout = parentLayout

        val bookmarkDao = BookmarkPicturesDao { client }

        controller = BookmarkPicturesController(mediaClient, bookmarkDao)

        fragment.controller = controller

        Whitebox.setInternalState(fragment, "gridAdapter", GridViewAdapter(
            context,
            0,
            listOf(media())
        ))
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewCreated() {
        fragment.onViewCreated(view, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testOnStop() {
        fragment.onStop()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        fragment.onDestroy()
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testSetAdapter() {
        shadowOf(getMainLooper()).idle()
        val method: Method =
            BookmarkPicturesFragment::class.java.getDeclaredMethod("setAdapter", List::class.java)
        method.isAccessible = true
        method.invoke(fragment, mediaList)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAdapter() {
        fragment.adapter
    }

    @Test
    @Throws(Exception::class)
    fun testHandleNoInternet() {
        val method: Method =
            BookmarkPicturesFragment::class.java.getDeclaredMethod("handleNoInternet")
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testHandleError() {
        val method: Method = BookmarkPicturesFragment::class.java.getDeclaredMethod(
            "handleError",
            Throwable::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, throwable)
    }

    @Test
    @Throws(Exception::class)
    fun testHandleSuccess() {
        gridAdapter.addItems(listOf(media()))
        val method: Method = BookmarkPicturesFragment::class.java.getDeclaredMethod(
            "handleSuccess",
            List::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, listOf(media()))
        verify(progressBar, times(1)).setVisibility(GONE)
        verify(statusTextView, times(1)).setVisibility(GONE)
        verify(gridView, times(1)).setVisibility(VISIBLE)
        verify(gridView, times(1)).setAdapter(any())
    }

    @Test
    @Throws(Exception::class)
    fun testInitErrorView() {
        val method: Method = BookmarkPicturesFragment::class.java.getDeclaredMethod("initErrorView")
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testInitEmptyBookmarkListView() {
        val method: Method =
            BookmarkPicturesFragment::class.java.getDeclaredMethod("initEmptyBookmarkListView")
        method.isAccessible = true
        method.invoke(fragment)
    }
}