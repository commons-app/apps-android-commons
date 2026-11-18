package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import fr.free.nrw.commons.nearby.WikidataFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyBottomSheetHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyBottomSheetHelperTest {
    private lateinit var helper: NearbyBottomSheetHelper
    private lateinit var activity: AppCompatActivity
    private lateinit var binding: FragmentNearbyParentBinding
    private lateinit var bookmarkLocationsDao: BookmarkLocationsDao
    private lateinit var actions: NearbyBottomSheetHelper.NearbyBottomSheetActions
    private lateinit var mockPlaceAdapter: PlaceAdapter

    private var depictsMode = false
    private var toggledPlace: Place? = null
    private var clickedMediaImageName: String? = null
    private var lastReportedSheetState: Int? = null
    private var uploadInitiatedPlace: Place? = null
    private var uploadInitiatedType: NearbyBottomSheetHelper.UploadType? = null

    /**
     * Initializes test fixtures, inflated binding, and mock callbacks.
     */
    @Before
    fun setUp() {
        activity =
            Robolectric
                .buildActivity(AppCompatActivity::class.java)
                .create()
                .start()
                .resume()
                .visible()
                .get()
        activity.setTheme(R.style.LightAppTheme)
        binding = FragmentNearbyParentBinding.inflate(LayoutInflater.from(activity))
        activity.setContentView(binding.root)
        bookmarkLocationsDao = mock()
        mockPlaceAdapter = mock()

        depictsMode = false
        toggledPlace = null
        clickedMediaImageName = null
        lastReportedSheetState = null
        uploadInitiatedPlace = null
        uploadInitiatedType = null

        actions =
            object : NearbyBottomSheetHelper.NearbyBottomSheetActions {
                override fun onCameraCenterRequested(place: Place) = Unit

                override fun onMediaClicked(imageName: String) {
                    clickedMediaImageName = imageName
                }

                override fun onBookmarkToggled(place: Place) {
                    toggledPlace = place
                }

                override fun onSheetStateChanged(newState: Int) {
                    lastReportedSheetState = newState
                }

                override fun isDepictsPickerMode(): Boolean = depictsMode

                override fun onUploadActionInitiated(
                    place: Place,
                    uploadType: NearbyBottomSheetHelper.UploadType,
                ) {
                    uploadInitiatedPlace = place
                    uploadInitiatedType = uploadType
                }

                override fun getCoroutineScope(): LifecycleCoroutineScope? = null

                override fun getPlaceAdapter(): PlaceAdapter? = mockPlaceAdapter
            }

        helper = NearbyBottomSheetHelper()
        helper.init(
            bindingProvider = { binding },
            contextProvider = { activity },
            activityProvider = { activity },
            bookmarkDaoProvider = { bookmarkLocationsDao },
            actions = actions,
        )
    }

    /**
     * Verifies that init properly configures initial bottom sheet behaviors and states.
     */
    @Test
    fun testInitConfiguresInitialBehaviorsAndStates() {
        assertNotNull(helper.bottomSheetListBehavior)
        assertNotNull(helper.bottomSheetDetailsBehavior)
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, helper.bottomSheetListBehavior?.state)
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
        assertFalse(helper.isListBottomSheetExpanded())
        assertFalse(helper.isDetailsBottomSheetVisible())
    }

    /**
     * Verifies that init in depicts picker mode hides the list bottom sheet.
     */
    @Test
    fun testInitInDepictsPickerMode() {
        depictsMode = true
        val depictsHelper = NearbyBottomSheetHelper()
        depictsHelper.init(
            bindingProvider = { binding },
            contextProvider = { activity },
            activityProvider = { activity },
            bookmarkDaoProvider = { bookmarkLocationsDao },
            actions = actions,
        )
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, depictsHelper.bottomSheetListBehavior?.state)
        assertEquals(View.GONE, binding.bottomSheetNearby.root.visibility)
    }

    /**
     * Verifies hideBottomSheet sets list behavior state to STATE_HIDDEN.
     */
    @Test
    fun testHideBottomSheet() {
        helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        helper.hideBottomSheet()
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetListBehavior?.state)
    }

    /**
     * Verifies hideBottomDetailsSheet sets details behavior state to STATE_HIDDEN.
     */
    @Test
    fun testHideBottomDetailsSheet() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        helper.hideBottomDetailsSheet()
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies setBottomSheetDetailsSmaller collapses an expanded sheet.
     */
    @Test
    fun testSetBottomSheetDetailsSmallerWhenExpanded() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        helper.setBottomSheetDetailsSmaller()
        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies setBottomSheetDetailsSmaller hides a collapsed sheet.
     */
    @Test
    fun testSetBottomSheetDetailsSmallerWhenCollapsed() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        helper.setBottomSheetDetailsSmaller()
        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies listOptionMenuItemClicked expands list sheet when collapsed.
     */
    @Test
    fun testListOptionMenuItemClickedWhenCollapsed() {
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

        helper.listOptionMenuItemClicked()

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetDetailsBehavior?.state)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, helper.bottomSheetListBehavior?.state)
    }

    /**
     * Verifies listOptionMenuItemClicked hides list sheet when expanded.
     */
    @Test
    fun testListOptionMenuItemClickedWhenExpanded() {
        helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_EXPANDED

        helper.listOptionMenuItemClicked()

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetListBehavior?.state)
    }

    /**
     * Verifies updateListFragment updates place items and hides no-results message.
     */
    @Test
    fun testUpdateListFragmentWithItems() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Tower in Paris")
        helper.updateListFragment(listOf(place))

        verify(mockPlaceAdapter).clear()
        verify(mockPlaceAdapter).items = listOf(place)
        assertEquals(View.GONE, binding.bottomSheetNearby.noResultsMessage.visibility)
    }

    /**
     * Verifies updateListFragment shows no-results message when place list is empty.
     */
    @Test
    fun testUpdateListFragmentWithEmptyList() {
        helper.updateListFragment(emptyList())

        verify(mockPlaceAdapter).clear()
        assertEquals(View.VISIBLE, binding.bottomSheetNearby.noResultsMessage.visibility)
    }

    /**
     * Verifies passInfoToSheet binds place details and action buttons.
     */
    @Test
    fun testPassInfoToSheetBindsTextsAndActions() {
        val place = createTestPlace("Q123", "Brandenburg Gate", "Brandenburg Gate (Monument in Berlin)")
        helper.passInfoToSheet(place)

        assertEquals(place, helper.selectedPlace)
        val titleText = binding.bottomSheetDetails.title.text
        val categoryText = binding.bottomSheetDetails.category.text
        val descriptionText = binding.bottomSheetDetails.description.text
        assertEquals("Brandenburg Gate", titleText.toString())
        assertEquals("100m", categoryText.toString())
        assertEquals("Monument in Berlin", descriptionText.toString())
        val dataList: List<*>? = Whitebox.getInternalState(helper, "dataList")
        assertNotNull(dataList)
        assertTrue(dataList!!.isNotEmpty())
        assertNotNull(helper.bottomSheetAdapter)
    }

    /**
     * Verifies passInfoToSheet in depicts picker mode shows confirm button and clicking it finishes with result.
     */
    @Test
    fun testPassInfoToSheetInDepictsPickerMode() {
        depictsMode = true
        val place = createTestPlace("Q456", "Louvre Museum", "Famous art museum in Paris")
        helper.passInfoToSheet(place)

        assertEquals(View.GONE, binding.bottomSheetDetails.bottomSheetRecyclerView.visibility)
        assertEquals(View.VISIBLE, binding.bottomSheetDetails.depictionsMapConfirm.visibility)

        binding.bottomSheetDetails.depictionsMapConfirm.performClick()

        val shadowActivity = Shadows.shadowOf(activity)
        assertEquals(Activity.RESULT_OK, shadowActivity.resultCode)
        val resultPlace =
            shadowActivity.resultIntent.getParcelableExtra<Place>(NearbyParentFragment.PICKER_RESULT_PLACE)
        assertEquals(place, resultPlace)
        assertTrue(activity.isFinishing)
    }

    /**
     * Verifies action item click on bookmark toggles bookmark state via actions callback.
     */
    @Test
    fun testOnBottomSheetItemClickBookmark() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Description")
        helper.passInfoToSheet(place)

        // Item 0 is Bookmark
        helper.itemClickListener.onBottomSheetItemClick(null, 0)

        assertEquals(place, toggledPlace)
    }

    /**
     * Verifies long-clicking an action item does not crash.
     */
    @Test
    fun testOnBottomSheetItemLongClick() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Description")
        helper.passInfoToSheet(place)

        // Long clicking Bookmark item
        helper.itemClickListener.onBottomSheetItemLongClick(null, 0)
    }

    /**
     * Verifies clicking bottom sheet details title collapses or expands sheet and hides list.
     */
    @Test
    fun testDetailsTitleClickTogglesSheetState() {
        helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.bottomSheetDetails.title.performClick()

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, helper.bottomSheetListBehavior?.state)
        assertEquals(BottomSheetBehavior.STATE_EXPANDED, helper.bottomSheetDetailsBehavior?.state)

        binding.bottomSheetDetails.title.performClick()

        assertEquals(BottomSheetBehavior.STATE_COLLAPSED, helper.bottomSheetDetailsBehavior?.state)
    }

    /**
     * Verifies clicking camera, gallery, and custom gallery FABs invokes actions callback.
     */
    @Test
    fun testUploadFabsTriggerActions() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Description")
        binding.fabCamera.visibility = View.VISIBLE
        binding.fabGallery.visibility = View.VISIBLE
        binding.fabCustomGallery.visibility = View.VISIBLE

        helper.passInfoToSheet(place)

        binding.fabCamera.performClick()
        assertEquals(place, uploadInitiatedPlace)
        assertEquals(NearbyBottomSheetHelper.UploadType.CAMERA, uploadInitiatedType)

        binding.fabGallery.performClick()
        assertEquals(NearbyBottomSheetHelper.UploadType.GALLERY, uploadInitiatedType)

        binding.fabCustomGallery.performClick()
        assertEquals(NearbyBottomSheetHelper.UploadType.CUSTOM_GALLERY, uploadInitiatedType)
    }

    /**
     * Verifies clicking place thumbnail invokes media click callback with image name.
     */
    @Test
    fun testThumbnailClickTriggersMediaClicked() {
        val place =
            createTestPlace("Q1", "Eiffel Tower", "Description").apply {
                this.pic = "https://commons.wikimedia.org/wiki/Special:FilePath/Eiffel_Tower.jpg"
            }
        helper.passInfoToSheet(place)

        binding.bottomSheetDetails.icon.performClick()
        assertEquals("Eiffel_Tower.jpg", clickedMediaImageName)
    }

    /**
     * Verifies long-clicking title bar copies place name to clipboard.
     */
    @Test
    fun testDetailsTitleLongClickCopiesToClipboard() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Description")
        helper.passInfoToSheet(place)
        binding.bottomSheetDetails.title.performLongClick()

        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        assertEquals("Eiffel Tower", clip?.getItemAt(0)?.text?.toString())
    }

    /**
     * Verifies formatPlaceDescription handles places with null long description without throwing NPE.
     */
    @Test
    fun testFormatPlaceDescriptionWithNullDescription() {
        val place =
            createTestPlace("Q1", "Eiffel Tower", "Description").apply {
                this.longDescription = null
            }
        helper.passInfoToSheet(place)

        val descriptionText =
            binding.bottomSheetDetails.description.text
                ?.toString() ?: ""
        assertEquals("", descriptionText)
    }

    /**
     * Verifies clicking feedback action item launches WikidataFeedback activity intent.
     */
    @Test
    fun testOnBottomSheetItemClickFeedback() {
        val place = createTestPlace("Q1", "Eiffel Tower", "Description")
        helper.passInfoToSheet(place)

        // Item 3 is Feedback / Report Problem
        helper.itemClickListener.onBottomSheetItemClick(null, 3)

        val nextStartedActivity = Shadows.shadowOf(activity).nextStartedActivity
        assertNotNull(nextStartedActivity)
        assertEquals(WikidataFeedback::class.java.name, nextStartedActivity.component?.className)
        assertEquals("Q1", nextStartedActivity.getStringExtra("qid"))
    }

    /**
     * Helper to construct a test Place instance.
     */
    private fun createTestPlace(
        qid: String,
        name: String,
        description: String,
    ): Place =
        Place().apply {
            this.language = "en"
            this.name = name
            this.label = Label.BUILDING
            this.longDescription = description
            this.location = LatLng(48.8584, 2.2945, 0f)
            this.category = "Monument"
            this.siteLinks =
                Sitelinks
                    .Builder()
                    .setWikidataLink("https://www.wikidata.org/wiki/$qid")
                    .setWikipediaLink("https://en.wikipedia.org/wiki/$name")
                    .setCommonsLink("https://commons.wikimedia.org/wiki/$name")
                    .build()
            this.pic = ""
            this.exists = true
            this.entityID = qid
            this.distance = "100m"
        }
}
