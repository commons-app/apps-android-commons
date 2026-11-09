package fr.free.nrw.commons.contributions

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.mwapi.UserStatsClient
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.notification.NotificationController
import fr.free.nrw.commons.upload.UploadProgressActivity
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionsFragmentUnitTests {
    @Mock
    private lateinit var mediaDetailPagerFragment: MediaDetailPagerFragment

    @Mock
    private lateinit var contributionsListFragment: ContributionsListFragment

    @Mock
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var menuInflater: MenuInflater

    @Mock
    private lateinit var menu: Menu

    @Mock
    private lateinit var menuItem: MenuItem

    @Mock
    private lateinit var notification: View

    @Mock
    private lateinit var store: JsonKvStore

    @Mock
    private lateinit var notificationController: NotificationController

    @Mock
    private lateinit var notificationCount: TextView

    @Mock
    private lateinit var singleUnreadCount: Single<Int>

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var userStatsClient: UserStatsClient

    @Mock
    private lateinit var contributionsPresenter: ContributionsPresenter

    private lateinit var fragment: ContributionsFragment
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var activity: MainActivity
    private lateinit var nearbyNotificationCardView: NearbyNotificationCardView
    private lateinit var campaignView: CampaignView

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        OkHttpConnectionFactory.CLIENT = createTestClient()

        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        fragment = ContributionsFragment.newInstance()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        layoutInflater = LayoutInflater.from(activity)
        view =
            LayoutInflater
                .from(activity)
                .inflate(R.layout.fragment_contributions, null) as View

        nearbyNotificationCardView = view.findViewById(R.id.card_view_nearby)
        campaignView = view.findViewById(R.id.campaigns_view)

        Whitebox.setInternalState(fragment, "contributionsListFragment", contributionsListFragment)
        Whitebox.setInternalState(fragment, "store", store)
        Whitebox.setInternalState(fragment, "notificationCount", notificationCount)
        Whitebox.setInternalState(fragment, "notificationController", notificationController)
        Whitebox.setInternalState(fragment, "compositeDisposable", compositeDisposable)
        Whitebox.setInternalState(fragment, "userStatsClient", userStatsClient)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaDetailPagerFragment() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        Assert.assertEquals(fragment.mediaDetailPagerFragment, mediaDetailPagerFragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenuUploadBadgeOpensUploadProgress() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val uploadBadge = TextView(context).apply { id = R.id.pending_uploads_count_badge }
        val uploadMenuItem = mock(MenuItem::class.java)
        `when`(menu.findItem(R.id.notifications)).thenReturn(menuItem)
        `when`(menuItem.actionView).thenReturn(notification)
        `when`(menu.findItem(R.id.upload_tab)).thenReturn(uploadMenuItem)
        `when`(uploadMenuItem.actionView).thenReturn(uploadBadge)
        fragment.onCreateOptionsMenu(menu, menuInflater)
        uploadBadge.performClick()
        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        Assert.assertEquals(
            UploadProgressActivity::class.java.name,
            startedIntent.component?.className,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetNotificationCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(notificationController.getUnreadCount()).thenReturn(singleUnreadCount)
        `when`(notificationController.getUnreadCount().subscribeOn(any())).thenReturn(
            singleUnreadCount,
        )
        `when`(
            notificationController.getUnreadCount().subscribeOn(any()).observeOn(
                any(),
            ),
        ).thenReturn(singleUnreadCount)
        `when`(
            notificationController
                .getUnreadCount()
                .subscribeOn(any())
                .observeOn(
                    any(),
                ).subscribe(),
        ).thenReturn(compositeDisposable)
        fragment.setNotificationCount()
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsCaseEmptyList() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsFragment::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsCaseNonEmptyList() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsFragment::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment, 2)
        verify(notificationCount).setText("2")
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsAboveBadgeCap() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsFragment::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment, 100)
        verify(notificationCount).setText("99+")
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateLimitedConnectionToggleCaseIsEnabled() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(menu.findItem(anyInt())).thenReturn(menuItem)
        `when`(menuItem.actionView).thenReturn(notification)
        `when`(store.getBoolean(anyString(), anyBoolean())).thenReturn(true)
    }

    @Test
    @Throws(Exception::class)
    fun testScrollToTop() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.scrollToTop()
        verify(contributionsListFragment).scrollToTop()
    }

    @Test
    @Throws(Exception::class)
    fun testOnAttach() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onAttach(context)
    }

    @Test
    @Throws(Exception::class)
    fun testNotifyDataSetChanged() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        fragment.notifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroyView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onDestroyView()
    }

    @Test
    @Throws(Exception::class)
    fun testShowCampaigns() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showCampaigns(Campaign("", "", "2000-01-01", "2000-01-02", ""))
    }

    @Test
    @Throws(Exception::class)
    fun testOnPause() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onPause()
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onSaveInstanceState(Bundle())
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewCreated() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onViewCreated(campaignView, Bundle())
    }

    @Test
    @Throws(Exception::class)
    fun testOnResumeCaseNonNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testOnResumeCaseNullReady() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testOnResumeCaseNullCaseIf() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        nearbyNotificationCardView.cardViewVisibilityState =
            NearbyNotificationCardView.CardViewVisibilityState.READY
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testOnResumeCaseNullCaseElse() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(store.getBoolean("displayNearbyCardView", true)).thenReturn(false)
        fragment.onResume()
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.getContributionStateAt(0)
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        fragment.viewPagerNotifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPosition() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.getMediaAtPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.getTotalMediaCount()
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshNominatedMedia() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.refreshNominatedMedia(0)
    }

    @Test
    @Throws(Exception::class)
    fun testShowDetail() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        `when`(mediaDetailPagerFragment.isVisible).thenReturn(false)
        fragment.showDetail(0, false)
    }

    @Test
    fun testRetryUploadSkipsPermanentFailure() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.contributionsPresenter = contributionsPresenter
        val contribution =
            Contribution(
                media = Media(),
                state = Contribution.STATE_FAILED,
                errorInfo = "filetype-banned",
            )

        fragment.retryUpload(contribution)

        Assert.assertEquals(0, contribution.retries)
        verifyNoInteractions(contributionsPresenter)
    }

    @Test
    fun testRetryUploadRetriesTemporaryFailure() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.contributionsPresenter = contributionsPresenter
        val contribution =
            Contribution(
                media = Media(),
                state = Contribution.STATE_FAILED,
                errorInfo = "stashfailed",
            )

        fragment.retryUpload(contribution)

        Assert.assertEquals(1, contribution.retries)
        verify(contributionsPresenter).checkDuplicateImageAndRestartContribution(contribution)
    }
}
