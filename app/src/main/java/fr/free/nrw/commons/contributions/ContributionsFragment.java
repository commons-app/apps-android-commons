package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;
import static fr.free.nrw.commons.contributions.MainActivity.CONTRIBUTIONS_TAB_POSITION;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.campaigns.Campaign;
import fr.free.nrw.commons.campaigns.CampaignView;
import fr.free.nrw.commons.campaigns.CampaignsPresenter;
import fr.free.nrw.commons.campaigns.ICampaignsView;
import fr.free.nrw.commons.contributions.ContributionsListAdapter.Callback;
import fr.free.nrw.commons.contributions.ContributionsListFragment.SourceRefresher;
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
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class ContributionsFragment
        extends CommonsDaggerSupportFragment
        implements
        MediaDetailProvider,
        OnBackStackChangedListener,
        SourceRefresher,
        LocationUpdateListener,
        ICampaignsView, ContributionsContract.View {
    @Inject @Named("default_preferences") JsonKvStore store;
    @Inject NearbyController nearbyController;
    @Inject OkHttpJsonApiClient okHttpJsonApiClient;
    @Inject CampaignsPresenter presenter;
    @Inject LocationServiceManager locationManager;

    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ContributionsListFragment contributionsListFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    private static final String CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag";
    static final String MEDIA_DETAIL_PAGER_FRAGMENT_TAG = "MediaDetailFragmentTag";

    @BindView(R.id.card_view_nearby) public NearbyNotificationCardView nearbyNotificationCardView;
    @BindView(R.id.campaigns_view) CampaignView campaignView;

    @Inject ContributionsPresenter contributionsPresenter;

    private LatLng curLatLng;

    private boolean firstLocationUpdate = true;
    private boolean isFragmentAttachedBefore = false;
    private View checkBoxView;
    private CheckBox checkBox;

    /**
     * Since we will need to use parent activity on onAuthCookieAcquired, we have to wait
     * fragment to be attached. Latch will be responsible for this sync.
     */
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder) binder)
                    .getService();
            isUploadServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            Timber.e(new RuntimeException("UploadService died but the rest of the process did not!"));
        }
    };
    private boolean shouldShowMediaDetailsFragment;
    private int numberOfContributions;
    private boolean isAuthCookieAcquired;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, view);
        presenter.onAttachView(this);
        contributionsPresenter.onAttachView(this);
        campaignView.setVisibility(View.GONE);
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

        if(shouldShowMediaDetailsFragment){
            showMediaDetailPagerFragment();
        }else{
            showContributionsListFragment();
        }

        if (!ConfigUtils.isBetaFlavour()) {
            setUploadCount();
        }

        getChildFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override public void onFragmentResumed(FragmentManager fm, Fragment f) {
                    super.onFragmentResumed(fm, f);
                    //If media detail pager fragment is visible, hide the campaigns view [might not be the best way to do, this but yeah, this proves to work for now]
                    Timber.e("onFragmentResumed %s", f.getClass().getName());
                    if (f instanceof MediaDetailPagerFragment) {
                        campaignView.setVisibility(View.GONE);
                    }
                }

                @Override public void onFragmentDetached(FragmentManager fm, Fragment f) {
                    super.onFragmentDetached(fm, f);
                    Timber.e("onFragmentDetached %s", f.getClass().getName());
                    //If media detail pager fragment is detached, ContributionsList fragment is gonna be visible, [becomes tightly coupled though]
                    if (f instanceof MediaDetailPagerFragment) {
                        fetchCampaigns();
                    }
                }
            }, true);

        return view;
    }

    /**
     * Initialose the ContributionsListFragment and MediaDetailPagerFragment fragment
     */
    private void initFragments() {
        if (null == contributionsListFragment) {
            contributionsListFragment = new ContributionsListFragment();
        }

        contributionsListFragment.setCallback(new Callback() {
            @Override
            public void retryUpload(Contribution contribution) {
                ContributionsFragment.this.retryUpload(contribution);
            }

            @Override
            public void deleteUpload(Contribution contribution) {
                contributionsPresenter.deleteUpload(contribution);
            }

            @Override
            public void openMediaDetail(int position) {
                showDetail(position);
            }

            @Override
            public Contribution getContributionForPosition(int position) {
                return (Contribution) contributionsPresenter.getItemAtPosition(position);
            }

            @Override
            public void fetchMediaUriFor(Contribution contribution) {
                Timber.d("Fetching thumbnail for %s", contribution.filename);
                contributionsPresenter.fetchMediaDetails(contribution);
            }
        });

        if(null==mediaDetailPagerFragment){
            mediaDetailPagerFragment=new MediaDetailPagerFragment();
        }
    }


    /**
     * Replaces the root frame layout with the given fragment
     * @param fragment
     * @param tag
     */
    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.root_frame, fragment, tag);
        transaction.addToBackStack(CONTRIBUTION_LIST_FRAGMENT_TAG);
        transaction.commit();
        getChildFragmentManager().executePendingTransactions();
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
            onAuthCookieAcquired();
            isFragmentAttachedBefore = true;
        }
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list. Creates
     * new one if null.
     */
    private void showContributionsListFragment() {
        // show tabs on contribution list is visible
        ((MainActivity) getActivity()).showTabs();
        // show nearby card view on contributions list is visible
        if (nearbyNotificationCardView != null) {
            if (store.getBoolean("displayNearbyCardView", true)) {
                if (nearbyNotificationCardView.cardViewVisibilityState
                        == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                    nearbyNotificationCardView.setVisibility(View.VISIBLE);
                }
            } else {
                nearbyNotificationCardView.setVisibility(View.GONE);
            }
        }
        showFragment(contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG);
    }

    /**
     * Replace FrameLayout with MediaDetailPagerFragment, user will see details of selected media.
     * Creates new one if null.
     */
    private void showMediaDetailPagerFragment() {
        // hide tabs on media detail view is visible
        ((MainActivity)getActivity()).hideTabs();
        // hide nearby card view on media detail is visible
        nearbyNotificationCardView.setVisibility(View.GONE);

        showFragment(mediaDetailPagerFragment,MEDIA_DETAIL_PAGER_FRAGMENT_TAG);

    }

    @Override
    public void onBackStackChanged() {
        ((MainActivity)getActivity()).initBackButton();
    }

    /**
     * Called when onAuthCookieAcquired is called on authenticated parent activity
     */
    void onAuthCookieAcquired() {
        // Since we call onAuthCookieAcquired method from onAttach, isAdded is still false. So don't use it
        isAuthCookieAcquired=true;
        if (getActivity() != null) { // If fragment is attached to parent activity
            getActivity().bindService(getUploadServiceIntent(), uploadServiceConnection, Context.BIND_AUTO_CREATE);
            isUploadServiceConnected = true;
        }

    }

    public Intent getUploadServiceIntent(){
        Intent intent = new Intent(getActivity(), UploadService.class);
        intent.setAction(UploadService.ACTION_START_SERVICE);
        return intent;
    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack.
     * Called when user selects a contribution.
     */
    private void showDetail(int i) {
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            mediaDetailPagerFragment = new MediaDetailPagerFragment();
            showMediaDetailPagerFragment();
        }
        mediaDetailPagerFragment.showImage(i);
    }

    @Override
    public void refreshSource() {

    }

    @Override
    public Media getMediaAtPosition(int i) {
        return contributionsPresenter.getItemAtPosition(i);
    }

    @Override
    public int getTotalMediaCount() {
        return numberOfContributions;
    }

    @SuppressWarnings("ConstantConditions")
    private void setUploadCount() {

        compositeDisposable.add(okHttpJsonApiClient
                .getUploadCount(((MainActivity)getActivity()).sessionManager.getCurrentAccount().name)
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

        ((MainActivity)getActivity()).setNumOfUploads(uploadCount);

    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeLocationListener(this);
        locationManager.unregisterLocationManager();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        boolean mediaDetailsVisible = mediaDetailPagerFragment != null && mediaDetailPagerFragment.isVisible();
        outState.putBoolean("mediaDetailsVisible", mediaDetailsVisible);
    }

    @Override
    public void onResume() {
        super.onResume();
        contributionsPresenter.onAttachView(this);
        firstLocationUpdate = true;
        locationManager.addLocationListener(this);

        boolean isSettingsChanged = store.getBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        store.putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        if (isSettingsChanged) {
            refreshSource();
        }


        if (store.getBoolean("displayNearbyCardView", true)) {
            checkPermissionsAndShowNearbyCardView();
            if (nearbyNotificationCardView.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                nearbyNotificationCardView.setVisibility(View.VISIBLE);
            }

        } else {
            // Hide nearby notification card view if related shared preferences is false
            nearbyNotificationCardView.setVisibility(View.GONE);
        }

        fetchCampaigns();
    }

    private void checkPermissionsAndShowNearbyCardView() {
        if (PermissionUtils.hasPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            onLocationPermissionGranted();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                && store.getBoolean("displayLocationPermissionForCardView", true)
                && (((MainActivity) getActivity()).viewPager.getCurrentItem() == CONTRIBUTIONS_TAB_POSITION)) {
            nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION;
            showNearbyCardPermissionRationale();
        }
    }

    private void requestLocationPermission() {
        PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                this::onLocationPermissionGranted,
                this::displayYouWontSeeNearbyMessage,
                -1,
                -1);
    }

    private void onLocationPermissionGranted() {
        nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED;
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
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.unable_to_display_nearest_place));
    }


    private void updateClosestNearbyCardViewInfo() {
        curLatLng = locationManager.getLastLocation();
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curLatLng, curLatLng, true, false)) // thanks to boolean, it will only return closest result
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNearbyNotification,
                        throwable -> {
                            Timber.d(throwable);
                            updateNearbyNotification(null);
                        }));
    }

    private void updateNearbyNotification(@Nullable NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {

        if (nearbyPlacesInfo != null && nearbyPlacesInfo.placeList != null && nearbyPlacesInfo.placeList.size() > 0) {
            Place closestNearbyPlace = nearbyPlacesInfo.placeList.get(0);
            String distance = formatDistanceBetween(curLatLng, closestNearbyPlace.location);
            closestNearbyPlace.setDistance(distance);
            nearbyNotificationCardView.updateContent(closestNearbyPlace);
            if (mediaDetailPagerFragment != null && mediaDetailPagerFragment.isVisible()) {
                nearbyNotificationCardView.setVisibility(View.GONE);
            }else {
                nearbyNotificationCardView.setVisibility(View.VISIBLE);
            }
        } else {
            // Means that no close nearby place is found
            nearbyNotificationCardView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        getChildFragmentManager().removeOnBackStackChangedListener(this);
        locationManager.unregisterLocationManager();
        locationManager.removeLocationListener(this);
        super.onDestroy();

        if (isUploadServiceConnected) {
            if (getActivity() != null) {
                getActivity().unbindService(uploadServiceConnection);
                isUploadServiceConnected = false;
            }
        }
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        // Will be called if location changed more than 1000 meter
        // Do nothing on slight changes for using network efficiently
        firstLocationUpdate = false;
        updateClosestNearbyCardViewInfo();
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        /* Update closest nearby notification card onLocationChangedSlightly
        If first time to update location after onResume, then no need to wait for significant
        location change. Any closest location is better than no location
        */
        if (firstLocationUpdate) {
            updateClosestNearbyCardViewInfo();
            // Turn it to false, since it is not first location update anymore. To change closest location
            // notification, we need to wait for a significant location change.
            firstLocationUpdate = false;
        }
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        // Update closest nearby card view if location changed more than 500 meters
        updateClosestNearbyCardViewInfo();
    }

    @Override public void onViewCreated(@NonNull View view,
        @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * ask the presenter to fetch the campaigns only if user has not manually disabled it
     */
    private void fetchCampaigns() {
        if (store.getBoolean("displayCampaignsCardView", true)) {
            presenter.getCampaigns();
        }
    }

    @Override public void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override public void showCampaigns(Campaign campaign) {
        if (campaign != null) {
            campaignView.setCampaign(campaign);
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        contributionsListFragment.showProgress(shouldShow);
    }

    /**
     * Retry upload when it is failed
     *
     * @param contribution contribution to be retried
     */
    private void retryUpload(Contribution contribution) {
        if (NetworkUtils.isInternetConnectionEstablished(getContext())) {
            if (contribution.getState() == STATE_FAILED && null != uploadService) {
                uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
                Timber.d("Restarting for %s", contribution.toString());
            } else {
                Timber.d("Skipping re-upload for non-failed %s", contribution.toString());
            }
        } else {
            ViewUtil.showLongToast(getContext(), R.string.this_function_needs_network_connection);
        }

    }
}

