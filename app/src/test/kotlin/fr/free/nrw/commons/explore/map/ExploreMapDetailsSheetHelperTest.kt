package fr.free.nrw.commons.explore.map

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.BottomSheetDetailsExploreBinding
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for ExploreMapDetailsSheetHelper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreMapDetailsSheetHelperTest {
    private lateinit var helper: ExploreMapDetailsSheetHelper
    private lateinit var binding: BottomSheetDetailsExploreBinding
    private lateinit var context: Context

    /**
     * Initializes helper and inflated layout binding before each test.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.LightAppTheme)
        val inflater = LayoutInflater.from(context)
        val exploreBinding = FragmentExploreMapBinding.inflate(inflater)
        binding = exploreBinding.bottomSheetDetailsBinding
        helper = ExploreMapDetailsSheetHelper()
        helper.init(binding)
    }

    /**
     * Verifies that init configures initial sheet visibility and hidden state.
     */
    @Test
    fun testInitConfiguresInitialState() {
        assertEquals(View.VISIBLE, binding.root.visibility)
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
        assertTrue(helper.isDetailsBottomSheetVisible())
    }

    /**
     * Verifies that hideBottomDetailsSheet transitions behavior to hidden state.
     */
    @Test
    fun testHideBottomDetailsSheet() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        helper.hideBottomDetailsSheet()
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies backButtonClicked collapses or hides when sheet is not hidden.
     */
    @Test
    fun testBackButtonClickedWhenVisible() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        val handled = helper.backButtonClicked()
        assertTrue(handled)
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies backButtonClicked returns false when sheet is already hidden.
     */
    @Test
    fun testBackButtonClickedWhenHidden() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        val handled = helper.backButtonClicked()
        assertFalse(handled)
    }

    /**
     * Verifies showDetails puts the bottom sheet in collapsed state.
     */
    @Test
    fun testShowDetails() {
        helper.showDetails()
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies passInfoToSheet populates titles, descriptions, and click handlers.
     */
    @Test
    fun testPassInfoToSheet() {
        val siteLinks: Sitelinks = mock()
        whenever(siteLinks.commonsLink).thenReturn(Uri.parse("https://commons.wikimedia.org"))

        val place =
            Place().apply {
                name = "File:SamplePicture.jpg"
                distance = "1.2 km"
                longDescription = "File:SamplePicture.jpg (A nice test photo.)"
                location = LatLng(10.0, 20.0, 0f)
                this.siteLinks = siteLinks
            }

        val media: Media = mock()
        whenever(media.filename).thenReturn("File:SamplePicture.jpg")
        val mediaList = listOf(media)

        var clickedIndex = -1
        helper.passInfoToSheet(
            context = context,
            place = place,
            mediaList = mediaList,
            zoomLevel = 15.0,
            onMediaClicked = { index -> clickedIndex = index },
        )

        assertEquals("SamplePicture", binding.title.text.toString())
        assertEquals("1.2 km", binding.category.text.toString())
        assertEquals("A nice test photo.", binding.description.text.toString())
        assertEquals(View.VISIBLE, binding.commonsButton.visibility)

        binding.mediaDetailsButton.performClick()
        assertEquals(0, clickedIndex)
    }

    /**
     * Verifies handleMapTap hides expanded sheet first.
     */
    @Test
    fun testHandleMapTapWhenExpanded() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        val handled = helper.handleMapTap()
        assertTrue(handled)
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies handleMapTap hides sheet when visible.
     */
    @Test
    fun testHandleMapTapWhenVisible() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        val handled = helper.handleMapTap()
        assertTrue(handled)
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }
}
