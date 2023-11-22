package fr.free.nrw.commons.media

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.explore.SearchActivity
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.Callable

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaDetailPagerFragmentUnitTests {
    private lateinit var fragment: MediaDetailPagerFragment
    private lateinit var context: Context
    private lateinit var fragmentManager: FragmentManager

    @Mock
    private lateinit var outState: Bundle

    @Mock
    private lateinit var media: Media

    @Mock
    internal var sessionManager: SessionManager? = null

    @Mock
    private lateinit var menu: Menu

    @Mock
    private lateinit var menuItem: MenuItem

    @Mock
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxJavaPlugins.setNewThreadSchedulerHandler { Schedulers.trampoline() }
        
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()

        AppAdapter.set(TestAppAdapter())

        SoLoader.setInTestMode()

        Fresco.initialize(context)

        val activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()

        fragment = MediaDetailPagerFragment.newInstance(false, true)
        fragment = MediaDetailPagerFragment.newInstance(false, false)
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        val fieldContext: Field =
            SessionManager::class.java.getDeclaredField("context")
        fieldContext.isAccessible = true
        fieldContext.set(sessionManager, context)

        doReturn(menuItem).`when`(menu).findItem(any())
        doReturn(menuItem).`when`(menuItem).isEnabled = any()
        doReturn(menuItem).`when`(menuItem).isVisible = any()
    }
    
    @After
    fun tearDown() {
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testClearRemoved() {
        fragment.clearRemoved()
    }

    @Test
    @Throws(Exception::class)
    fun testGetRemovedItems() {
        fragment.removedItems
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateSaveInstanceNotNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreate(outState)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaDetailProvider() {
        fragment.mediaDetailProvider
    }

    @Test
    @Throws(Exception::class)
    fun testSetWallpaperCaseNull() {
        val method: Method = MediaDetailPagerFragment::class.java.getDeclaredMethod(
            "setWallpaper",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testSetWallpaperCaseNonNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(media.imageUrl).thenReturn("url")
        val method: Method = MediaDetailPagerFragment::class.java.getDeclaredMethod(
            "setWallpaper",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }


    @Test
    @Throws(Exception::class)
    fun testSetAvatarCaseNull() {
        val method: Method = MediaDetailPagerFragment::class.java.getDeclaredMethod(
            "setAvatar",
            Media::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, media)
    }

    @Test
    @Throws(Exception::class)
    fun testShowImage() {
        fragment.showImage(0, false)
    }

    @Test
    @Throws(Exception::class)
    fun testShowImageSingle() {
        fragment.showImage(0)
    }

    @Test
    @Throws(Exception::class)
    fun testNotifyDataSetChangedCaseNull() {
        fragment.notifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnPageScrolled() {
        fragment.onPageScrolled(0, 0.0F, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPageScrolledCaseNull() {
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.remove(fragment)
        fragmentTransaction.commit()
        fragment.onPageScrolled(0, 0.0F, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPageSelected() {
        fragment.onPageSelected(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPageScrollStateChanged() {
        fragment.onPageScrollStateChanged(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDataSetChanged() {
        fragment.onDataSetChanged()
    }

    private fun invokeHandleBackgroundColorMenuItems(getBitmap: Callable<Bitmap>) {
        val method: Method = MediaDetailPagerFragment::class.java.getDeclaredMethod(
            "handleBackgroundColorMenuItems",
            Callable::class.java,
            Menu::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, getBitmap, menu)
    }

    @Test
    fun testShouldDisplayBackgroundColorMenuWithTransparentMedia() {
        doReturn(true).`when`(bitmap).hasAlpha()

        invokeHandleBackgroundColorMenuItems {
            bitmap
        }

        verify(bitmap, times(1)).hasAlpha()
        verify(menu, times(2)).findItem(any())
        verify(menuItem, times(2)).isEnabled = true
        verify(menuItem, times(2)).isVisible = true
    }

    @Test
    fun testShouldNotDisplayBackgroundColorMenuWithOpaqueMedia() {
        doReturn(false).`when`(bitmap).hasAlpha()

        invokeHandleBackgroundColorMenuItems {
            bitmap
        }

        verify(bitmap, times(1)).hasAlpha()
        verify(menu, never()).findItem(any())
        verify(menuItem, never()).isEnabled = true
        verify(menuItem, never()).isVisible = true
    }
}