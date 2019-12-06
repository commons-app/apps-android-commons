package fr.free.nrw.commons.nearby.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.Style;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.utils.FragmentUtils;
import fr.free.nrw.commons.utils.LayoutUtils;
import fr.free.nrw.commons.utils.NearbyFABUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.MainActivity.CONTRIBUTIONS_TAB_POSITION;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.nearby.Label.TEXT_TO_DESCRIPTION;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;


public class NearbyParentFragment extends CommonsDaggerSupportFragment
        implements NearbyParentFragmentContract.View,
        WikidataEditListener.WikidataP18EditListener {

    @BindView(R.id.bottom_sheet) View bottomSheetList;
    @BindView(R.id.bottom_sheet_details) View bottomSheetDetails;
    @BindView(R.id.transparentView) View transparentView;
    @BindView(R.id.directionsButtonText) TextView directionsButtonText;
    @BindView(R.id.wikipediaButtonText) TextView wikipediaButtonText;
    @BindView(R.id.wikidataButtonText) TextView wikidataButtonText;
    @BindView(R.id.commonsButtonText) TextView commonsButtonText;
    @BindView(R.id.fab_plus) FloatingActionButton fabPlus;
    @BindView(R.id.fab_camera) FloatingActionButton fabCamera;
    @BindView(R.id.fab_gallery) FloatingActionButton fabGallery;
    @BindView(R.id.fab_recenter) FloatingActionButton fabRecenter;
    @BindView(R.id.bookmarkButtonImage) ImageView bookmarkButtonImage;
    @BindView(R.id.bookmarkButton) LinearLayout bookmarkButton;
    @BindView(R.id.wikipediaButton) LinearLayout wikipediaButton;
    @BindView(R.id.wikidataButton) LinearLayout wikidataButton;
    @BindView(R.id.directionsButton) LinearLayout directionsButton;
    @BindView(R.id.commonsButton) LinearLayout commonsButton;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.category) TextView distance;
    @BindView(R.id.icon) ImageView icon;
    @BindView(R.id.search_this_area_button) Button searchThisAreaButton;
    @BindView(R.id.map_progress_bar) ProgressBar progressBar;
    @BindView(R.id.container_sheet) FrameLayout frameLayout;
    @BindView(R.id.loading_nearby_list) ConstraintLayout loadingNearbyLayout;

    @BindView(R.id.choice_chip_exists) Chip chipExists;
    @BindView(R.id.choice_chip_needs_photo) Chip chipNeedsPhoto;
    @BindView(R.id.choice_chip_group) ChipGroup choiceChipGroup;
    @BindView(R.id.search_view) SearchView searchView;
    @BindView(R.id.search_list_view) RecyclerView recyclerView;
    @BindView(R.id.nearby_filter_list) View nearbyFilterList;
    @BindView(R.id.checkbox_tri_states) CheckBoxTriStates checkBoxTriStates;

    @Inject LocationServiceManager locationManager;
    @Inject NearbyController nearbyController;
    @Inject @Named("default_preferences") JsonKvStore applicationKvStore;
    @Inject BookmarkLocationsDao bookmarkLocationDao;
    @Inject ContributionController controller;
    @Inject WikidataEditListener wikidataEditListener;

    private NearbyFilterSearchRecyclerViewAdapter nearbyFilterSearchRecyclerViewAdapter;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private static final double ZOOM_LEVEL = 14f;
    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred = false;
    private Snackbar snackbar;
    private View view;
    private NearbyParentFragmentPresenter nearbyParentFragmentPresenter;
    private boolean isDarkTheme;
    private boolean isFABsExpanded;
    private Marker selectedMarker;
    private Place selectedPlace;

    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;

    private NearbyMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;

    public static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_nearby_parent, container, false);
        ButterKnife.bind(this, view);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        nearbyMapFragment = getNearbyMapFragment();
        nearbyListFragment = getListFragment();
    }


    private void initViews() {
        Timber.d("init views called");
        initBottomSheets();
        loadAnimations();
        setBottomSheetCallbacks();
        decideButtonVisibilities();
        addActionToTitle();
    }

    /**
     * Creates bottom sheet behaviours from bottom sheets, sets initial states and visibility
     */
    private void initBottomSheets() {
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);
    }

    public void initNearbyFilter() {
        nearbyFilterList.setVisibility(View.GONE);

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                nearbyParentFragmentPresenter.searchViewGainedFocus();
                nearbyFilterList.setVisibility(View.VISIBLE);
            } else {
                nearbyFilterList.setVisibility(View.GONE);
            }
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        nearbyFilterSearchRecyclerViewAdapter = new NearbyFilterSearchRecyclerViewAdapter(getContext(),new ArrayList<>(TEXT_TO_DESCRIPTION.values()), recyclerView);
        nearbyFilterList.getLayoutParams().width = (int) LayoutUtils.getScreenWidth(getActivity(), 0.75);
        recyclerView.setAdapter(nearbyFilterSearchRecyclerViewAdapter);
        LayoutUtils.setLayoutHeightAllignedToWidth(1, nearbyFilterList);

        compositeDisposable.add(RxSearchView.queryTextChanges(searchView)
                .takeUntil(RxView.detaches(searchView))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( query -> {
                    ((NearbyFilterSearchRecyclerViewAdapter) recyclerView.getAdapter()).getFilter().filter(query.toString());
                }));
        initFilterChips();
    }

    @Override
    public void setCheckBoxAction() {
        checkBoxTriStates.addAction();
        checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
    }

    @Override
    public void setCheckBoxState(int state) {
        checkBoxTriStates.setState(state);
    }

    @Override
    public void setFilterState() {
        Log.d("deneme5","setfilterState");
        chipNeedsPhoto.setChecked(NearbyFilterState.getInstance().isNeedPhotoSelected());
        chipExists.setChecked(NearbyFilterState.getInstance().isExistsSelected());

        if (NearbyController.currentLocation != null) {
            NearbyParentFragmentPresenter.getInstance().filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
        }

    }

    private void initFilterChips() {

        chipNeedsPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                if (isChecked) {
                    NearbyFilterState.getInstance().setNeedPhotoSelected(true);
                    NearbyParentFragmentPresenter.getInstance().filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                } else {
                    NearbyFilterState.getInstance().setNeedPhotoSelected(false);
                    NearbyParentFragmentPresenter.getInstance().filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                }
            } else {
                chipNeedsPhoto.setChecked(!isChecked);
            }
        });


        chipExists.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                if (isChecked) {
                    NearbyFilterState.getInstance().setExistsSelected(true);
                    NearbyParentFragmentPresenter.getInstance().filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                } else {
                    NearbyFilterState.getInstance().setExistsSelected(false);
                    NearbyParentFragmentPresenter.getInstance().filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                }
            } else {
                chipExists.setChecked(!isChecked);
            }
        });
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private void setBottomSheetCallbacks() {
        bottomSheetDetailsBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetDetails.setOnClickListener(v -> {
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        bottomSheetList.getLayoutParams().height = getActivity().getWindowManager()
                .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);

        bottomSheetListBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    /**
     * Loads animations will be used for FABs
     */
    private void loadAnimations() {
        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);
    }

    /**
     * Fits buttons according to our layout
     */
    private void decideButtonVisibilities() {
        // Remove button text if they exceed 1 line or if internal layout has not been built
        // Only need to check for directions button because it is the longest
        if (directionsButtonText.getLineCount() > 1 || directionsButtonText.getLineCount() == 0) {
            wikipediaButtonText.setVisibility(View.GONE);
            wikidataButtonText.setVisibility(View.GONE);
            commonsButtonText.setVisibility(View.GONE);
            directionsButtonText.setVisibility(View.GONE);
        }
    }

    /**
     *
     */
    private void addActionToTitle() {
        title.setOnClickListener(view -> {
            Utils.copy("place", title.getText().toString(), getContext());
            Toast.makeText(getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    /**
     * Returns the list fragment added to child fragment manager previously, if exists.
     */
    private NearbyListFragment getListFragment() {
        NearbyListFragment existingFragment = (NearbyListFragment) getChildFragmentManager()
                .findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
        if (existingFragment == null) {
            existingFragment = setListFragment();
        }
        return existingFragment;
    }

    /**
     * Returns the map fragment added to child fragment manager previously, if exists.
     */
    private NearbyMapFragment getNearbyMapFragment() {
        NearbyMapFragment existingFragment = (NearbyMapFragment) getChildFragmentManager()
                .findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
        if (existingFragment == null) {
            existingFragment = setMapFragment();
        }
        return existingFragment;
    }

    /**
     * Creates the map fragment and prepares map
     * @return the map fragment created
     */
    private NearbyMapFragment setMapFragment() {
        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(getActivity(), getString(R.string.mapbox_commons_app_token));
        NearbyMapFragment mapFragment;

        // Create fragment
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

        // Build mapboxMap
        isDarkTheme = applicationKvStore.getBoolean("theme", false);
        MapboxMapOptions options = new MapboxMapOptions()
                .compassGravity(Gravity.BOTTOM | Gravity.LEFT)
                .compassMargins(new int[]{12, 0, 0, 24})
                .logoEnabled(false)
                .attributionEnabled(false)
                .camera(new CameraPosition.Builder()
                        .zoom(ZOOM_LEVEL)
                        .target(new com.mapbox.mapboxsdk.geometry.LatLng(-52.6885, -70.1395))
                        .build());

        // Create map fragment
        mapFragment = NearbyMapFragment.newInstance(options);

        // Add map fragment to parent container
        getChildFragmentManager().executePendingTransactions();
        transaction.add(R.id.container, mapFragment, TAG_RETAINED_MAP_FRAGMENT);
        transaction.commit();

        mapFragment.getMapAsync(mapboxMap ->
                mapboxMap.setStyle(
                        NearbyParentFragment.this.isDarkTheme ? Style.DARK : Style.OUTDOORS, style -> {
                            NearbyParentFragment.this.childMapFragmentAttached();
                            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
                        }));
        return mapFragment;
    }

    /**
     * Creates the list fragment and put it into container
     * @return the list fragment created
     */
    private NearbyListFragment setListFragment() {
        NearbyListFragment nearbyListFragment;
        loadingNearbyLayout.setVisibility(View.GONE);
        frameLayout.setVisibility(View.VISIBLE);
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        nearbyListFragment = new NearbyListFragment();
        nearbyListFragment.setArguments(null);
        fragmentTransaction.replace(R.id.container_sheet, nearbyListFragment, TAG_RETAINED_LIST_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();
        return nearbyListFragment;
    }

    private void removeListFragment() {
        if (nearbyListFragment != null) {
            FragmentManager fm = getChildFragmentManager();
            fm.beginTransaction().remove(nearbyListFragment).commit();
            nearbyListFragment = null;
        }
    }

    private void removeMapFragment() {
        if (nearbyMapFragment != null) {
            FragmentManager fm = getChildFragmentManager();
            fm.beginTransaction().remove(nearbyMapFragment).commit();
            nearbyMapFragment = null;
        }
    }

    /**
     * Calls presenter to center map to any place
     * @param place the place we want to center map
     */
    public void centerMapToPlace(Place place) {
        if (nearbyMapFragment != null) {
            nearbyParentFragmentPresenter.centerMapToPlace(place,
                    getActivity().getResources().getConfiguration().orientation ==
                            Configuration.ORIENTATION_PORTRAIT);
        }
    }


    /**
     * Thanks to this method we make sure NearbyMapFragment is ready and attached. So that we can
     * prevent NPE caused by null child fragment. This method is called from child fragment when
     * it is attached.
     */
    private void childMapFragmentAttached() {
        Timber.d("Child map fragment attached");
        nearbyParentFragmentPresenter = NearbyParentFragmentPresenter.getInstance
                (nearbyListFragment,this, nearbyMapFragment, locationManager);
        nearbyParentFragmentPresenter.nearbyFragmentsAreReady();
        initViews();
        nearbyParentFragmentPresenter.setActionListeners(applicationKvStore);
        initNearbyFilter();
    }

    @Override
    public void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener) {
        nearbyMapFragment.getMapboxMap().addOnCameraMoveListener(onCameraMoveListener);
    }

    @Override
    public void registerLocationUpdates(LocationServiceManager locationManager) {
        locationManager.registerLocationManager();
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
                            setSearchThisAreaButtonVisibility(false);
                            setProgressBarVisibility(false);
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
        if(bottomSheetListBehavior.getState()== BottomSheetBehavior.STATE_COLLAPSED || bottomSheetListBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(bottomSheetListBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public void populatePlaces(fr.free.nrw.commons.location.LatLng curlatLng, fr.free.nrw.commons.location.LatLng searchLatLng) {
        if (curlatLng.equals(searchLatLng)) { // Means we are checking around current location
            populatePlacesForCurrentLocation(curlatLng, searchLatLng);
        } else {
            populatePlacesForAnotherLocation(curlatLng, searchLatLng);
        }
    }

    private void populatePlacesForCurrentLocation(fr.free.nrw.commons.location.LatLng curlatLng,
                                                  fr.free.nrw.commons.location.LatLng searchLatLng) {
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMapMarkers,
                        throwable -> {
                            Timber.d(throwable);
                            // TODO: find out why NPE occurs here
                            // showErrorMessage(getString(R.string.error_fetching_nearby_places));
                            setProgressBarVisibility(false);
                            nearbyParentFragmentPresenter.lockUnlockNearby(false);
                            setFilterState();
                        }));
    }

    private void populatePlacesForAnotherLocation(fr.free.nrw.commons.location.LatLng curlatLng,
                                                  fr.free.nrw.commons.location.LatLng searchLatLng) {
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, false))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMapMarkersForCustomLocation,
                        throwable -> {
                            Timber.d(throwable);
                            // TODO: find out why NPE occurs here
                            // showErrorMessage(getString(R.string.error_fetching_nearby_places));
                            setProgressBarVisibility(false);
                            nearbyParentFragmentPresenter.lockUnlockNearby(false);
                        }));
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        nearbyParentFragmentPresenter.updateMapMarkers(nearbyPlacesInfo, selectedMarker);
        setFilterState();
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkersForCustomLocation(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        nearbyParentFragmentPresenter.updateMapMarkersForCustomLocation(nearbyPlacesInfo, selectedMarker);
        setFilterState();
    }


    @Override
    public boolean isListBottomSheetExpanded() {
        return bottomSheetListBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
    }

    @Override
    public boolean isDetailsBottomSheetVisible() {
        return !(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void setBottomSheetDetailsSmaller() {
        if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
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
    public boolean isSearchThisAreaButtonVisible() {
        if (searchThisAreaButton.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setRecyclerViewAdapterAllSelected() {
        if (nearbyFilterSearchRecyclerViewAdapter != null && NearbyController.currentLocation != null) {
            nearbyFilterSearchRecyclerViewAdapter.setRecyclerViewAdapterAllSelected();
        }
    }

    @Override
    public void setRecyclerViewAdapterItemsGreyedOut() {
        if (nearbyFilterSearchRecyclerViewAdapter != null && NearbyController.currentLocation != null) {
            nearbyFilterSearchRecyclerViewAdapter.setRecyclerViewAdapterItemsGreyedOut();
        }
    }

    @Override
    public void setProgressBarVisibility(boolean isVisible) {
        if (isVisible) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void setTabItemContributions() {
        ((MainActivity)getActivity()).viewPager.setCurrentItem(0);
    }

    @Override
    public void checkPermissionsAndPerformAction(Runnable runnable) {
        Timber.d("Checking permission and perfoming action");
        PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                runnable,
                () -> ((MainActivity) getActivity()).viewPager.setCurrentItem(CONTRIBUTIONS_TAB_POSITION),
                R.string.location_permission_title,
                R.string.location_permission_rationale_nearby);
    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    @Override
    public void animateFABs() {
        if (fabPlus.isShown()){
            if (isFABsExpanded) {
                collapseFABs(isFABsExpanded);
            } else {
                expandFABs(isFABsExpanded);
            }
        }
    }

    private void showFABs() {
            NearbyFABUtils.addAnchorToBigFABs(fabPlus, bottomSheetDetails.getId());
            fabPlus.show();
            NearbyFABUtils.addAnchorToSmallFABs(fabGallery, getView().findViewById(R.id.empty_view).getId());
            NearbyFABUtils.addAnchorToSmallFABs(fabCamera, getView().findViewById(R.id.empty_view1).getId());
    }

    /**
     * Expands camera and gallery FABs, turn forward plus FAB
     * @param isFABsExpanded true if they are already expanded
     */
    private void expandFABs(boolean isFABsExpanded){
        if (!isFABsExpanded) {
            showFABs();
            fabPlus.startAnimation(rotate_forward);
            fabCamera.startAnimation(fab_open);
            fabGallery.startAnimation(fab_open);
            fabCamera.show();
            fabGallery.show();
            this.isFABsExpanded = true;
        }
    }

    /**
     * Hides all fabs
     */
    private void hideFABs() {
        NearbyFABUtils.removeAnchorFromFAB(fabPlus);
        fabPlus.hide();
        NearbyFABUtils.removeAnchorFromFAB(fabCamera);
        fabCamera.hide();
        NearbyFABUtils.removeAnchorFromFAB(fabGallery);
        fabGallery.hide();
    }

    /**
     * Collapses camera and gallery FABs, turn back plus FAB
     * @param isFABsExpanded
     */
    private void collapseFABs(boolean isFABsExpanded){
        if (isFABsExpanded) {
            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();
            this.isFABsExpanded = false;
        }
    }

    @Override
    public void displayLoginSkippedWarning() {
        if (applicationKvStore.getBoolean("login_skipped", false)) {
            // prompt the user to login
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.login_alert_message)
                    .setPositiveButton(R.string.login, (dialog, which) -> {
                        // logout of the app
                        BaseLogoutListener logoutListener = new BaseLogoutListener();
                        CommonsApplication app = (CommonsApplication) getActivity().getApplication();
                        app.clearApplicationData(getContext(), logoutListener);
                    })
                    .show();
        }
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are cleared.
     */
    private class BaseLogoutListener implements CommonsApplication.LogoutListener {
        @Override
        public void onLogoutComplete() {
            Timber.d("Logout complete callback received.");
            Intent nearbyIntent = new Intent( getActivity(), LoginActivity.class);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(nearbyIntent);
            getActivity().finish();
        }
    }

    @Override
    public void setFABPlusAction(View.OnClickListener onClickListener) {
        fabPlus.setOnClickListener(onClickListener);
    }

    @Override
    public void setFABRecenterAction(View.OnClickListener onClickListener) {
        fabRecenter.setOnClickListener(onClickListener);
    }

    @Override
    public void disableFABRecenter() {
        fabRecenter.setEnabled(false);
    }

    @Override
    public void enableFABRecenter() {
        fabRecenter.setEnabled(true);
    }

    @Override
    public void recenterMap(fr.free.nrw.commons.location.LatLng curLatLng) {
        nearbyMapFragment.addCurrentLocationMarker(curLatLng);
        CameraPosition position;

        if (ViewUtil.isPortrait(getActivity())){
            position = new CameraPosition.Builder()
                    .target(isListBottomSheetExpanded() ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isListBottomSheetExpanded() ?
                            ZOOM_LEVEL
                            : nearbyMapFragment.getMapboxMap().getCameraPosition().zoom) // Same zoom level
                    .build();
        }else {
            position = new CameraPosition.Builder()
                    .target(isListBottomSheetExpanded() ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isListBottomSheetExpanded() ?
                            ZOOM_LEVEL
                            : nearbyMapFragment.getMapboxMap().getCameraPosition().zoom) // Same zoom level
                    .build();
        }

        nearbyMapFragment.getMapboxMap().animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
    }

    @Override
    public void hideBottomSheet() {
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void displayBottomSheetWithInfo(Marker marker) {
        this.selectedMarker = marker;
        NearbyMarker nearbyMarker = (NearbyMarker) marker;
        Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
        passInfoToSheet(place);
        hideBottomSheet();
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus
     * If nearby details bottom sheet state is expanded: show fab plus
     * If nearby details bottom sheet state is hidden: hide all fabs
     * @param bottomSheetState see bottom sheet states
     */
    public void prepareViewsForSheetPosition(int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                collapseFABs(isFABsExpanded);
                if (!fabPlus.isShown()) showFABs();
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_EXPANDED):
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                nearbyMapFragment.getMapboxMap().deselectMarkers();
                transparentView.setClickable(false);
                transparentView.setAlpha(0);
                collapseFABs(isFABsExpanded);
                hideFABs();
                if (this.getView() != null) {
                    this.getView().requestFocus();
                }
                break;
        }
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     * @param place Place of clicked nearby marker
     */
    private void passInfoToSheet(Place place) {
        this.selectedPlace = place;
        updateBookmarkButtonImage(this.selectedPlace);

        bookmarkButton.setOnClickListener(view -> {
            boolean isBookmarked = bookmarkLocationDao.updateBookmarkLocation(this.selectedPlace);
            updateBookmarkButtonImage(this.selectedPlace);
            nearbyMapFragment.updateMarker(isBookmarked, this.selectedPlace, locationManager.getLastLocation());
        });

        wikipediaButton.setVisibility(place.hasWikipediaLink()?View.VISIBLE:View.GONE);
        wikipediaButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), this.selectedPlace.siteLinks.getWikipediaLink()));

        wikidataButton.setVisibility(place.hasWikidataLink()?View.VISIBLE:View.GONE);
        wikidataButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), this.selectedPlace.siteLinks.getWikidataLink()));

        directionsButton.setOnClickListener(view -> Utils.handleGeoCoordinates(getActivity(), this.selectedPlace.getLocation()));

        commonsButton.setVisibility(this.selectedPlace.hasCommonsLink()?View.VISIBLE:View.GONE);
        commonsButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), this.selectedPlace.siteLinks.getCommonsLink()));

        icon.setImageResource(this.selectedPlace.getLabel().getIcon());

        title.setText(this.selectedPlace.name);
        distance.setText(this.selectedPlace.distance);
        description.setText(this.selectedPlace.getLongDescription());

        fabCamera.setOnClickListener(view -> {
            if (fabCamera.isShown()) {
                Timber.d("Camera button tapped. Place: %s", this.selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateCameraPick(getActivity());
            }
        });

        fabGallery.setOnClickListener(view -> {
            if (fabGallery.isShown()) {
                Timber.d("Gallery button tapped. Place: %s", this.selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateGalleryPick(getActivity(), false);
            }
        });
    }

    private void storeSharedPrefs(Place selectedPlace) {
        Timber.d("Store place object %s", selectedPlace.toString());
        applicationKvStore.putJson(PLACE_OBJECT, selectedPlace);
    }

    private void updateBookmarkButtonImage(Place place) {
        int bookmarkIcon;
        if (bookmarkLocationDao.findBookmarkLocation(place)) {
            bookmarkIcon = R.drawable.ic_round_star_filled_24px;
        } else {
            bookmarkIcon = R.drawable.ic_round_star_border_24px;
        }
        if (bookmarkButtonImage != null) {
            bookmarkButtonImage.setImageResource(bookmarkIcon);
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

    @Override
    public void onPause() {
        super.onPause();
        // this means that this activity will not be recreated now, user is leaving it
        // or the activity is otherwise finishing
        if(getActivity().isFinishing()) {
            // we will not need this fragment anymore, this may also be a good place to signal
            // to the retained fragment object to perform its own cleanup.
            removeMapFragment();
            removeListFragment();

        }
        if (broadcastReceiver != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

        if (locationManager != null && nearbyParentFragmentPresenter != null) {
            locationManager.removeLocationListener(nearbyParentFragmentPresenter);
            locationManager.unregisterLocationManager();
        }
    }

    @Override
    public void onWikidataEditSuccessful() {
        if (nearbyMapFragment != null && nearbyParentFragmentPresenter != null && locationManager != null) {
            nearbyParentFragmentPresenter.updateMapAndList(MAP_UPDATED, locationManager.getLastLocation());
        }
    }

    private void showErrorMessage(String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

}
