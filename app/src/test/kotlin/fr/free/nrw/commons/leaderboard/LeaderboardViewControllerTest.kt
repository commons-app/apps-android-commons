package fr.free.nrw.commons.leaderboard

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentLeaderboardBinding
import fr.free.nrw.commons.profile.leaderboard.LeaderboardViewController
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LeaderboardViewControllerTest {
    @Mock
    private lateinit var layoutManager: LinearLayoutManager

    @Mock
    private lateinit var recyclerView: RecyclerView

    private lateinit var binding: FragmentLeaderboardBinding
    private lateinit var viewController: LeaderboardViewController
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        activity.setTheme(R.style.LightAppTheme)

        binding = FragmentLeaderboardBinding.inflate(android.view.LayoutInflater.from(activity))

        val originalRecycler = binding.leaderboardList
        val parent = originalRecycler.parent as android.view.ViewGroup
        val index = parent.indexOfChild(originalRecycler)
        val layoutParams = originalRecycler.layoutParams

        parent.removeView(originalRecycler)
        parent.addView(recyclerView, index, layoutParams)

        `when`(recyclerView.layoutManager).thenReturn(layoutManager)

        viewController = LeaderboardViewController(binding)
    }

    @Test
    fun testHideProgressBar() {
        viewController.hideProgressBar()
        assertEquals(View.GONE, binding.progressBar.visibility)
        assertEquals(View.VISIBLE, binding.categorySpinner.visibility)
        assertEquals(View.VISIBLE, binding.durationSpinner.visibility)
        assertEquals(View.VISIBLE, binding.scroll.visibility)
        assertEquals(View.VISIBLE, binding.leaderboardList.visibility)
    }

    @Test
    fun testShowProgressBar() {
        viewController.showProgressBar()
        assertEquals(View.VISIBLE, binding.progressBar.visibility)
        assertEquals(View.INVISIBLE, binding.scroll.visibility)
    }

    @Test
    fun testHideLayouts() {
        viewController.hideLayouts()
        assertEquals(View.INVISIBLE, binding.categorySpinner.visibility)
        assertEquals(View.INVISIBLE, binding.durationSpinner.visibility)
        assertEquals(View.INVISIBLE, binding.leaderboardList.visibility)
    }

    @Test
    fun testSetSpinners() {
        viewController.setSpinners()
        val categories = activity.resources.getStringArray(R.array.leaderboard_categories)
        val durations = activity.resources.getStringArray(R.array.leaderboard_durations)
        assertEquals(categories.size, binding.categorySpinner.adapter.count)
        assertEquals(durations.size, binding.durationSpinner.adapter.count)
        assertEquals(categories[0], binding.categorySpinner.selectedItem)
        assertEquals(durations[0], binding.durationSpinner.selectedItem)
    }

    @Test
    fun updateScrollButtonState_belowThreshold_showsMyRank() {
        `when`(layoutManager.findFirstVisibleItemPosition()).thenReturn(5)

        viewController.updateScrollButtonState(userRank = 50, threshold = 20)

        assertEquals(activity.getString(R.string.leaderboard_my_rank_button_text), binding.scroll.text)
        assertEquals(View.VISIBLE, binding.scroll.visibility)
    }

    @Test
    fun updateScrollButtonState_aboveThresholdButBelowUserRank_showsMyRank() {
        `when`(layoutManager.findFirstVisibleItemPosition()).thenReturn(30)

        viewController.updateScrollButtonState(userRank = 50, threshold = 20)

        assertEquals(activity.getString(R.string.leaderboard_my_rank_button_text), binding.scroll.text)
        assertEquals(View.VISIBLE, binding.scroll.visibility)
    }

    @Test
    fun updateScrollButtonState_aboveThresholdAndPastUserRank_showsGoToTop() {
        `when`(layoutManager.findFirstVisibleItemPosition()).thenReturn(60)

        viewController.updateScrollButtonState(userRank = 50, threshold = 20)

        assertEquals(activity.getString(R.string.leaderboard_go_to_top_button_text), binding.scroll.text)
        assertEquals(View.VISIBLE, binding.scroll.visibility)
    }

    @Test
    fun updateScrollButtonState_aboveThresholdAndUserRankIsZero_showsGoToTop() {
        `when`(layoutManager.findFirstVisibleItemPosition()).thenReturn(30)

        viewController.updateScrollButtonState(userRank = 0, threshold = 20)

        assertEquals(activity.getString(R.string.leaderboard_go_to_top_button_text), binding.scroll.text)
        assertEquals(View.VISIBLE, binding.scroll.visibility)
    }
}
