package fr.free.nrw.commons.leaderboard

import android.accounts.Account
import android.content.Context
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment
import fr.free.nrw.commons.profile.leaderboard.LeaderboardListAdapter
import fr.free.nrw.commons.profile.leaderboard.LeaderboardListViewModel
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LeaderboardFragmentUnitTests {

    private lateinit var fragment: LeaderboardFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var progressBar: ProgressBar

    @Mock
    private lateinit var spinner: Spinner

    @Mock
    private lateinit var viewModel: LeaderboardListViewModel

    @Mock
    private lateinit var recyclerView: RecyclerView

    @Mock
    private lateinit var adapter: LeaderboardListAdapter

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var account: Account

    @Mock
    private lateinit var button: Button

    @Mock
    private lateinit var parentView: ViewGroup

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()

        AppAdapter.set(TestAppAdapter())

        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = LeaderboardFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)
        view = LayoutInflater.from(activity)
            .inflate(R.layout.fragment_leaderboard, null) as View

        Whitebox.setInternalState(fragment, "progressBar", progressBar)
        Whitebox.setInternalState(fragment, "categorySpinner", spinner)
        Whitebox.setInternalState(fragment, "durationSpinner", spinner)
        Whitebox.setInternalState(fragment, "viewModel", viewModel)
        Whitebox.setInternalState(fragment, "scrollButton", button)
        Whitebox.setInternalState(fragment, "leaderboardListRecyclerView", recyclerView)
        Whitebox.setInternalState(fragment, "mView", parentView)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        fragment.onCreateView(layoutInflater, null, null)
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshLeaderboard() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "refreshLeaderboard"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testScrollToUserRank() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "scrollToUserRank"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testScrollToUserRankCaseNonZeroTrue() {
        Whitebox.setInternalState(fragment, "userRank", 1)
        `when`(recyclerView.adapter).thenReturn(adapter)
        `when`(adapter.itemCount).thenReturn(3)
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "scrollToUserRank"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testScrollToUserRankCaseNonZeroFalse() {
        Whitebox.setInternalState(fragment, "userRank", 1)
        `when`(recyclerView.adapter).thenReturn(adapter)
        `when`(adapter.itemCount).thenReturn(1)
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "scrollToUserRank"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }


    @Test
    @Throws(Exception::class)
    fun testSetLeaderboard() {
        Whitebox.setInternalState(fragment, "sessionManager", sessionManager)
        `when`(sessionManager.currentAccount).thenReturn(account)
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "setLeaderboard",
            String::class.java,
            String::class.java,
            Int::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "", "", 0, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testHideProgressBar() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "hideProgressBar"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgressBar() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "showProgressBar"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnError() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "onError"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testMenuVisibilityOverrideNotVisible() {
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "setMenuVisibility",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, false)
        Assert.assertNull(ShadowToast.getLatestToast())
    }

    @Test
    @Throws(Exception::class)
    fun testMenuVisibilityOverrideVisibleWithContext() {
        shadowOf(getMainLooper()).idle()
        `when`(parentView.context).thenReturn(context)
        val method: Method = LeaderboardFragment::class.java.getDeclaredMethod(
            "setMenuVisibility",
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
        if(isBetaFlavour) {
            Assert.assertEquals(
                ShadowToast.getTextOfLatestToast().toString(),
                context.getString(R.string.leaderboard_unavailable_beta)
            )
        } else {
            Assert.assertNull(
                ShadowToast.getTextOfLatestToast()
            )
        }

    }

}