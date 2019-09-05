package fr.free.nrw.commons.nearby.mvp.fragments;

import android.Manifest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.mvp.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.utils.FragmentUtils;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.MainActivity.CONTRIBUTIONS_TAB_POSITION;
import static fr.free.nrw.commons.contributions.MainActivity.NEARBY_TAB_POSITION;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;

/**
 * This fragment is under MainActivity at the came level with ContributionFragment and holds
 * two nearby element fragments as NearbyMapFragment and NearbyListFragment
 */
public class NearbyParentFragment extends CommonsDaggerSupportFragment
        implements WikidataEditListener.WikidataP18EditListener,
                    NearbyParentFragmentContract.View, OnMapReadyCallback {

    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.bottom_sheet)
    LinearLayout bottomSheet;
    @BindView(R.id.bottom_sheet_details)
    LinearLayout bottomSheetDetails;
    @BindView(R.id.transparentView)
    View transparentView;
    @BindView(R.id.container_sheet)
    FrameLayout frameLayout;
    @BindView(R.id.loading_nearby_list)
    ConstraintLayout loadingNearbyLayout;
    @BindView(R.id.search_this_area_button)
    Button searchThisAreaButton;
    @BindView(R.id.search_this_area_button_progress_bar)
    ProgressBar searchThisAreaButtonProgressBar;

    @Inject
    NearbyController nearbyController;
    @Inject
    WikidataEditListener wikidataEditListener;
    @Inject
    Gson gson;
    @Inject
    LocationServiceManager locationManager;

    private NearbyParentFragmentContract.UserActions userActions;

    private SupportMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = SupportMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();
    public NearbyParentFragmentPresenter nearbyParentFragmentPresenter;

    // Variables for adding network broadcast receiver.
    private Snackbar snackbar;
    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred = false;
    public View view;

    // Variables for bottom sheet behaviour management
    private BottomSheetBehavior bottomSheetBehavior; // Behavior for list bottom sheet
    private BottomSheetBehavior bottomSheetBehaviorForDetails; // Behavior for details bottom sheet


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        Mapbox.getTelemetry().setUserTelemetryRequestState(false);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);
        this.view = view;
        initBottomSheetBehaviour();
        Timber.d("onCreateView");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume the fragment if exist
        resumeFragment();
        if (!((MainActivity) getActivity()).isContributionsFragmentVisible) {
            checkPermissionsAndPerformAction(nearbyParentFragmentPresenter::performNearbyOperationsIfPermissionGiven);
        }
    }

    @Override
    public void checkPermissionsAndPerformAction(Runnable runnable) {
        PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                runnable,
                () -> ((MainActivity) getActivity()).viewPager.setCurrentItem(CONTRIBUTIONS_TAB_POSITION),
                R.string.location_permission_title,
                R.string.location_permission_rationale_nearby);
    }

    /**
     * Thanks to this method we make sure NearbyMapFragment is ready and attached. So that we can
     * prevent NPE caused by null child fragment. This method is called from child fragment when
     * it is attached.
     */
    public void childMapFragmentAttached() {
        nearbyParentFragmentPresenter = new NearbyParentFragmentPresenter
                                (this, null, locationManager);
        Timber.d("Child fragment attached");
    }

    @Override
    public void addSearchThisAreaButtonAction() {
        searchThisAreaButton.setOnClickListener(nearbyParentFragmentPresenter.onSearchThisAreaClicked());
    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {
        if (isVisible) {
            searchThisAreaButton.setVisibility(View.VISIBLE);
        } else {
            searchThisAreaButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void setSearchThisAreaProgressVisibility(boolean isVisible) {
        if (isVisible) {
            searchThisAreaButtonProgressBar.setVisibility(View.VISIBLE);
        } else {
            searchThisAreaButtonProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        wikidataEditListener.setAuthenticationStateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wikidataEditListener.setAuthenticationStateListener(null);
    }

    /**
     * Populates places and calls update map markers method
     * @param curlatLng current location that user is at
     * @param searchLatLng the location user searches around
     */
    @Override
    public void populatePlaces(LatLng curlatLng, LatLng searchLatLng){
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMapMarkers,
                        throwable -> {
                            Timber.d(throwable);
                            //showErrorMessage(getString(R.string.error_fetching_nearby_places));
                            // TODO solve first unneeded method call here
                            progressBar.setVisibility(View.GONE);
                            //nearbyParentFragmentPresenter.lockNearby(false);
                        }));
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        nearbyParentFragmentPresenter.updateMapMarkers(nearbyPlacesInfo);
    }

    /**
     * Returns the map fragment added to child fragment manager previously, if exists.
     */
    private SupportMapFragment getMapFragment() {
        SupportMapFragment existingFragment = (SupportMapFragment) getChildFragmentManager()
                                                    .findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
        if (existingFragment == null) {
            existingFragment = setMapFragment();
        }
        return existingFragment;
    }

    private SupportMapFragment setMapFragment() {
        Log.d("deneme2","setMapFragment is called");
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        // Build mapboxMap
        MapboxMapOptions options = new MapboxMapOptions();
        options.camera(new CameraPosition.Builder()
                .target(new com.mapbox.mapboxsdk.geometry.LatLng(-52.6885, -70.1395))
                .zoom(9)
                .build());

        // Create map fragment
        SupportMapFragment nearbyMapFragment = SupportMapFragment.newInstance(options);

        //NearbyMapFragment2 nearbyMapFragment = new NearbyMapFragment2();
        fragmentTransaction.replace(R.id.container, nearbyMapFragment, TAG_RETAINED_MAP_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();

        //nearbyMapFragment.getMapAsync(this);
        nearbyMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                Log.d("deneme2","on map ready is finally called, problem is solved!");
            }
        });
        /*if (nearbyMapFragment.getMapboxMap()!=null){

        }*/
        return nearbyMapFragment;
    }

    /**
     * Returns the list fragment added to child fragment manager previously, if exists.
     */
    private NearbyListFragment getListFragment() {
        return (NearbyListFragment) getChildFragmentManager().findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
    }

    @Override
    public void onWikidataEditSuccessful() {

    }

    @Override
    public void setListFragmentExpanded() {

    }

    @Override
    public void refreshView() {

    }

    /**
     * This method first checks if the location permissions has been granted and then register the
     * location manager for updates.
     * @param locationServiceManager passed from presenter to check updates if location
     *                               permissions are given
     */
    @Override
    public void registerLocationUpdates(LocationServiceManager locationServiceManager) {
        locationManager.registerLocationManager();
    }

    /**
     * Resume fragments if they exists
     */
    @Override
    public void resumeFragment() {
        // Find the retained fragment on activity restarts
        nearbyMapFragment = getMapFragment();
        nearbyListFragment = getListFragment();
        addNetworkBroadcastReceiver();


    }

    @Override
    public void displayLoginSkippedWarning() {

    }

    @Override
    public void setFABPlusAction(View.OnClickListener onClickListener) {

    }

    @Override
    public void setFABRecenterAction(View.OnClickListener onClickListener) {

    }

    @Override
    public void animateFABs() {

    }

    @Override
    public void recenterMap(LatLng curLatLng) {

    }

    @Override
    public boolean isNetworkConnectionEstablished() {
        return NetworkUtils.isInternetConnectionEstablished(getActivity());
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    @Override
    public void addNetworkBroadcastReceiver() {
        if (!FragmentUtils.isFragmentUIActive(this)) {
            return;
        }

        if (broadcastReceiver != null) {
            return;
        }

        IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() != null) {
                    if (NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                        if (isNetworkErrorOccurred) {
                            nearbyParentFragmentPresenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED, null);
                            isNetworkErrorOccurred = false;
                        }

                        if (snackbar != null) {
                            snackbar.dismiss();
                            snackbar = null;
                        }
                    } else {
                        if (snackbar == null) {
                            snackbar = Snackbar.make(view, R.string.no_internet, Snackbar.LENGTH_INDEFINITE);
                            // TODO make search this area button invisible
                        }

                        isNetworkErrorOccurred = true;
                        snackbar.show();
                    }
                }
            }
        };

        getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Initialize bottom sheet behaviour (sheet for map list.) Set height 9/16 of all window.
     * Add callback for bottom sheet changes, so that we can sync it with bottom sheet for details
     * (sheet for nearby details)
     */
    private void initBottomSheetBehaviour() {

        transparentView.setAlpha(0);
        bottomSheet.getLayoutParams().height = getActivity().getWindowManager()
                .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(View bottomSheet, int unusedNewState) {
                //prepareViewsForSheetPosition();
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehaviorForDetails = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    @Override
    public void listOptionMenuItemClicked() {
        if(bottomSheetBehavior.getState()== BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public boolean isBottomSheetExpanded() {
        return bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        Log.d("deneme2","on map ready");
    }
}
