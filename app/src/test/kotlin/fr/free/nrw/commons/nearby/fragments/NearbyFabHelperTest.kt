package fr.free.nrw.commons.nearby.fragments

import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

/**
 * Unit tests for [NearbyFabHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyFabHelperTest {
    private lateinit var helper: NearbyFabHelper
    private lateinit var activity: AppCompatActivity
    private lateinit var binding: FragmentNearbyParentBinding
    private lateinit var mockApplicationKvStore: JsonKvStore
    private lateinit var actions: NearbyFabHelper.NearbyFabActions

    private var depictsMode = false
    private var photoMarkerCheckRequested = false

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

        mockApplicationKvStore = mock()
        depictsMode = false
        photoMarkerCheckRequested = false

        actions =
            object : NearbyFabHelper.NearbyFabActions {
                override fun isDepictsPickerMode(): Boolean = depictsMode

                override fun onPhotoMarkerCheckRequested() {
                    photoMarkerCheckRequested = true
                }
            }

        helper = NearbyFabHelper()
    }

    private fun initHelper() {
        helper.init(
            bindingProvider = { binding },
            contextProvider = { activity },
            activityProvider = { activity },
            applicationKvStoreProvider = { mockApplicationKvStore },
            actions = actions,
        )
    }

    @Test
    fun testInitPreloadsAnimations() {
        initHelper()
        assertNotNull(Whitebox.getInternalState<Any?>(helper, "fabOpen"))
        assertNotNull(Whitebox.getInternalState<Any?>(helper, "fabClose"))
        assertNotNull(Whitebox.getInternalState<Any?>(helper, "rotateForward"))
        assertNotNull(Whitebox.getInternalState<Any?>(helper, "rotateBackward"))
    }

    @Test
    fun testAnimateFABsTogglesExpandedState() {
        initHelper()
        binding.fabPlus.visibility = View.VISIBLE
        Whitebox.setInternalState(helper, "isFABsExpanded", false)

        helper.animateFABs()
        assertTrue(helper.isFABsExpanded)

        helper.animateFABs()
        assertFalse(helper.isFABsExpanded)
    }

    @Test
    fun testPrepareViewsForSheetPositionCollapsedShowsFABsWhenNotDepictsMode() {
        initHelper()
        depictsMode = false
        binding.fabPlus.visibility = View.GONE

        helper.prepareViewsForSheetPosition(BottomSheetBehavior.STATE_COLLAPSED)
        assertEquals(View.VISIBLE, binding.fabPlus.visibility)
    }

    @Test
    fun testPrepareViewsForSheetPositionHiddenHidesFABs() {
        initHelper()
        binding.fabPlus.visibility = View.VISIBLE
        binding.transparentView.alpha = 1.0f

        helper.prepareViewsForSheetPosition(BottomSheetBehavior.STATE_HIDDEN)
        assertEquals(View.GONE, binding.fabPlus.visibility)
        assertEquals(0.0f, binding.transparentView.alpha, 0.001f)
        assertFalse(binding.transparentView.isClickable)
    }

    @Test
    fun testSetFABPlusAndRecenterActions() {
        initHelper()
        var plusClicked = false
        var recenterClicked = false

        helper.setFABPlusAction { plusClicked = true }
        helper.setFABRecenterAction { recenterClicked = true }

        binding.fabPlus.performClick()
        binding.fabRecenter.performClick()

        assertTrue(plusClicked)
        assertTrue(recenterClicked)
    }

    @Test
    fun testDisableAndEnableFABRecenter() {
        initHelper()

        helper.disableFABRecenter()
        assertFalse(binding.fabRecenter.isEnabled)
        assertTrue(photoMarkerCheckRequested)

        helper.enableFABRecenter()
        assertTrue(binding.fabRecenter.isEnabled)
    }

    @Test
    fun testApplyNearbyFabLayoutAdjustsSpacersAndContainer() {
        initHelper()
        helper.applyNearbyFabLayout()

        val res = activity.resources
        val spacer1Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer1_height)
        val spacer2Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer2_height)
        val spacer3Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer3_height)

        assertEquals(spacer1Px, binding.emptyView1.layoutParams.height)
        assertEquals(spacer2Px, binding.emptyView.layoutParams.height)
        assertEquals(spacer3Px, binding.emptyView2.layoutParams.height)

        val layoutParams = binding.fabContainer.layoutParams as RelativeLayout.LayoutParams
        val alignStart = res.getBoolean(R.bool.nearby_fab_container_align_start)
        val marginPx = res.getDimensionPixelSize(R.dimen.nearby_fab_container_margin)
        if (alignStart) {
            assertEquals(marginPx, layoutParams.marginStart)
        } else {
            assertEquals(marginPx, layoutParams.marginEnd)
        }
    }

    @Test
    fun testDisplayLoginSkippedWarningWhenSkippedShowsDialog() {
        initHelper()
        whenever(mockApplicationKvStore.getBoolean("login_skipped", false)).thenReturn(true)

        helper.displayLoginSkippedWarning()
        val latestDialog = ShadowAlertDialog.getLatestDialog() as? AlertDialog
        assertNotNull(latestDialog)
        assertTrue(latestDialog!!.isShowing)
    }

    @Test
    fun testDisplayLoginSkippedWarningWhenNotSkippedDoesNotShowDialog() {
        initHelper()
        whenever(mockApplicationKvStore.getBoolean("login_skipped", false)).thenReturn(false)

        helper.displayLoginSkippedWarning()
        val latestDialog = ShadowAlertDialog.getLatestDialog()
        assertEquals(null, latestDialog)
    }
}
