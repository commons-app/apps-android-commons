package fr.free.nrw.commons.media

import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.Callable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaDetailMenuHandlerTest {
    private lateinit var bookmarkDao: BookmarkPicturesDao
    private lateinit var sessionManager: SessionManager
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient
    private lateinit var menuHandler: MediaDetailMenuHandler

    private lateinit var activity: Activity
    private lateinit var media: Media
    private lateinit var menu: Menu
    private lateinit var menuItem: MenuItem
    private lateinit var bookmark: Bookmark

    @Before
    fun setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setComputationSchedulerHandler { Schedulers.trampoline() }

        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)

        bookmarkDao = mock()
        sessionManager = mock()
        okHttpJsonApiClient = mock()
        menuHandler = MediaDetailMenuHandler(bookmarkDao, sessionManager, okHttpJsonApiClient)

        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        media = mock()
        menu = mock()
        menuItem = mock()
        bookmark = mock()

        whenever(menu.findItem(any())).thenReturn(menuItem)
        whenever(menuItem.setVisible(any())).thenReturn(menuItem)
        whenever(menuItem.setEnabled(any())).thenReturn(menuItem)
    }

    @After
    fun tearDown() {
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }

    @Test
    fun testSetWallpaper_nullUrl() {
        whenever(media.imageUrl).thenReturn(null)
        menuHandler.setWallpaper(activity, media)
    }

    @Test
    fun testSetWallpaper_emptyUrl() {
        whenever(media.imageUrl).thenReturn("")
        menuHandler.setWallpaper(activity, media)
    }

    @Test
    fun testSetWallpaper_validUrl() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        whenever(media.imageUrl).thenReturn("https://upload.wikimedia.org/test.jpg")
        menuHandler.setWallpaper(activity, media)
    }

    @Test
    fun testSetAvatar_nullUrl() {
        whenever(media.imageUrl).thenReturn(null)
        menuHandler.setAvatar(activity, media)
    }

    @Test
    fun testSetAvatar_emptyUrl() {
        whenever(media.imageUrl).thenReturn("")
        menuHandler.setAvatar(activity, media)
    }

    @Test
    fun testSetAvatar_validUrl() {
        whenever(media.imageUrl).thenReturn("https://upload.wikimedia.org/test.jpg")
        val account = Account("TestUser", "fr.free.nrw.commons")
        whenever(sessionManager.currentAccount).thenReturn(account)
        menuHandler.setAvatar(activity, media)
    }

    @Test
    fun testHandleBackgroundColorMenuItems_transparentBitmap() {
        val bitmap = mock<Bitmap>()
        whenever(bitmap.hasAlpha()).thenReturn(true)

        menuHandler.handleBackgroundColorMenuItems(Callable { bitmap }, menu)

        verify(bitmap).hasAlpha()
        verify(menu).findItem(R.id.menu_view_set_white_background)
        verify(menu).findItem(R.id.menu_view_set_black_background)
        verify(menuItem, times(2)).isEnabled = true
        verify(menuItem, times(2)).isVisible = true
    }

    @Test
    fun testHandleBackgroundColorMenuItems_opaqueBitmap() {
        val bitmap = mock<Bitmap>()
        whenever(bitmap.hasAlpha()).thenReturn(false)

        menuHandler.handleBackgroundColorMenuItems(Callable { bitmap }, menu)

        verify(bitmap).hasAlpha()
        verify(menu, never()).findItem(any())
        verify(menuItem, never()).isEnabled = true
        verify(menuItem, never()).isVisible = true
    }

    @Test
    fun testUpdateBookmarkState_isBookmarked() {
        whenever(bookmarkDao.findBookmark(bookmark)).thenReturn(true)
        val removedItems = arrayListOf(0, 1)

        menuHandler.updateBookmarkState(menuItem, bookmark, 0, removedItems)

        assertFalse(removedItems.contains(0))
        verify(menuItem).setIcon(R.drawable.menu_ic_round_star_filled_24px)
    }

    @Test
    fun testUpdateBookmarkState_notBookmarked() {
        whenever(bookmarkDao.findBookmark(bookmark)).thenReturn(false)
        val removedItems = arrayListOf<Int>()

        menuHandler.updateBookmarkState(menuItem, bookmark, 0, removedItems)

        assertTrue(removedItems.contains(0))
        verify(menuItem).setIcon(R.drawable.menu_ic_round_star_border_24px)
    }
}
