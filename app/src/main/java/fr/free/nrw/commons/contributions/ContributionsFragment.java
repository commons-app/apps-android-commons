package fr.free.nrw.commons.contributions;

import static android.content.Context.SENSOR_SERVICE;
import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;
import static fr.free.nrw.commons.contributions.Contribution.STATE_PAUSED;
import static fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.WLM_URL;
import static fr.free.nrw.commons.profile.ProfileActivity.KEY_USERNAME;
import static fr.free.nrw.commons.utils.LengthUtils.computeBearing;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.databinding.FragmentContributionsBinding;
import fr.free.nrw.commons.notification.models.Notification;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.theme.BaseActivity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import androidx.work.WorkManager;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.campaigns.models.Campaign;
import fr.free.nrw.commons.campaigns.CampaignView;
import fr.free.nrw.commons.campaigns.CampaignsPresenter;
import fr.free.nrw.commons.campaigns.ICampaignsView;
import fr.free.nrw.commons.contributions.ContributionsListFragment.Callback;
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyNotificationCardView;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.upload.worker.UploadWorker;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ContributionsFragment
    extends CommonsDaggerSupportFragment
    implements
    OnBackStackChangedListener,
    LocationUpdateListener,
    MediaDetailProvider,
    SensorEventListener,
    ICampaignsView, ContributionsContract.View, Callback {

    @Inject
    @Named("default_preferences")
    JsonKvStore store;
    @Inject
    NearbyController nearbyController;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;
    @Inject
    CampaignsPresenter presenter;
    @Inject
    LocationServiceManager locationManager;
    @Inject
    NotificationController notificationController;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ContributionsListFragment contributionsListFragment;
    private static final String CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag";
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    static final String MEDIA_DETAIL_PAGER_FRAGMENT_TAG = "MediaDetailFragmentTag";
    private static final int MAX_RETRIES = 10;


    public FragmentContributionsBinding binding;

    @Inject ContributionsPresenter contributionsPresenter;

    @Inject
    SessionManager sessionManager;

    private LatLng currentLatLng;

    private boolean isFragmentAttachedBefore = false;
    private View checkBoxView;
    private CheckBox checkBox;

    public TextView notificationCount;

    private Campaign wlmCampaign;

    String userName;
    private boolean isUserProfile;

    private SensorManager mSensorManager;
    private Sensor mLight;
    private float direction;
    private ActivityResultLauncher<String[]> nearbyLocationPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                boolean areAllGranted = true;
                for (final boolean b : result.values()) {
                    areAllGranted = areAllGranted && b;
                }

                if (areAllGranted) {
                    onLocationPermissionGranted();
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        && store.getBoolean("displayLocationPermissionForCardView", true)
                        && !store.getBoolean("doNotAskForLocationPermission", false)
                        && (((MainActivity) getActivity()).activeFragment == ActiveFragment.CONTRIBUTIONS)) {
                        binding.cardViewNearby.permissionType = NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION;
                        showNearbyCardPermissionRationale();
                    } else {
                        displayYouWontSeeNearbyMessage();
                    }
                }
            }
        });

    @NonNull
    public static ContributionsFragment newInstance() {
        ContributionsFragment fragment = new ContributionsFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    private boolean shouldShowMediaDetailsFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().getString(KEY_USERNAME) != null) {
            userName = getArguments().getString(KEY_USERNAME);
            isUserProfile = true;
        }
        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {

        binding = FragmentContributionsBinding.inflate(inflater, container, false);

        initWLMCampaign();
        presenter.onAttachView(this);
        contributionsPresenter.onAttachView(this);
        binding.campaignsView.setVisibility(View.GONE);
        checkBoxView = View.inflate(getActivity(), R.layout.nearby_permission_dialog, null);
        checkBox = (CheckBox) checkBoxView.findViewById(R.id.never_ask_again);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Do not ask for permission on activity start again
                store.putBoolean("displayLocationPermissionForCardView",false);
            }
        });


        if (savedInstanceState != null) {
            mediaDetailPagerFragment = (MediaDetailPagerFragment) getChildFragmentManager()
                .findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
            contributionsListFragment = (ContributionsListFragment) getChildFragmentManager()
                .findFragmentByTag(CONTRIBUTION_LIST_FRAGMENT_TAG);
            shouldShowMediaDetailsFragment = savedInstanceState.getBoolean("mediaDetailsVisible");
        }

        initFragments();
        if(isUserProfile) {
            binding.limitedConnectionEnabledLayout.setVisibility(View.GONE);
        }else {
            upDateUploadCount();
        }
        if (shouldShowMediaDetailsFragment) {
            showMediaDetailPagerFragment();
        } else {
            if (mediaDetailPagerFragment != null) {
                removeFragment(mediaDetailPagerFragment);
            }
            showContributionsListFragment();
        }

        if (!ConfigUtils.isBetaFlavour() && sessionManager.isUserLoggedIn()
            && sessionManager.getCurrentAccount() != null && !isUserProfile) {
            setUploadCount();
        }
        binding.limitedConnectionEnabledLayout.setOnClickListener(toggleDescriptionListener);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    /**
     * Initialise the campaign object for WML
     */
    private void initWLMCampaign() {
        wlmCampaign = new Campaign(getString(R.string.wlm_campaign_title),
            getString(R.string.wlm_campaign_description), Utils.getWLMStartDate().toString(),
            Utils.getWLMEndDate().toString(), WLM_URL, true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
        @NonNull final MenuInflater inflater) {

        // Removing contributions menu items for ProfileActivity
        if (getActivity() instanceof ProfileActivity) {
            return;
        }

        inflater.inflate(R.menu.contribution_activity_notification_menu, menu);

        MenuItem notificationsMenuItem = menu.findItem(R.id.notifications);
        final View notification = notificationsMenuItem.getActionView();
        notificationCount = notification.findViewById(R.id.notification_count_badge);
        notification.setOnClickListener(view -> {
            NotificationActivity.startYourself(getContext(), "unread");
        });
        updateLimitedConnectionToggle(menu);
    }

    @SuppressLint("CheckResult")
    public void setNotificationCount() {
        compositeDisposable.add(notificationController.getNotifications(false)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::initNotificationViews,
                throwable -> Timber.e(throwable, "Error occurred while loading notifications")));
    }

    public void scrollToTop() {
        if (contributionsListFragment != null) {
            contributionsListFragment.scrollToTop();
        }
    }

    private void initNotificationViews(List<Notification> notificationList) {
        Timber.d("Number of notifications is %d", notificationList.size());
        if (notificationList.isEmpty()) {
            notificationCount.setVisibility(View.GONE);
        } else {
            notificationCount.setVisibility(View.VISIBLE);
            notificationCount.setText(String.valueOf(notificationList.size()));
        }
    }

    public void updateLimitedConnectionToggle(Menu menu) {
        MenuItem checkable = menu.findItem(R.id.toggle_limited_connection_mode);
        boolean isEnabled = store
            .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false);

        checkable.setChecked(isEnabled);
        if (binding!=null) {
            binding.limitedConnectionEnabledLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        }

        checkable.setIcon((isEnabled) ? R.drawable.ic_baseline_cloud_off_24:R.drawable.ic_baseline_cloud_queue_24);
        checkable.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ((MainActivity) getActivity()).toggleLimitedConnectionMode();
                boolean isEnabled = store.getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false);
                binding.limitedConnectionEnabledLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
                checkable.setIcon((isEnabled) ? R.drawable.ic_baseline_cloud_off_24:R.drawable.ic_baseline_cloud_queue_24);
                return false;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*
        - There are some operations we need auth, so we need to make sure isAuthCookieAcquired.
        - And since we use same retained fragment doesn't want to make all network operations
        all over again on same fragment attached to recreated activity, we do this network
        operations on first time fragment attached to an activity. Then they will be retained
        until fragment life time ends.
         */
        if (!isFragmentAttachedBefore && getActivity() != null) {
            isFragmentAttachedBefore = true;
        }
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list. Creates
     * new one if null.
     */
    private void showContributionsListFragment() {
        // show nearby card view on contributions list is visible
        if (binding.cardViewNearby != null && !isUserProfile) {
            if (store.getBoolean("displayNearbyCardView", true)) {
                if (binding.cardViewNearby.cardViewVisibilityState
                    == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                    binding.cardViewNearby.setVisibility(View.VISIBLE);
                }
            } else {
                binding.cardViewNearby.setVisibility(View.GONE);
            }
        }
        showFragment(contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG,
            mediaDetailPagerFragment);
    }

    private void showMediaDetailPagerFragment() {
        // hide nearby card view on media detail is visible
        setupViewForMediaDetails();
        showFragment(mediaDetailPagerFragment, MEDIA_DETAIL_PAGER_FRAGMENT_TAG,
            contributionsListFragment);
    }

    private void setupViewForMediaDetails() {
        if (binding!=null) {
            binding.campaignsView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackStackChanged() {
        fetchCampaigns();
    }

    private void initFragments() {
        if (null == contributionsListFragment) {
            contributionsListFragment = new ContributionsListFragment();
            Bundle contributionsListBundle = new Bundle();
            contributionsListBundle.putString(KEY_USERNAME, userName);
            contributionsListFragment.setArguments(contributionsListBundle);
        }

        if (shouldShowMediaDetailsFragment) {
            showMediaDetailPagerFragment();
        } else {
            showContributionsListFragment();
        }

        showFragment(contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG,
            mediaDetailPagerFragment);
    }

    /**
     * Replaces the root frame layout with the given fragment
     *
     * @param fragment
     * @param tag
     * @param otherFragment
     */
    private void showFragment(Fragment fragment, String tag, Fragment otherFragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        if (fragment.isAdded() && otherFragment != null) {
            transaction.hide(otherFragment);
            transaction.show(fragment);
            transaction.addToBackStack(tag);
            transaction.commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (fragment.isAdded() && otherFragment == null) {
            transaction.show(fragment);
            transaction.addToBackStack(tag);
            transaction.commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (!fragment.isAdded() && otherFragment != null) {
            transaction.hide(otherFragment);
            transaction.add(R.id.root_frame, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (!fragment.isAdded()) {
            transaction.replace(R.id.root_frame, fragment, tag);
            transaction.addToBackStack(tag);
            transaction.commit();
            getChildFragmentManager().executePendingTransactions();
        }
    }

    public void removeFragment(Fragment fragment) {
        getChildFragmentManager()
            .beginTransaction()
            .remove(fragment)
            .commit();
        getChildFragmentManager().executePendingTransactions();
    }

    @SuppressWarnings("ConstantConditions")
    private void setUploadCount() {
        compositeDisposable.add(okHttpJsonApiClient
            .getUploadCount(((MainActivity) getActivity()).sessionManager.getCurrentAccount().name)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::displayUploadCount,
                t -> Timber.e(t, "Fetching upload count failed")
            ));
    }

    private void displayUploadCount(Integer uploadCount) {
        if (getActivity().isFinishing()
            || getResources() == null) {
            return;
        }

        ((MainActivity) getActivity()).setNumOfUploads(uploadCount);

    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeLocationListener(this);
        locationManager.unregisterLocationManager();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        contributionsPresenter.onAttachView(this);
        locationManager.addLocationListener(this);

        if (binding==null) {
            return;
        }

        binding.cardViewNearby.permissionRequestButton.setOnClickListener(v -> {
            showNearbyCardPermissionRationale();
        });

        // Notification cards should only be seen on contributions list, not in media details
        if (mediaDetailPagerFragment == null && !isUserProfile) {
            if (store.getBoolean("displayNearbyCardView", true)) {
                checkPermissionsAndShowNearbyCardView();

                // Calling nearby card to keep showing it even when user clicks on it and comes back
                try {
                    updateClosestNearbyCardViewInfo();
                } catch (Exception e) {
                    Timber.e(e);
                }
                if (binding.cardViewNearby.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                    binding.cardViewNearby.setVisibility(View.VISIBLE);
                }

            } else {
                // Hide nearby notification card view if related shared preferences is false
                binding.cardViewNearby.setVisibility(View.GONE);
            }

            // Notification Count and Campaigns should not be set, if it is used in User Profile
            if(!isUserProfile) {
                setNotificationCount();
                fetchCampaigns();
            }
        }
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_UI);
    }

    private void checkPermissionsAndShowNearbyCardView() {
        if (PermissionUtils.hasPermission(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) {
            onLocationPermissionGranted();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            && store.getBoolean("displayLocationPermissionForCardView", true)
            && !store.getBoolean("doNotAskForLocationPermission", false)
            && (((MainActivity) getActivity()).activeFragment == ActiveFragment.CONTRIBUTIONS)) {
            binding.cardViewNearby.permissionType = NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION;
            showNearbyCardPermissionRationale();
        }
    }

    private void requestLocationPermission() {
        nearbyLocationPermissionLauncher.launch(new String[]{permission.ACCESS_FINE_LOCATION});
    }

    private void onLocationPermissionGranted() {
        binding.cardViewNearby.permissionType = NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED;
        locationManager.registerLocationManager();
    }

    private void showNearbyCardPermissionRationale() {
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.nearby_card_permission_title),
            getString(R.string.nearby_card_permission_explanation),
            this::requestLocationPermission,
            this::displayYouWontSeeNearbyMessage,
            checkBoxView,
            false);
    }

    private void displayYouWontSeeNearbyMessage() {
        ViewUtil.showLongToast(getActivity(),
            getResources().getString(R.string.unable_to_display_nearest_place));
        store.putBoolean("doNotAskForLocationPermission", true);
    }


    private void updateClosestNearbyCardViewInfo() {
        currentLatLng = locationManager.getLastLocation();
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(currentLatLng, currentLatLng, true,
                    false)) // thanks to boolean, it will only return closest result
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateNearbyNotification,
                throwable -> {
                    Timber.d(throwable);
                    updateNearbyNotification(null);
                }));
    }

    private void updateNearbyNotification(
        @Nullable NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        if (nearbyPlacesInfo != null && nearbyPlacesInfo.placeList != null
            && nearbyPlacesInfo.placeList.size() > 0) {
            Place closestNearbyPlace = null;
            // Find the first nearby place that has no image and exists
            for (Place place : nearbyPlacesInfo.placeList) {
                if (place.pic.equals("") && place.exists) {
                    closestNearbyPlace = place;
                    break;
                }
            }

            if (closestNearbyPlace == null) {
                binding.cardViewNearby.setVisibility(View.GONE);
            } else {
                String distance = formatDistanceBetween(currentLatLng, closestNearbyPlace.location);
                closestNearbyPlace.setDistance(distance);
                direction = (float) computeBearing(currentLatLng, closestNearbyPlace.location);
                binding.cardViewNearby.updateContent(closestNearbyPlace);
            }
        } else {
            // Means that no close nearby place is found
            binding.cardViewNearby.setVisibility(View.GONE);
        }

        // Prevent Nearby banner from appearing in Media Details, fixing bug https://github.com/commons-app/apps-android-commons/issues/4731
        if (mediaDetailPagerFragment != null && !contributionsListFragment.isVisible()) {
            binding.cardViewNearby.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        try {
            compositeDisposable.clear();
            getChildFragmentManager().removeOnBackStackChangedListener(this);
            locationManager.unregisterLocationManager();
            locationManager.removeLocationListener(this);
            super.onDestroy();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Timber.e(exception);
        }
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        // Will be called if location changed more than 1000 meter
        updateClosestNearbyCardViewInfo();
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        /* Update closest nearby notification card onLocationChangedSlightly
         */
        try {
            updateClosestNearbyCardViewInfo();
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        // Update closest nearby card view if location changed more than 500 meters
        updateClosestNearbyCardViewInfo();
    }

    @Override
    public void onViewCreated(@NonNull View view,
        @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * As the home screen has limited space, we have choosen to show either campaigns or WLM card.
     * The WLM Card gets the priority over monuments, so if the WLM is going on we show that instead
     * of campaigns on the campaigns card
     */
    private void fetchCampaigns() {
        if (Utils.isMonumentsEnabled(new Date())) {
            if (binding!=null) {
                binding.campaignsView.setCampaign(wlmCampaign);
                binding.campaignsView.setVisibility(View.VISIBLE);
            }
        } else if (store.getBoolean(CampaignView.CAMPAIGNS_DEFAULT_PREFERENCE, true)) {
            presenter.getCampaigns();
        } else {
            if (binding!=null) {
                binding.campaignsView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showCampaigns(Campaign campaign) {
        if (campaign != null && !isUserProfile) {
            if (binding!=null) {
                binding.campaignsView.setCampaign(campaign);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    @Override
    public void notifyDataSetChanged() {
        if (mediaDetailPagerFragment != null) {
            mediaDetailPagerFragment.notifyDataSetChanged();
        }
    }

    /**
     * Restarts the upload process for a contribution
     *
     * @param contribution
     */
    public void restartUpload(Contribution contribution) {
        contribution.setState(Contribution.STATE_QUEUED);
        contributionsPresenter.saveContribution(contribution);
        Timber.d("Restarting for %s", contribution.toString());
    }

    /**
     * Retry upload when it is failed
     *
     * @param contribution contribution to be retried
     */
    @Override
    public void retryUpload(Contribution contribution) {
        if (NetworkUtils.isInternetConnectionEstablished(getContext())) {
            if (contribution.getState() == STATE_PAUSED
                || contribution.getState() == Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE) {
                restartUpload(contribution);
            } else if (contribution.getState() == STATE_FAILED) {
                int retries = contribution.getRetries();
                // TODO: Improve UX. Additional details: https://github.com/commons-app/apps-android-commons/pull/5257#discussion_r1304662562
                /* Limit the number of retries for a failed upload
                   to handle cases like invalid filename as such uploads
                   will never be successful */
                if (retries < MAX_RETRIES) {
                    contribution.setRetries(retries + 1);
                    Timber.d("Retried uploading %s %d times", contribution.getMedia().getFilename(),
                        retries + 1);
                    restartUpload(contribution);
                } else {
                    // TODO: Show the exact reason for failure
                    Toast.makeText(getContext(),
                        R.string.retry_limit_reached, Toast.LENGTH_SHORT).show();
                }
            } else {
                Timber.d("Skipping re-upload for non-failed %s", contribution.toString());
            }
        } else {
            ViewUtil.showLongToast(getContext(), R.string.this_function_needs_network_connection);
        }

    }

    /**
     * Pauses the upload
     *
     * @param contribution
     */
    @Override
    public void pauseUpload(Contribution contribution) {
        //Pause the upload in the global singleton
        CommonsApplication.pauseUploads.put(contribution.getPageId(), true);
        //Retain the paused state in DB
        contribution.setState(STATE_PAUSED);
        contributionsPresenter.saveContribution(contribution);
    }

    /**
     * Notify the viewpager that number of items have changed.
     */
    @Override
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetailPagerFragment != null) {
            mediaDetailPagerFragment.notifyDataSetChanged();
        }
    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack. Called when user selects
     * a contribution.
     */
    @Override
    public void showDetail(int position, boolean isWikipediaButtonDisplayed) {
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true);
            if (isUserProfile) {
                ((ProfileActivity) getActivity()).setScroll(false);
            }
            showMediaDetailPagerFragment();
        }
        mediaDetailPagerFragment.showImage(position, isWikipediaButtonDisplayed);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        return contributionsListFragment.getMediaAtPosition(i);
    }

    @Override
    public int getTotalMediaCount() {
        return contributionsListFragment.getTotalMediaCount();
    }

    @Override
    public Integer getContributionStateAt(int position) {
        return contributionsListFragment.getContributionStateAt(position);
    }

    public boolean backButtonClicked() {
        if (mediaDetailPagerFragment != null && mediaDetailPagerFragment.isVisible()) {
            if (store.getBoolean("displayNearbyCardView", true) && !isUserProfile) {
                if (binding.cardViewNearby.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                    binding.cardViewNearby.setVisibility(View.VISIBLE);
                }
            } else {
                binding.cardViewNearby.setVisibility(View.GONE);
            }
            removeFragment(mediaDetailPagerFragment);
            showFragment(contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG,
                mediaDetailPagerFragment);
            if (isUserProfile) {
                // Fragment is associated with ProfileActivity
                // Enable ParentViewPager Scroll
                ((ProfileActivity) getActivity()).setScroll(true);
            } else {
                fetchCampaigns();
            }
            if (getActivity() instanceof MainActivity) {
                // Fragment is associated with MainActivity
                ((BaseActivity) getActivity()).getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
                ((MainActivity) getActivity()).showTabs();
            }
            return true;
        }
        return false;
    }

    // Getter for mediaDetailPagerFragment
    public MediaDetailPagerFragment getMediaDetailPagerFragment() {
        return mediaDetailPagerFragment;
    }


    /**
     * this function updates the number of contributions
     */
    void upDateUploadCount() {
        WorkManager.getInstance(getContext())
            .getWorkInfosForUniqueWorkLiveData(UploadWorker.class.getSimpleName()).observe(
                getViewLifecycleOwner(), workInfos -> {
                    if (workInfos.size() > 0) {
                        setUploadCount();
                    }
                });
    }


    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    @Override
    public void refreshNominatedMedia(int index) {
        if (mediaDetailPagerFragment != null && !contributionsListFragment.isVisible()) {
            removeFragment(mediaDetailPagerFragment);
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true);
            mediaDetailPagerFragment.showImage(index);
            showMediaDetailPagerFragment();
        }
    }

    // click listener to toggle description that means uses can press the limited connection
    // banner and description will hide. Tap again to show description.
    private View.OnClickListener toggleDescriptionListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            View view2 = binding.limitedConnectionDescriptionTextView;
            if (view2.getVisibility() == View.GONE) {
                view2.setVisibility(View.VISIBLE);
            } else {
                view2.setVisibility(View.GONE);
            }
        }
    };

    /**
     * When the device rotates, rotate the Nearby banner's compass arrow in tandem.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        float rotateDegree = Math.round(event.values[0]);
        binding.cardViewNearby.rotateCompass(rotateDegree, direction);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing to do.
    }
}
