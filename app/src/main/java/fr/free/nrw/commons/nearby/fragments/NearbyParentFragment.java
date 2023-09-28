package fr.free.nrw.commons.nearby.fragments;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.CUSTOM_QUERY;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding3.appcompat.RxSearchView;
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
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.pluginscalebar.ScaleBarOptions;
import com.mapbox.pluginscalebar.ScaleBarPlugin;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MapController.NearbyPlacesInfo;
import fr.free.nrw.commons.MapStyle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.fragments.AdvanceQueryFragment.Callback;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.DialogUtil;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;


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
    @BindView(R.id.choice_chip_wlm) Chip chipWlm;
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
    @BindView(R.id.no_results_message) TextView noResultsView;
    @BindView(R.id.tv_attribution)
    AppCompatTextView tvAttribution;
    @BindView(R.id.rl_container_wlm_month_message)
    RelativeLayout rlContainerWLMMonthMessage;
    @BindView(R.id.tv_learn_more)
    AppCompatTextView tvLearnMore;
    @BindView(R.id.iv_toggle_chips)
    AppCompatImageView ivToggleChips;
    @BindView(R.id.chip_view)
    View llContainerChips;
    @BindView(R.id.btn_advanced_options)
    AppCompatButton btnAdvancedOptions;
    @BindView(R.id.fl_container_nearby_children)
    FrameLayout flConainerNearbyChildren;

    @Inject LocationServiceManager locationManager;
    @Inject NearbyController nearbyController;
    @Inject @Named("default_preferences") JsonKvStore applicationKvStore;
    @Inject BookmarkLocationsDao bookmarkLocationDao;
    @Inject ContributionController controller;
    @Inject WikidataEditListener wikidataEditListener;

    @Inject
    SystemThemeUtils systemThemeUtils;
    @Inject
    CommonPlaceClickActions commonPlaceClickActions;

    private NearbyFilterSearchRecyclerViewAdapter nearbyFilterSearchRecyclerViewAdapter;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private static final float ZOOM_LEVEL = 14f;
    private static final float ZOOM_OUT = 0f;
    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private View view;
    private NearbyParentFragmentPresenter presenter;
    private boolean isDarkTheme;
    private boolean isFABsExpanded;
    private Marker selectedMarker;
    private Place selectedPlace;

    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;

    private boolean isMapBoxReady;
    private boolean isPermissionDenied;
    private boolean recenterToUserLocation;
    private MapboxMap mapBox;
    IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);
    private Marker currentLocationMarker;
    private Polygon currentLocationPolygon;
    private Place lastPlaceToCenter;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation;
    private boolean isVisibleToUser;
    private MapboxMap.OnCameraMoveListener cameraMoveListener;
    private fr.free.nrw.commons.location.LatLng lastFocusLocation;
    private LatLngBounds latLngBounds;
    private PlaceAdapter adapter;
    private NearbyParentFragmentInstanceReadyCallback nearbyParentFragmentInstanceReadyCallback;
    private boolean isAdvancedQueryFragmentVisible = false;
    private ActivityResultLauncher<String[]> inAppCameraLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for (final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }

            if (areAllGranted) {
                controller.locationPermissionCallback.onLocationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    controller.handleShowRationaleFlowCameraLocation(getActivity());
                } else {
                    controller.locationPermissionCallback.onLocationPermissionDenied(getActivity().getString(R.string.in_app_camera_location_permission_denied));
                }
            }
        }
    });

    private ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for (final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }

            if (areAllGranted) {
                locationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    DialogUtil.showAlertDialog(getActivity(), getActivity().getString(R.string.location_permission_title),
                        getActivity().getString(R.string.location_permission_rationale_nearby),
                        getActivity().getString(android.R.string.ok),
                        getActivity().getString(android.R.string.cancel),
                        () -> {
                            if (!(locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled())) {
                                showLocationOffDialog();
                            }
                        },
                        () -> isPermissionDenied = true,
                        null,
                        false);
                } else {
                    isPermissionDenied = true;
                }
            }
        }
    });

    /**
     * Holds filtered markers that are to be shown
     */
    private List<NearbyBaseMarker> filteredMarkers;
    /**
     * Holds all the markers
     */
    private List<NearbyBaseMarker> allMarkers =new ArrayList<>();

    /**
     * WLM URL
     */
    public static final String WLM_URL = "https://commons.wikimedia.org/wiki/Commons:Mobile_app/Contributing_to_WLM_using_the_app";
    /**
     * Saves response of list of places for the first time
     */
    private List<Place> places = new ArrayList<>();

    @NonNull
    public static NearbyParentFragment newInstance() {
        NearbyParentFragment fragment = new NearbyParentFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_nearby_parent, container, false);
        ButterKnife.bind(this, view);
        initNetworkBroadCastReceiver();
        presenter=new NearbyParentFragmentPresenter(bookmarkLocationDao);
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.nearby_fragment_menu, menu);
        MenuItem listMenu = menu.findItem(R.id.list_sheet);
        listMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                listOptionMenuItemClicked();
                return false;
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        if (Utils.isMonumentsEnabled(new Date())) {
            rlContainerWLMMonthMessage.setVisibility(View.VISIBLE);
        } else {
            rlContainerWLMMonthMessage.setVisibility(View.GONE);
        }

        cameraMoveListener= () -> presenter.onCameraMove(mapBox.getCameraPosition().target);
        addCheckBoxCallback();
        presenter.attachView(this);
        isPermissionDenied = false;
        recenterToUserLocation = false;
        initRvNearbyList();
        initThemePreferences();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapBoxMap -> {
            mapBox =mapBoxMap;
            initViews();
            presenter.setActionListeners(applicationKvStore);
            initNearbyFilter();
            mapBoxMap.setStyle(isDarkTheme? MapStyle.DARK :
                MapStyle.OUTDOORS, style -> {
                final UiSettings uiSettings = mapBoxMap.getUiSettings();
                uiSettings.setCompassGravity(Gravity.BOTTOM | Gravity.LEFT);
                uiSettings.setCompassMargins(12, 0, 0, 24);
                uiSettings.setLogoEnabled(false);
                uiSettings.setAttributionEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                isMapBoxReady =true;
                if(nearbyParentFragmentInstanceReadyCallback!=null){
                    nearbyParentFragmentInstanceReadyCallback.onReady();
                }
                performMapReadyActions();
                final CameraPosition cameraPosition;
                if(applicationKvStore.getString("LastLocation")!=null) { // Checking for last searched location
                    String[] locationLatLng = applicationKvStore.getString("LastLocation").split(",");
                    cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(Double.valueOf(locationLatLng[0]),
                            Double.valueOf(locationLatLng[1])))
                        .zoom(ZOOM_LEVEL)
                        .build();
                }else {
                    cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(51.50550, -0.07520))
                        .zoom(ZOOM_OUT)
                        .build();
                }
                mapBoxMap.setCameraPosition(cameraPosition);

                final ScaleBarPlugin scaleBarPlugin = new ScaleBarPlugin(mapView, mapBoxMap);
                final int color = isDarkTheme ? R.color.bottom_bar_light : R.color.bottom_bar_dark;
                final ScaleBarOptions scaleBarOptions = new ScaleBarOptions(getContext())
                    .setTextColor(color)
                    .setTextSize(R.dimen.description_text_size)
                    .setBarHeight(R.dimen.tiny_gap)
                    .setBorderWidth(R.dimen.miniscule_margin)
                    .setMarginTop(R.dimen.tiny_padding)
                    .setMarginLeft(R.dimen.tiny_padding)
                    .setTextBarMargin(R.dimen.tiny_padding);
                scaleBarPlugin.create(scaleBarOptions);
                onResume();
            });
        });

        tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
        tvAttribution.setMovementMethod(LinkMovementMethod.getInstance());

        btnAdvancedOptions.setOnClickListener(v -> {
            searchView.clearFocus();
            showHideAdvancedQueryFragment(true);
            final AdvanceQueryFragment fragment = new AdvanceQueryFragment();
            final Bundle bundle=new Bundle();
            try {
                bundle.putString("query", FileUtils.readFromResource("/queries/nearby_query.rq"));
            } catch (IOException e) {
                Timber.e(e);
            }
            fragment.setArguments(bundle);
            fragment.callback = new Callback() {
                @Override
                public void close() {
                    showHideAdvancedQueryFragment(false);
                }

                @Override
                public void reset() {
                    presenter.setAdvancedQuery(null);
                    presenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
                    showHideAdvancedQueryFragment(false);
                }

                @Override
                public void apply(@NotNull final String query) {
                    presenter.setAdvancedQuery(query);
                    presenter.updateMapAndList(CUSTOM_QUERY);
                    showHideAdvancedQueryFragment(false);
                }
            };
            getChildFragmentManager().beginTransaction()
                .replace(R.id.fl_container_nearby_children, fragment)
                .commit();
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
        adapter = new PlaceAdapter(bookmarkLocationDao,
            place -> {
                centerMapToPlace(place);
                return Unit.INSTANCE;
            },
            (place, isBookmarked) -> {
                updateMarker(isBookmarked, place, null);
                return Unit.INSTANCE;
            },
            commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher
        );
        rvNearbyList.setAdapter(adapter);
    }

    private void addCheckBoxCallback() {
        checkBoxTriStates.setCallback((o, state, b, b1) -> presenter.filterByMarkerType(o,state,b,b1));
    }

    private void performMapReadyActions() {
        if (((MainActivity)getActivity()).activeFragment == ActiveFragment.NEARBY && isMapBoxReady) {
            if(!applicationKvStore.getBoolean("doNotAskForLocationPermission", false) ||
                PermissionUtils.hasPermission(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION})){
                checkPermissionsAndPerformAction();
            }else{
                isPermissionDenied = true;
                addOnCameraMoveListener();
            }
        }
    }

    private void locationPermissionGranted() {
        isPermissionDenied = false;

        applicationKvStore.putBoolean("doNotAskForLocationPermission", false);
        lastKnownLocation = locationManager.getLastLocation();
        fr.free.nrw.commons.location.LatLng target=lastFocusLocation;
        if(null==lastFocusLocation){
            target=lastKnownLocation;
        }
        if (lastKnownLocation != null) {
            final CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(target)) // Sets the new camera position
                .zoom(ZOOM_LEVEL) // Same zoom level
                .build();
            mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
        else if(locationManager.isGPSProviderEnabled()||locationManager.isNetworkProviderEnabled()){
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            setProgressBarVisibility(true);
        }
        else {
            Toast.makeText(getContext(), getString(R.string.nearby_location_not_available), Toast.LENGTH_LONG).show();
        }
        presenter.onMapReady();
        registerUnregisterLocationListener(false);
        addOnCameraMoveListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed() && ((MainActivity)getActivity()).activeFragment == ActiveFragment.NEARBY) {
            if(!isPermissionDenied && !applicationKvStore.getBoolean("doNotAskForLocationPermission", false)){
                if (!locationManager.isGPSProviderEnabled()) {
                    startMapWithCondition("Without GPS");
                } else {
                    startTheMap();
                }
            }else{
                startMapWithCondition("Without Permission");
            }
        }
    }

    /**
     * Starts the map without GPS and without permission
     * By default it points to 51.50550,-0.07520 coordinates, other than that it points to the
     * last known location which can be get by the key "LastLocation" from applicationKvStore
     *
     * @param condition : for which condition the map should start
     */
    private void startMapWithCondition(final String condition) {
        mapView.onStart();

        if (condition.equals("Without Permission")) {
            applicationKvStore.putBoolean("doNotAskForLocationPermission", true);
        }

        final CameraPosition position;
        if (applicationKvStore.getString("LastLocation")!=null) {
            final String[] locationLatLng
                = applicationKvStore.getString("LastLocation").split(",");
            lastKnownLocation
                = new fr.free.nrw.commons.location.LatLng(Double.parseDouble(locationLatLng[0]),
                Double.parseDouble(locationLatLng[1]), 1f);
            position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(lastKnownLocation))
                .zoom(ZOOM_LEVEL)
                .build();

        } else {
            lastKnownLocation = new fr.free.nrw.commons.location.LatLng(51.50550,
                -0.07520,1f);
            position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(lastKnownLocation))
                .zoom(ZOOM_OUT)
                .build();
        }

        if(mapBox != null){
            mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
            addOnCameraMoveListener();
            presenter.onMapReady();
            removeCurrentLocationMarker();
        }
    }

    private void registerNetworkReceiver() {
        if (getActivity() != null) {
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
        }
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
        }catch (final Exception e){
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
    public void onSaveInstanceState(final Bundle outState) {
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
        presenter.removeNearbyPreferences(applicationKvStore);
    }

    private void initViews() {
        Timber.d("init views called");
        initBottomSheets();
        loadAnimations();
        setBottomSheetCallbacks();
        decideButtonVisibilities();
        addActionToTitle();
        if (!Utils.isMonumentsEnabled(new Date())) {
            chipWlm.setVisibility(View.GONE);
        }
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheets, sets initial states and visibility
     * b) Gets the touch event on the map to perform following actions:
     *      if fab is open then close fab.
     *      if bottom sheet details are expanded then collapse bottom sheet details.
     *      if bottom sheet details are collapsed then hide the bottom sheet details.
     *      if listBottomSheet is open then hide the list bottom sheet.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initBottomSheets() {
        bottomSheetListBehavior = BottomSheetBehavior.from(rlBottomSheet);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mapView.setOnTouchListener((v, event) -> {

            // Motion event is triggered two times on a touch event, one as ACTION_UP
            // and other as ACTION_DOWN, we only want one trigger per touch event.

            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFABsExpanded) {
                    collapseFABs(true);
                } else if (bottomSheetDetailsBehavior.getState()
                    == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (bottomSheetDetailsBehavior.getState()
                    == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else if (isListBottomSheetExpanded()) {
                    bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
            return false;
        });
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

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        nearbyFilterSearchRecyclerViewAdapter = new NearbyFilterSearchRecyclerViewAdapter(getContext(), new ArrayList<>(Label.valuesAsList()), recyclerView);
        nearbyFilterSearchRecyclerViewAdapter.setCallback(new NearbyFilterSearchRecyclerViewAdapter.Callback() {
            @Override
            public void setCheckboxUnknown() {
                presenter.setCheckboxUnknown();
            }

            @Override
            public void filterByMarkerType(final ArrayList<Label> selectedLabels, final int i, final boolean b, final boolean b1) {
                presenter.filterByMarkerType(selectedLabels,i,b,b1);
            }

            @Override
            public boolean isDarkTheme() {
                return isDarkTheme;
            }
        });
        nearbyFilterList.getLayoutParams().width = (int) LayoutUtils.getScreenWidth(getActivity(), 0.75);
        recyclerView.setAdapter(nearbyFilterSearchRecyclerViewAdapter);
        LayoutUtils.setLayoutHeightAllignedToWidth(1.25, nearbyFilterList);

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
    public void setCheckBoxState(final int state) {
        checkBoxTriStates.setState(state);
    }

    @Override
    public void setFilterState() {
        chipNeedsPhoto.setChecked(NearbyFilterState.getInstance().isNeedPhotoSelected());
        chipExists.setChecked(NearbyFilterState.getInstance().isExistsSelected());
        chipWlm.setChecked(NearbyFilterState.getInstance().isWlmSelected());
        if (NearbyController.currentLocation != null) {
            presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
        }

    }

    private void initFilterChips() {
        chipNeedsPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                NearbyFilterState.setNeedPhotoSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                updatePlaceList(chipNeedsPhoto.isChecked(),
                    chipExists.isChecked(), chipWlm.isChecked());
            } else {
                chipNeedsPhoto.setChecked(!isChecked);
            }
        });


        chipExists.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                NearbyFilterState.setExistsSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                updatePlaceList(chipNeedsPhoto.isChecked(),
                    chipExists.isChecked(), chipWlm.isChecked());
            } else {
                chipExists.setChecked(!isChecked);
            }
        });


        chipWlm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(NearbyController.currentLocation!=null){
                checkBoxTriStates.setState(CheckBoxTriStates.UNKNOWN);
                NearbyFilterState.setWlmSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels, checkBoxTriStates.getState(), true, false);
                updatePlaceList(chipNeedsPhoto.isChecked(),
                    chipExists.isChecked(), chipWlm.isChecked());
            }else{
                chipWlm.setChecked(!isChecked);
            }
        });
    }

    /**
     * Updates Nearby place list according to available chip states
     *
     * @param needsPhoto is chipNeedsPhoto checked
     * @param exists is chipExists checked
     * @param isWlm is chipWlm checked
     */
    private void updatePlaceList(final boolean needsPhoto, final boolean exists,
        final boolean isWlm) {
        final List<Place> updatedPlaces = new ArrayList<>();

        if (needsPhoto) {
            for (final Place place :
                places) {
                if (place.pic.trim().isEmpty() && !updatedPlaces.contains(place)) {
                    updatedPlaces.add(place);
                }
            }
        } else {
            updatedPlaces.addAll(places);
        }

        if (exists) {
            for(final Iterator<Place> placeIterator = updatedPlaces.iterator();
                placeIterator.hasNext();){
                final Place place = placeIterator.next();
                if (!place.exists) {
                    placeIterator.remove();
                }
            }
        }

        if (!isWlm) {
            for (final Place place :
                places) {
                if (place.isMonument() && updatedPlaces.contains(place)) {
                    updatedPlaces.remove(place);
                }
            }
        } else {
            for (final Place place :
                places) {
                if (place.isMonument() && !updatedPlaces.contains(place)) {
                    updatedPlaces.add(place);
                }
            }
        }

        adapter.setItems(updatedPlaces);
        noResultsView.setVisibility(updatedPlaces.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private void setBottomSheetCallbacks() {
        bottomSheetDetailsBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {

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
            public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {

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
    @Override
    public void centerMapToPlace(@Nullable final Place place) {
        Timber.d("Map is centered to place");
        final double cameraShift;
        if(null!=place){
            lastPlaceToCenter=place;
        }

        if (null != lastPlaceToCenter) {
            final Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
            } else {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
            }
            final CameraPosition position = new CameraPosition.Builder()
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
    public void updateListFragment(final List<Place> placeList) {
        places = placeList;
        adapter.setItems(placeList);
        noResultsView.setVisibility(placeList.isEmpty() ? View.VISIBLE : View.GONE);
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
        if (latLngBounds == null || currentLocationMarker==null) {
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
    public boolean isAdvancedQueryFragmentVisible() {
        return isAdvancedQueryFragmentVisible;
    }

    @Override
    public void showHideAdvancedQueryFragment(final boolean shouldShow) {
        setHasOptionsMenu(!shouldShow);
        flConainerNearbyChildren.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        isAdvancedQueryFragmentVisible = shouldShow;
    }

    @Override
    public void centerMapToPosition(fr.free.nrw.commons.location.LatLng searchLatLng) {
        final CameraPosition cameraPosition = mapBox.getCameraPosition();
        if (null != searchLatLng && !(
            cameraPosition.target.getLatitude() == searchLatLng.getLatitude()
                && cameraPosition.target.getLongitude() == searchLatLng.getLongitude())) {
            final CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(searchLatLng))
                .zoom(ZOOM_LEVEL) // Same zoom level
                .build();
            mapBox.setCameraPosition(position);
        }
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
            public void onReceive(final Context context, final Intent intent) {
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
    public void populatePlaces(final fr.free.nrw.commons.location.LatLng curlatLng) {
        if (curlatLng.equals(lastFocusLocation) || lastFocusLocation == null || recenterToUserLocation) { // Means we are checking around current location
            populatePlacesForCurrentLocation(lastKnownLocation, curlatLng, null);
        } else {
            populatePlacesForAnotherLocation(lastKnownLocation, curlatLng, null);
        }
        if(recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    @Override
    public void populatePlaces(final fr.free.nrw.commons.location.LatLng curlatLng,
        @Nullable final String customQuery) {
        if (customQuery == null || customQuery.isEmpty()) {
            populatePlaces(curlatLng);
            return;
        }

        if (curlatLng.equals(lastFocusLocation) || lastFocusLocation == null
            || recenterToUserLocation) { // Means we are checking around current location
            populatePlacesForCurrentLocation(lastKnownLocation, curlatLng, customQuery);
        } else {
            populatePlacesForAnotherLocation(lastKnownLocation, curlatLng, customQuery);
        }
        if (recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    private void populatePlacesForCurrentLocation(
        final fr.free.nrw.commons.location.LatLng curlatLng,
        final fr.free.nrw.commons.location.LatLng searchLatLng, @Nullable final String customQuery){

        final Observable<NearbyController.NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng,
                    false, true, Utils.isMonumentsEnabled(new Date()), customQuery));

        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(nearbyPlacesInfo -> {
                    if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                        showErrorMessage(getString(R.string.no_nearby_places_around));
                    } else {
                        updateMapMarkers(nearbyPlacesInfo, true);
                        lastFocusLocation = searchLatLng;
                    }
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    private void populatePlacesForAnotherLocation(
        final fr.free.nrw.commons.location.LatLng curlatLng,
        final fr.free.nrw.commons.location.LatLng searchLatLng, @Nullable final String customQuery){
        final Observable<NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng,
                    false, true, Utils.isMonumentsEnabled(new Date()), customQuery));

        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(nearbyPlacesInfo -> {
                    if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                        showErrorMessage(getString(R.string.no_nearby_places_around));
                    } else {
                        // Updating last searched location
                        applicationKvStore.putString("LastLocation", searchLatLng.getLatitude() + "," + searchLatLng.getLongitude());
                        updateMapMarkers(nearbyPlacesInfo, false);
                        lastFocusLocation = searchLatLng;
                    }
                },
                throwable -> {
                    Timber.e(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(final NearbyController.NearbyPlacesInfo nearbyPlacesInfo, final boolean shouldUpdateSelectedMarker) {
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
    public void setSearchThisAreaButtonVisibility(final boolean isVisible) {
        if (isVisible) {
            searchThisAreaButton.setVisibility(View.VISIBLE);
        } else {
            searchThisAreaButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isSearchThisAreaButtonVisible() {
        return searchThisAreaButton.getVisibility() == View.VISIBLE;
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
    public void setProgressBarVisibility(final boolean isVisible) {
        if (isVisible) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void setTabItemContributions() {
        ((MainActivity)getActivity()).viewPager.setCurrentItem(0);
        // TODO
    }

    @Override
    public void checkPermissionsAndPerformAction() {
        Timber.d("Checking permission and perfoming action");
        locationPermissionLauncher.launch(new String[]{permission.ACCESS_FINE_LOCATION});
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
    private void expandFABs(final boolean isFABsExpanded){
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
    private void collapseFABs(final boolean isFABsExpanded){
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

    private void handleLocationUpdate(final fr.free.nrw.commons.location.LatLng latLng, final LocationServiceManager.LocationChangeType locationChangeType){
        lastKnownLocation = latLng;
        NearbyController.currentLocation = lastKnownLocation;
        presenter.updateMapAndList(locationChangeType);
    }

    private boolean isUserBrowsing() {
        final boolean isUserBrowsing = lastKnownLocation!=null && !presenter.areLocationsClose(getCameraTarget(), lastKnownLocation);
        return isUserBrowsing;
    }

    @Override
    public void onLocationChangedSignificantly(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location significantly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {
            handleLocationUpdate(latLng,LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedSlightly(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location slightly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng,LOCATION_SLIGHTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedMedium(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location changed medium");
        if (isMapBoxReady && latLng != null && !isUserBrowsing()) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng, LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    public boolean backButtonClicked() {
        return presenter.backButtonClicked();
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are cleared.
     */
    private class BaseLogoutListener implements CommonsApplication.LogoutListener {
        @Override
        public void onLogoutComplete() {
            Timber.d("Logout complete callback received.");
            final Intent nearbyIntent = new Intent( getActivity(), LoginActivity.class);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(nearbyIntent);
            getActivity().finish();
        }
    }

    @Override
    public void setFABPlusAction(final View.OnClickListener onClickListener) {
        fabPlus.setOnClickListener(onClickListener);
    }

    @Override
    public void setFABRecenterAction(final View.OnClickListener onClickListener) {
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
    public void addCurrentLocationMarker(final fr.free.nrw.commons.location.LatLng curLatLng) {
        if (null != curLatLng && !isPermissionDenied && locationManager.isGPSProviderEnabled()) {
            ExecutorUtils.get().submit(() -> {
                mapView.post(() -> removeCurrentLocationMarker());
                Timber.d("Adds current location marker");

                final Icon icon = IconFactory.getInstance(getContext())
                        .fromResource(R.drawable.current_location_marker);

                final MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                        .position(new LatLng(curLatLng.getLatitude(),
                                curLatLng.getLongitude()));
                currentLocationMarkerOptions.setIcon(icon); // Set custom icon
                mapView.post(
                    () -> currentLocationMarker = mapBox.addMarker(currentLocationMarkerOptions));

                final List<LatLng> circle = UiUtils
                        .createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                                curLatLng.getAccuracy() * 2, 100);

                final PolygonOptions currentLocationPolygonOptions = new PolygonOptions()
                        .addAll(circle)
                        .strokeColor(getResources().getColor(R.color.current_marker_stroke))
                        .fillColor(getResources().getColor(R.color.current_marker_fill));
                mapView.post(
                    () -> currentLocationPolygon = mapBox
                        .addPolygon(currentLocationPolygonOptions));
            });
        } else {
            Timber.d("not adding current location marker..current location is null");
        }
    }

    private void removeCurrentLocationMarker() {
        if (currentLocationMarker != null && mapBox!=null) {
            mapBox.removeMarker(currentLocationMarker);
            if (currentLocationPolygon != null) {
                mapBox.removePolygon(currentLocationPolygon);
            }
        }
    }


    /**
     * Makes map camera follow users location with animation
     * @param curLatLng current location of user
     */
    @Override
    public void updateMapToTrackPosition(final fr.free.nrw.commons.location.LatLng curLatLng) {
        Timber.d("Updates map camera to track user position");
        final CameraPosition cameraPosition;
        if(isPermissionDenied){
            cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        }else{
            cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng))
                .zoom(ZOOM_LEVEL).build();
        }
        if(null!=mapBox) {
            mapBox.setCameraPosition(cameraPosition);
            mapBox.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition), 1000);
        }
    }

    @Override
    public void updateMapMarkers(final List<NearbyBaseMarker> nearbyBaseMarkers, final Marker selectedMarker) {
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
        hideAllMarkers();
    }

    /**
     * Displays all markers
     */
    @Override
    public void displayAllMarkers() {
        for (final MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
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
    public void filterMarkersByLabels(final List<Label> selectedLabels,
        final boolean displayExists,
        final boolean displayNeedsPhoto,
        final boolean displayWlm,
        final boolean filterForPlaceState,
        final boolean filterForAllNoneType) {

        // Remove the previous markers before updating them
        hideAllMarkers();

        filteredMarkers = new ArrayList<>();

        for (final MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
            final Place place = markerPlaceGroup.getPlace();

            // When label filter is engaged
            // then compare it against place's label
            if (selectedLabels != null && (selectedLabels.size() != 0 || !filterForPlaceState)
                && (!selectedLabels.contains(place.getLabel())
                    && !(selectedLabels.contains(Label.BOOKMARKS) && markerPlaceGroup.getIsBookmarked()))) {
                continue;
            }

            if (!displayWlm && place.isMonument()) {
                continue;
            }

            boolean shouldUpdateMarker=false;

            if (displayWlm && place.isMonument()) {
                shouldUpdateMarker=true;
            }
            else if (displayExists && displayNeedsPhoto) {
                // Exists and needs photo
                if (place.exists && place.pic.trim().isEmpty()) {
                    shouldUpdateMarker=true;
                }
            } else if (displayExists && !displayNeedsPhoto) {
                // Exists and all included needs and doesn't needs photo
                if (place.exists) {
                    shouldUpdateMarker=true;
                }
            } else if (!displayExists && displayNeedsPhoto) {
                // All and only needs photo
                if (place.pic.trim().isEmpty()) {
                    shouldUpdateMarker=true;
                }
            } else if (!displayExists && !displayNeedsPhoto) {
                // all
                shouldUpdateMarker=true;
            }

            if(shouldUpdateMarker){
                updateMarker(markerPlaceGroup.getIsBookmarked(), place, NearbyController.currentLocation);
            }
        }

        mapBox.clear();
        mapBox.addMarkers(filteredMarkers);
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
    public void updateMarker(final boolean isBookmarked, final Place place, @Nullable final fr.free.nrw.commons.location.LatLng curLatLng) {
        VectorDrawableCompat vectorDrawable = VectorDrawableCompat.create(
            getContext().getResources(), getIconFor(place, isBookmarked), getContext().getTheme());

        if(curLatLng != null) {
            for (NearbyBaseMarker nearbyMarker : allMarkers) {
                if (nearbyMarker.getMarker().getTitle() != null && nearbyMarker.getMarker().getTitle().equals(place.getName())) {

                    final Bitmap icon = UiUtils.getBitmap(vectorDrawable);

                    final String distance = formatDistanceBetween(curLatLng, place.location);
                    place.setDistance(distance);

                    final NearbyBaseMarker nearbyBaseMarker = new NearbyBaseMarker();
                    nearbyBaseMarker.title(place.name);
                    nearbyBaseMarker.position(
                        new com.mapbox.mapboxsdk.geometry.LatLng(
                            place.location.getLatitude(),
                            place.location.getLongitude()));
                    nearbyBaseMarker.place(place);
                    nearbyBaseMarker.icon(IconFactory.getInstance(getContext())
                        .fromBitmap(icon));
                    nearbyMarker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
                    filteredMarkers.add(nearbyBaseMarker);
                }
            }
        } else {
            for (Marker marker : mapBox.getMarkers()) {
                if (marker.getTitle() != null && marker.getTitle().equals(place.getName())) {

                    final Bitmap icon = UiUtils.getBitmap(vectorDrawable);
                    marker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
                }
            }
        }
    }

    private @DrawableRes int getIconFor(Place place, Boolean isBookmarked) {
        if(place.isMonument()){
            return R.drawable.ic_custom_map_marker_monuments;
        }
        else if (!place.pic.trim().isEmpty()) {
            return (isBookmarked ?
                R.drawable.ic_custom_map_marker_green_bookmarked :
                R.drawable.ic_custom_map_marker_green);
        } else if (!place.exists) { // Means that the topic of the Wikidata item does not exist in the real world anymore, for instance it is a past event, or a place that was destroyed
            return (isBookmarked ?
                R.drawable.ic_custom_map_marker_grey_bookmarked :
                R.drawable.ic_custom_map_marker_grey);
        } else {
            return (isBookmarked ?
                R.drawable.ic_custom_map_marker_blue_bookmarked :
                R.drawable.ic_custom_map_marker);
        }
    }

    /**
     * Removes all markers except current location marker, an icon has been used
     * but it is transparent more than grey(as the name of the icon might suggest)
     * since grey icon may lead the users to believe that it is disabled or prohibited contribution
     */
    private void hideAllMarkers() {
        final VectorDrawableCompat vectorDrawable;
        vectorDrawable = VectorDrawableCompat.create(
                getContext().getResources(), R.drawable.ic_custom_greyed_out_marker, getContext().getTheme());
        final Bitmap icon = UiUtils.getBitmap(vectorDrawable);
        for (final Marker marker : mapBox.getMarkers()) {
            if (!marker.equals(currentLocationMarker)) {
                marker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
            }
        }
        addCurrentLocationMarker(NearbyController.currentLocation);
    }

    private void addNearbyMarkersToMapBoxMap(final List<NearbyBaseMarker> nearbyBaseMarkers, final Marker selectedMarker) {
        if (isMapBoxReady && mapBox != null) {
            allMarkers = new ArrayList<>(nearbyBaseMarkers);
            mapBox.addMarkers(nearbyBaseMarkers);
            setMapMarkerActions(selectedMarker);
            presenter.updateMapMarkersToController(nearbyBaseMarkers);
        }
    }

    private void setMapMarkerActions(final Marker selectedMarker) {
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
        if (isPermissionDenied || curLatLng == null) {
            recenterToUserLocation = true;
            checkPermissionsAndPerformAction();
            if (!isPermissionDenied && !(locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled())) {
                showLocationOffDialog();
            }
            return;
        }
        addCurrentLocationMarker(curLatLng);
        final CameraPosition position;

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
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = getActivity().getPackageManager();

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
    public void displayBottomSheetWithInfo(final Marker marker) {
        selectedMarker = marker;
        final NearbyMarker nearbyMarker = (NearbyMarker) marker;
        final Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
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
    public void prepareViewsForSheetPosition(final int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                collapseFABs(isFABsExpanded);
                if (!fabPlus.isShown()) {
                    showFABs();
                }
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                if (null != mapBox) {
                    mapBox.deselectMarkers();
                }
                transparentView.setClickable(false);
                transparentView.setAlpha(0);
                collapseFABs(isFABsExpanded);
                hideFABs();
                break;
        }
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     * @param place Place of clicked nearby marker
     */
    private void passInfoToSheet(final Place place) {
        selectedPlace = place;
        updateBookmarkButtonImage(selectedPlace);

        bookmarkButton.setOnClickListener(view -> {
            final boolean isBookmarked = bookmarkLocationDao.updateBookmarkLocation(selectedPlace);
            updateBookmarkButtonImage(selectedPlace);
            updateMarker(isBookmarked, selectedPlace, locationManager.getLastLocation());
        });

        wikipediaButton.setVisibility(place.hasWikipediaLink()?View.VISIBLE:View.GONE);
        wikipediaButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getWikipediaLink()));

        wikidataButton.setVisibility(place.hasWikidataLink()?View.VISIBLE:View.GONE);
        wikidataButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getWikidataLink()));

        directionsButton.setOnClickListener(view -> Utils.handleGeoCoordinates(getActivity(),
            selectedPlace.getLocation()));

        commonsButton.setVisibility(selectedPlace.hasCommonsLink()?View.VISIBLE:View.GONE);
        commonsButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getCommonsLink()));

        icon.setImageResource(selectedPlace.getLabel().getIcon());

        title.setText(selectedPlace.name);
        distance.setText(selectedPlace.distance);
        // Remove label since it is double information
        String descriptionText = selectedPlace.getLongDescription()
            .replace(selectedPlace.getName() + " (","");
        descriptionText = (descriptionText.equals(selectedPlace.getLongDescription()) ? descriptionText : descriptionText.replaceFirst(".$",""));
        // Set the short description after we remove place name from long description
        description.setText(descriptionText);

        fabCamera.setOnClickListener(view -> {
            if (fabCamera.isShown()) {
                Timber.d("Camera button tapped. Place: %s", selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateCameraPick(getActivity(), inAppCameraLocationPermissionLauncher);
            }
        });

        fabGallery.setOnClickListener(view -> {
            if (fabGallery.isShown()) {
                Timber.d("Gallery button tapped. Place: %s", selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateGalleryPick(getActivity(), chipWlm.isChecked());
            }
        });
    }

    private void storeSharedPrefs(final Place selectedPlace) {
        applicationKvStore.putJson(PLACE_OBJECT, selectedPlace);
        Place place = applicationKvStore.getJson(PLACE_OBJECT, Place.class);

        Timber.d("Stored place object %s", place.toString());
    }

    private void updateBookmarkButtonImage(final Place place) {
        final int bookmarkIcon;
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
    public void onAttach(final Context context) {
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

    private void showErrorMessage(final String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    public void registerUnregisterLocationListener(final boolean removeLocationListener) {
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
        }catch (final Exception e){
            Timber.e(e);
            //Broadcasts are tricky, should be catchedonR
        }
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
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

    public interface  NearbyParentFragmentInstanceReadyCallback{
        void onReady();
    }

    public void setNearbyParentFragmentInstanceReadyCallback(NearbyParentFragmentInstanceReadyCallback nearbyParentFragmentInstanceReadyCallback) {
        this.nearbyParentFragmentInstanceReadyCallback = nearbyParentFragmentInstanceReadyCallback;
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.LayoutParams rlBottomSheetLayoutParams = rlBottomSheet.getLayoutParams();
        rlBottomSheetLayoutParams.height = getActivity().getWindowManager().getDefaultDisplay().getHeight() / 16 * 9;
        rlBottomSheet.setLayoutParams(rlBottomSheetLayoutParams);
        }

    @OnClick(R.id.tv_learn_more)
    public void onLearnMoreClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(WLM_URL));
        startActivity(intent);
    }

    @OnClick(R.id.iv_toggle_chips)
    public void onToggleChipsClicked() {
        if (llContainerChips.getVisibility() == View.VISIBLE) {
            llContainerChips.setVisibility(View.GONE);
        } else {
            llContainerChips.setVisibility(View.VISIBLE);
        }
        ivToggleChips.setRotation(ivToggleChips.getRotation() + 180);
    }
}
