package fr.free.nrw.commons.contributions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.campaigns.CampaignsPresenter
import fr.free.nrw.commons.databinding.FragmentContributionsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.mwapi.UserStatsClient
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.notification.NotificationController
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.upload.worker.isPermanentUploadError
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

/** Coordinates the lifecycle of the contributions screen and its focused controllers. */
class ContributionsFragment(
    private val navigator: ContributionsNavigator = ContributionsNavigator(),
) : CommonsDaggerSupportFragment(),
    MediaDetailProvider by navigator,
    ContributionsContract.View,
    ContributionsListFragment.Callback by navigator,
    ContributionsNavigationActions by navigator {
    init {
        navigator.fragment = this
    }

    internal val renderer = ContributionsFragmentRenderer(this)
    internal val campaigns = ContributionsCampaignController(this)
    internal val menuController = ContributionsMenuController(this)
    private val nearbyPermission = ContributionsNearbyPermission(this)
    private val nearby = ContributionsNearbyController(this)

    /** Binds contributions badge controls to the options menu. */
    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        menuController.onCreateOptionsMenu(menu, inflater)
    }

    @JvmField
    @Inject
    @Named("default_preferences")
    var store: JsonKvStore? = null

    @JvmField
    @Inject
    var nearbyController: NearbyController? = null

    @JvmField
    @Inject
    var userStatsClient: UserStatsClient? = null

    @JvmField
    @Inject
    var presenter: CampaignsPresenter? = null

    @JvmField
    @Inject
    var locationManager: LocationServiceManager? = null

    @JvmField
    @Inject
    var notificationController: NotificationController? = null

    @JvmField
    @Inject
    var contributionController: ContributionController? = null

    override var compositeDisposable: CompositeDisposable = CompositeDisposable()

    /** Shares this fragment's subscription lifetime with its screen controllers. */
    internal val subscriptions: CompositeDisposable
        get() = compositeDisposable

    internal var contributionsListFragment: ContributionsListFragment? = null

    // Getter for mediaDetailPagerFragment
    var mediaDetailPagerFragment: MediaDetailPagerFragment? = null
        internal set
    var binding: FragmentContributionsBinding? = null

    @JvmField
    @Inject
    var contributionsPresenter: ContributionsPresenter? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    var notificationCount: TextView? = null

    var pendingUploadsCountTextView: TextView? = null

    var uploadsErrorTextView: TextView? = null

    var pendingUploadsImageView: ImageView? = null

    internal var userName: String? = null
    internal var isUserProfile = false

    internal var shouldShowMediaDetailsFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null && requireArguments().getString(ProfileActivity.KEY_USERNAME) != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
            isUserProfile = true
        }
        nearby.create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentContributionsBinding.inflate(inflater, container, false)

        campaigns.initWLMCampaign()
        presenter!!.onAttachView(campaigns)
        contributionsPresenter!!.onAttachView(this)
        binding!!.campaignsView.visibility = View.GONE
        nearbyPermission.createPermissionView()

        if (savedInstanceState != null) {
            mediaDetailPagerFragment =
                childFragmentManager
                    .findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG) as MediaDetailPagerFragment?
            contributionsListFragment =
                childFragmentManager
                    .findFragmentByTag(
                        ContributionsNavigator.CONTRIBUTION_LIST_FRAGMENT_TAG,
                    ) as ContributionsListFragment?
            shouldShowMediaDetailsFragment = savedInstanceState.getBoolean("mediaDetailsVisible")
        }

        navigator.initFragments()
        if (!isUserProfile) {
            menuController.upDateUploadCount()
        }
        if (shouldShowMediaDetailsFragment) {
            renderer.showMediaDetailPagerFragment()
        } else {
            if (mediaDetailPagerFragment != null) {
                renderer.removeFragment(mediaDetailPagerFragment!!)
            }
            renderer.showContributionsListFragment()
        }

        if (!isBetaFlavour &&
            sessionManager!!.isUserLoggedIn &&
            sessionManager!!.currentAccount != null &&
            !isUserProfile
        ) {
            menuController.setUploadCount()
        }
        setHasOptionsMenu(true)
        return binding!!.root
    }

    override fun onPause() {
        super.onPause()
        nearby.pause()
    }

    override fun onResume() {
        super.onResume()
        contributionsPresenter!!.onAttachView(this)
        nearby.resumeLocation()

        if (binding == null) {
            return
        }

        binding!!.cardViewNearby.permissionRequestButton.setOnClickListener { v: View? ->
            nearbyPermission.showNearbyCardPermissionRationale()
        }

        // Notification cards should only be seen on contributions list, not in media details
        if (mediaDetailPagerFragment == null && !isUserProfile) {
            if (store!!.getBoolean("displayNearbyCardView", true)) {
                nearbyPermission.checkPermissionsAndShowNearbyCardView()

                // Calling nearby card to keep showing it even when user clicks on it and comes back
                try {
                    nearby.updateClosestNearbyCardViewInfo()
                } catch (e: Exception) {
                    Timber.e(e)
                }
                if (binding!!.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY
                ) {
                    binding!!.cardViewNearby.visibility = View.VISIBLE
                }
            } else {
                // Hide nearby notification card view if related shared preferences is false
                binding!!.cardViewNearby.visibility = View.GONE
            }

            // Notification Count and Campaigns should not be set, if it is used in User Profile
            if (!isUserProfile) {
                menuController.setNotificationCount()
                campaigns.fetchCampaigns()
                // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
                // setUploadIconVisibility();
                menuController.setUploadIconCount()
            }
        }
        nearby.resumeSensor()
    }

    override fun onDestroy() {
        try {
            compositeDisposable.clear()
            childFragmentManager.removeOnBackStackChangedListener(campaigns)
            locationManager!!.unregisterLocationManager()
            locationManager!!.removeLocationListener(nearby)
            super.onDestroy()
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
        } catch (exception: IllegalStateException) {
            Timber.e(exception)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter!!.onDetachView()
    }

    /**
     * Restarts the upload process for a contribution
     *
     * @param contribution
     */
    fun restartUpload(contribution: Contribution) {
        contribution.dateUploadStarted = Calendar.getInstance().time
        if (contribution.state == Contribution.STATE_FAILED) {
            if (contribution.errorInfo == null) {
                contribution.chunkInfo = null
                contribution.transferred = 0
            }
            contributionsPresenter!!.checkDuplicateImageAndRestartContribution(contribution)
        } else {
            contribution.state = Contribution.STATE_QUEUED
            contributionsPresenter!!.saveContribution(contribution)
            Timber.d("Restarting for %s", contribution.toString())
        }
    }

    /**
     * Retry upload when it is failed
     *
     * @param contribution contribution to be retried
     */
    fun retryUpload(contribution: Contribution) {
        if (isInternetConnectionEstablished(context)) {
            if (contribution.state == Contribution.STATE_PAUSED) {
                restartUpload(contribution)
            } else if (contribution.isRetriedAfterFailure() &&
                isPermanentUploadError(contribution.errorInfo)
            ) {
                // Retrying cannot fix this failure, so it is not retried automatically
                Timber.d(
                    "Skipping re-upload of %s, it failed with %s",
                    contribution.media.filename,
                    contribution.errorInfo,
                )
            } else if (contribution.isRetriedAfterFailure()) {
                val retries = contribution.retries
                // TODO: Improve UX. Additional details: https://github.com/commons-app/apps-android-commons/pull/5257#discussion_r1304662562
                // Limit the number of retries for a failed upload
                // to handle cases like invalid filename as such uploads
                // will never be successful
                if (retries < MAX_RETRIES) {
                    contribution.retries = retries + 1
                    Timber.d(
                        "Retried uploading %s %d times",
                        contribution.media.filename,
                        retries + 1,
                    )
                    restartUpload(contribution)
                } else {
                    // TODO: Show the exact reason for failure
                    Toast
                        .makeText(
                            context,
                            R.string.retry_limit_reached,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            } else {
                Timber.d("Skipping re-upload for non-failed %s", contribution.toString())
            }
        } else {
            context?.let { showLongToast(it, R.string.this_function_needs_network_connection) }
        }
    }

    /**
     * True for an upload that failed, or whose captions and depicts failed after the file was
     * published. Both are retried with the same limit.
     */
    private fun Contribution.isRetriedAfterFailure() =
        state == Contribution.STATE_FAILED || state == Contribution.STATE_METADATA_PENDING

    companion object {
        const val MEDIA_DETAIL_PAGER_FRAGMENT_TAG: String = "MediaDetailFragmentTag"
        private const val MAX_RETRIES = 10

        /** Creates the retained contributions screen used by the main activity. */
        @JvmStatic
        fun newInstance(): ContributionsFragment {
            val fragment = ContributionsFragment()
            fragment.retainInstance = true
            return fragment
        }
    }
}
