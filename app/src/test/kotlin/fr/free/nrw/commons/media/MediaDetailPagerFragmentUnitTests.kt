package fr.free.nrw.commons.media

import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method

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

    @InjectMocks
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Before
    fun setUp() {

        MockitoAnnotations.initMocks(this)

        context = RuntimeEnvironment.application.applicationContext

        AppAdapter.set(TestAppAdapter())

        SoLoader.setInTestMode()

        Fresco.initialize(context)

        val activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()

        fragment = MediaDetailPagerFragment()
        fragment = MediaDetailPagerFragment(false, false)
        fragment = MediaDetailPagerFragment(false, false, 0)
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        val fieldContext: Field =
            SessionManager::class.java.getDeclaredField("context")
        fieldContext.isAccessible = true
        fieldContext.set(sessionManager, context)
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

}