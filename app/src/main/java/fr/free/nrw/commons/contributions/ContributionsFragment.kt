package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.work.WorkInfo
import androidx.work.WorkManager
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.campaigns.CampaignView
import fr.free.nrw.commons.campaigns.CampaignsPresenter
import fr.free.nrw.commons.campaigns.ICampaignsView
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.databinding.FragmentContributionsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyNotificationCardView
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.notification.NotificationActivity.Companion.startYourself
import fr.free.nrw.commons.notification.NotificationController
import fr.free.nrw.commons.notification.models.Notification
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.UploadProgressActivity
import fr.free.nrw.commons.upload.worker.UploadWorker
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.LengthUtils.computeBearing
import fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.isMonumentsEnabled
import fr.free.nrw.commons.utils.wLMEndDate
import fr.free.nrw.commons.utils.wLMStartDate
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class ContributionsFragment : CommonsDaggerSupportFragment(), FragmentManager.OnBackStackChangedListener,
    LocationUpdateListener, MediaDetailProvider, SensorEventListener, ICampaignsView,
    ContributionsContract.View, ContributionsListFragment.Callback {
    @JvmField
    @Inject
    @Named("default_preferences")
    var store: JsonKvStore? = null

    @JvmField
    @Inject
    var nearbyController: NearbyController? = null

    @JvmField
    @Inject
    var okHttpJsonApiClient: OkHttpJsonApiClient? = null

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

    private var contributionsListFragment: ContributionsListFragment? = null

    // Getter for mediaDetailPagerFragment
    var mediaDetailPagerFragment: MediaDetailPagerFragment? = null
        private set
    var binding: FragmentContributionsBinding? = null

    @JvmField
    @Inject
    var contributionsPresenter: ContributionsPresenter? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    private var currentLatLng: LatLng? = null

    private var isFragmentAttachedBefore = false
    private var checkBoxView: View? = null
    private var checkBox: CheckBox? = null

    var notificationCount: TextView? = null

    var pendingUploadsCountTextView: TextView? = null

    var uploadsErrorTextView: TextView? = null

    var pendingUploadsImageView: ImageView? = null

    private var wlmCampaign: Campaign? = null

    private var userName: String? = null
    private var isUserProfile = false

    private var mSensorManager: SensorManager? = null
    private var mLight: Sensor? = null
    private var direction = 0f
    private val nearbyLocationPermissionLauncher =
        registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            object : ActivityResultCallback<Map<String, Boolean>> {
                override fun onActivityResult(result: Map<String, Boolean>) {
                    var areAllGranted = true
                    for (b in result.values) {
                        areAllGranted = areAllGranted && b
                    }

                    if (areAllGranted) {
                        onLocationPermissionGranted()
                    } else {
                        if (shouldShowRequestPermissionRationale(
                                permission.ACCESS_FINE_LOCATION
                            )
                            && store!!.getBoolean("displayLocationPermissionForCardView", true)
                            && !store!!.getBoolean("doNotAskForLocationPermission", false)
                            && ((activity as MainActivity).activeFragment
                                    == ActiveFragment.CONTRIBUTIONS)
                        ) {
                            binding!!.cardViewNearby.permissionType =
                                NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION
                        } else {
                            displayYouWontSeeNearbyMessage()
                        }
                    }
                }
            })

    private var shouldShowMediaDetailsFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null && requireArguments().getString(ProfileActivity.KEY_USERNAME) != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
            isUserProfile = true
        }
        mSensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLight = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContributionsBinding.inflate(inflater, container, false)

        initWLMCampaign()
        presenter!!.onAttachView(this)
        contributionsPresenter!!.onAttachView(this)
        binding!!.campaignsView.visibility = View.GONE
        checkBoxView = View.inflate(activity, R.layout.nearby_permission_dialog, null)
        checkBox = checkBoxView?.findViewById<View>(R.id.never_ask_again) as CheckBox
        checkBox!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                // Do not ask for permission on activity start again
                store!!.putBoolean("displayLocationPermissionForCardView", false)
            }
        }

        if (savedInstanceState != null) {
            mediaDetailPagerFragment = childFragmentManager
                .findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG) as MediaDetailPagerFragment?
            contributionsListFragment = childFragmentManager
                .findFragmentByTag(CONTRIBUTION_LIST_FRAGMENT_TAG) as ContributionsListFragment?
            shouldShowMediaDetailsFragment = savedInstanceState.getBoolean("mediaDetailsVisible")
        }

        initFragments()
        if (!isUserProfile) {
            upDateUploadCount()
        }
        if (shouldShowMediaDetailsFragment) {
            showMediaDetailPagerFragment()
        } else {
            if (mediaDetailPagerFragment != null) {
                removeFragment(mediaDetailPagerFragment!!)
            }
            showContributionsListFragment()
        }

        if (!isBetaFlavour && sessionManager!!.isUserLoggedIn
            && sessionManager!!.currentAccount != null && !isUserProfile
        ) {
            setUploadCount()
        }
        setHasOptionsMenu(true)
        return binding!!.root
    }

    /**
     * Initialise the campaign object for WML
     */
    private fun initWLMCampaign() {
        wlmCampaign = Campaign(
            getString(R.string.wlm_campaign_title),
            getString(R.string.wlm_campaign_description), wLMStartDate,
            wLMEndDate, NearbyParentFragment.WLM_URL, true
        )
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        // Removing contributions menu items for ProfileActivity

        if (activity is ProfileActivity) {
            return
        }

        inflater.inflate(R.menu.contribution_activity_notification_menu, menu)

        val notificationsMenuItem = menu.findItem(R.id.notifications)
        val notification = notificationsMenuItem.actionView
        notificationCount = notification!!.findViewById(R.id.notification_count_badge)
        val uploadMenuItem = menu.findItem(R.id.upload_tab)
        val uploadMenuItemActionView = uploadMenuItem.actionView
        pendingUploadsCountTextView = uploadMenuItemActionView!!.findViewById(
            R.id.pending_uploads_count_badge
        )
        uploadsErrorTextView = uploadMenuItemActionView.findViewById(
            R.id.uploads_error_count_badge
        )
        pendingUploadsImageView = uploadMenuItemActionView.findViewById(
            R.id.pending_uploads_image_view
        )
        if (pendingUploadsImageView != null) {
            pendingUploadsImageView!!.setOnClickListener { view: View? ->
                startActivity(
                    Intent(
                        context,
                        UploadProgressActivity::class.java
                    )
                )
            }
        }
        if (pendingUploadsCountTextView != null) {
            pendingUploadsCountTextView!!.setOnClickListener { view: View? ->
                startActivity(
                    Intent(
                        context,
                        UploadProgressActivity::class.java
                    )
                )
            }
        }
        if (uploadsErrorTextView != null) {
            uploadsErrorTextView!!.setOnClickListener { view: View? ->
                startActivity(
                    Intent(
                        context,
                        UploadProgressActivity::class.java
                    )
                )
            }
        }
        notification.setOnClickListener { view: View? ->
            context?.let {
                startYourself(
                    it, "unread"
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    fun setNotificationCount() {
        compositeDisposable.add(
            notificationController!!.getNotifications(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { notificationList: List<Notification> ->
                        this.initNotificationViews(
                            notificationList
                        )
                    },
                    { throwable: Throwable? ->
                        Timber.e(
                            throwable,
                            "Error occurred while loading notifications"
                        )
                    })
        )
    }

    /**
     * Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
     * Sets the visibility of the upload icon based on the number of failed and pending
     * contributions.
     */
    //    public void setUploadIconVisibility() {
    //        contributionController.getFailedAndPendingContributions();
    //        contributionController.failedAndPendingContributionList.observe(getViewLifecycleOwner(),
    //            list -> {
    //                updateUploadIcon(list.size());
    //            });
    //    }
    /**
     * Sets the count for the upload icon based on the number of pending and failed contributions.
     */
    fun setUploadIconCount() {
        contributionController!!.pendingContributions
        contributionController!!.pendingContributionList!!.observe(
            viewLifecycleOwner,
            Observer<PagedList<Contribution>> { list: PagedList<Contribution> ->
                updatePendingIcon(list.size)
            })
        contributionController!!.failedContributions
        contributionController!!.failedContributionList!!.observe(
            viewLifecycleOwner,
            Observer<PagedList<Contribution>> { list: PagedList<Contribution> ->
                updateErrorIcon(list.size)
            })
    }

    fun scrollToTop() {
        if (contributionsListFragment != null) {
            contributionsListFragment!!.scrollToTop()
        }
    }

    private fun initNotificationViews(notificationList: List<Notification>) {
        Timber.d("Number of notifications is %d", notificationList.size)
        if (notificationList.isEmpty()) {
            notificationCount!!.visibility = View.GONE
        } else {
            notificationCount!!.visibility = View.VISIBLE
            notificationCount!!.text = notificationList.size.toString()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        /*
        - There are some operations we need auth, so we need to make sure isAuthCookieAcquired.
        - And since we use same retained fragment doesn't want to make all network operations
        all over again on same fragment attached to recreated activity, we do this network
        operations on first time fragment attached to an activity. Then they will be retained
        until fragment life time ends.
         */
        if (!isFragmentAttachedBefore && activity != null) {
            isFragmentAttachedBefore = true
        }
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list. Creates
     * new one if null.
     */
    private fun showContributionsListFragment() {
        // show nearby card view on contributions list is visible
        if (binding!!.cardViewNearby != null && !isUserProfile) {
            if (store!!.getBoolean("displayNearbyCardView", true)) {
                if (binding!!.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY
                ) {
                    binding!!.cardViewNearby.visibility = View.VISIBLE
                }
            } else {
                binding!!.cardViewNearby.visibility = View.GONE
            }
        }
        showFragment(
            contributionsListFragment!!, CONTRIBUTION_LIST_FRAGMENT_TAG,
            mediaDetailPagerFragment
        )
    }

    private fun showMediaDetailPagerFragment() {
        // hide nearby card view on media detail is visible
        setupViewForMediaDetails()
        showFragment(
            mediaDetailPagerFragment!!, MEDIA_DETAIL_PAGER_FRAGMENT_TAG,
            contributionsListFragment
        )
    }

    private fun setupViewForMediaDetails() {
        if (binding != null) {
            binding!!.campaignsView.visibility = View.GONE
        }
    }

    override fun onBackStackChanged() {
        fetchCampaigns()
    }

    private fun initFragments() {
        if (null == contributionsListFragment) {
            contributionsListFragment = ContributionsListFragment()
            val contributionsListBundle = Bundle()
            contributionsListBundle.putString(ProfileActivity.KEY_USERNAME, userName)
            contributionsListFragment!!.arguments = contributionsListBundle
        }

        if (shouldShowMediaDetailsFragment) {
            showMediaDetailPagerFragment()
        } else {
            showContributionsListFragment()
        }

        showFragment(
            contributionsListFragment!!, CONTRIBUTION_LIST_FRAGMENT_TAG,
            mediaDetailPagerFragment
        )
    }

    /**
     * Replaces the root frame layout with the given fragment
     *
     * @param fragment
     * @param tag
     * @param otherFragment
     */
    private fun showFragment(fragment: Fragment, tag: String, otherFragment: Fragment?) {
        val transaction = childFragmentManager.beginTransaction()
        if (fragment.isAdded && otherFragment != null) {
            transaction.hide(otherFragment)
            transaction.show(fragment)
            transaction.addToBackStack(tag)
            transaction.commit()
            childFragmentManager.executePendingTransactions()
        } else if (fragment.isAdded && otherFragment == null) {
            transaction.show(fragment)
            transaction.addToBackStack(tag)
            transaction.commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded && otherFragment != null) {
            transaction.hide(otherFragment)
            transaction.add(R.id.root_frame, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded) {
            transaction.replace(R.id.root_frame, fragment, tag)
            transaction.addToBackStack(tag)
            transaction.commit()
            childFragmentManager.executePendingTransactions()
        }
    }

    fun removeFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        childFragmentManager.executePendingTransactions()
    }

    private fun setUploadCount() {
        okHttpJsonApiClient
            ?.getUploadCount(sessionManager?.currentAccount!!.name)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())?.let {
                compositeDisposable.add(
                it
                    .subscribe(
                        { uploadCount: Int -> this.displayUploadCount(uploadCount) },
                        { t: Throwable? -> Timber.e(t, "Fetching upload count failed") }
                    ))
            }
    }

    private fun displayUploadCount(uploadCount: Int) {
        if (requireActivity().isFinishing
            || resources == null
        ) {
            return
        }

        (activity as MainActivity).setNumOfUploads(uploadCount)
    }

    override fun onPause() {
        super.onPause()
        locationManager!!.removeLocationListener(this)
        locationManager!!.unregisterLocationManager()
        mSensorManager!!.unregisterListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        contributionsPresenter!!.onAttachView(this)
        locationManager!!.addLocationListener(this)

        if (binding == null) {
            return
        }

        binding!!.cardViewNearby.permissionRequestButton.setOnClickListener { v: View? ->
            showNearbyCardPermissionRationale()
        }

        // Notification cards should only be seen on contributions list, not in media details
        if (mediaDetailPagerFragment == null && !isUserProfile) {
            if (store!!.getBoolean("displayNearbyCardView", true)) {
                checkPermissionsAndShowNearbyCardView()

                // Calling nearby card to keep showing it even when user clicks on it and comes back
                try {
                    updateClosestNearbyCardViewInfo()
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
                setNotificationCount()
                fetchCampaigns()
                // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
                // setUploadIconVisibility();
                setUploadIconCount()
            }
        }
        mSensorManager!!.registerListener(this, mLight, SensorManager.SENSOR_DELAY_UI)
    }

    private fun checkPermissionsAndShowNearbyCardView() {
        if (hasPermission(
                requireActivity(),
                arrayOf(permission.ACCESS_FINE_LOCATION)
            )
        ) {
            onLocationPermissionGranted()
        } else if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)
            && store!!.getBoolean("displayLocationPermissionForCardView", true)
            && !store!!.getBoolean("doNotAskForLocationPermission", false)
            && ((activity as MainActivity).activeFragment == ActiveFragment.CONTRIBUTIONS)
        ) {
            binding!!.cardViewNearby.permissionType =
                NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION
            showNearbyCardPermissionRationale()
        }
    }

    private fun requestLocationPermission() {
        nearbyLocationPermissionLauncher.launch(arrayOf(permission.ACCESS_FINE_LOCATION))
    }

    private fun onLocationPermissionGranted() {
        binding!!.cardViewNearby.permissionType =
            NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED
        locationManager!!.registerLocationManager()
    }

    private fun showNearbyCardPermissionRationale() {
        showAlertDialog(
            requireActivity(),
            getString(R.string.nearby_card_permission_title),
            getString(R.string.nearby_card_permission_explanation),
            { this.requestLocationPermission() },
            { this.displayYouWontSeeNearbyMessage() },
            checkBoxView
        )
    }

    private fun displayYouWontSeeNearbyMessage() {
        showLongToast(
            requireActivity(),
            resources.getString(R.string.unable_to_display_nearest_place)
        )
        // Set to true as the user doesn't want the app to ask for location permission anymore
        store!!.putBoolean("doNotAskForLocationPermission", true)
    }


    private fun updateClosestNearbyCardViewInfo() {
        currentLatLng = locationManager!!.getLastLocation()
        compositeDisposable.add(Observable.fromCallable {
            nearbyController?.loadAttractionsFromLocation(
                    currentLatLng, currentLatLng, true,
                    false
                )
        } // thanks to boolean, it will only return closest result
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { nearbyPlacesInfo: NearbyPlacesInfo? ->
                    this.updateNearbyNotification(
                        nearbyPlacesInfo
                    )
                },
                { throwable: Throwable? ->
                    Timber.d(throwable)
                    updateNearbyNotification(null)
                })
        )
    }

    private fun updateNearbyNotification(
        nearbyPlacesInfo: NearbyPlacesInfo?
    ) {
        if (nearbyPlacesInfo?.placeList != null && nearbyPlacesInfo.placeList.size > 0) {
            var closestNearbyPlace: Place? = null
            // Find the first nearby place that has no image and exists
            for (place in nearbyPlacesInfo.placeList) {
                if (place.pic == "" && place.exists) {
                    closestNearbyPlace = place
                    break
                }
            }

            if (closestNearbyPlace == null) {
                binding!!.cardViewNearby.visibility = View.GONE
            } else {
                val distance = formatDistanceBetween(currentLatLng, closestNearbyPlace.location)
                closestNearbyPlace.setDistance(distance)
                direction = computeBearing(currentLatLng!!, closestNearbyPlace.location).toFloat()
                binding!!.cardViewNearby.updateContent(closestNearbyPlace)
            }
        } else {
            // Means that no close nearby place is found
            binding!!.cardViewNearby.visibility = View.GONE
        }

        // Prevent Nearby banner from appearing in Media Details, fixing bug https://github.com/commons-app/apps-android-commons/issues/4731
        if (mediaDetailPagerFragment != null && !contributionsListFragment!!.isVisible) {
            binding!!.cardViewNearby.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        try {
            compositeDisposable.clear()
            childFragmentManager.removeOnBackStackChangedListener(this)
            locationManager!!.unregisterLocationManager()
            locationManager!!.removeLocationListener(this)
            super.onDestroy()
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception)
        } catch (exception: IllegalStateException) {
            Timber.e(exception)
        }
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) {
        // Will be called if location changed more than 1000 meter
        updateClosestNearbyCardViewInfo()
    }

    override fun onLocationChangedSlightly(latLng: LatLng) {
        /* Update closest nearby notification card onLocationChangedSlightly
         */
        try {
            updateClosestNearbyCardViewInfo()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onLocationChangedMedium(latLng: LatLng) {
        // Update closest nearby card view if location changed more than 500 meters
        updateClosestNearbyCardViewInfo()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * As the home screen has limited space, we have choosen to show either campaigns or WLM card.
     * The WLM Card gets the priority over monuments, so if the WLM is going on we show that instead
     * of campaigns on the campaigns card
     */
    private fun fetchCampaigns() {
        if (isMonumentsEnabled) {
            if (binding != null) {
                binding!!.campaignsView.setCampaign(wlmCampaign)
                binding!!.campaignsView.visibility = View.VISIBLE
            }
        } else if (store!!.getBoolean(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE, true)) {
            presenter!!.getCampaigns()
        } else {
            if (binding != null) {
                binding!!.campaignsView.visibility = View.GONE
            }
        }
    }

    override fun showCampaigns(campaign: Campaign?) {
        if (campaign != null && !isUserProfile) {
            if (binding != null) {
                binding!!.campaignsView.setCampaign(campaign)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter!!.onDetachView()
    }

    override fun notifyDataSetChanged() {
        if (mediaDetailPagerFragment != null) {
            mediaDetailPagerFragment!!.notifyDataSetChanged()
        }
    }

    /**
     * Notify the viewpager that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        if (mediaDetailPagerFragment != null) {
            mediaDetailPagerFragment!!.notifyDataSetChanged()
        }
    }

    /**
     * Updates the visibility and text of the pending uploads count TextView based on the given
     * count.
     *
     * @param pendingCount The number of pending uploads.
     */
    fun updatePendingIcon(pendingCount: Int) {
        if (pendingUploadsCountTextView != null) {
            if (pendingCount != 0) {
                pendingUploadsCountTextView!!.visibility = View.VISIBLE
                pendingUploadsCountTextView!!.text = pendingCount.toString()
            } else {
                pendingUploadsCountTextView!!.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Updates the visibility and text of the error uploads TextView based on the given count.
     *
     * @param errorCount The number of error uploads.
     */
    fun updateErrorIcon(errorCount: Int) {
        if (uploadsErrorTextView != null) {
            if (errorCount != 0) {
                uploadsErrorTextView!!.visibility = View.VISIBLE
                uploadsErrorTextView!!.text = errorCount.toString()
            } else {
                uploadsErrorTextView!!.visibility = View.GONE
            }
        }
    }

    // /**
    //  * Temporarily disabled. See issue [#5847](https://github.com/commons-app/apps-android-commons/issues/5847)
    //  * @param count The number of pending uploads.
    //  */
    // public void updateUploadIcon(int count) {
    //    public void updateUploadIcon(int count) {
    //        if (pendingUploadsImageView != null) {
    //            if (count != 0) {
    //                pendingUploadsImageView.setVisibility(View.VISIBLE);
    //            } else {
    //                pendingUploadsImageView.setVisibility(View.GONE);
    //            }
    //        }
    //    }
    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack. Called when user selects
     * a contribution.
     */
    override fun showDetail(position: Int, isWikipediaButtonDisplayed: Boolean) {
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment!!.isVisible) {
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            if (isUserProfile) {
                (activity as ProfileActivity).setScroll(false)
            }
            showMediaDetailPagerFragment()
        }
        mediaDetailPagerFragment!!.showImage(position, isWikipediaButtonDisplayed)
    }

    override fun getMediaAtPosition(i: Int): Media? {
        return contributionsListFragment!!.getMediaAtPosition(i)
    }

    override fun getTotalMediaCount(): Int {
        return contributionsListFragment!!.totalMediaCount
    }

    override fun getContributionStateAt(position: Int): Int {
        return contributionsListFragment!!.getContributionStateAt(position)
    }

    fun backButtonClicked(): Boolean {
        if (mediaDetailPagerFragment != null && mediaDetailPagerFragment!!.isVisible) {
            if (store!!.getBoolean("displayNearbyCardView", true) && !isUserProfile) {
                if (binding!!.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY
                ) {
                    binding!!.cardViewNearby.visibility = View.VISIBLE
                }
            } else {
                binding!!.cardViewNearby.visibility = View.GONE
            }
            removeFragment(mediaDetailPagerFragment!!)
            showFragment(
                contributionsListFragment!!, CONTRIBUTION_LIST_FRAGMENT_TAG,
                mediaDetailPagerFragment
            )
            if (isUserProfile) {
                // Fragment is associated with ProfileActivity
                // Enable ParentViewPager Scroll
                (activity as ProfileActivity).setScroll(true)
            } else {
                fetchCampaigns()
            }
            if (activity is MainActivity) {
                // Fragment is associated with MainActivity
                (activity as BaseActivity).supportActionBar
                    ?.setDisplayHomeAsUpEnabled(false)
                (activity as MainActivity).showTabs()
            }
            return true
        }
        return false
    }


    /**
     * this function updates the number of contributions
     */
    fun upDateUploadCount() {
        context?.let {
            WorkManager.getInstance(it)
                .getWorkInfosForUniqueWorkLiveData(UploadWorker::class.java.simpleName).observe(
                    viewLifecycleOwner
                ) { workInfos: List<WorkInfo?> ->
                    if (workInfos.size > 0) {
                        setUploadCount()
                    }
                }
        }
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
            } else if (contribution.state == Contribution.STATE_FAILED) {
                val retries = contribution.retries
                // TODO: Improve UX. Additional details: https://github.com/commons-app/apps-android-commons/pull/5257#discussion_r1304662562
                /* Limit the number of retries for a failed upload
                   to handle cases like invalid filename as such uploads
                   will never be successful */
                if (retries < MAX_RETRIES) {
                    contribution.retries = retries + 1
                    Timber.d(
                        "Retried uploading %s %d times", contribution.media.filename,
                        retries + 1
                    )
                    restartUpload(contribution)
                } else {
                    // TODO: Show the exact reason for failure
                    Toast.makeText(
                        context,
                        R.string.retry_limit_reached, Toast.LENGTH_SHORT
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
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetailPagerFragment != null && !contributionsListFragment!!.isVisible) {
            removeFragment(mediaDetailPagerFragment!!)
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            mediaDetailPagerFragment?.showImage(index)
            showMediaDetailPagerFragment()
        }
    }

    /**
     * When the device rotates, rotate the Nearby banner's compass arrow in tandem.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val rotateDegree = Math.round(event.values[0]).toFloat()
        binding!!.cardViewNearby.rotateCompass(rotateDegree, direction)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Nothing to do.
    }

    companion object {
        private const val CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag"
        const val MEDIA_DETAIL_PAGER_FRAGMENT_TAG: String = "MediaDetailFragmentTag"
        private const val MAX_RETRIES = 10

        @JvmStatic
        fun newInstance(): ContributionsFragment {
            val fragment = ContributionsFragment()
            fragment.retainInstance = true
            return fragment
        }
    }
}
