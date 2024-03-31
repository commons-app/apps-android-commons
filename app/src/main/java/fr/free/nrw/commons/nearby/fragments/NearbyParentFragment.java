package fr.free.nrw.commons.nearby.fragments;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.CUSTOM_QUERY;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding3.appcompat.RxSearchView;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.CommonsApplication.BaseLogoutListener;
import fr.free.nrw.commons.MapController.NearbyPlacesInfo;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment;
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.fragments.AdvanceQueryFragment.Callback;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ExecutorUtils;
import fr.free.nrw.commons.utils.LayoutUtils;
import fr.free.nrw.commons.utils.NearbyFABUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.constants.GeoConstants;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.ScaleDiskOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import timber.log.Timber;


public class NearbyParentFragment extends CommonsDaggerSupportFragment
    implements NearbyParentFragmentContract.View,
    WikidataEditListener.WikidataP18EditListener, LocationUpdateListener {


    FragmentNearbyParentBinding binding;

    @Inject
    LocationServiceManager locationManager;
    @Inject
    NearbyController nearbyController;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;
    @Inject
    BookmarkLocationsDao bookmarkLocationDao;
    @Inject
    ContributionController controller;
    @Inject
    WikidataEditListener wikidataEditListener;
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
    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private View view;
    private NearbyParentFragmentPresenter presenter;
    private boolean isDarkTheme;
    private boolean isFABsExpanded;
    private Place selectedPlace;
    private Place clickedMarkerPlace;
    private boolean isClickedMarkerBookmarked;
    private ProgressDialog progressDialog;
    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;
    private boolean isPermissionDenied;
    private boolean recenterToUserLocation;
    private GeoPoint mapCenter;
    IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);
    private Place lastPlaceToCenter;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation;
    private boolean isVisibleToUser;
    private fr.free.nrw.commons.location.LatLng lastFocusLocation;
    private PlaceAdapter adapter;
    private GeoPoint lastMapFocus;
    private NearbyParentFragmentInstanceReadyCallback nearbyParentFragmentInstanceReadyCallback;
    private boolean isAdvancedQueryFragmentVisible = false;
    private Place nearestPlace;
    private ActivityResultLauncher<String[]> inAppCameraLocationPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        new ActivityResultCallback<Map<String, Boolean>>() {
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
                        controller.locationPermissionCallback.onLocationPermissionDenied(
                            getActivity().getString(
                                R.string.in_app_camera_location_permission_denied));
                    }
                }
            }
        });

    private ActivityResultLauncher<String> locationPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                locationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    DialogUtil.showAlertDialog(getActivity(),
                        getActivity().getString(R.string.location_permission_title),
                        getActivity().getString(R.string.location_permission_rationale_nearby),
                        getActivity().getString(android.R.string.ok),
                        getActivity().getString(android.R.string.cancel),
                        () -> {
                            if (!(locationManager.isNetworkProviderEnabled()
                                || locationManager.isGPSProviderEnabled())) {
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
        });

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
        binding = FragmentNearbyParentBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        initNetworkBroadCastReceiver();
        presenter = new NearbyParentFragmentPresenter(bookmarkLocationDao);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Saving in progress...");
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu,
        @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.nearby_fragment_menu, menu);
        MenuItem listMenu = menu.findItem(R.id.list_sheet);
        MenuItem saveAsGPXButton = menu.findItem(R.id.list_item_gpx);
        MenuItem saveAsKMLButton = menu.findItem(R.id.list_item_kml);
        listMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                listOptionMenuItemClicked();
                return false;
            }
        });
        saveAsGPXButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                try {
                    progressDialog.setTitle(getString(R.string.saving_gpx_file));
                    progressDialog.show();
                    savePlacesAsGPX();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        });
        saveAsKMLButton.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(@NonNull MenuItem item) {
                try {
                    progressDialog.setTitle(getString(R.string.saving_kml_file));
                    progressDialog.show();
                    savePlacesAsKML();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return false;
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        if (Utils.isMonumentsEnabled(new Date())) {
            binding.rlContainerWlmMonthMessage.setVisibility(View.VISIBLE);
        } else {
            binding.rlContainerWlmMonthMessage.setVisibility(View.GONE);
        }

        presenter.attachView(this);
        isPermissionDenied = false;
        recenterToUserLocation = false;
        initThemePreferences();
        initViews();
        presenter.setActionListeners(applicationKvStore);
        org.osmdroid.config.Configuration.getInstance().load(this.getContext(),
            PreferenceManager.getDefaultSharedPreferences(this.getContext()));

        // Use the Wikimedia tile server, rather than OpenStreetMap (Mapnik) which has various
        // restrictions that we do not satisfy.
        binding.map.setTileSource(TileSourceFactory.WIKIMEDIA);
        binding.map.setTilesScaledToDpi(true);

        // Add referer HTTP header because the Wikimedia tile server requires it.
        // This was suggested by Dmitry Brant within an email thread between us and WMF.
        org.osmdroid.config.Configuration.getInstance().getAdditionalHttpRequestProperties().put(
            "Referer", "http://maps.wikimedia.org/"
        );

        if (applicationKvStore.getString("LastLocation")
            != null) { // Checking for last searched location
            String[] locationLatLng = applicationKvStore.getString("LastLocation").split(",");
            lastMapFocus = new GeoPoint(Double.valueOf(locationLatLng[0]),
                Double.valueOf(locationLatLng[1]));
        } else {
            lastMapFocus = new GeoPoint(51.50550, -0.07520);
        }
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(binding.map);
        scaleBarOverlay.setScaleBarOffset(15, 25);
        Paint barPaint = new Paint();
        barPaint.setARGB(200, 255, 250, 250);
        scaleBarOverlay.setBackgroundPaint(barPaint);
        scaleBarOverlay.enableScaleBar();
        binding.map.getOverlays().add(scaleBarOverlay);
        binding.map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.map.getController().setZoom(ZOOM_LEVEL);
        binding.map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (clickedMarkerPlace != null) {
                    removeMarker(clickedMarkerPlace);
                    addMarkerToMap(clickedMarkerPlace, isClickedMarkerBookmarked);
                } else {
                    Timber.e("CLICKED MARKER IS NULL");
                }
                if (isListBottomSheetExpanded()) {
                    // Back should first hide the bottom sheet if it is expanded
                    hideBottomSheet();
                } else if (isDetailsBottomSheetVisible()) {
                    hideBottomDetailsSheet();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        binding.map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (lastMapFocus != null) {
                    Location mylocation = new Location("");
                    Location dest_location = new Location("");
                    dest_location.setLatitude(binding.map.getMapCenter().getLatitude());
                    dest_location.setLongitude(binding.map.getMapCenter().getLongitude());
                    mylocation.setLatitude(lastMapFocus.getLatitude());
                    mylocation.setLongitude(lastMapFocus.getLongitude());
                    Float distance = mylocation.distanceTo(dest_location);//in meters
                    if (lastMapFocus != null) {
                        if (isNetworkConnectionEstablished() && (event.getX() > 0
                            || event.getY() > 0)) {
                            if (distance > 2000.0) {
                                setSearchThisAreaButtonVisibility(true);
                            } else {
                                setSearchThisAreaButtonVisibility(false);
                            }
                        }
                    } else {
                        setSearchThisAreaButtonVisibility(false);
                    }
                }

                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }

        });

        binding.map.setMultiTouchControls(true);
        if (nearbyParentFragmentInstanceReadyCallback != null) {
            nearbyParentFragmentInstanceReadyCallback.onReady();
        }
        initNearbyFilter();
        addCheckBoxCallback();
        performMapReadyActions();
        moveCameraToPosition(lastMapFocus);
        initRvNearbyList();
        onResume();
         binding.tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
         binding.tvAttribution.setMovementMethod(LinkMovementMethod.getInstance());
        binding.nearbyFilterList.btnAdvancedOptions.setOnClickListener(v -> {
              binding.nearbyFilter.searchViewLayout.searchView.clearFocus();
            showHideAdvancedQueryFragment(true);
            final AdvanceQueryFragment fragment = new AdvanceQueryFragment();
            final Bundle bundle = new Bundle();
            try {
                bundle.putString("query",
                    FileUtils.readFromResource("/queries/radius_query_for_upload_wizard.rq"));
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

        binding.tvLearnMore.setOnClickListener(v ->onLearnMoreClicked());
        binding.nearbyFilter.ivToggleChips.setOnClickListener(v -> onToggleChipsClicked());
    }

    /**
     * Initialise background based on theme, this should be doe ideally via styles, that would need
     * another refactor
     */
    private void initThemePreferences() {
        if (isDarkTheme) {
            binding.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                getContext().getResources().getColor(R.color.contributionListDarkBackground));
            binding.nearbyFilterList.checkboxTriStates.setTextColor(
                getContext().getResources().getColor(android.R.color.white));
            binding.nearbyFilterList.checkboxTriStates.setTextColor(
                getContext().getResources().getColor(android.R.color.white));
            binding.nearbyFilterList.getRoot().setBackgroundColor(
                getContext().getResources().getColor(R.color.contributionListDarkBackground));
            binding.map.getOverlayManager().getTilesOverlay()
                .setColorFilter(TilesOverlay.INVERT_COLORS);
        } else {
            binding.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                getContext().getResources().getColor(android.R.color.white));
            binding.nearbyFilterList.checkboxTriStates.setTextColor(
                getContext().getResources().getColor(R.color.contributionListDarkBackground));
            binding.nearbyFilterList.getRoot().setBackgroundColor(
                getContext().getResources().getColor(android.R.color.white));
            binding.nearbyFilterList.getRoot().setBackgroundColor(
                getContext().getResources().getColor(android.R.color.white));
        }
    }

    private void initRvNearbyList() {
        binding.bottomSheetNearby.rvNearbyList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaceAdapter(bookmarkLocationDao,
            place -> {
                moveCameraToPosition(
                    new GeoPoint(place.location.getLatitude(), place.location.getLongitude()));
                return Unit.INSTANCE;
            },
            (place, isBookmarked) -> {
                updateMarker(isBookmarked, place, null);
                binding.map.invalidate();
                return Unit.INSTANCE;
            },
            commonPlaceClickActions,
            inAppCameraLocationPermissionLauncher
        );
        binding.bottomSheetNearby.rvNearbyList.setAdapter(adapter);
    }

    private void addCheckBoxCallback() {
        binding.nearbyFilterList.checkboxTriStates.setCallback(
            (o, state, b, b1) -> presenter.filterByMarkerType(o, state, b, b1));
    }

    private void performMapReadyActions() {
        if (((MainActivity) getActivity()).activeFragment == ActiveFragment.NEARBY) {
            if (!applicationKvStore.getBoolean("doNotAskForLocationPermission", false) ||
                PermissionUtils.hasPermission(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) {
                checkPermissionsAndPerformAction();
            } else {
                isPermissionDenied = true;
            }
        }
    }

    private void locationPermissionGranted() {
        isPermissionDenied = false;
        applicationKvStore.putBoolean("doNotAskForLocationPermission", false);
        lastKnownLocation = locationManager.getLastLocation();
        fr.free.nrw.commons.location.LatLng target = lastKnownLocation;
        if (lastKnownLocation != null) {
            GeoPoint targetP = new GeoPoint(target.getLatitude(), target.getLongitude());
            mapCenter = targetP;
            binding.map.getController().setCenter(targetP);
            recenterMarkerToPosition(targetP);
            moveCameraToPosition(targetP);
        } else if (locationManager.isGPSProviderEnabled()
            || locationManager.isNetworkProviderEnabled()) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            setProgressBarVisibility(true);
        } else {
            Toast.makeText(getContext(), getString(R.string.nearby_location_not_available),
                Toast.LENGTH_LONG).show();
        }
        presenter.onMapReady();
        registerUnregisterLocationListener(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed() && ((MainActivity) getActivity()).activeFragment == ActiveFragment.NEARBY) {
            if (!isPermissionDenied && !applicationKvStore.getBoolean(
                "doNotAskForLocationPermission", false)) {
                if (!locationManager.isGPSProviderEnabled()) {
                    startMapWithCondition("Without GPS");
                } else {
                    startTheMap();
                }
            } else {
                startMapWithCondition("Without Permission");
            }
        }
    }

    /**
     * Starts the map without GPS and without permission By default it points to 51.50550,-0.07520
     * coordinates, other than that it points to the last known location which can be get by the key
     * "LastLocation" from applicationKvStore
     *
     * @param condition : for which condition the map should start
     */
    private void startMapWithCondition(final String condition) {
        if (condition.equals("Without Permission")) {
            applicationKvStore.putBoolean("doNotAskForLocationPermission", true);
        }
        if (applicationKvStore.getString("LastLocation") != null) {
            final String[] locationLatLng
                = applicationKvStore.getString("LastLocation").split(",");
            lastKnownLocation
                = new fr.free.nrw.commons.location.LatLng(Double.parseDouble(locationLatLng[0]),
                Double.parseDouble(locationLatLng[1]), 1f);
        } else {
            lastKnownLocation = new fr.free.nrw.commons.location.LatLng(51.50550,
                -0.07520, 1f);
        }
        if (binding.map != null) {
            recenterMap(lastKnownLocation);
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
        binding.map.onPause();
        compositeDisposable.clear();
        presenter.detachView();
        registerUnregisterLocationListener(true);
        try {
            if (broadcastReceiver != null && getActivity() != null) {
                getContext().unregisterReceiver(broadcastReceiver);
            }

            if (locationManager != null && presenter != null) {
                locationManager.removeLocationListener(presenter);
                locationManager.unregisterLocationManager();
            }
        } catch (final Exception e) {
            Timber.e(e);
            //Broadcast receivers should always be unregistered inside catch, you never know if they were already registered or not
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
            NearbyFilterState.setWlmSelected(false);
            binding.nearbyFilter.chipView.choiceChipWlm.setVisibility(View.GONE);
        }
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheets, sets initial states and visibility b)
     * Gets the touch event on the map to perform following actions: if fab is open then close fab.
     * if bottom sheet details are expanded then collapse bottom sheet details. if bottom sheet
     * details are collapsed then hide the bottom sheet details. if listBottomSheet is open then
     * hide the list bottom sheet.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initBottomSheets() {
        bottomSheetListBehavior = BottomSheetBehavior.from(binding.bottomSheetNearby.bottomSheet);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(binding.bottomSheetDetails.getRoot());
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomSheetDetails.getRoot().setVisibility(View.VISIBLE);
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void initNearbyFilter() {
        binding.nearbyFilterList.getRoot().setVisibility(View.GONE);
        hideBottomSheet();
          binding.nearbyFilter.searchViewLayout.searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            LayoutUtils.setLayoutHeightAllignedToWidth(1.25, binding.nearbyFilterList.getRoot());
            if (hasFocus) {
                binding.nearbyFilterList.getRoot().setVisibility(View.VISIBLE);
                presenter.searchViewGainedFocus();
            } else {
                binding.nearbyFilterList.getRoot().setVisibility(View.GONE);
            }
        });
          binding.nearbyFilterList.searchListView.setHasFixedSize(true);
          binding.nearbyFilterList.searchListView.addItemDecoration(new DividerItemDecoration(getContext(),
            DividerItemDecoration.VERTICAL));
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
          binding.nearbyFilterList.searchListView.setLayoutManager(linearLayoutManager);
        nearbyFilterSearchRecyclerViewAdapter = new NearbyFilterSearchRecyclerViewAdapter(
            getContext(), new ArrayList<>(Label.valuesAsList()),   binding.nearbyFilterList.searchListView);
        nearbyFilterSearchRecyclerViewAdapter.setCallback(
            new NearbyFilterSearchRecyclerViewAdapter.Callback() {
                @Override
                public void setCheckboxUnknown() {
                    presenter.setCheckboxUnknown();
                }

                @Override
                public void filterByMarkerType(final ArrayList<Label> selectedLabels, final int i,
                    final boolean b, final boolean b1) {
                    presenter.filterByMarkerType(selectedLabels, i, b, b1);
                }

                @Override
                public boolean isDarkTheme() {
                    return isDarkTheme;
                }
            });
        binding.nearbyFilterList.getRoot().getLayoutParams().width = (int) LayoutUtils.getScreenWidth(getActivity(),
            0.75);
          binding.nearbyFilterList.searchListView.setAdapter(nearbyFilterSearchRecyclerViewAdapter);
        LayoutUtils.setLayoutHeightAllignedToWidth(1.25, binding.nearbyFilterList.getRoot());
        compositeDisposable.add(RxSearchView.queryTextChanges(  binding.nearbyFilter.searchViewLayout.searchView)
            .takeUntil(RxView.detaches(binding.nearbyFilter.searchViewLayout.searchView))
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(query -> {
                ((NearbyFilterSearchRecyclerViewAdapter)   binding.nearbyFilterList.searchListView.getAdapter()).getFilter()
                    .filter(query.toString());
            }));
        initFilterChips();
    }

    @Override
    public void setCheckBoxAction() {
        binding.nearbyFilterList.checkboxTriStates.addAction();
        binding.nearbyFilterList.checkboxTriStates.setState(CheckBoxTriStates.UNKNOWN);
    }

    @Override
    public void setCheckBoxState(final int state) {
        binding.nearbyFilterList.checkboxTriStates.setState(state);
    }

    @Override
    public void setFilterState() {
         binding.nearbyFilter.chipView.choiceChipNeedsPhoto.setChecked(NearbyFilterState.getInstance().isNeedPhotoSelected());
         binding.nearbyFilter.chipView.choiceChipExists.setChecked(NearbyFilterState.getInstance().isExistsSelected());
         binding.nearbyFilter.chipView.choiceChipWlm.setChecked(NearbyFilterState.getInstance().isWlmSelected());
        if (NearbyController.currentLocation != null) {
            presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels,
                binding.nearbyFilterList.checkboxTriStates.getState(), true, false);
        }
    }

    private void initFilterChips() {
         binding.nearbyFilter.chipView.choiceChipNeedsPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                binding.nearbyFilterList.checkboxTriStates.setState(CheckBoxTriStates.CHECKED);
                NearbyFilterState.setNeedPhotoSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels,
                    binding.nearbyFilterList.checkboxTriStates.getState(), true, true);
                updatePlaceList( binding.nearbyFilter.chipView.choiceChipNeedsPhoto.isChecked(),
                     binding.nearbyFilter.chipView.choiceChipExists.isChecked(),  binding.nearbyFilter.chipView.choiceChipWlm.isChecked());
            } else {
                 binding.nearbyFilter.chipView.choiceChipNeedsPhoto.setChecked(!isChecked);
            }
        });

         binding.nearbyFilter.chipView.choiceChipExists.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                binding.nearbyFilterList.checkboxTriStates.setState(CheckBoxTriStates.CHECKED);
                NearbyFilterState.setExistsSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels,
                    binding.nearbyFilterList.checkboxTriStates.getState(), true, true);
                updatePlaceList( binding.nearbyFilter.chipView.choiceChipNeedsPhoto.isChecked(),
                     binding.nearbyFilter.chipView.choiceChipExists.isChecked(),  binding.nearbyFilter.chipView.choiceChipWlm.isChecked());
            } else {
                 binding.nearbyFilter.chipView.choiceChipExists.setChecked(!isChecked);
            }
        });

         binding.nearbyFilter.chipView.choiceChipWlm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (NearbyController.currentLocation != null) {
                binding.nearbyFilterList.checkboxTriStates.setState(CheckBoxTriStates.CHECKED);
                NearbyFilterState.setWlmSelected(isChecked);
                presenter.filterByMarkerType(nearbyFilterSearchRecyclerViewAdapter.selectedLabels,
                    binding.nearbyFilterList.checkboxTriStates.getState(), true, true);
                updatePlaceList( binding.nearbyFilter.chipView.choiceChipNeedsPhoto.isChecked(),
                     binding.nearbyFilter.chipView.choiceChipExists.isChecked(),  binding.nearbyFilter.chipView.choiceChipWlm.isChecked());
            } else {
                 binding.nearbyFilter.chipView.choiceChipWlm.setChecked(!isChecked);
            }
        });
    }

    /**
     * Updates Nearby place list according to available chip states
     *
     * @param needsPhoto is chipNeedsPhoto checked
     * @param exists     is chipExists checked
     * @param isWlm      is chipWlm checked
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
            for (final Iterator<Place> placeIterator = updatedPlaces.iterator();
                placeIterator.hasNext(); ) {
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
         binding.bottomSheetNearby.noResultsMessage.setVisibility(updatedPlaces.isEmpty() ? View.VISIBLE : View.GONE);
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

        binding.bottomSheetDetails.getRoot().setOnClickListener(v -> {
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetDetailsBehavior.getState()
                == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        binding.bottomSheetNearby.bottomSheet.getLayoutParams().height = getActivity().getWindowManager()
            .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetListBehavior = BottomSheetBehavior.from(binding.bottomSheetNearby.bottomSheet);
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
        if ( binding.bottomSheetDetails.directionsButtonText.getLineCount() > 1 ||  binding.bottomSheetDetails.directionsButtonText.getLineCount() == 0) {
            binding.bottomSheetDetails.wikipediaButtonText.setVisibility(View.GONE);
            binding.bottomSheetDetails.wikidataButtonText.setVisibility(View.GONE);
            binding.bottomSheetDetails.commonsButtonText.setVisibility(View.GONE);
             binding.bottomSheetDetails.directionsButtonText.setVisibility(View.GONE);
        }
    }

    /**
     *
     */
    private void addActionToTitle() {
        binding.bottomSheetDetails.title.setOnLongClickListener(view -> {
            Utils.copy("place", binding.bottomSheetDetails.title.getText().toString(), getContext());
            Toast.makeText(getContext(), R.string.text_copy, Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.bottomSheetDetails.title.setOnClickListener(view -> {
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    /**
     * Centers the map in nearby fragment to a given place and updates nearestPlace
     *
     * @param place is new center of the map
     */
    @Override
    public void centerMapToPlace(@Nullable final Place place) {
        Timber.d("Map is centered to place");
        final double cameraShift;
        if (null != place) {
            lastPlaceToCenter = place;
            nearestPlace = place;
        }

        if (null != lastPlaceToCenter) {
            final Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
            } else {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
            }
            recenterMap(new fr.free.nrw.commons.location.LatLng(
                lastPlaceToCenter.location.getLatitude() - cameraShift,
                lastPlaceToCenter.getLocation().getLongitude(), 0));
        }
    }


    @Override
    public void updateListFragment(final List<Place> placeList) {
        places = placeList;
        adapter.setItems(placeList);
         binding.bottomSheetNearby.noResultsMessage.setVisibility(placeList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getLastLocation() {
        return lastKnownLocation;
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getLastMapFocus() {
        fr.free.nrw.commons.location.LatLng latLng = new fr.free.nrw.commons.location.LatLng(
            lastMapFocus.getLatitude(), lastMapFocus.getLongitude(), 100);
        return latLng;
    }

    /**
     * Computes location where map should be centered
     *
     * @return returns the last location, if available, else returns default location
     */
    @Override
    public fr.free.nrw.commons.location.LatLng getMapCenter() {
        if (applicationKvStore.getString("LastLocation") != null) {
            final String[] locationLatLng
                = applicationKvStore.getString("LastLocation").split(",");
            lastKnownLocation
                = new fr.free.nrw.commons.location.LatLng(Double.parseDouble(locationLatLng[0]),
                Double.parseDouble(locationLatLng[1]), 1f);
        } else {
            lastKnownLocation = new fr.free.nrw.commons.location.LatLng(51.50550,
                -0.07520, 1f);
        }
        fr.free.nrw.commons.location.LatLng latLnge = lastKnownLocation;
        if (mapCenter != null) {
            latLnge = new fr.free.nrw.commons.location.LatLng(
                mapCenter.getLatitude(), mapCenter.getLongitude(), 100);
        }
        return latLnge;
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getMapFocus() {
        fr.free.nrw.commons.location.LatLng mapFocusedLatLng = new fr.free.nrw.commons.location.LatLng(
            binding.map.getMapCenter().getLatitude(), binding.map.getMapCenter().getLongitude(), 100);
        return mapFocusedLatLng;
    }

    @Override
    public boolean isAdvancedQueryFragmentVisible() {
        return isAdvancedQueryFragmentVisible;
    }

    @Override
    public void showHideAdvancedQueryFragment(final boolean shouldShow) {
        setHasOptionsMenu(!shouldShow);
        binding.flContainerNearbyChildren.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        isAdvancedQueryFragmentVisible = shouldShow;
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
                            snackbar = Snackbar.make(view, R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE);
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
        if (bottomSheetListBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED
            || bottomSheetListBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else if (bottomSheetListBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    public void populatePlaces(final fr.free.nrw.commons.location.LatLng currentLatLng) {
        IGeoPoint screenTopRight = binding.map.getProjection().fromPixels(binding.map.getWidth(), 0);
        IGeoPoint screenBottomLeft = binding.map.getProjection().fromPixels(0, binding.map.getHeight());
        fr.free.nrw.commons.location.LatLng screenTopRightLatLng = new fr.free.nrw.commons.location.LatLng(
            screenBottomLeft.getLatitude(), screenBottomLeft.getLongitude(), 0);
        fr.free.nrw.commons.location.LatLng screenBottomLeftLatLng = new fr.free.nrw.commons.location.LatLng(
            screenTopRight.getLatitude(), screenTopRight.getLongitude(), 0);

        // When the nearby fragment is opened immediately upon app launch, the {screenTopRightLatLng}
        // and {screenBottomLeftLatLng} variables return {LatLng(0.0,0.0)} as output.
        // To address this issue, A small delta value {delta = 0.02} is used to adjust the latitude
        // and longitude values for {ZOOM_LEVEL = 14f}.
        // This adjustment helps in calculating the east and west corner LatLng accurately.
        // Note: This only happens when the nearby fragment is opened immediately upon app launch,
        // otherwise {screenTopRightLatLng} and {screenBottomLeftLatLng} are used to determine
        // the east and west corner LatLng.
        if (screenTopRightLatLng.getLatitude() == 0.0 && screenTopRightLatLng.getLongitude() == 0.0
            && screenBottomLeftLatLng.getLatitude() == 0.0
            && screenBottomLeftLatLng.getLongitude() == 0.0) {
            final double delta = 0.02;
            final double westCornerLat = currentLatLng.getLatitude() - delta;
            final double westCornerLong = currentLatLng.getLongitude() - delta;
            final double eastCornerLat = currentLatLng.getLatitude() + delta;
            final double eastCornerLong = currentLatLng.getLongitude() + delta;
            screenTopRightLatLng = new fr.free.nrw.commons.location.LatLng(westCornerLat,
                westCornerLong, 0);
            screenBottomLeftLatLng = new fr.free.nrw.commons.location.LatLng(eastCornerLat,
                eastCornerLong, 0);
            if (currentLatLng.equals(
                getLastMapFocus())) { // Means we are checking around current location
                populatePlacesForCurrentLocation(getLastMapFocus(), screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null);
            } else {
                populatePlacesForAnotherLocation(getLastMapFocus(), screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null);
            }
        } else {
            if (currentLatLng.equals(
                getLastMapFocus())) { // Means we are checking around current location
                populatePlacesForCurrentLocation(getLastMapFocus(), screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null);
            } else {
                populatePlacesForAnotherLocation(getLastMapFocus(), screenTopRightLatLng,
                    screenBottomLeftLatLng, currentLatLng, null);
            }
        }

        if (recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    @Override
    public void populatePlaces(final fr.free.nrw.commons.location.LatLng currentLatLng,
        @Nullable final String customQuery) {
        if (customQuery == null || customQuery.isEmpty()) {
            populatePlaces(currentLatLng);
            return;
        }
        IGeoPoint screenTopRight = binding.map.getProjection().fromPixels(binding.map.getWidth(), 0);
        IGeoPoint screenBottomLeft = binding.map.getProjection().fromPixels(0, binding.map.getHeight());
        fr.free.nrw.commons.location.LatLng screenTopRightLatLng = new fr.free.nrw.commons.location.LatLng(
            screenBottomLeft.getLatitude(), screenBottomLeft.getLongitude(), 0);
        fr.free.nrw.commons.location.LatLng screenBottomLeftLatLng = new fr.free.nrw.commons.location.LatLng(
            screenTopRight.getLatitude(), screenTopRight.getLongitude(), 0);

        if (currentLatLng.equals(lastFocusLocation) || lastFocusLocation == null
            || recenterToUserLocation) { // Means we are checking around current location
            populatePlacesForCurrentLocation(lastKnownLocation, screenTopRightLatLng,
                screenBottomLeftLatLng, currentLatLng, customQuery);
        } else {
            populatePlacesForAnotherLocation(lastKnownLocation, screenTopRightLatLng,
                screenBottomLeftLatLng, currentLatLng, customQuery);
        }
        if (recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    private void savePlacesAsKML() {
        final Observable<String> savePlacesObservable = Observable
            .fromCallable(() -> nearbyController
                .getPlacesAsKML(getMapFocus()));
        compositeDisposable.add(savePlacesObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(kmlString -> {
                    if (kmlString != null) {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
                        String fileName =
                            "KML_" + timeStamp + "_" + System.currentTimeMillis() + ".kml";
                        boolean saved = saveFile(kmlString, fileName);
                        progressDialog.hide();
                        if (saved) {
                            showOpenFileDialog(getContext(), fileName, false);
                        } else {
                            Toast.makeText(this.getContext(),
                                getString(R.string.failed_to_save_kml_file),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)
                        + throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    private void savePlacesAsGPX() {
        final Observable<String> savePlacesObservable = Observable
            .fromCallable(() -> nearbyController
                .getPlacesAsGPX(getMapFocus()));
        compositeDisposable.add(savePlacesObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(gpxString -> {
                    if (gpxString != null) {
                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                            Locale.getDefault()).format(new Date());
                        String fileName =
                            "GPX_" + timeStamp + "_" + System.currentTimeMillis() + ".gpx";
                        boolean saved = saveFile(gpxString, fileName);
                        progressDialog.hide();
                        if (saved) {
                            showOpenFileDialog(getContext(), fileName, true);
                        } else {
                            Toast.makeText(this.getContext(),
                                getString(R.string.failed_to_save_gpx_file),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)
                        + throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    public static boolean saveFile(String string, String fileName) {

        if (!isExternalStorageWritable()) {
            return false;
        }

        File downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS);
        File kmlFile = new File(downloadsDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(kmlFile);
            fos.write(string.getBytes());
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showOpenFileDialog(Context context, String fileName, Boolean isGPX) {
        String title = getString(R.string.file_saved_successfully);
        String message =
            (isGPX) ? getString(R.string.do_you_want_to_open_gpx_file)
                : getString(R.string.do_you_want_to_open_kml_file);
        Runnable runnable = () -> openFile(context, fileName, isGPX);
        DialogUtil.showAlertDialog(getActivity(), title, message, runnable,() -> {});
    }

    private void openFile(Context context, String fileName, Boolean isGPX) {
        File file = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName);
        Uri uri = FileProvider.getUriForFile(context,
            context.getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);

        if (isGPX) {
            intent.setDataAndType(uri, "application/gpx");
        } else {
            intent.setDataAndType(uri, "application/kml");
        }

        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_application_available_to_open_gpx_files,
                Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void populatePlacesForCurrentLocation(
        final fr.free.nrw.commons.location.LatLng currentLatLng,
        final fr.free.nrw.commons.location.LatLng screenTopRight,
        final fr.free.nrw.commons.location.LatLng screenBottomLeft,
        final fr.free.nrw.commons.location.LatLng searchLatLng,
        @Nullable final String customQuery) {
        final Observable<NearbyController.NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(currentLatLng, screenTopRight, screenBottomLeft,
                    searchLatLng,
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
                        lastMapFocus = new GeoPoint(searchLatLng.getLatitude(),
                            searchLatLng.getLongitude());
                    }
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)
                        + throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    private void populatePlacesForAnotherLocation(
        final fr.free.nrw.commons.location.LatLng currentLatLng,
        final fr.free.nrw.commons.location.LatLng screenTopRight,
        final fr.free.nrw.commons.location.LatLng screenBottomLeft,
        final fr.free.nrw.commons.location.LatLng searchLatLng,
        @Nullable final String customQuery) {
        final Observable<NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(currentLatLng, screenTopRight, screenBottomLeft,
                    searchLatLng,
                    false, true, Utils.isMonumentsEnabled(new Date()), customQuery));

        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(nearbyPlacesInfo -> {
                    if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                        showErrorMessage(getString(R.string.no_nearby_places_around));
                    } else {
                        // Updating last searched location
                        applicationKvStore.putString("LastLocation",
                            searchLatLng.getLatitude() + "," + searchLatLng.getLongitude());
                        updateMapMarkers(nearbyPlacesInfo, false);
                        lastMapFocus = new GeoPoint(searchLatLng.getLatitude(),
                            searchLatLng.getLongitude());
                    }
                },
                throwable -> {
                    Timber.e(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)
                        + throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                    setFilterState();
                }));
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     *
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(final NearbyController.NearbyPlacesInfo nearbyPlacesInfo,
        final boolean shouldUpdateSelectedMarker) {
        presenter.updateMapMarkers(nearbyPlacesInfo, shouldUpdateSelectedMarker);
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
        binding.searchThisAreaButton.setOnClickListener(presenter.onSearchThisAreaClicked());
    }

    @Override
    public void setSearchThisAreaButtonVisibility(final boolean isVisible) {
        if (isVisible) {
            binding.searchThisAreaButton.setVisibility(View.VISIBLE);
        } else {
            binding.searchThisAreaButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void setRecyclerViewAdapterAllSelected() {
        if (nearbyFilterSearchRecyclerViewAdapter != null
            && NearbyController.currentLocation != null) {
            nearbyFilterSearchRecyclerViewAdapter.setRecyclerViewAdapterAllSelected();
        }
    }

    @Override
    public void setRecyclerViewAdapterItemsGreyedOut() {
        if (nearbyFilterSearchRecyclerViewAdapter != null
            && NearbyController.currentLocation != null) {
            nearbyFilterSearchRecyclerViewAdapter.setRecyclerViewAdapterItemsGreyedOut();
        }
    }

    @Override
    public void setProgressBarVisibility(final boolean isVisible) {
        if (isVisible) {
             binding.mapProgressBar.setVisibility(View.VISIBLE);
        } else {
             binding.mapProgressBar.setVisibility(View.GONE);
        }
    }

    public void setTabItemContributions() {
        ((MainActivity) getActivity()).binding.pager.setCurrentItem(0);
        // TODO
    }

    @Override
    public void checkPermissionsAndPerformAction() {
        Timber.d("Checking permission and perfoming action");
        locationPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION);
    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    @Override
    public void animateFABs() {
        if (binding.fabPlus.isShown()) {
            if (isFABsExpanded) {
                collapseFABs(isFABsExpanded);
            } else {
                expandFABs(isFABsExpanded);
            }
        }
    }

    private void showFABs() {
        NearbyFABUtils.addAnchorToBigFABs(binding.fabPlus, binding.bottomSheetDetails.getRoot().getId());
        binding.fabPlus.show();
        NearbyFABUtils.addAnchorToSmallFABs(binding.fabGallery,
            getView().findViewById(R.id.empty_view).getId());
        NearbyFABUtils.addAnchorToSmallFABs(binding.fabCamera,
            getView().findViewById(R.id.empty_view1).getId());
        NearbyFABUtils.addAnchorToSmallFABs(binding.fabCustomGallery,
            getView().findViewById(R.id.empty_view2).getId());
    }

    /**
     * Expands camera and gallery FABs, turn forward plus FAB
     *
     * @param isFABsExpanded true if they are already expanded
     */
    private void expandFABs(final boolean isFABsExpanded) {
        if (!isFABsExpanded) {
            showFABs();
            binding.fabPlus.startAnimation(rotate_forward);
            binding.fabCamera.startAnimation(fab_open);
            binding.fabGallery.startAnimation(fab_open);
            binding.fabCustomGallery.startAnimation(fab_open);
            binding.fabCustomGallery.show();
            binding.fabCamera.show();
            binding.fabGallery.show();
            this.isFABsExpanded = true;
        }
    }

    /**
     * Hides all fabs
     */
    private void hideFABs() {
        NearbyFABUtils.removeAnchorFromFAB(binding.fabPlus);
        binding.fabPlus.hide();
        NearbyFABUtils.removeAnchorFromFAB(binding.fabCamera);
        binding.fabCamera.hide();
        NearbyFABUtils.removeAnchorFromFAB(binding.fabGallery);
        binding.fabGallery.hide();
        NearbyFABUtils.removeAnchorFromFAB(binding.fabCustomGallery);
        binding.fabCustomGallery.hide();
    }

    /**
     * Collapses camera and gallery FABs, turn back plus FAB
     *
     * @param isFABsExpanded
     */
    private void collapseFABs(final boolean isFABsExpanded) {
        if (isFABsExpanded) {
            binding.fabPlus.startAnimation(rotate_backward);
            binding.fabCamera.startAnimation(fab_close);
            binding.fabGallery.startAnimation(fab_close);
            binding.fabCustomGallery.startAnimation(fab_close);
            binding.fabCustomGallery.hide();
            binding.fabCamera.hide();
            binding.fabGallery.hide();
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
                    BaseLogoutListener logoutListener = new BaseLogoutListener(getActivity());
                    CommonsApplication app = (CommonsApplication) getActivity().getApplication();
                    app.clearApplicationData(getContext(), logoutListener);
                })
                .show();
        }
    }

    private void handleLocationUpdate(final fr.free.nrw.commons.location.LatLng latLng,
        final LocationServiceManager.LocationChangeType locationChangeType) {
        lastKnownLocation = latLng;
        NearbyController.currentLocation = lastKnownLocation;
        presenter.updateMapAndList(locationChangeType);
    }

    @Override
    public void onLocationChangedSignificantly(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location significantly changed");
        if (latLng != null) {
            handleLocationUpdate(latLng, LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedSlightly(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location slightly changed");
        if (latLng != null) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng, LOCATION_SLIGHTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedMedium(final fr.free.nrw.commons.location.LatLng latLng) {
        Timber.d("Location changed medium");
        if (latLng != null) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng, LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    public boolean backButtonClicked() {
        return presenter.backButtonClicked();
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are
     * cleared.
     */

    @Override
    public void setFABPlusAction(final View.OnClickListener onClickListener) {
        binding.fabPlus.setOnClickListener(onClickListener);
    }

    @Override
    public void setFABRecenterAction(final View.OnClickListener onClickListener) {
         binding.fabRecenter.setOnClickListener(onClickListener);
    }

    @Override
    public void disableFABRecenter() {
         binding.fabRecenter.setEnabled(false);
    }

    @Override
    public void enableFABRecenter() {
         binding.fabRecenter.setEnabled(true);
    }

    /**
     * Adds a marker for the user's current position. Adds a circle which uses the accuracy * 2, to
     * draw a circle which represents the user's position with an accuracy of 95%.
     * <p>
     * Should be called only on creation of Map, there is other method to update markers location
     * with users move.
     *
     * @param currentLatLng current location
     */
    @Override
    public void addCurrentLocationMarker(final fr.free.nrw.commons.location.LatLng currentLatLng) {
        if (null != currentLatLng && !isPermissionDenied
            && locationManager.isGPSProviderEnabled()) {
            ExecutorUtils.get().submit(() -> {
                Timber.d("Adds current location marker");
                recenterMarkerToPosition(
                    new GeoPoint(currentLatLng.getLatitude(), currentLatLng.getLongitude()));
            });
        } else {
            Timber.d("not adding current location marker..current location is null");
        }
    }

    @Override
    public void updateMapMarkers(final List<BaseMarker> BaseMarkers) {
        if (binding.map != null) {
            presenter.updateMapMarkersToController(BaseMarkers);
        }
    }

    @Override
    public void filterOutAllMarkers() {
        clearAllMarkers();
    }

    /**
     * Filters markers based on selectedLabels and chips
     *
     * @param selectedLabels       label list that user clicked
     * @param displayExists        chip for displaying only existing places
     * @param displayNeedsPhoto    chip for displaying only places need photos
     * @param filterForPlaceState  true if we filter places for place state
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
        clearAllMarkers();
        for (final MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
            final Place place = markerPlaceGroup.getPlace();
            // When label filter is engaged
            // then compare it against place's label
            if (selectedLabels != null && (selectedLabels.size() != 0 || !filterForPlaceState)
                && (!selectedLabels.contains(place.getLabel())
                && !(selectedLabels.contains(Label.BOOKMARKS)
                && markerPlaceGroup.getIsBookmarked()))) {
                continue;
            }

            if (!displayWlm && place.isMonument()) {
                continue;
            }

            boolean shouldUpdateMarker = false;

            if (displayWlm && place.isMonument()) {
                shouldUpdateMarker = true;
            } else if (displayExists && displayNeedsPhoto) {
                // Exists and needs photo
                if (place.exists && place.pic.trim().isEmpty()) {
                    shouldUpdateMarker = true;
                }
            } else if (displayExists && !displayNeedsPhoto) {
                // Exists and all included needs and doesn't needs photo
                if (place.exists) {
                    shouldUpdateMarker = true;
                }
            } else if (!displayExists && displayNeedsPhoto) {
                // All and only needs photo
                if (place.pic.trim().isEmpty()) {
                    shouldUpdateMarker = true;
                }
            } else if (!displayExists && !displayNeedsPhoto) {
                // all
                shouldUpdateMarker = true;
            }

            if (shouldUpdateMarker) {
                updateMarker(markerPlaceGroup.getIsBookmarked(), place,
                    NearbyController.currentLocation);
            }
        }
        if (selectedLabels == null || selectedLabels.size() == 0) {
            ArrayList<BaseMarker> markerArrayList = new ArrayList<>();
            for (final MarkerPlaceGroup markerPlaceGroup : NearbyController.markerLabelList) {
                BaseMarker nearbyBaseMarker = new BaseMarker();
                nearbyBaseMarker.setPlace(markerPlaceGroup.getPlace());
                markerArrayList.add(nearbyBaseMarker);
            }
            addMarkersToMap(markerArrayList);
        }
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getCameraTarget() {
        return binding.map == null ? null : getMapFocus();
    }

    /**
     * Sets marker icon according to marker status. Sets title and distance.
     *
     * @param isBookmarked  true if place is bookmarked
     * @param place
     * @param currentLatLng current location
     */
    public void updateMarker(final boolean isBookmarked, final Place place,
        @Nullable final fr.free.nrw.commons.location.LatLng currentLatLng) {
        addMarkerToMap(place, isBookmarked);
    }

    /**
     * Highlights nearest place when user clicks on home nearby banner
     *
     * @param nearestPlace nearest place, which has to be highlighted
     */
    private void highlightNearestPlace(Place nearestPlace) {
        passInfoToSheet(nearestPlace);
        hideBottomSheet();
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * Returns drawable of marker icon for given place
     *
     * @param place        where marker is to be added
     * @param isBookmarked true if place is bookmarked
     * @return returns the drawable of marker according to the place information
     */
    private @DrawableRes int getIconFor(Place place, Boolean isBookmarked) {
        if (nearestPlace != null) {
            if (place.name.equals(nearestPlace.name)) {
                // Highlight nearest place only when user clicks on the home nearby banner
                highlightNearestPlace(place);
                return (isBookmarked ?
                    R.drawable.ic_custom_map_marker_purple_bookmarked :
                    R.drawable.ic_custom_map_marker_purple);
            }
        }
        if (place.isMonument()) {
            return R.drawable.ic_custom_map_marker_monuments;
        } else if (!place.pic.trim().isEmpty()) {
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
     * Adds a marker representing a place to the map with optional bookmark icon.
     *
     * @param place        The Place object containing information about the location.
     * @param isBookMarked A Boolean flag indicating whether the place is bookmarked or not.
     */
    private void addMarkerToMap(Place place, Boolean isBookMarked) {
        ArrayList<OverlayItem> items = new ArrayList<>();
        Drawable icon = ContextCompat.getDrawable(getContext(), getIconFor(place, isBookMarked));
        GeoPoint point = new GeoPoint(place.location.getLatitude(), place.location.getLongitude());
        OverlayItem item = new OverlayItem(place.name, null, point);
        item.setMarker(icon);
        items.add(item);
        ItemizedOverlayWithFocus overlay = new ItemizedOverlayWithFocus(items,
            new OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    passInfoToSheet(place);
                    hideBottomSheet();
                    if (clickedMarkerPlace != null) {
                        removeMarker(clickedMarkerPlace);
                        addMarkerToMap(clickedMarkerPlace, isClickedMarkerBookmarked);
                    }
                    clickedMarkerPlace = place;
                    isClickedMarkerBookmarked = isBookMarked;
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            }, getContext());

        overlay.setFocusItemsOnTap(true);
        binding.map.getOverlays().add(overlay); // Add the overlay to the map
    }

    /**
     * Adds multiple markers representing places to the map and handles item gestures.
     *
     * @param nearbyBaseMarkers The list of Place objects containing information about the
     *                          locations.
     */
    private void addMarkersToMap(List<BaseMarker> nearbyBaseMarkers) {
        ArrayList<OverlayItem> items = new ArrayList<>();
        for (int i = 0; i < nearbyBaseMarkers.size(); i++) {
            Drawable icon = ContextCompat.getDrawable(getContext(),
                getIconFor(nearbyBaseMarkers.get(i).getPlace(), false));
            GeoPoint point = new GeoPoint(
                nearbyBaseMarkers.get(i).getPlace().location.getLatitude(),
                nearbyBaseMarkers.get(i).getPlace().location.getLongitude());
            OverlayItem item = new OverlayItem(nearbyBaseMarkers.get(i).getPlace().name, null,
                point);
            item.setMarker(icon);
            items.add(item);
        }
        ItemizedOverlayWithFocus overlay = new ItemizedOverlayWithFocus(items,
            new OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    final Place place = nearbyBaseMarkers.get(index).getPlace();
                    passInfoToSheet(place);
                    hideBottomSheet();
                    if (clickedMarkerPlace != null) {
                        removeMarker(clickedMarkerPlace);
                        addMarkerToMap(clickedMarkerPlace, isClickedMarkerBookmarked);
                    }
                    clickedMarkerPlace = place;
                    isClickedMarkerBookmarked = false;
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            }, getContext());
        overlay.setFocusItemsOnTap(true);
        binding.map.getOverlays().add(overlay);
    }

    private void removeMarker(Place place){
        List<Overlay> overlays = binding.map.getOverlays();
        for (int i = 0; i < overlays.size();i++){
            if (overlays.get(i) instanceof ItemizedOverlayWithFocus){
                ItemizedOverlayWithFocus item = (ItemizedOverlayWithFocus)overlays.get(i);
                OverlayItem overlayItem = item.getItem(0);
                fr.free.nrw.commons.location.LatLng diffLatLang = new fr.free.nrw.commons.location.LatLng(overlayItem.getPoint().getLatitude(),overlayItem.getPoint().getLongitude(),100);
                if (place.location.getLatitude() == overlayItem.getPoint().getLatitude() && place.location.getLongitude() == overlayItem.getPoint().getLongitude()){
                    binding.map.getOverlays().remove(i);
                    binding.map.invalidate();
                    break;
                }
            }
        }
    }

    @Override
    public void recenterMap(fr.free.nrw.commons.location.LatLng currentLatLng) {
        if (isPermissionDenied || currentLatLng == null) {
            recenterToUserLocation = true;
            checkPermissionsAndPerformAction();
            if (!isPermissionDenied && !(locationManager.isNetworkProviderEnabled()
                || locationManager.isGPSProviderEnabled())) {
                showLocationOffDialog();
            }
            return;
        }
        addCurrentLocationMarker(currentLatLng);
        binding.map.getController()
            .animateTo(new GeoPoint(currentLatLng.getLatitude(), currentLatLng.getLongitude()));
        if (lastMapFocus != null) {
            Location mylocation = new Location("");
            Location dest_location = new Location("");
            dest_location.setLatitude(binding.map.getMapCenter().getLatitude());
            dest_location.setLongitude(binding.map.getMapCenter().getLongitude());
            mylocation.setLatitude(lastMapFocus.getLatitude());
            mylocation.setLongitude(lastMapFocus.getLongitude());
            Float distance = mylocation.distanceTo(dest_location);//in meters
            if (lastMapFocus != null) {
                if (isNetworkConnectionEstablished()) {
                    if (distance > 2000.0) {
                        setSearchThisAreaButtonVisibility(true);
                    } else {
                        setSearchThisAreaButtonVisibility(false);
                    }
                }
            } else {
                setSearchThisAreaButtonVisibility(false);
            }
        }
    }

    @Override
    public void showLocationOffDialog() {
        // This creates a dialog box that prompts the user to enable location
        DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.ask_to_turn_location_on),
                getString(R.string.nearby_needs_location),
                getString(R.string.yes), getString(R.string.no), this::openLocationSettings, null);
    }

    @Override
    public void openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = getActivity().getPackageManager();

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
            Toast.makeText(getContext(), R.string.recommend_high_accuracy_mode, Toast.LENGTH_LONG)
                .show();
        } else {
            Toast.makeText(getContext(), R.string.cannot_open_location_settings, Toast.LENGTH_LONG)
                .show();
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

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus If nearby details bottom
     * sheet state is expanded: show fab plus If nearby details bottom sheet state is hidden: hide
     * all fabs
     *
     * @param bottomSheetState see bottom sheet states
     */
    public void prepareViewsForSheetPosition(final int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                collapseFABs(isFABsExpanded);
                if (!binding.fabPlus.isShown()) {
                    showFABs();
                }
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                binding.transparentView.setClickable(false);
                binding.transparentView.setAlpha(0);
                collapseFABs(isFABsExpanded);
                hideFABs();
                break;
        }
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private void passInfoToSheet(final Place place) {
        selectedPlace = place;
        updateBookmarkButtonImage(selectedPlace);

        binding.bottomSheetDetails.bookmarkButton.setOnClickListener(view -> {
            final boolean isBookmarked = bookmarkLocationDao.updateBookmarkLocation(selectedPlace);
            updateBookmarkButtonImage(selectedPlace);
            updateMarker(isBookmarked, selectedPlace, locationManager.getLastLocation());
            binding.map.invalidate();
        });
        binding.bottomSheetDetails.bookmarkButton.setOnLongClickListener(view -> {
            Toast.makeText(getContext(), R.string.menu_bookmark, Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.bottomSheetDetails.wikipediaButton.setVisibility(place.hasWikipediaLink() ? View.VISIBLE : View.GONE);
        binding.bottomSheetDetails.wikipediaButton.setOnClickListener(
            view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getWikipediaLink()));
        binding.bottomSheetDetails.wikipediaButton.setOnLongClickListener(view -> {
            Toast.makeText(getContext(), R.string.nearby_wikipedia, Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.bottomSheetDetails.wikidataButton.setVisibility(place.hasWikidataLink() ? View.VISIBLE : View.GONE);
        binding.bottomSheetDetails.wikidataButton.setOnClickListener(
            view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getWikidataLink()));
        binding.bottomSheetDetails.wikidataButton.setOnLongClickListener(view -> {
            Toast.makeText(getContext(), R.string.nearby_wikidata, Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.bottomSheetDetails.directionsButton.setOnClickListener(view -> Utils.handleGeoCoordinates(getActivity(),
            selectedPlace.getLocation()));
        binding.bottomSheetDetails.directionsButton.setOnLongClickListener(view -> {
            Toast.makeText(getContext(), R.string.nearby_directions, Toast.LENGTH_SHORT).show();
            return true;
        });

         binding.bottomSheetDetails.commonsButton.setVisibility(selectedPlace.hasCommonsLink() ? View.VISIBLE : View.GONE);
         binding.bottomSheetDetails.commonsButton.setOnClickListener(
            view -> Utils.handleWebUrl(getContext(), selectedPlace.siteLinks.getCommonsLink()));
         binding.bottomSheetDetails.commonsButton.setOnLongClickListener(view -> {
            Toast.makeText(getContext(), R.string.nearby_commons, Toast.LENGTH_SHORT).show();
            return true;
        });

        binding.bottomSheetDetails.icon.setImageResource(selectedPlace.getLabel().getIcon());

        binding.bottomSheetDetails.title.setText(selectedPlace.name);
        binding.bottomSheetDetails.category.setText(selectedPlace.distance);
        // Remove label since it is double information
        String descriptionText = selectedPlace.getLongDescription()
            .replace(selectedPlace.getName() + " (", "");
        descriptionText = (descriptionText.equals(selectedPlace.getLongDescription())
            ? descriptionText : descriptionText.replaceFirst(".$", ""));
        // Set the short description after we remove place name from long description
         binding.bottomSheetDetails.description.setText(descriptionText);

        binding.fabCamera.setOnClickListener(view -> {
            if (binding.fabCamera.isShown()) {
                Timber.d("Camera button tapped. Place: %s", selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateCameraPick(getActivity(), inAppCameraLocationPermissionLauncher);
            }
        });

        binding.fabGallery.setOnClickListener(view -> {
            if (binding.fabGallery.isShown()) {
                Timber.d("Gallery button tapped. Place: %s", selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateGalleryPick(getActivity(),  binding.nearbyFilter.chipView.choiceChipWlm.isChecked());
            }
        });

        binding.fabCustomGallery.setOnClickListener(view -> {
            if (binding.fabCustomGallery.isShown()) {
                Timber.d("Gallery button tapped. Place: %s", selectedPlace.toString());
                storeSharedPrefs(selectedPlace);
                controller.initiateCustomGalleryPickWithPermission(getActivity());
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
        if ( binding.bottomSheetDetails.bookmarkButtonImage != null) {
             binding.bottomSheetDetails.bookmarkButtonImage.setImageResource(bookmarkIcon);
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
        if (presenter != null && locationManager != null) {
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
        } catch (final Exception e) {
            Timber.e(e);
            //Broadcasts are tricky, should be catchedonR
        }
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
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
        performMapReadyActions();
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    @Override
    public void clearAllMarkers() {
        binding.map.getOverlayManager().clear();
        binding.map.invalidate();
        GeoPoint geoPoint = mapCenter;
        if (geoPoint != null) {
            List<Overlay> overlays = binding.map.getOverlays();
            ScaleDiskOverlay diskOverlay =
                new ScaleDiskOverlay(this.getContext(),
                    geoPoint, 2000, GeoConstants.UnitOfMeasure.foot);
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.rgb(128, 128, 128));
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2f);
            diskOverlay.setCirclePaint2(circlePaint);
            Paint diskPaint = new Paint();
            diskPaint.setColor(Color.argb(40, 128, 128, 128));
            diskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            diskOverlay.setCirclePaint1(diskPaint);
            diskOverlay.setDisplaySizeMin(900);
            diskOverlay.setDisplaySizeMax(1700);
            binding.map.getOverlays().add(diskOverlay);
            org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(
                binding.map);
            startMarker.setPosition(geoPoint);
            startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(
                ContextCompat.getDrawable(this.getContext(), R.drawable.current_location_marker));
            startMarker.setTitle("Your Location");
            startMarker.setTextLabelFontSize(24);
            binding.map.getOverlays().add(startMarker);
        }
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(binding.map);
        scaleBarOverlay.setScaleBarOffset(15, 25);
        Paint barPaint = new Paint();
        barPaint.setARGB(200, 255, 250, 250);
        scaleBarOverlay.setBackgroundPaint(barPaint);
        scaleBarOverlay.enableScaleBar();
        binding.map.getOverlays().add(scaleBarOverlay);
        binding.map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (clickedMarkerPlace != null) {
                    removeMarker(clickedMarkerPlace);
                    addMarkerToMap(clickedMarkerPlace, isClickedMarkerBookmarked);
                } else {
                    Timber.e("CLICKED MARKER IS NULL");
                }
                if (isListBottomSheetExpanded()) {
                    // Back should first hide the bottom sheet if it is expanded
                    hideBottomSheet();
                } else if (isDetailsBottomSheetVisible()) {
                    hideBottomDetailsSheet();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        binding.map.setMultiTouchControls(true);
    }

    /**
     * Recenters the map to the Center and adds a scale disk overlay and a marker at the position.
     *
     * @param geoPoint The GeoPoint representing the new center position of the map.
     */
    private void recenterMarkerToPosition(GeoPoint geoPoint) {
        if (geoPoint != null) {
            binding.map.getController().setCenter(geoPoint);
            List<Overlay> overlays = binding.map.getOverlays();
            for (int i = 0; i < overlays.size(); i++) {
                if (overlays.get(i) instanceof org.osmdroid.views.overlay.Marker) {
                    binding.map.getOverlays().remove(i);
                } else if (overlays.get(i) instanceof ScaleDiskOverlay) {
                    binding.map.getOverlays().remove(i);
                }
            }
            ScaleDiskOverlay diskOverlay =
                new ScaleDiskOverlay(this.getContext(),
                    geoPoint, 2000, GeoConstants.UnitOfMeasure.foot);
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.rgb(128, 128, 128));
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2f);
            diskOverlay.setCirclePaint2(circlePaint);
            Paint diskPaint = new Paint();
            diskPaint.setColor(Color.argb(40, 128, 128, 128));
            diskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            diskOverlay.setCirclePaint1(diskPaint);
            diskOverlay.setDisplaySizeMin(900);
            diskOverlay.setDisplaySizeMax(1700);
            binding.map.getOverlays().add(diskOverlay);
            org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(
                binding.map);
            startMarker.setPosition(geoPoint);
            startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(
                ContextCompat.getDrawable(this.getContext(), R.drawable.current_location_marker));
            startMarker.setTitle("Your Location");
            startMarker.setTextLabelFontSize(24);
            binding.map.getOverlays().add(startMarker);
        }
    }

    private void moveCameraToPosition(GeoPoint geoPoint) {
        binding.map.getController().animateTo(geoPoint);
    }

    public interface NearbyParentFragmentInstanceReadyCallback {

        void onReady();
    }

    public void setNearbyParentFragmentInstanceReadyCallback(
        NearbyParentFragmentInstanceReadyCallback nearbyParentFragmentInstanceReadyCallback) {
        this.nearbyParentFragmentInstanceReadyCallback = nearbyParentFragmentInstanceReadyCallback;
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup.LayoutParams rlBottomSheetLayoutParams = binding.bottomSheetNearby.bottomSheet.getLayoutParams();
        rlBottomSheetLayoutParams.height =
            getActivity().getWindowManager().getDefaultDisplay().getHeight() / 16 * 9;
        binding.bottomSheetNearby.bottomSheet.setLayoutParams(rlBottomSheetLayoutParams);
    }


    public void onLearnMoreClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(WLM_URL));
        startActivity(intent);
    }

    public void onToggleChipsClicked() {
        if (  binding.nearbyFilter.chipView.getRoot().getVisibility() == View.VISIBLE) {
              binding.nearbyFilter.chipView.getRoot().setVisibility(View.GONE);
        } else {
              binding.nearbyFilter.chipView.getRoot().setVisibility(View.VISIBLE);
        }
        binding.nearbyFilter.ivToggleChips.setRotation(binding.nearbyFilter.ivToggleChips.getRotation() + 180);
    }
}
