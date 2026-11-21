package fr.free.nrw.commons.nearby.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyMediaDetailHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyMediaDetailHelperTest {
    private lateinit var helper: NearbyMediaDetailHelper
    private lateinit var mediaClient: MediaClient
    private lateinit var fragmentManager: FragmentManager
    private lateinit var fragmentTransaction: FragmentTransaction
    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }

        helper = NearbyMediaDetailHelper()
        mediaClient = mock()
        fragmentManager = mock()
        fragmentTransaction = mock()
        compositeDisposable = CompositeDisposable()

        whenever(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction)
        whenever(fragmentTransaction.remove(any<Fragment>())).thenReturn(fragmentTransaction)
        whenever(fragmentTransaction.replace(any<Int>(), any<Fragment>(), any<String>()))
            .thenReturn(fragmentTransaction)
        whenever(fragmentTransaction.addToBackStack(any())).thenReturn(fragmentTransaction)

        helper.init(
            mediaClientProvider = { mediaClient },
            childFragmentManagerProvider = { fragmentManager },
            bindingProvider = { null },
            activityProvider = { null },
            compositeDisposableProvider = { compositeDisposable },
        )
    }

    @After
    fun tearDown() {
        compositeDisposable.dispose()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun testDefaultMediaProperties() {
        assertNull(helper.getMediaAtPosition(0))
        assertEquals(2, helper.getTotalMediaCount())
        assertNull(helper.getContributionStateAt(0))
    }

    @Test
    fun testHandleMediaClickSuccess() {
        val response = Media(pageId = "12345", filename = "Test.jpg")
        whenever(mediaClient.getMedia("File:Test.jpg")).thenReturn(Single.just(response))

        helper.handleMediaClick("Test.jpg")

        val media = helper.getMediaAtPosition(0)
        assertNotNull(media)
        assertEquals("12345", media?.pageId)
        assertEquals("Test.jpg", media?.filename)
    }

    @Test
    fun testHandleMediaClickError() {
        whenever(mediaClient.getMedia("File:Error.jpg"))
            .thenReturn(Single.error(RuntimeException("Network failure")))

        helper.handleMediaClick("Error.jpg")

        assertNull(helper.getMediaAtPosition(0))
    }

    @Test
    fun testRemoveFragment() {
        val fragment = mock<Fragment>()
        helper.removeFragment(fragment)

        verify(fragmentManager).beginTransaction()
        verify(fragmentTransaction).remove(fragment)
        verify(fragmentTransaction).commit()
        verify(fragmentManager).executePendingTransactions()
    }

    @Test
    fun testRefreshNominatedMediaWhenMapClickableDoesNotRemove() {
        helper.refreshNominatedMedia(0)

        verify(fragmentManager, never()).beginTransaction()
    }

    @Test
    fun testHandleBackPressWhenMediaDetailsNullReturnsFalse() {
        helper.mediaDetails = null
        assertFalse(helper.handleBackPress())
    }

    @Test
    fun testHandleBackPressWhenMediaDetailsVisibleDismissesAndReturnsTrue() {
        val mockPagerFragment = mock<MediaDetailPagerFragment>()
        whenever(mockPagerFragment.isVisible).thenReturn(true)
        helper.mediaDetails = mockPagerFragment

        assertTrue(helper.handleBackPress())
        verify(fragmentManager, atLeastOnce()).beginTransaction()
        verify(fragmentTransaction).remove(mockPagerFragment)
    }
}
