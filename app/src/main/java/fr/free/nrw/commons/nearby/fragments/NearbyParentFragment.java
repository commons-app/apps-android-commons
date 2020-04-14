package fr.free.nrw.commons.nearby.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.pluginscalebar.ScaleBarOptions;
import com.mapbox.pluginscalebar.ScaleBarPlugin;
import com.pedrogomez.renderers.RVRendererAdapter;

import fr.free.nrw.commons.utils.DialogUtil;
import java.util.ArrayList;
import java.util.List;
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
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyAdapterFactory;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.utils.ExecutorUtils;
import fr.free.nrw.commons.utils.LayoutUtils;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.NearbyFABUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.UiUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.MainActivity.CONTRIBUTIONS_TAB_POSITION;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.nearby.Label.TEXT_TO_DESCRIPTION;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;


public class NearbyParentFragment extends CommonsDaggerSupportFragment
        implements NearbyParentFragmentContract.View,
        WikidataEditListener.WikidataP18EditListener, LocationUpdateListener {

    @BindView(R.id.bottom_sheet) RelativeLayout rlBottomSheet;
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
    @BindView(R.id.choice_chip_exists) Chip chipExists;
    @BindView(R.id.choice_chip_needs_photo) Chip chipNeedsPhoto;
    @BindView(R.id.choice_chip_group) ChipGroup choiceChipGroup;
    @BindView(R.id.search_view) SearchView searchView;
    @BindView(R.id.search_list_view) RecyclerView recyclerView;
    @BindView(R.id.nearby_filter_list) View nearbyFilterList;
    @BindView(R.id.checkbox_tri_states) CheckBoxTriStates checkBoxTriStates;
    @BindView(R.id.map_view)
    MapView mapView;
    @BindView(R.id.rv_nearby_list)
    RecyclerView rvNearbyList;

    @Inject LocationServiceManager locationManager;
    @Inject NearbyController nearbyController;
    @Inject @Named("default_preferences") JsonKvStore applicationKvStore;
    @Inject BookmarkLocationsDao bookmarkLocationDao;
    @Inject ContributionController controller;
    @Inject WikidataEditListener wikidataEditListener;

    @Inject
    SystemThemeUtils systemThemeUtils;

    private NearbyFilterSearchRecyclerViewAdapter nearbyFilterSearchRecyclerViewAdapter;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private static final float ZOOM_LEVEL = 14f;
    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred = false;
    private Snackbar snackbar;
    private View view;
    private NearbyParentFragmentPresenter presenter;
    private boolean isDarkTheme;
    private boolean isFABsExpanded;
    private Marker selectedMarker;
    private Place selectedPlace;

    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;

    private boolean isMapBoxReady=false;
    private MapboxMap mapBox;
    IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);
    private Marker currentLocationMarker;
    private Polygon currentLocationPolygon;
    private Place lastPlaceToCenter;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation;
    private fr.free.nrw.commons.location.LatLng currentLocation=null;
    private NearbyController.NearbyPlacesInfo nearbyPlacesInfo;
    private NearbyAdapterFactory adapterFactory;
    private boolean isVisibleToUser;
    private MapboxMap.OnCameraMoveListener cameraMoveListener;
    private fr.free.nrw.commons.location.LatLng lastFocusLocation;
    private LatLngBounds latLngBounds;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_nearby_parent, container, false);
        ButterKnife.bind(this, view);
        initNetworkBroadCastReceiver();
        presenter=new NearbyParentFragmentPresenter(bookmarkLocationDao);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        cameraMoveListener= () -> presenter.onCameraMove(mapBox.getCameraPosition().target);
        addCheckBoxCallback();
        presenter.attachView(this);
        initRvNearbyList();
        initThemePreferences();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapBoxMap -> {
            this.mapBox=mapBoxMap;
            initViews();
            presenter.setActionListeners(applicationKvStore);
            initNearbyFilter();
            mapBoxMap.setStyle(isDarkTheme?Style.DARK:Style.OUTDOORS, style -> {
                UiSettings uiSettings = mapBoxMap.getUiSettings();
                uiSettings.setCompassGravity(Gravity.BOTTOM | Gravity.LEFT);
                uiSettings.setCompassMargins(12, 0, 0, 24);
                uiSettings.setLogoEnabled(true);
                uiSettings.setAttributionEnabled(true);
                uiSettings.setRotateGesturesEnabled(false);
                NearbyParentFragment.this.isMapBoxReady=true;
                performMapReadyActions();
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(51.50550, -0.07520))
                        .zoom(ZOOM_LEVEL)
                        .build();
                mapBoxMap.setCameraPosition(cameraPosition);

                ScaleBarPlugin scaleBarPlugin = new ScaleBarPlugin(mapView, mapBoxMap);
                int color = isDarkTheme ? R.color.bottom_bar_light : R.color.bottom_bar_dark;
                ScaleBarOptions scaleBarOptions = new ScaleBarOptions(getContext())
                    .setTextColor(color)
                    .setTextSize(R.dimen.description_text_size)
                    .setBarHeight(R.dimen.tiny_gap)
                    .setBorderWidth(R.dimen.miniscule_margin)
                    .setMarginTop(R.dimen.tiny_padding)
                    .setMarginLeft(R.dimen.tiny_padding)
                    .setTextBarMargin(R.dimen.tiny_padding);
                scaleBarPlugin.create(scaleBarOptions);
            });
        });
    }

    /**
     * Initialise background based on theme, this should be doe ideally via styles, that would need another refactor
     */
    private void initThemePreferences() {
        if(isDarkTheme){
            rvNearbyList.setBackgroundColor(getContext().getResources().getColor(R.color.contributionListDarkBackground));
            checkBoxTriStates.setTextColor(getContext().getResources().getColor(android.R.color.white));
            checkBoxTriStates.setTextColor(getContext().getResources().getColor(android.R.color.white));
            nearbyFilterList.setBackgroundColor(getContext().getResources().getColor(R.color.contributionListDarkBackground));
        }else{
            rvNearbyList.setBackgroundColor(getContext().getResources().getColor(android.R.color.white));
            checkBoxTriStates.setTextColor(getContext().getResources().getColor(R.color.contributionListDarkBackground));
            nearbyFilterList.setBackgroundColor(getContext().getResources().getColor(android.R.color.white));
            nearbyFilterList.setBackgroundColor(getContext().getResources().getColor(android.R.color.white));
        }
    }

    private void initRvNearbyList() {
        rvNearbyList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterFactory = new NearbyAdapterFactory(this, controller);
        rvNearbyList.setAdapter(adapterFactory.create(null));
    }

    private void addCheckBoxCallback() {
        checkBoxTriStates.setCallback((o, state, b, b1) -> presenter.filterByMarkerType(o,state,b,b1));
    }

    private void performMapReadyActions() {
        if (isVisible() && isVisibleToUser && isMapBoxReady) {
            checkPermissionsAndPerformAction(() -> {
                this.lastKnownLocation = locationManager.getLastLocation();
                fr.free.nrw.commons.location.LatLng target=lastFocusLocation;
                if(null==lastFocusLocation){
                    target=lastKnownLocation;
                }
                if (lastKnownLocation != null) {
                    CameraPosition position = new CameraPosition.Builder()
                            .target(LocationUtils.commonsLatLngToMapBoxLatLng(target)) // Sets the new camera position
                            .zoom(ZOOM_LEVEL) // Same zoom level
                            .build();
                    mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
                } else {
                    Toast.makeText(getContext(), getString(R.string.nearby_location_not_available), Toast.LENGTH_LONG).show();
                }
                presenter.onMapReady();
                registerUnregisterLocationListener(false);
                addOnCameraMoveListener();
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed() && isVisibleToUser) {
            startTheMap();
        }
    }

    private void registerNetworkReceiver() {
        if (getActivity() != null)
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        compositeDisposable.clear();
        presenter.detachView();
        registerUnregisterLocationListener(true);
        try {
            if (broadcastReceiver != null && getActivity()!=null) {
                getContext().unregisterReceiver(broadcastReceiver);
            }

            if (locationManager != null && presenter != null) {
                locationManager.removeLocationListener(presenter);
                locationManager.unregisterLocationManager();
            }
            if (null != mapBox) {
                mapBox.removeOnCameraMoveListener(cameraMoveListener);
            }
        }catch (Exception e){
            Timber.e(e);
            //Broadcast receivers should always be unregistered inside catch, you never know if they were already registered or not
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDestroy();
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
        bottomSheetListBehavior = BottomSheetBehavior.from(rlBottomSheet);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void initNearbyFilter() {
        nearbyFilterList.setVisibility(View.GONE);

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                presenter.searchViewGainedFocus();
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
        nearbyFilterSearchRecyclerViewAdapter.setCallback(new NearbyFilterSearchRecyclerViewAdapter.Callback() {
            @Override
            public void setCheckboxUnknown() {
                presenter.setCheckboxUnknown();
            }

            @Override
            public void filterByMarkerType(ArrayList<Label> selectedLabels, int i, boolean b, boolean b1) {
                presenter.filterByMarkerType(selectedLabels,i,b,b1);
            }

            @Override
            public boolean isDarkTheme() {
                return isDarkTheme;
            }
        });
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
            presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
        }

    }

    private void initFilterChips() {
        chipNeedsPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                if (isChecked) {
                    NearbyFilterState.setNeedPhotoSelected(true);
                    presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                } else {
                    NearbyFilterState.setNeedPhotoSelected(false);
                    presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                }
            } else {
                chipNeedsPhoto.setChecked(!isChecked);
            }
        });


        chipExists.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                if (isChecked) {
                    NearbyFilterState.setExistsSelected(true);
                    presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                } else {
                    NearbyFilterState.setExistsSelected(false);
                    presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
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

        rlBottomSheet.getLayoutParams().height = getActivity().getWindowManager()
                .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetListBehavior = BottomSheetBehavior.from(rlBottomSheet);
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
        title.setOnLongClickListener(view -> {
                Utils.copy("place", title.getText().toString(), getContext());
                Toast.makeText(getContext(), R.string.text_copy, Toast.LENGTH_SHORT).show();
                return true;
            });

        title.setOnClickListener(view -> {
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    /**
     * Centers the map in nearby fragment to a given place
     * @param place is new center of the map
     */
    public void centerMapToPlace(Place place) {
        Timber.d("Map is centered to place");
        double cameraShift;
        if(null!=place){
            lastPlaceToCenter=place;
        }

        if (null != lastPlaceToCenter) {
            Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
            } else {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
            }
            CameraPosition position = new CameraPosition.Builder()
                    .target(LocationUtils.commonsLatLngToMapBoxLatLng(
                            new fr.free.nrw.commons.location.LatLng(lastPlaceToCenter.location.getLatitude() - cameraShift,
                                    lastPlaceToCenter.getLocation().getLongitude(),
                                    0))) // Sets the new camera position
                    .zoom(ZOOM_LEVEL) // Same zoom level
                    .build();
            mapBox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
        }
    }

    @Override
    public void updateListFragment(List<Place> placeList) {
        adapterFactory.updateAdapterData(placeList, (RVRendererAdapter<Place>) rvNearbyList.getAdapter());
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getLastLocation() {
        return lastKnownLocation;
    }

    @Override
    public LatLng getLastFocusLocation() {
        return lastFocusLocation==null?null:LocationUtils.commonsLatLngToMapBoxLatLng(lastFocusLocation);
    }

    @Override
    public boolean isCurrentLocationMarkerVisible() {
        if (latLngBounds == null) {
            Timber.d("Map projection bounds are null");
            return false;
        } else {
            Timber.d("Current location marker %s" , latLngBounds.contains(currentLocationMarker.getPosition()) ? "visible" : "invisible");
            return latLngBounds.contains(currentLocationMarker.getPosition());
        }
    }

    @Override
    public void setProjectorLatLngBounds() {
        latLngBounds = mapBox.getProjection().getVisibleRegion().latLngBounds;
    }

    @Override
    public boolean isNetworkConnectionEstablished() {
        return NetworkUtils.isInternetConnectionEstablished(getActivity());
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private void initNetworkBroadCastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() != null) {
                    if (NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                        if (isNetworkErrorOccurred) {
                            presenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
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
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    @Override
    public void listOptionMenuItemClicked() {
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if(bottomSheetListBehavior.getState()== BottomSheetBehavior.STATE_COLLAPSED || bottomSheetListBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(bottomSheetListBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    public void populatePlaces(fr.free.nrw.commons.location.LatLng curlatLng) {
        if (lastKnownLocation == null) {
            lastKnownLocation = currentLocation;
        }
        if (curlatLng.equals(lastFocusLocation)|| lastFocusLocation==null) { // Means we are checking around current location
            populatePlacesForCurrentLocation(lastKnownLocation, curlatLng);
        } else {
            populatePlacesForAnotherLocation(lastKnownLocation, curlatLng);
        }
    }

    private void populatePlacesForCurrentLocation(fr.free.nrw.commons.location.LatLng curlatLng,
                                                  fr.free.nrw.commons.location.LatLng searchLatLng) {
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, true))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nearbyPlacesInfo -> {
                            updateMapMarkers(nearbyPlacesInfo, true);
                            lastFocusLocation=searchLatLng;
                        },
                        throwable -> {
                            Timber.d(throwable);
                            showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                            setProgressBarVisibility(false);
                            presenter.lockUnlockNearby(false);
                            setFilterState();
                        }));
    }

    private void populatePlacesForAnotherLocation(fr.free.nrw.commons.location.LatLng curlatLng,
                                                  fr.free.nrw.commons.location.LatLng searchLatLng) {
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, false))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nearbyPlacesInfo -> {
                            updateMapMarkers(nearbyPlacesInfo, false);
                            lastFocusLocation=searchLatLng;
                        },
                        throwable -> {
                            Timber.d(throwable);
                            showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                            setProgressBarVisibility(false);
                            presenter.lockUnlockNearby(false);
                        }));
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo,boolean shouldUpdateSelectedMarker) {
        this.nearbyPlacesInfo=nearbyPlacesInfo;
        presenter.updateMapMarkers(nearbyPlacesInfo, selectedMarker,shouldUpdateSelectedMarker);
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
        searchThisAreaButton.setOnClickListener(presenter.onSearchThisAreaClicked());
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

    private void handleLocationUpdate(fr.free.nrw.commons.location.LatLng latLng, LocationServiceManager.LocationChangeType locationChangeType){
        this.lastKnownLocation = latLng;
        NearbyController.currentLocation = lastKnownLocation;
        presenter.updateMapAndList(locationChangeType);
    }

    private boolean isUserBrowsing() {
        boolean isUserBrowsing = lastKnownLocation!=null && !presenter.areLocationsClose(getCameraTarget(), lastKnownLocation);
        return isUserBrowsing;
    }

    @Override
    public void onLocationChangedSignificantly(fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location significantly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {
            handleLocationUpdate(latLng,LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedSlightly(fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location slightly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng,LOCATION_SLIGHTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedMedium(fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location changed medium");
        if (isMapBoxReady && latLng != null && !isUserBrowsing()) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng, LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    public void backButtonClicked() {
        presenter.backButtonClicked();
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

    /**
     * Adds a marker for the user's current position. Adds a
     * circle which uses the accuracy * 2, to draw a circle
     * which represents the user's position with an accuracy
     * of 95%.
     *
     * Should be called only on creation of mapboxMap, there
     * is other method to update markers location with users
     * move.
     * @param curLatLng current location
     */
    @Override
    public void addCurrentLocationMarker(fr.free.nrw.commons.location.LatLng curLatLng) {
        if (null != curLatLng) {
            ExecutorUtils.get().submit(() -> {
                mapView.post(() -> removeCurrentLocationMarker());
                Timber.d("Adds current location marker");

                Icon icon = IconFactory.getInstance(getContext())
                        .fromResource(R.drawable.current_location_marker);

                MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                        .position(new LatLng(curLatLng.getLatitude(),
                                curLatLng.getLongitude()));
                currentLocationMarkerOptions.setIcon(icon); // Set custom icon
                mapView.post(() -> currentLocationMarker = mapBox.addMarker(currentLocationMarkerOptions));


                List<LatLng> circle = UiUtils
                        .createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                                curLatLng.getAccuracy() * 2, 100);

                PolygonOptions currentLocationPolygonOptions = new PolygonOptions()
                        .addAll(circle)
                        .strokeColor(getResources().getColor(R.color.current_marker_stroke))
                        .fillColor(getResources().getColor(R.color.current_marker_fill));
                mapView.post(() -> currentLocationPolygon = mapBox.addPolygon(currentLocationPolygonOptions));

            });
        } else {
            Timber.d("not adding current location marker..current location is null");
        }
    }

    private void removeCurrentLocationMarker() {
        if (currentLocationMarker != null && mapBox!=null) {
            mapBox.removeMarker(currentLocationMarker);
            mapBox.removePolygon(currentLocationPolygon);
        }
    }


    /**
     * Makes map camera follow users location with animation
     * @param curLatLng current location of user
     */
    @Override
    public void updateMapToTrackPosition(fr.free.nrw.commons.location.LatLng curLatLng) {
        Timber.d("Updates map camera to track user position");
        CameraPosition cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        if(null!=mapBox) {
            mapBox.setCameraPosition(cameraPosition);
            mapBox.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition), 1000);
        }
    }

    @Override
    public void updateMapMarkers(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker) {
        if(mapBox!=null && isMapBoxReady){
            mapBox.clear();
            addNearbyMarkersToMapBoxMap(nearbyBaseMarkers, selectedMarker);
            presenter.updateMapMarkersToController(nearbyBaseMarkers);
            // Re-enable mapbox gestures on custom location markers load
            mapBox.getUiSettings().setAllGesturesEnabled(true);
        }
    }

    @Override
    public void filterOutAllMarkers() {
        hideAllMArkers();
    }

    /**
     * Displays all markers
     */
    @Override
    public void displayAllMarkers() {
        for (MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
            updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
        }
    }

    /**
     * Filters markers based on selectedLabels and chips
     * @param selectedLabels label list that user clicked
     * @param displayExists chip for displaying only existing places
     * @param displayNeedsPhoto chip for displaying only places need photos
     * @param filterForPlaceState true if we filter places for place state
     * @param filterForAllNoneType true if we filter places with all none button
     */
    @Override
    public void filterMarkersByLabels(List<Label> selectedLabels, boolean displayExists,
                                      boolean displayNeedsPhoto,
                                      boolean filterForPlaceState,
                                      boolean filterForAllNoneType) {
        if (selectedLabels.size() == 0 && filterForPlaceState) { // If nothing is selected, display all
            // remove the previous markers before updating them
            hideAllMArkers();
            for (MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
                if (displayExists && displayNeedsPhoto) {
                    // Exists and needs photo
                    if (markerPlaceGroup.getPlace().destroyed.trim().isEmpty() && markerPlaceGroup.getPlace().pic.trim().isEmpty()) {
                        updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                    }
                } else if (displayExists && !displayNeedsPhoto) {
                    // Exists and all included needs and doesn't needs photo
                    if (markerPlaceGroup.getPlace().destroyed.trim().isEmpty()) {
                        updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                    }
                } else if (!displayExists && displayNeedsPhoto) {
                    // All and only needs photo
                    if (markerPlaceGroup.getPlace().pic.trim().isEmpty()) {
                        updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                    }
                } else if (!displayExists && !displayNeedsPhoto) {
                    // all
                    updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                }

            }
        } else {
            // First remove all the markers
            hideAllMArkers();
            for (MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
                for (Label label : selectedLabels) {
                    if (markerPlaceGroup.getPlace().getLabel().toString().equals(label.toString())) {

                        if (displayExists && displayNeedsPhoto) {
                            // Exists and needs photo
                            if (markerPlaceGroup.getPlace().destroyed.trim().isEmpty() && markerPlaceGroup.getPlace().pic.trim().isEmpty()) {
                                updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                            }
                        } else if (displayExists && !displayNeedsPhoto) {
                            // Exists and all included needs and doesn't needs photo
                            if (markerPlaceGroup.getPlace().destroyed.trim().isEmpty()) {
                                updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                            }
                        } else if (!displayExists && displayNeedsPhoto) {
                            // All and only needs photo
                            if (markerPlaceGroup.getPlace().pic.trim().isEmpty()) {
                                updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                            }
                        } else if (!displayExists && !displayNeedsPhoto) {
                            // all
                            updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
                        }
                    }
                }
            }
        }
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getCameraTarget() {
        return mapBox==null?null:LocationUtils.mapBoxLatLngToCommonsLatLng(mapBox.getCameraPosition().target);
    }

    /**
     * Sets marker icon according to marker status. Sets title and distance.
     * @param isBookmarked true if place is bookmarked
     * @param place
     * @param curLatLng current location
     */
    public void updateMarker(boolean isBookmarked, Place place, @Nullable fr.free.nrw.commons.location.LatLng curLatLng) {
        VectorDrawableCompat vectorDrawable;
        if (isBookmarked) {
            vectorDrawable = VectorDrawableCompat.create(
                    getContext().getResources(), R.drawable.ic_custom_bookmark_marker, getContext().getTheme()
            );
        } else if (!place.pic.trim().isEmpty()) {
            vectorDrawable = VectorDrawableCompat.create( // Means place has picture
                    getContext().getResources(), R.drawable.ic_custom_map_marker_green, getContext().getTheme()
            );
        } else if (!place.destroyed.trim().isEmpty()) { // Means place is destroyed
            vectorDrawable = VectorDrawableCompat.create( // Means place has picture
                    getContext().getResources(), R.drawable.ic_custom_map_marker_grey, getContext().getTheme()
            );
        } else {
            vectorDrawable = VectorDrawableCompat.create(
                    getContext().getResources(), R.drawable.ic_custom_map_marker, getContext().getTheme()
            );
        }
        for (Marker marker : mapBox.getMarkers()) {
            if (marker.getTitle() != null && marker.getTitle().equals(place.getName())) {

                Bitmap icon = UiUtils.getBitmap(vectorDrawable);
                if (curLatLng != null) {
                    String distance = formatDistanceBetween(curLatLng, place.location);
                    place.setDistance(distance);
                }

                NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
                nearbyBaseMarker.title(place.name);
                nearbyBaseMarker.position(
                        new com.mapbox.mapboxsdk.geometry.LatLng(
                                place.location.getLatitude(),
                                place.location.getLongitude()));
                nearbyBaseMarker.place(place);
                nearbyBaseMarker.icon(IconFactory.getInstance(getContext())
                        .fromBitmap(icon));
                marker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
            }
        }
    }

    /**
     * Removes all markers except current location marker, an icon has been used
     * but it is transparent more than grey(as the name of the icon might suggest)
     * since grey icon may lead the users to believe that it is disabled or prohibited contribution
     */
    private void hideAllMArkers() {
        if(currentLocationMarker==null){
            return;
        }
        VectorDrawableCompat vectorDrawable;
        vectorDrawable = VectorDrawableCompat.create(
                getContext().getResources(), R.drawable.ic_custom_greyed_out_marker, getContext().getTheme());
        Bitmap icon = UiUtils.getBitmap(vectorDrawable);
        for (Marker marker : mapBox.getMarkers()) {
            if (!marker.equals(currentLocationMarker)) {
                marker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
            }
        }
        addCurrentLocationMarker(NearbyController.currentLocation);
    }

    private void addNearbyMarkersToMapBoxMap(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker) {
        if (isMapBoxReady && mapBox != null) {
            mapBox.addMarkers(nearbyBaseMarkers);
            setMapMarkerActions(selectedMarker);
            presenter.updateMapMarkersToController(nearbyBaseMarkers);
        }
    }

    private void setMapMarkerActions(Marker selectedMarker) {
        if (mapBox != null) {
            mapBox.setOnInfoWindowCloseListener(marker -> {
                if (marker == selectedMarker) {
                    presenter.markerUnselected();
                }
            });

            mapBox.setOnMarkerClickListener(marker -> {
                if (marker instanceof NearbyMarker) {
                    presenter.markerSelected(marker);
                }
                return false;
            });
        }
    }

    @Override
    public void recenterMap(fr.free.nrw.commons.location.LatLng curLatLng) {
        if (curLatLng == null) {
            if (!(locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled())) {
                showLocationOffDialog();
            }
            return;
        }
        addCurrentLocationMarker(curLatLng);
        CameraPosition position;

        if (ViewUtil.isPortrait(getActivity())) {
            position = new CameraPosition.Builder()
                    .target(isListBottomSheetExpanded() ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isListBottomSheetExpanded() ?
                            ZOOM_LEVEL
                            : mapBox.getCameraPosition().zoom) // Same zoom level
                    .build();
        } else {
            position = new CameraPosition.Builder()
                    .target(isListBottomSheetExpanded() ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isListBottomSheetExpanded() ?
                            ZOOM_LEVEL
                            : mapBox.getCameraPosition().zoom) // Same zoom level
                    .build();
        }

        mapBox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
    }

    @Override
    public void showLocationOffDialog() {
        // This creates a dialog box that prompts the user to enable location
        DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.ask_to_turn_location_on), getString(R.string.nearby_needs_location),
                getString(R.string.yes), getString(R.string.no),  this::openLocationSettings, null);
    }

    @Override
    public void openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        PackageManager packageManager = getActivity().getPackageManager();

        if (intent.resolveActivity(packageManager)!= null) {
            startActivity(intent);
            Toast.makeText(getContext(), R.string.recommend_high_accuracy_mode, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), R.string.cannot_open_location_settings, Toast.LENGTH_LONG).show();
        }
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

    @Override
    public void addOnCameraMoveListener() {
        mapBox.addOnCameraMoveListener(cameraMoveListener);
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
                if (null != mapBox) {
                    mapBox.deselectMarkers();
                }
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
            updateMarker(isBookmarked, this.selectedPlace, locationManager.getLastLocation());
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
    public void onWikidataEditSuccessful() {
        if (mapBox != null && presenter != null && locationManager != null) {
            presenter.updateMapAndList(MAP_UPDATED);
        }
    }

    private void showErrorMessage(String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    public void registerUnregisterLocationListener(boolean removeLocationListener) {
        try {
            if (removeLocationListener) {
                locationManager.unregisterLocationManager();
                locationManager.removeLocationListener(this);
                Timber.d("Location service manager unregistered and removed");
            } else {
                locationManager.addLocationListener(this);
                locationManager.registerLocationManager();
                Timber.d("Location service manager added and registered");
            }
        }catch (Exception e){
            Timber.e(e);
            //Broadcasts are tricky, should be catchedonR
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser=isVisibleToUser;
        if (isResumed() && isVisibleToUser) {
            startTheMap();
        } else {
            if (null != bottomSheetListBehavior) {
                bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }

            if (null != bottomSheetDetailsBehavior) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }
    }

    private void startTheMap() {
        mapView.onStart();
        performMapReadyActions();
    }
}
