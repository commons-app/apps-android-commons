package fr.free.nrw.commons.contributions

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.upload.WikidataPlace
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionsListFragmentUnitTests {

    private lateinit var activity: MainActivity
    private lateinit var fragment: ContributionsListFragment
    private lateinit var context: Context
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var rvContributionsList: RecyclerView

    @Mock
    private lateinit var adapter: ContributionsListAdapter

    @Mock
    private lateinit var contribution: Contribution

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var wikidataPlace: WikidataPlace

    @Mock
    private lateinit var callback: ContributionsListFragment.Callback

    @Mock
    private lateinit var layoutManager: RecyclerView.LayoutManager

    @Mock
    private lateinit var gridLayoutManager: GridLayoutManager

    @Mock
    private lateinit var noContributionsYet: TextView

    @Mock
    private lateinit var progressBar: ProgressBar

    @Mock
    private lateinit var fabPlus: FloatingActionButton

    @Mock
    private lateinit var fabCamera: FloatingActionButton

    @Mock
    private lateinit var fabGallery: FloatingActionButton

    @Mock
    private lateinit var fabCustomGallery: FloatingActionButton

    @Mock
    private lateinit var newConfig: Configuration

    @Mock
    private lateinit var fabLayout: LinearLayout

    @Mock
    private lateinit var contributionsListPresenter: ContributionsListPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        AppAdapter.set(TestAppAdapter())

        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        layoutInflater = LayoutInflater.from(activity)

        fragment = ContributionsListFragment()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        Whitebox.setInternalState(fragment, "rvContributionsList", rvContributionsList)
        Whitebox.setInternalState(fragment, "adapter", adapter)
        Whitebox.setInternalState(fragment, "callback", callback)
        Whitebox.setInternalState(fragment, "noContributionsYet", noContributionsYet)
        Whitebox.setInternalState(fragment, "progressBar", progressBar)
        Whitebox.setInternalState(fragment, "fabPlus", fabPlus)
        Whitebox.setInternalState(fragment, "fabCamera", fabCamera)
        Whitebox.setInternalState(fragment, "fabGallery", fabGallery)
        Whitebox.setInternalState(fragment, "fabCustomGallery", fabCustomGallery)
        Whitebox.setInternalState(fragment, "fab_layout", fabLayout)
        Whitebox.setInternalState(
            fragment,
            "contributionsListPresenter",
            contributionsListPresenter
        )
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
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
        fragment.scrollToTop()
        verify(rvContributionsList).smoothScrollToPosition(0)
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
    fun testResumeUpload() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.resumeUpload(contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testPauseUpload() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.pauseUpload(contribution)
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
    fun testDeleteUpload() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.deleteUpload(contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testRetryUpload() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.retryUpload(contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewStateRestored() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(rvContributionsList.layoutManager).thenReturn(layoutManager)
        fragment.onViewStateRestored(savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(rvContributionsList.layoutManager).thenReturn(gridLayoutManager)
        fragment.onSaveInstanceState(savedInstanceState)
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
        `when`(fabPlus.isShown).thenReturn(false)
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
        `when`(fabPlus.isShown).thenReturn(true)
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
        `when`(fabPlus.isShown).thenReturn(true)
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
        newConfig.orientation = Configuration.ORIENTATION_LANDSCAPE
        fragment.onConfigurationChanged(newConfig)
    }

}