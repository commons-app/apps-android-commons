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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.FragmentContributionsBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.mwapi.UserStatsClient
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.nearby.Place
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
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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
        fragment.menuController.setNotificationCount()
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsCaseEmptyList() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsMenuController::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment.menuController, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsCaseNonEmptyList() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsMenuController::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment.menuController, 2)
        verify(notificationCount).setText("2")
    }

    @Test
    @Throws(Exception::class)
    fun testInitNotificationViewsAboveBadgeCap() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            ContributionsMenuController::class.java.getDeclaredMethod(
                "initNotificationViews",
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(fragment.menuController, 100)
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
        fragment.campaigns.showCampaigns(Campaign("", "", "2000-01-01", "2000-01-02", ""))
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

    @Test
    fun testRetryUploadOfUploadedFileOnlyQueuesItAgain() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.contributionsPresenter = contributionsPresenter
        val contribution =
            Contribution(
                media = Media(),
                state = Contribution.STATE_METADATA_PENDING,
                errorInfo = "internal_api_error_DBQueryError",
                uploadedFilename = "Example.jpg",
            )

        fragment.retryUpload(contribution)

        // The duplicate check would find the file on Commons and delete the upload
        Assert.assertEquals(1, contribution.retries)
        Assert.assertEquals(Contribution.STATE_QUEUED, contribution.state)
        verify(contributionsPresenter).saveContribution(contribution)
        verify(contributionsPresenter, never()).checkDuplicateImageAndRestartContribution(contribution)
    }

    /** Pending badges keep their space when the count returns to zero. */
    @Test
    fun testMenuPendingBadgeVisibility() {
        val badge = TextView(context)
        fragment.pendingUploadsCountTextView = badge

        fragment.menuController.updatePendingIcon(2)
        Assert.assertEquals(View.VISIBLE, badge.visibility)
        Assert.assertEquals("2", badge.text.toString())

        fragment.menuController.updatePendingIcon(0)
        Assert.assertEquals(View.INVISIBLE, badge.visibility)
    }

    /** Error badges disappear when all failed uploads are cleared. */
    @Test
    fun testMenuErrorBadgeVisibility() {
        val badge = TextView(context)
        fragment.uploadsErrorTextView = badge

        fragment.menuController.updateErrorIcon(1)
        Assert.assertEquals(View.VISIBLE, badge.visibility)
        Assert.assertEquals("1", badge.text.toString())

        fragment.menuController.updateErrorIcon(0)
        Assert.assertEquals(View.GONE, badge.visibility)
    }

    /** Fetched campaigns are shown on the contributions screen. */
    @Test
    fun testCampaignControllerDisplaysCampaign() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.binding = FragmentContributionsBinding.bind(view)
        val campaign = Campaign("Campaign", "Description", "2000-01-01", "2000-01-02", "")
        campaignView.visibility = View.GONE

        fragment.campaigns.showCampaigns(campaign)

        Assert.assertEquals(View.VISIBLE, campaignView.visibility)
        Assert.assertSame(campaign, Whitebox.getInternalState<Campaign>(campaignView, "campaign"))
    }

    /** Profile screens and null results must not display a campaign. */
    @Test
    fun testCampaignControllerSkipsProfileAndNullResults() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.binding = FragmentContributionsBinding.bind(view)
        campaignView.visibility = View.GONE
        fragment.isUserProfile = true

        fragment.campaigns.showCampaigns(Campaign("", "", "2000-01-01", "2000-01-02", ""))
        Assert.assertEquals(View.GONE, campaignView.visibility)

        fragment.isUserProfile = false
        fragment.campaigns.showCampaigns(null)
        Assert.assertEquals(View.GONE, campaignView.visibility)
    }

    /** Nearby cards are hidden if all returned places have images or no longer exist. */
    @Test
    fun testNearbyControllerHidesUnsuitablePlaces() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.binding = FragmentContributionsBinding.bind(view)
        val controller = ContributionsNearbyController(fragment)
        val places = mock(MapController::class.java).NearbyPlacesInfo()
        places.placeList =
            listOf(
                Place().apply {
                    pic = "Example.jpg"
                    exists = true
                },
                Place().apply {
                    pic = ""
                    exists = false
                },
            )
        nearbyNotificationCardView.visibility = View.VISIBLE

        controller.updateNearbyNotification(places)
        Assert.assertEquals(View.GONE, nearbyNotificationCardView.visibility)

        nearbyNotificationCardView.visibility = View.VISIBLE
        controller.updateNearbyNotification(null)
        Assert.assertEquals(View.GONE, nearbyNotificationCardView.visibility)
    }

    /** Child removals use the contributions parent's manager, even for detached children. */
    @Test
    fun testRendererRemovesFromHostChildManager() {
        val host = mock(ContributionsFragment::class.java)
        val manager = mock(FragmentManager::class.java)
        val transaction = mock(FragmentTransaction::class.java)
        val child = mock(Fragment::class.java)
        `when`(host.childFragmentManager).thenReturn(manager)
        `when`(manager.beginTransaction()).thenReturn(transaction)
        `when`(transaction.remove(child)).thenReturn(transaction)

        ContributionsFragmentRenderer(host).removeFragment(child)

        verify(transaction).remove(child)
        verify(transaction).commit()
        verify(manager).executePendingTransactions()
        verifyNoInteractions(child)
    }

    /** Back presses remain available to the activity while the list is displayed. */
    @Test
    fun testNavigatorDoesNotConsumeBackOnList() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.mediaDetailPagerFragment = null
        Assert.assertFalse(fragment.backButtonClicked())
    }

    /** New list fragments are inserted through the contributions parent's child manager. */
    @Test
    fun testRendererAddsToHostChildManager() {
        val host = mock(ContributionsFragment::class.java)
        val manager = mock(FragmentManager::class.java)
        val transaction = mock(FragmentTransaction::class.java)
        val child = mock(Fragment::class.java)
        `when`(host.childFragmentManager).thenReturn(manager)
        `when`(manager.beginTransaction()).thenReturn(transaction)

        ContributionsFragmentRenderer(host).showFragment(child, "list", null)

        verify(transaction).replace(R.id.root_frame, child, "list")
        verify(transaction).addToBackStack("list")
        verify(transaction).commit()
        verify(manager).executePendingTransactions()
    }

    /** Slight location changes log exceptions but continue to propagate fatal errors. */
    @Test
    fun testNearbyControllerPreservesRefreshFailureHandling() {
        val locationManager = mock(LocationServiceManager::class.java)
        fragment.locationManager = locationManager
        val controller = ContributionsNearbyController(fragment)
        val location = LatLng(0.0, 0.0, 0.0f)
        `when`(locationManager.getLastLocation()).thenThrow(IllegalStateException("No location"))

        controller.onLocationChangedSlightly(location)

        doThrow(AssertionError("Fatal error")).`when`(locationManager).getLastLocation()
        Assert.assertThrows(AssertionError::class.java) {
            controller.onLocationChangedSlightly(location)
        }
    }

    /** The nearby controller unregisters the same listener it registered on resume. */
    @Test
    fun testNearbyControllerListenerLifetime() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val locationManager = mock(LocationServiceManager::class.java)
        fragment.locationManager = locationManager
        val controller = ContributionsNearbyController(fragment)
        controller.create()

        controller.resumeLocation()
        controller.resumeSensor()
        controller.pause()

        verify(locationManager).addLocationListener(controller)
        verify(locationManager).removeLocationListener(controller)
        verify(locationManager).unregisterLocationManager()
    }
}
