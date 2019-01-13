package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CheckBox;

import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.campaigns.Campaign;
import fr.free.nrw.commons.campaigns.CampaignView;
import fr.free.nrw.commons.campaigns.CampaignsPresenter;
import fr.free.nrw.commons.campaigns.ICampaignsView;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyNotificationCardView;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.notification.UnreadNotificationsCheckAsync;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.ContributionDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.location.LocationServiceManager.LOCATION_REQUEST;
import static fr.free.nrw.commons.settings.Prefs.UPLOADS_SHOWING;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

public class ContributionsFragment
        extends CommonsDaggerSupportFragment
        implements  LoaderManager.LoaderCallbacks<Cursor>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener,
                    ContributionsListFragment.SourceRefresher,
                    LocationUpdateListener,ICampaignsView
                    {
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;
    @Inject
    ContributionDao contributionDao;
    @Inject
    MediaWikiApi mediaWikiApi;
    @Inject
    NotificationController notificationController;
    @Inject
    NearbyController nearbyController;

    private ArrayList<DataSetObserver> observersWaitingForLoad = new ArrayList<>();
    private Cursor allContributions;
    private UploadService uploadService;
    private boolean isUploadServiceConnected;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    CountDownLatch waitForContributionsListFragment = new CountDownLatch(1);

    private ContributionsListFragment contributionsListFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    public static final String CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag";
    public static final String MEDIA_DETAIL_PAGER_FRAGMENT_TAG = "MediaDetailFragmentTag";

    public NearbyNotificationCardView nearbyNotificationCardView;
    private Disposable placesDisposable;
    private LatLng curLatLng;

    private boolean firstLocationUpdate = true;
    public LocationServiceManager locationManager;

    private boolean isFragmentAttachedBefore = false;
    private View checkBoxView;
    private CheckBox checkBox;
    private CampaignsPresenter presenter;


    @BindView(R.id.campaigns_view) CampaignView campaignView;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, view);
        presenter = new CampaignsPresenter();
        presenter.onAttachView(this);
        campaignView.setVisibility(View.GONE);
        nearbyNotificationCardView = view.findViewById(R.id.card_view_nearby);
        checkBoxView = View.inflate(getActivity(), R.layout.nearby_permission_dialog, null);
        checkBox = (CheckBox) checkBoxView.findViewById(R.id.never_ask_again);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Do not ask for permission on activity start again
                prefs.edit().putBoolean("displayLocationPermissionForCardView",false).apply();
            }
        });

        if (savedInstanceState != null) {
            mediaDetailPagerFragment = (MediaDetailPagerFragment)getChildFragmentManager().findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
            contributionsListFragment = (ContributionsListFragment) getChildFragmentManager().findFragmentByTag(CONTRIBUTION_LIST_FRAGMENT_TAG);

            if (savedInstanceState.getBoolean("mediaDetailsVisible")) {
                setMediaDetailPagerFragment();
            } else {
                setContributionsListFragment();
            }
        } else {
            setContributionsListFragment();
        }

        if (!ConfigUtils.isBetaFlavour()) {
            setUploadCount();
        }

        getChildFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override public void onFragmentResumed(FragmentManager fm, Fragment f) {
                    super.onFragmentResumed(fm, f);
                    //If media detail pager fragment is visible, hide the campaigns view [might not be the best way to do, this but yeah, this proves to work for now]
                    Log.e("#CF#", "onFragmentResumed" + f.getClass().getName());
                    if (f instanceof MediaDetailPagerFragment) {
                        campaignView.setVisibility(View.GONE);
                    }
                }

                @Override public void onFragmentDetached(FragmentManager fm, Fragment f) {
                    super.onFragmentDetached(fm, f);
                    Log.e("#CF#", "onFragmentDetached" + f.getClass().getName());
                    //If media detail pager fragment is detached, ContributionsList fragment is gonna be visible, [becomes tightly coupled though]
                    if (f instanceof MediaDetailPagerFragment) {
                        fetchCampaigns();
                    }
                }
            }, true);

        return view;
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
        if (((MainActivity)getActivity()).isAuthCookieAcquired && !isFragmentAttachedBefore) {
            onAuthCookieAcquired(((MainActivity)getActivity()).uploadServiceIntent);
            isFragmentAttachedBefore = true;
            new UnreadNotificationsCheckAsync((MainActivity) getActivity(), notificationController).execute();

        }
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list.
     * Creates new one if null.
     */
    public void setContributionsListFragment() {
        // show tabs on contribution list is visible
        ((MainActivity)getActivity()).showTabs();
        // show nearby card view on contributions list is visible
        if (nearbyNotificationCardView != null) {
            if (prefs.getBoolean("displayNearbyCardView", true)) {
                if (nearbyNotificationCardView.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                    nearbyNotificationCardView.setVisibility(View.VISIBLE);
                }
            } else {
                nearbyNotificationCardView.setVisibility(View.GONE);
            }
        }

        // Create if null
        if (getContributionsListFragment() == null) {
            contributionsListFragment =  new ContributionsListFragment();
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        // When this container fragment is created, we fill it with our ContributionsListFragment
        transaction.replace(R.id.root_frame, contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG);
        transaction.addToBackStack(CONTRIBUTION_LIST_FRAGMENT_TAG);
        transaction.commit();
        getChildFragmentManager().executePendingTransactions();
    }

    /**
     * Replace FrameLayout with MediaDetailPagerFragment, user will see details of selected media.
     * Creates new one if null.
     */
    public void setMediaDetailPagerFragment() {
        // hide tabs on media detail view is visible
        ((MainActivity)getActivity()).hideTabs();
        // hide nearby card view on media detail is visible
        nearbyNotificationCardView.setVisibility(View.GONE);

        // Create if null
        if (getMediaDetailPagerFragment() == null) {
            mediaDetailPagerFragment =  new MediaDetailPagerFragment();
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        // When this container fragment is created, we fill it with our MediaDetailPagerFragment
        //transaction.addToBackStack(null);
        transaction.add(R.id.root_frame, mediaDetailPagerFragment, MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
        transaction.addToBackStack(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
        transaction.commit();
        getChildFragmentManager().executePendingTransactions();

    }

    /**
     * Just getter method of ContributionsListFragment child of ContributionsFragment
     * @return contributionsListFragment, if any created
     */
    public ContributionsListFragment getContributionsListFragment() {
        return contributionsListFragment;
    }

    /**
     * Just getter method of MediaDetailPagerFragment child of ContributionsFragment
     * @return mediaDetailsFragment, if any created
     */
    public MediaDetailPagerFragment getMediaDetailPagerFragment() {
        return mediaDetailPagerFragment;
    }

    @Override
    public void onBackStackChanged() {
        ((MainActivity)getActivity()).initBackButton();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int uploads = prefs.getInt(UPLOADS_SHOWING, 100);
        return new CursorLoader(getActivity(), BASE_URI, //TODO find out the reason we pass activity here
                ALL_FIELDS, "", null,
                ContributionDao.CONTRIBUTION_SORT + "LIMIT " + uploads);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (contributionsListFragment != null) {
            contributionsListFragment.changeProgressBarVisibility(false);

            if (contributionsListFragment.getAdapter() == null) {
                contributionsListFragment.setAdapter(new ContributionsListAdapter(getActivity().getApplicationContext(),
                        cursor, 0, contributionDao));
            } else {
                ((CursorAdapter) contributionsListFragment.getAdapter()).swapCursor(cursor);
            }

            contributionsListFragment.clearSyncMessage();
            notifyAndMigrateDataSetObservers();

            ((ContributionsListAdapter)contributionsListFragment.getAdapter()).setUploadService(uploadService);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        ((CursorAdapter) contributionsListFragment.getAdapter()).swapCursor(null);
    }

    private void notifyAndMigrateDataSetObservers() {
        Adapter adapter = contributionsListFragment.getAdapter();

        // First, move the observers over to the adapter now that we have it.
        for (DataSetObserver observer : observersWaitingForLoad) {
            adapter.registerDataSetObserver(observer);
        }
        observersWaitingForLoad.clear();

        // Now fire off a first notification...
        for (DataSetObserver observer : observersWaitingForLoad) {
            observer.onChanged();
        }
    }

    /**
     * Called when onAuthCookieAcquired is called on authenticated parent activity
     * @param uploadServiceIntent
     */
    public void onAuthCookieAcquired(Intent uploadServiceIntent) {
        // Since we call onAuthCookieAcquired method from onAttach, isAdded is still false. So don't use it

        if (getActivity() != null) { // If fragment is attached to parent activity
            getActivity().bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
            isUploadServiceConnected = true;
            allContributions = contributionDao.loadAllContributions();
            getActivity().getSupportLoaderManager().initLoader(0, null, ContributionsFragment.this);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult");
        switch (requestCode) {
            case LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission granted, refreshing view");
                    // No need to display permission request button anymore
                    locationManager.registerLocationManager();
                } else {
                    if (prefs.getBoolean("displayLocationPermissionForCardView", true)) {
                        // Still ask for permission
                        DialogUtil.showAlertDialog(getActivity(),
                                getString(R.string.nearby_card_permission_title),
                                getString(R.string.nearby_card_permission_explanation),
                                this::displayYouWontSeeNearbyMessage,
                                this::enableLocationPermission,
                                checkBoxView,
                                false);
                    }
                }
            }
            break;

            default:
                // This is needed to allow the request codes from the Fragments to be routed appropriately
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // show detail at a position
        showDetail(i);
    }


    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack.
     * Called when user selects a contribution.
     */
    private void showDetail(int i) {
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            mediaDetailPagerFragment = new MediaDetailPagerFragment();
            setMediaDetailPagerFragment();
        }
        mediaDetailPagerFragment.showImage(i);
    }

    @Override
    public void refreshSource() {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (contributionsListFragment.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return contributionDao.fromCursor((Cursor) contributionsListFragment.getAdapter().getItem(i));
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (contributionsListFragment.getAdapter() == null) {
            return 0;
        }
        return contributionsListFragment.getAdapter().getCount();
    }

    @Override
    public void notifyDatasetChanged() {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsListFragment.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.add(observer);
        } else {
            adapter.registerDataSetObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsListFragment.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.remove(observer);
        } else {
            adapter.unregisterDataSetObserver(observer);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setUploadCount() {

        compositeDisposable.add(mediaWikiApi
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

    public void betaSetUploadCount(int betaUploadCount) {
        displayUploadCount(betaUploadCount);
    }

    /**
     * Updates notification indicator on toolbar to indicate there are unread notifications
     * @param isThereUnreadNotifications true if user checked notifications before last notification date
     */
    public void updateNotificationsNotification(boolean isThereUnreadNotifications) {
        ((MainActivity)getActivity()).updateNotificationIcon(isThereUnreadNotifications);
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
        locationManager = new LocationServiceManager(getActivity());

        firstLocationUpdate = true;
        locationManager.addLocationListener(this);

        boolean isSettingsChanged = prefs.getBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        prefs.edit().putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false).apply();
        if (isSettingsChanged) {
            refreshSource();
        }


        if (prefs.getBoolean("displayNearbyCardView", true)) {
            checkGPS();
            if (nearbyNotificationCardView.cardViewVisibilityState == NearbyNotificationCardView.CardViewVisibilityState.READY) {
                nearbyNotificationCardView.setVisibility(View.VISIBLE);
            }

        } else {
            // Hide nearby notification card view if related shared preferences is false
            nearbyNotificationCardView.setVisibility(View.GONE);
        }

        fetchCampaigns();
    }

    /**
     * Check GPS to decide displaying request permission button or not.
     */
    private void checkGPS() {
        if (!locationManager.isProviderEnabled()) {
            Timber.d("GPS is not enabled");
            nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.ENABLE_GPS;
            if (prefs.getBoolean("displayLocationPermissionForCardView", true)) {
                DialogUtil.showAlertDialog(getActivity(),
                        getString(R.string.nearby_card_permission_title),
                        getString(R.string.nearby_card_permission_explanation),
                        this::displayYouWontSeeNearbyMessage,
                        this::enableGPS,
                        checkBoxView,
                        false);
            }
        } else {
            Timber.d("GPS is enabled");
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationManager.isLocationPermissionGranted()) {
                nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED;
                locationManager.registerLocationManager();
            } else {
                nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.ENABLE_LOCATION_PERMISSION;
                // If user didn't selected Don't ask again
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        && prefs.getBoolean("displayLocationPermissionForCardView", true)) {
                        DialogUtil.showAlertDialog(getActivity(),
                                getString(R.string.nearby_card_permission_title),
                                getString(R.string.nearby_card_permission_explanation),
                                this::displayYouWontSeeNearbyMessage,
                                this::enableLocationPermission,
                                checkBoxView,
                                false);
                }
            }
        } else {
            // If device is under Marshmallow, we already checked for GPS
            nearbyNotificationCardView.permissionType = NearbyNotificationCardView.PermissionType.NO_PERMISSION_NEEDED;
            locationManager.registerLocationManager();
        }
    }

    private void enableLocationPermission() {
        if (!getActivity().isFinishing()) {
            ((MainActivity) getActivity()).locationManager.requestPermissions(getActivity());
        }
    }

    private void enableGPS() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.gps_disabled)
                .setCancelable(false)
                .setPositiveButton(R.string.enable_gps,
                        (dialog, id) -> {
                            Intent callGPSSettingIntent = new Intent(
                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            Timber.d("Loaded settings page");
                            ((MainActivity) getActivity()).startActivityForResult(callGPSSettingIntent, 1);
                        })
                .setNegativeButton(R.string.menu_cancel_upload, (dialog, id) -> {
                    dialog.cancel();
                    displayYouWontSeeNearbyMessage();
                })
                .create()
                .show();
    }

    private void displayYouWontSeeNearbyMessage() {
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.unable_to_display_nearest_place));
    }


    private void updateClosestNearbyCardViewInfo() {
        curLatLng = locationManager.getLastLocation();

        placesDisposable = Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curLatLng, curLatLng, true, false)) // thanks to boolean, it will only return closest result
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNearbyNotification,
                        throwable -> {
                            Timber.d(throwable);
                            updateNearbyNotification(null);
                        });
    }

    private void updateNearbyNotification(@Nullable NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {

        if (nearbyPlacesInfo != null && nearbyPlacesInfo.placeList != null && nearbyPlacesInfo.placeList.size() > 0) {
            Place closestNearbyPlace = nearbyPlacesInfo.placeList.get(0);
            String distance = formatDistanceBetween(curLatLng, closestNearbyPlace.location);
            closestNearbyPlace.setDistance(distance);
            nearbyNotificationCardView.updateContent (true, closestNearbyPlace);
        } else {
            // Means that no close nearby place is found
            nearbyNotificationCardView.updateContent (false, null);
        }
    }

    @Override
    public void onDestroy() {
        compositeDisposable.clear();
        getChildFragmentManager().removeOnBackStackChangedListener(this);
        locationManager.unregisterLocationManager();
        locationManager.removeLocationListener(this);
        // Try to prevent a possible NPE
        locationManager.context = null;
        super.onDestroy();

        if (isUploadServiceConnected) {
            if (getActivity() != null) {
                getActivity().unbindService(uploadServiceConnection);
                isUploadServiceConnected = false;
            }
        }

        if (placesDisposable != null) {
            placesDisposable.dispose();
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
        if (prefs.getBoolean("displayCampaignsCardView", true)) {
            presenter.getCampaigns();
        }
    }

    @Override public void showMessage(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override public MediaWikiApi getMediaWikiApi() {
        return mediaWikiApi;
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
}

