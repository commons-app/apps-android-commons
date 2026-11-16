package fr.free.nrw.commons.nearby.fragments

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.PlaceTypeLabelsClient
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.ArrayList

/**
 * Unit tests for [NearbyFilterHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyFilterHelperTest {
    private lateinit var helper: NearbyFilterHelper
    private lateinit var activity: AppCompatActivity
    private lateinit var binding: FragmentNearbyParentBinding
    private lateinit var mockPresenter: NearbyParentFragmentContract.UserActions
    private lateinit var mockPlaceTypeLabelsClient: PlaceTypeLabelsClient
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var actions: NearbyFilterHelper.NearbyFilterActions

    private var bottomSheetHidden = false
    private var hasOptionsMenuState: Boolean? = null
    private var isDarkThemeActive = false

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

        mockPresenter = mock()
        mockPlaceTypeLabelsClient = mock()
        whenever(mockPlaceTypeLabelsClient.getLabels(com.nhaarman.mockitokotlin2.any()))
            .thenReturn(Single.just(emptyMap()))
        compositeDisposable = CompositeDisposable()

        bottomSheetHidden = false
        hasOptionsMenuState = null
        isDarkThemeActive = false

        actions =
            object : NearbyFilterHelper.NearbyFilterActions {
                override fun hideBottomSheet() {
                    bottomSheetHidden = true
                }

                override fun isDarkTheme(): Boolean = isDarkThemeActive

                override fun setHasOptionsMenu(hasMenu: Boolean) {
                    hasOptionsMenuState = hasMenu
                }
            }

        helper = NearbyFilterHelper()
    }

    private fun initHelper() {
        helper.init(
            bindingProvider = { binding },
            contextProvider = { activity },
            activityProvider = { activity },
            dependencies =
                NearbyFilterHelper.NearbyFilterDependencies(
                    placeTypeLabelsClientProvider = { mockPlaceTypeLabelsClient },
                    compositeDisposableProvider = { compositeDisposable },
                    presenterProvider = { mockPresenter },
                ),
            actions = actions,
        )
    }

    @Test
    fun testInitSetsUpViewsAndHidesBottomSheet() {
        initHelper()
        assertTrue(bottomSheetHidden)
        assertEquals(View.GONE, binding.nearbyFilterList.root.visibility)
        assertNotNull(helper.nearbyFilterSearchRecyclerViewAdapter)
        assertNotNull(helper.searchQueryDisposable)
    }

    @Test
    fun testIsNearbyFilterListVisible() {
        initHelper()
        binding.nearbyFilterList.root.visibility = View.VISIBLE
        assertTrue(helper.isNearbyFilterListVisible())

        binding.nearbyFilterList.root.visibility = View.GONE
        assertFalse(helper.isNearbyFilterListVisible())
    }

    @Test
    fun testHideNearbyFilterList() {
        initHelper()
        binding.nearbyFilterList.root.visibility = View.VISIBLE
        helper.hideNearbyFilterList()
        assertEquals(View.GONE, binding.nearbyFilterList.root.visibility)
    }

    @Test
    fun testSetCheckBoxActionAndState() {
        initHelper()
        helper.setCheckBoxAction()
        assertEquals(CheckBoxTriStates.UNKNOWN, binding.nearbyFilterList.checkboxTriStates.state)

        helper.setCheckBoxState(CheckBoxTriStates.CHECKED)
        assertEquals(CheckBoxTriStates.CHECKED, binding.nearbyFilterList.checkboxTriStates.state)
    }

    @Test
    fun testSetFilterStateWhenLocationAvailable() {
        initHelper()
        NearbyController.currentLocation = LatLng(51.5, -0.08, 100f)
        binding.nearbyFilterList.checkboxTriStates.state = CheckBoxTriStates.CHECKED

        helper.setFilterState()
        verify(mockPresenter).filterByMarkerType(
            helper.nearbyFilterSearchRecyclerViewAdapter!!.selectedLabels,
            CheckBoxTriStates.CHECKED,
            true,
            false,
        )
    }

    @Test
    fun testSetRecyclerViewAdapterAllSelectedAndGreyedOut() {
        initHelper()
        NearbyController.currentLocation = LatLng(51.5, -0.08, 100f)

        helper.setRecyclerViewAdapterAllSelected()
        assertEquals(Label.values().size, helper.nearbyFilterSearchRecyclerViewAdapter!!.selectedLabels.size)

        helper.setRecyclerViewAdapterItemsGreyedOut()
        assertEquals(0, helper.nearbyFilterSearchRecyclerViewAdapter!!.selectedLabels.size)
    }

    @Test
    fun testShowHideAdvancedQueryFragment() {
        initHelper()
        helper.showHideAdvancedQueryFragment(true)
        assertTrue(helper.isAdvancedQueryFragmentVisible())
        assertEquals(View.VISIBLE, binding.flContainerNearbyChildren.visibility)
        assertEquals(false, hasOptionsMenuState)

        helper.showHideAdvancedQueryFragment(false)
        assertFalse(helper.isAdvancedQueryFragmentVisible())
        assertEquals(View.GONE, binding.flContainerNearbyChildren.visibility)
        assertEquals(true, hasOptionsMenuState)
    }

    @Test
    fun testResetAdvancedQuery() {
        initHelper()
        helper.resetAdvancedQuery()
        verify(mockPresenter).setAdvancedQuery("")
        verify(mockPresenter).updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    @Test
    fun testUpdateFilterBadgeCount() {
        val adapter =
            NearbyFilterSearchRecyclerViewAdapter(
                activity,
                ArrayList(Label.valuesAsList()),
                binding.nearbyFilterList.searchListView,
            )
        adapter.selectedLabels.clear()
        updateFilterBadge(binding, adapter)
        assertEquals(View.GONE, binding.nearbyFilter.searchViewLayout.filterCount.visibility)

        adapter.selectedLabels.add(Label.values()[0])
        updateFilterBadge(binding, adapter)
        assertEquals(View.VISIBLE, binding.nearbyFilter.searchViewLayout.filterCount.visibility)
        assertEquals(
            "1",
            binding.nearbyFilter.searchViewLayout.filterCount.text
                .toString(),
        )

        Label.valuesAsList().forEach { adapter.selectedLabels.add(it) }
        updateFilterBadge(binding, adapter)
        assertEquals(View.GONE, binding.nearbyFilter.searchViewLayout.filterCount.visibility)
    }

    @Test
    fun testRestoreStoredFilterSelection() {
        val adapter =
            NearbyFilterSearchRecyclerViewAdapter(
                activity,
                ArrayList(Label.valuesAsList()),
                binding.nearbyFilterList.searchListView,
            )
        val testLabels = arrayListOf(Label.BUILDING, Label.PARK)
        NearbyFilterState.setSelectedLabels(testLabels)

        restoreStoredFilterSelection(adapter, binding)
        assertEquals(2, adapter.selectedLabels.size)
        assertTrue(adapter.selectedLabels.contains(Label.BUILDING))
        assertTrue(adapter.selectedLabels.contains(Label.PARK))
    }

    @Test
    fun testApplyThemeDarkAndLight() {
        initHelper()

        helper.applyTheme(true)
        assertEquals(
            activity.resources.getColor(R.color.white),
            binding.nearbyFilterList.checkboxTriStates.currentTextColor,
        )

        helper.applyTheme(false)
        assertEquals(
            activity.resources.getColor(R.color.contributionListDarkBackground),
            binding.nearbyFilterList.checkboxTriStates.currentTextColor,
        )
    }

    @Test
    fun testOnDestroyViewDisposesSearchSubscription() {
        initHelper()
        assertNotNull(helper.searchQueryDisposable)
        helper.onDestroyView()
        assertNull(helper.searchQueryDisposable)
    }
}
