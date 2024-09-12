package fr.free.nrw.commons.contributions

import android.content.res.Configuration
import android.os.Looper
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.upload.WikidataPlace
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionsListFragmentUnitTests {

    private lateinit var scenario: FragmentScenario<ContributionsListFragment>
    private lateinit var fragment: ContributionsListFragment

    private val adapter: ContributionsListAdapter = mock()
    private val contribution: Contribution = mock()
    private val media: Media = mock()
    private val wikidataPlace: WikidataPlace = mock()

    @Before
    fun setUp() {
        OkHttpConnectionFactory.CLIENT = createTestClient()

        scenario = launchFragmentInContainer(
            initialState = Lifecycle.State.RESUMED,
            themeResId = R.style.LightAppTheme
        ) {
            ContributionsListFragment().apply {
                contributionsListPresenter = mock()
                callback = mock()
            }.also {
                fragment = it
            }
        }

        scenario.onFragment {
            it.adapter = adapter
        }
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDetach() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onDetach()
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(adapter.getContributionForPosition(anyInt())).thenReturn(contribution)
        fragment.getContributionStateAt(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnScrollToTop() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.rvContributionsList = mock()
        fragment.scrollToTop()
        verify(fragment.rvContributionsList).smoothScrollToPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnConfirmClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.media).thenReturn(media)
        `when`(media.wikiCode).thenReturn("")
        `when`(contribution.wikidataPlace).thenReturn(wikidataPlace)
        fragment.onConfirmClicked(contribution, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.totalMediaCount
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPositionCaseNonNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(adapter.getContributionForPosition(anyInt())).thenReturn(contribution)
        `when`(contribution.media).thenReturn(media)
        fragment.getMediaAtPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPositionCaseNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(adapter.getContributionForPosition(anyInt())).thenReturn(null)
        fragment.getMediaAtPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testShowAddImageToWikipediaInstructions() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "showAddImageToWikipediaInstructions",
            Contribution::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testAddImageToWikipedia() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.addImageToWikipedia(contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testOpenMediaDetail() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.openMediaDetail(0, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewStateRestored() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewStateRestored(mock())
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onSaveInstanceState(mock())
    }

    @Test
    @Throws(Exception::class)
    fun testShowNoContributionsUI() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showNoContributionsUI(true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgress() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showProgress(true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowWelcomeTip() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showWelcomeTip(true)
    }

    @Test
    @Throws(Exception::class)
    fun testAnimateFAB() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment {
            it.requireView().findViewById<FloatingActionButton>(R.id.fab_plus).hide()
        }
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "animateFAB",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test
    @Throws(Exception::class)
    fun testAnimateFABCaseShownAndOpen() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment {
            it.requireView().findViewById<FloatingActionButton>(R.id.fab_plus).show()
        }
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "animateFAB",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test
    @Throws(Exception::class)
    fun testAnimateFABCaseShownAndClose() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        scenario.onFragment {
            it.requireView().findViewById<FloatingActionButton>(R.id.fab_plus).show()
        }
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "animateFAB",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testSetListeners() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "setListeners"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testInitializeAnimations() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ContributionsListFragment::class.java.getDeclaredMethod(
            "initializeAnimations"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnConfigurationChanged() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val newConfig: Configuration = mock()
        newConfig.orientation = Configuration.ORIENTATION_LANDSCAPE
        fragment.onConfigurationChanged(newConfig)
    }
}