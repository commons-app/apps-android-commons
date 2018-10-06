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
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.PermissionChecker;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.BuildConfig;
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
import fr.free.nrw.commons.nearby.NearbyNoificationCardView;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.notification.UnreadNotificationsCheckAsync;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.ContributionListViewUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;
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
                    LocationUpdateListener
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

    public NearbyNoificationCardView nearbyNoificationCardView;
    private Disposable placesDisposable;
    private LatLng curLatLng;

    private boolean firstLocationUpdate = true;
    private LocationServiceManager locationManager;


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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions, container, false);
        nearbyNoificationCardView = view.findViewById(R.id.card_view_nearby);

        if (savedInstanceState != null) {
            mediaDetailPagerFragment = (MediaDetailPagerFragment)getChildFragmentManager().findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
            contributionsListFragment = (ContributionsListFragment) getChildFragmentManager().findFragmentByTag(CONTRIBUTION_LIST_FRAGMENT_TAG);
            //getSupportLoaderManager().initLoader(0, null, this);
            if (savedInstanceState.getBoolean("mediaDetailsVisible")) {
                setMediaDetailPagerFragment();
            } else {
                setContributionsListFragment();
            }
        } else {
            setContributionsListFragment();
        }

        if(!BuildConfig.FLAVOR.equalsIgnoreCase("beta")){
            setUploadCount();
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (((ContributionsActivity)getActivity()).isAuthCookieAcquired) {
            onAuthCookieAcquired(((ContributionsActivity)getActivity()).uploadServiceIntent);
        }
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list.
     * Creates new one if null.
     */
    public void setContributionsListFragment() {
        // show tabs on contribution list is visible
        ((ContributionsActivity)getActivity()).showTabs();
        // show nearby card view on contributions list is visible
        if (prefs.getBoolean("displayNearbyCardView", true)) {
            nearbyNoificationCardView.setVisibility(View.VISIBLE);
            Log.d("deneme9","nearbyNoificationCardView.setVisibility(View.VISIBLE)");
        } else {
            nearbyNoificationCardView.setVisibility(View.GONE);
            Log.d("deneme9","nearbyNoificationCardView.setVisibility(View.GONE)");

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
        ((ContributionsActivity)getActivity()).hideTabs();
        // hide nearby card view on media detail is visible
        nearbyNoificationCardView.setVisibility(View.GONE);
        Log.d("deneme9","nearbyNoificationCardView.setVisibility(View.VISIBLE)");


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
        ((ContributionsActivity)getActivity()).initBackButton();
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
                Log.d("deneme", "contributionsListFragment adapter set");
                contributionsListFragment.setAdapter(new ContributionsListAdapter(getActivity().getApplicationContext(),
                        cursor, 0, contributionDao));
            } else {
                ((CursorAdapter) contributionsListFragment.getAdapter()).swapCursor(cursor);
            }

            contributionsListFragment.clearSyncMessage();
            notifyAndMigrateDataSetObservers();
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
            allContributions = contributionDao.loadAllContributions();
            getActivity().getSupportLoaderManager().initLoader(0, null, ContributionsFragment.this);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission granted, refreshing view");
                    // No need to display permission request button anymore
                    nearbyNoificationCardView.displayPermissionRequestButton(false);
                    locationManager.registerLocationManager();
                    Log.d("deneme7","location manager registered, location manager:"+((ContributionsActivity)getActivity()).locationManager);

                } else {
                    // Still ask for permission
                    nearbyNoificationCardView.displayPermissionRequestButton(true);
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

    /**
     * Retry upload when it is failed
     * @param i position of upload which will be retried
     */
    public void retryUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = contributionDao.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Timber.d("Restarting for %s", c.toString());
        } else {
            Timber.d("Skipping re-upload for non-failed %s", c.toString());
        }
    }

    /**
     * Delete a failed upload attempt
     * @param i position of upload attempt which will be deteled
     */
    public void deleteUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = contributionDao.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            Timber.d("Deleting failed contrib %s", c.toString());
            contributionDao.delete(c);
        } else {
            Timber.d("Skipping deletion for non-failed contrib %s", c.toString());
        }
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
                .getUploadCount(((ContributionsActivity)getActivity()).sessionManager.getCurrentAccount().name)
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

        ((ContributionsActivity)getActivity()).setNumOfUploads(uploadCount);

    }

    public void betaSetUploadCount(int betaUploadCount) {
        displayUploadCount(betaUploadCount);
    }

    /**
     * Updates notification indicator on toolbar to indicate there are unread notifications
     * @param isThereUnreadNotifications true if user checked notifications before last notification date
     */
    public void updateNotificationsNotification(boolean isThereUnreadNotifications) {
        ((ContributionsActivity)getActivity()).updateNotificationIcon(isThereUnreadNotifications);
    }

    @Override
    public void onStart() {
        super.onStart();
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
        Log.d("deneme10","onResume, card view"+nearbyNoificationCardView);
        locationManager = new LocationServiceManager(getActivity());

        firstLocationUpdate = true;
        locationManager.addLocationListener(this);

        boolean isSettingsChanged = prefs.getBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false);
        prefs.edit().putBoolean(Prefs.IS_CONTRIBUTION_COUNT_CHANGED, false).apply();
        if (isSettingsChanged) {
            refreshSource();
        }

        new UnreadNotificationsCheckAsync((ContributionsActivity) getActivity(), notificationController).execute();


        if (prefs.getBoolean("displayNearbyCardView", true)) {
            nearbyNoificationCardView.cardViewVisibilityState = NearbyNoificationCardView.CardViewVisibilityState.LOADING;
            nearbyNoificationCardView.setVisibility(View.VISIBLE);
            Log.d("deneme9","nearbyNoificationCardView.setVisibility(View.VISIBLE)");

            checkGPS();

        } else {
            // Hide nearby notification card view if related shared preferences is false
            nearbyNoificationCardView.setVisibility(View.GONE);
            Log.d("deneme9","nearbyNoificationCardView.setVisibility(View.GONE)");
        }


    }

    private void checkGPS() {
        if (!locationManager.isProviderEnabled()) {
            Timber.d("GPS is not enabled");
            nearbyNoificationCardView.permissionType = NearbyNoificationCardView.PermissionType.ENABLE_GPS;
            nearbyNoificationCardView.displayPermissionRequestButton(true);
        } else {
            Log.d("deneme11","GPS is enabled");
            Timber.d("GPS is enabled");
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationManager.isLocationPermissionGranted()) {
                nearbyNoificationCardView.permissionType = NearbyNoificationCardView.PermissionType.NO_PERMISSION_NEEDED;
                nearbyNoificationCardView.displayPermissionRequestButton(false);
                locationManager.registerLocationManager();
            } else {
                nearbyNoificationCardView.permissionType = NearbyNoificationCardView.PermissionType.ENABLE_LOCATION_PERMISSON;
                nearbyNoificationCardView.displayPermissionRequestButton(true);
            }
        } else {
            // If device is under Marshmallow, we already checked for GPS
            nearbyNoificationCardView.permissionType = NearbyNoificationCardView.PermissionType.NO_PERMISSION_NEEDED;
            nearbyNoificationCardView.displayPermissionRequestButton(false);
            locationManager.registerLocationManager();
        }
    }


    private void updateClosestNearbyCardViewInfo() {

        curLatLng = locationManager.getLastLocation();

        Log.d("deneme7"," updateClosestNearbyCardViewInfo called/ curlatlng is:"+curLatLng+" nearby controller is:"+nearbyController);

        placesDisposable = Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curLatLng, true)) // thanks to boolean, it will only return closest result
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateNearbyNotification,
                        throwable -> {
                            Timber.d(throwable);
                            Log.d("deneme7","error"+throwable);
                            updateNearbyNotification(null);
                        });
    }

    private void updateNearbyNotification(@Nullable NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        Log.d("deneme7", "update nearby location called");
        Log.d("deneme7", "nearbyPlacesInfo:"+nearbyPlacesInfo+" nearbyPlacesInfo.placeList:"+nearbyPlacesInfo.placeList+" nearbyPlacesInfo.placeList.size():"+nearbyPlacesInfo.placeList.size());

        if (nearbyPlacesInfo != null && nearbyPlacesInfo.placeList != null && nearbyPlacesInfo.placeList.size() > 0) {
            Place closestNearbyPlace = nearbyPlacesInfo.placeList.get(0);
            String distance = formatDistanceBetween(curLatLng, closestNearbyPlace.location);
            closestNearbyPlace.setDistance(distance);
            nearbyNoificationCardView.updateContent (true, closestNearbyPlace);
            Log.d("deneme7","placelist size > 0");
        } else {
            // Means that no close nearby place is found
            Log.d("deneme7","placelist bos");
            nearbyNoificationCardView.updateContent (false, null);
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
        Log.d("deneme7","onLocationChangedSignificantly called");
        firstLocationUpdate = false;
        updateClosestNearbyCardViewInfo();
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        // Update closest nearby notification card onLocationChangedSlightly
        Log.d("deneme7","onLocationChangedSlightly called");
        // If first time to update location after onResume, then no need to wait for significant
        // location change. Any closest location is better than no location
        if (firstLocationUpdate) {
            updateClosestNearbyCardViewInfo();
            // Turn it to false, since it is not first location update anymore. To change closest location
            // notifiction, we need to wait for a significant location change.
            firstLocationUpdate = false;
        }
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        // Update closest nearby card view if location changed more than 500 meters
        updateClosestNearbyCardViewInfo();
    }

}

