package fr.free.nrw.commons.nearby.mvp.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;

/**
 * This fragment is under MainActivity at the came level with ContributionFragment and holds
 * two nearby element fragments as NearbyMapFragment and NearbyListFragment
 */
public class NearbyParentFragment extends CommonsDaggerSupportFragment
        implements LocationUpdateListener,
                    WikidataEditListener.WikidataP18EditListener,
                    NearbyParentFragmentContract.View {

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
    ConstraintLayout loading_nearby_layout;

    @Inject
    NearbyController nearbyController;
    @Inject
    WikidataEditListener wikidataEditListener;
    @Inject
    Gson gson;

    private NearbyParentFragmentContract.UserActions userActions;

    private NearbyMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);
        this.view = view;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeFragment();
    }

    /**
     * Thanks to this method we make sure NearbyMapFragment is ready and attached. So that we can
     * prevent NPE caused by null child fragment. This method is called from child fragment when
     * it is attached.
     */
    public void childMapFragmentAttached() {
        nearbyParentFragmentPresenter = new NearbyParentFragmentPresenter(this, nearbyMapFragment);
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
     * Resume fragments if they exists
     */
    private void resumeFragment() {
        // Find the retained fragment on activity restarts
        nearbyMapFragment = getMapFragment();
        nearbyListFragment = getListFragment();
    }

    /**
     * Returns the map fragment added to child fragment manager previously, if exists.
     */
    private NearbyMapFragment getMapFragment() {
        return (NearbyMapFragment) getChildFragmentManager().findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
    }

    /**
     * Returns the list fragment added to child fragment manager previously, if exists.
     */
    private NearbyListFragment getListFragment() {
        return (NearbyListFragment) getChildFragmentManager().findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Log.d("deneme","location changed significantly");
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Log.d("deneme","location changed significantly");
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        Log.d("deneme","location changed significantly");
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationServiceManager.isLocationPermissionGranted(requireContext())) {
                locationServiceManager.registerLocationManager();
            } else {
                // Should we show an explanation?
                if (locationServiceManager.isPermissionExplanationRequired(getActivity())) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                requestLocationPermissions(locationServiceManager);
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                                showLocationPermissionDeniedErrorDialog(locationServiceManager);
                                dialog.cancel();
                            })
                            .create()
                            .show();

                } else {
                    // No explanation needed, we can request the permission.
                    requestLocationPermissions(locationServiceManager);
                }
            }
        } else {
            locationServiceManager.registerLocationManager();
        }
    }

    /**
     * Request location permission if activity is not null
     * @param locationServiceManager passed from presenter, to listen/un-listen location changes
     */
    @Override
    public void requestLocationPermissions(LocationServiceManager locationServiceManager) {
        if (!getActivity().isFinishing()) {
            locationServiceManager.requestPermissions(getActivity());
        }
    }

    /**
     * Will warn user if location is denied
     * @param locationServiceManager will be passed to checkGps if needs permission
     */
    @Override
    public void showLocationPermissionDeniedErrorDialog(LocationServiceManager locationServiceManager) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.nearby_needs_permissions)
                .setCancelable(false)
                .setPositiveButton(R.string.give_permission, (dialog, which) -> {
                    //will ask for the location permission again
                    checkGps(locationServiceManager);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    //dismiss dialog and send user to contributions tab instead
                    dialog.cancel();
                    ((MainActivity)getActivity()).viewPager.setCurrentItem(((MainActivity)getActivity()).CONTRIBUTIONS_TAB_POSITION);
                })
                .create()
                .show();

    }

    /**
     * Checks device GPS permission first for all API levels
     * @param locationServiceManager will be used to check if provider is enable
     */
    @Override
    public void checkGps(LocationServiceManager locationServiceManager) {
        Timber.d("checking GPS");
        if (!locationServiceManager.isProviderEnabled()) {
            Timber.d("GPS is not enabled");
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.gps_disabled)
                    .setCancelable(false)
                    .setPositiveButton(R.string.enable_gps,
                            (dialog, id) -> {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                Timber.d("Loaded settings page");
                                startActivityForResult(callGPSSettingIntent, 1);
                            })
                    .setNegativeButton(R.string.menu_cancel_upload, (dialog, id) -> {
                        showLocationPermissionDeniedErrorDialog(locationServiceManager);
                        dialog.cancel();
                    })
                    .create()
                    .show();
        } else {
            Timber.d("GPS is enabled");
            checkLocationPermission(locationServiceManager);
        }
    }

    /**
     * This method ideally should be called from inside of CheckGPS method. If device GPS is enabled
     * then we need to control app specific permissions for >=M devices. For other devices, enabled
     * GPS is enough for nearby, so directly call refresh view.
     * @param locationServiceManager will be used to detect if location permission is granted or not
     */
    @Override
    public void checkLocationPermission(LocationServiceManager locationServiceManager) {
        Timber.d("Checking location permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationServiceManager.isLocationPermissionGranted(requireContext())) {
                nearbyParentFragmentPresenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
            } else {
                // Should we show an explanation?
                if (locationServiceManager.isPermissionExplanationRequired(getActivity())) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                requestLocationPermissions(locationServiceManager);
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                                showLocationPermissionDeniedErrorDialog(locationServiceManager);
                                dialog.cancel();
                            })
                            .create()
                            .show();

                } else {
                    // No explanation needed, we can request the permission.
                    requestLocationPermissions(locationServiceManager);
                }
            }
        } else {
            nearbyParentFragmentPresenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
        }
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
                            nearbyParentFragmentPresenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
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
}
