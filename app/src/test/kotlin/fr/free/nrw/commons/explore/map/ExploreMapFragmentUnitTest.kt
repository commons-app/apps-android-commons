package fr.free.nrw.commons.explore.map

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.utils.SystemThemeUtils
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.osmdroid.views.overlay.OverlayItem
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field
import java.lang.reflect.Method

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreMapFragmentUnitTest {
    private lateinit var fragment: ExploreMapFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var activity: MainActivity

    @Mock
    private lateinit var liveDataConverter: LiveDataConverter

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var locationManager: LocationServiceManager

    @Mock
    private lateinit var exploreMapController: ExploreMapController

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Mock
    private lateinit var systemThemeUtils: SystemThemeUtils

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var overlayItem: OverlayItem

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        OkHttpConnectionFactory.CLIENT = OkHttpClient()

        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        fragment = ExploreMapFragment()

        Whitebox.setInternalState(fragment, "liveDataConverter", liveDataConverter)
        Whitebox.setInternalState(fragment, "mediaClient", mediaClient)
        Whitebox.setInternalState(fragment, "locationManager", locationManager)
        Whitebox.setInternalState(fragment, "exploreMapController", exploreMapController)
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(fragment, "bookmarkLocationDao", bookmarkLocationDao)
        Whitebox.setInternalState(fragment, "systemThemeUtils", systemThemeUtils)

        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()
    }

    @Test
    fun testFragmentNotNull() {
        assertNotNull(fragment)
    }

    @Test
    fun testNewInstance() {
        val instance = ExploreMapFragment.newInstance()
        assertNotNull(instance)
    }

    @Test
    fun testRecentlyCameFromNearbyMap() {
        fragment.setRecentlyCameFromNearbyMap(true)
        assertEquals(true, fragment.recentlyCameFromNearbyMap())

        fragment.setRecentlyCameFromNearbyMap(false)
        assertEquals(false, fragment.recentlyCameFromNearbyMap())
    }

    @Test
    fun testGetOverlayWithNullItem() {
        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getOverlay", OverlayItem::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, null as OverlayItem?)
        assertNull(result)
    }

    @Test
    fun testGetOverlayWithNullBinding() {
        val field: Field = ExploreMapFragment::class.java.getDeclaredField("binding")
        field.isAccessible = true
        field.set(fragment, null)

        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getOverlay", OverlayItem::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, overlayItem)
        assertNull(result)
    }

    @Test
    fun testGetMediaFromImageURLWithNullUrl() {
        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getMediaFromImageURL", String::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, null as String?)
        assertNull(result)
    }

    @Test
    fun testGetMediaFromImageURLWithNullMediaList() {
        val field: Field = ExploreMapFragment::class.java.getDeclaredField("mediaList")
        field.isAccessible = true
        field.set(fragment, null)

        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getMediaFromImageURL", String::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, "http://example.com/image.jpg")
        assertNull(result)
    }

    @Test
    fun testGetMediaFromImageURLWithMatchingMedia() {
        whenever(media.imageUrl).thenReturn("http://example.com/image.jpg")
        val mediaList = mutableListOf(media)
        Whitebox.setInternalState(fragment, "mediaList", mediaList)

        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getMediaFromImageURL", String::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, "http://example.com/image.jpg")
        assertEquals(media, result)
    }

    @Test
    fun testGetMediaFromImageURLWithNonMatchingMedia() {
        whenever(media.imageUrl).thenReturn("http://example.com/other.jpg")
        val mediaList = mutableListOf(media)
        Whitebox.setInternalState(fragment, "mediaList", mediaList)

        val method: Method =
            ExploreMapFragment::class.java.getDeclaredMethod("getMediaFromImageURL", String::class.java)
        method.isAccessible = true
        val result = method.invoke(fragment, "http://example.com/image.jpg")
        assertNull(result)
    }
}
