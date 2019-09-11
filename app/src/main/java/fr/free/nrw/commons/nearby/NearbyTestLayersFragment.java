package fr.free.nrw.commons.nearby;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.mvp.fragments.NearbyParentFragment;
import fr.free.nrw.commons.nearby.mvp.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.FragmentUtils;
import fr.free.nrw.commons.utils.NearbyFABUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.ContributionsFragment.CONTRIBUTION_LIST_FRAGMENT_TAG;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.nearby.NearbyTestFragmentLayersActivity.CONTRIBUTIONS_TAB_POSITION;


public class NearbyTestLayersFragment extends CommonsDaggerSupportFragment implements NearbyParentFragmentContract.View {

    @BindView(R.id.bottom_sheet)
    View bottomSheetList;

    @BindView(R.id.bottom_sheet_details)
    View bottomSheetDetails;

    @BindView(R.id.transparentView)
    View transparentView;

    @BindView(R.id.directionsButtonText)
    TextView directionsButtonText;

    @BindView(R.id.wikipediaButtonText)
    TextView wikipediaButtonText;

    @BindView(R.id.wikidataButtonText)
    TextView wikidataButtonText;

    @BindView(R.id.commonsButtonText)
    TextView commonsButtonText;

    @BindView(R.id.fab_plus)
    FloatingActionButton fabPlus;

    @BindView(R.id.fab_camera)
    FloatingActionButton fabCamera;

    @BindView(R.id.fab_gallery)
    FloatingActionButton fabGallery;

    @BindView(R.id.fab_recenter)
    FloatingActionButton fabRecenter;

    @BindView(R.id.bookmarkButtonImage)
    ImageView bookmarkButtonImage;

    @BindView(R.id.bookmarkButton)
    LinearLayout bookmarkButton;

    @BindView(R.id.wikipediaButton)
    LinearLayout wikipediaButton;

    @BindView(R.id.wikidataButton)
    LinearLayout wikidataButton;

    @BindView(R.id.directionsButton)
    LinearLayout directionsButton;

    @BindView(R.id.commonsButton)
    LinearLayout commonsButton;

    @BindView(R.id.description)
    TextView description;

    @BindView(R.id.title)
    TextView title;

    @BindView(R.id.category)
    TextView distance;

    @BindView(R.id.icon)
    ImageView icon;

    @BindView(R.id.search_this_area_button)
    Button searchThisAreaButton;

    @BindView(R.id.search_this_area_button_progress_bar)
    ProgressBar searchThisAreaButtonProgressBar;

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
    FragmentTransaction transaction;
    View view;

    NearbyParentFragmentPresenter nearbyParentFragmentPresenter;
    SupportMapFragment mapFragment;
    boolean isDarkTheme;
    boolean isFabOpen;
    boolean isBottomListSheetExpanded;
    private Marker selectedMarker;
    private Place selectedPlace;

    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.06;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.04;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_simple, container, false);
        ButterKnife.bind(this, view);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setMapFragment(savedInstanceState);
    }

    public void initViews() {
        Timber.d("init views called");
        ButterKnife.bind(this, view);
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);

        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);

        bottomSheetDetailsBehavior.setBottomSheetCallback(new BottomSheetBehavior
                .BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0) {
                    transparentView.setAlpha(slideOffset);
                    if (slideOffset == 1) {
                        transparentView.setClickable(true);
                    } else if (slideOffset == 0) {
                        transparentView.setClickable(false);
                    }
                }
            }
        });

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

        // Remove button text if they exceed 1 line or if internal layout has not been built
        // Only need to check for directions button because it is the longest
        if (directionsButtonText.getLineCount() > 1 || directionsButtonText.getLineCount() == 0) {
            wikipediaButtonText.setVisibility(View.GONE);
            wikidataButtonText.setVisibility(View.GONE);
            commonsButtonText.setVisibility(View.GONE);
            directionsButtonText.setVisibility(View.GONE);
        }
        title.setOnLongClickListener(view -> {
                    Utils.copy("place", title.getText().toString(), getContext());
                    Toast.makeText(getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                    return true;
                }
        );
        title.setOnClickListener(view -> {
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    public void setMapFragment(Bundle savedInstanceState) {
        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(getActivity(), getString(R.string.mapbox_commons_app_token));

        // Create supportMapFragment
        if (savedInstanceState == null) {

            // Create fragment
            transaction = getChildFragmentManager().beginTransaction();

            // Build mapboxMap
            isDarkTheme = applicationKvStore.getBoolean("theme", false);
            MapboxMapOptions options = new MapboxMapOptions()
                    .compassGravity(Gravity.BOTTOM | Gravity.LEFT)
                    .compassMargins(new int[]{12, 0, 0, 24})
                    //.styleUrl(isDarkTheme ? Style.DARK : Style.OUTDOORS)
                    .logoEnabled(false)
                    .attributionEnabled(false)
                    .camera(new CameraPosition.Builder()
                            .zoom(ZOOM_LEVEL)
                            .target(new com.mapbox.mapboxsdk.geometry.LatLng(-52.6885, -70.1395))
                            .build());

            // Create map fragment
            mapFragment = SupportMapFragment.newInstance(options);

            // Add map fragment to parent container
            getChildFragmentManager().executePendingTransactions();
            transaction.add(R.id.container, mapFragment, "com.mapbox.map");
            transaction.commit();
        } else {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentByTag("com.mapbox.map");
        }

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {

                mapboxMap.setStyle(NearbyTestLayersFragment.this.isDarkTheme ? Style.DARK : Style.OUTDOORS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        NearbyTestLayersFragment.this.childMapFragmentAttached();

                        Log.d("NearbyTests","Fragment inside fragment with map works");
                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments

                    }
                });
            }
        });
    }

    /**
     * Thanks to this method we make sure NearbyMapFragment is ready and attached. So that we can
     * prevent NPE caused by null child fragment. This method is called from child fragment when
     * it is attached.
     */
    public void childMapFragmentAttached() {

        Log.d("denemeTest","this:"+this+", location manager is:"+locationManager);
        nearbyParentFragmentPresenter = new NearbyParentFragmentPresenter
                (this, mapFragment, locationManager);
        Timber.d("Child fragment attached");
        nearbyParentFragmentPresenter.nearbyFragmentsAreReady();
        initViews();
        nearbyParentFragmentPresenter.setActionListeners(applicationKvStore);

    }

    @Override
    public void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener) {
        Log.d("denemeTestt","on camera move listener is set");
        mapFragment.getMapboxMap().addOnCameraMoveListener(onCameraMoveListener);
    }

    @Override
    public void setListFragmentExpanded() {

    }

    @Override
    public void refreshView() {

    }
    @Override
    public void registerLocationUpdates(LocationServiceManager locationManager) {
        locationManager.registerLocationManager();
    }

    public void registerLocationUpdates() {
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
        Log.d("denemeTest","addNetworkBroadcastReceiver");
        if (!FragmentUtils.isFragmentUIActive(this)) {
            Log.d("denemeTest","!FragmentUtils.isFragmentUIActive(this)");
            return;
        }

        if (broadcastReceiver != null) {
            Log.d("denemeTest","broadcastReceiver != null");
            return;
        }

        IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() != null) {
                    if (NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                        Log.d("denemeTest","NetworkUtils.isInternetConnectionEstablished(getActivity())");
                        if (isNetworkErrorOccurred) {
                            Log.d("denemeTest","isNetworkErrorOccurred");
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

    @Override
    public void listOptionMenuItemClicked() {

    }

    @Override
    public void populatePlaces(fr.free.nrw.commons.location.LatLng curlatLng, fr.free.nrw.commons.location.LatLng searchLatLng) {
        boolean checkingAroundCurretLocation;
        if (curlatLng.equals(searchLatLng)) { // Means we are checking around current location
            Log.d("denemeTestt","checking around current location1");
            checkingAroundCurretLocation = true;
        } else {
            Log.d("denemeTestt","not checking around current location2");
            checkingAroundCurretLocation = false;
        }

        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, checkingAroundCurretLocation))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateMapMarkers,
                        throwable -> {
                            Timber.d(throwable);
                            //showErrorMessage(getString(R.string.error_fetching_nearby_places));
                            // TODO solve first unneeded method call here
                            //progressBar.setVisibility(View.GONE);
                            //nearbyParentFragmentPresenter.lockNearby(false);
                        }));
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        nearbyParentFragmentPresenter.updateMapMarkers(nearbyPlacesInfo, selectedMarker);
    }

    @Override
    public boolean isBottomSheetExpanded() {
        return false;
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
    public void checkPermissionsAndPerformAction(Runnable runnable) {
        Log.d("denemeTest","checkPermissionsAndPerformAction is called");
        PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION,
                runnable,
                () -> ((NearbyTestFragmentLayersActivity) getActivity()).viewPager.setCurrentItem(CONTRIBUTIONS_TAB_POSITION),
                R.string.location_permission_title,
                R.string.location_permission_rationale_nearby);
    }

    @Override
    public void resumeFragment() {

    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    @Override
    public void animateFABs() {
        if (fabPlus.isShown()){
            /*if (isFabOpen) {
                fabPlus.startAnimation(rotate_backward);
                fabCamera.startAnimation(fab_close);
                fabGallery.startAnimation(fab_close);
                fabCamera.hide();
                fabGallery.hide();
            } else {
                fabPlus.startAnimation(rotate_forward);
                fabCamera.startAnimation(fab_open);
                fabGallery.startAnimation(fab_open);
                fabCamera.show();
                fabGallery.show();
            }
            this.isFabOpen=!isFabOpen;*/
            if (isFabOpen) {
                closeFABs(isFabOpen);
            } else {
                openFABs(isFabOpen);
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
     * Hides camera and gallery FABs, turn back plus FAB
     * @param isFabOpen
     */
    private void openFABs( boolean isFabOpen){
        if (!isFabOpen) {
            showFABs();
            fabPlus.startAnimation(rotate_forward);
            fabCamera.startAnimation(fab_open);
            fabGallery.startAnimation(fab_open);
            fabCamera.show();
            fabGallery.show();
            this.isFabOpen = true;
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
     * Hides camera and gallery FABs, turn back plus FAB
     * @param isFabOpen
     */
    private void closeFABs( boolean isFabOpen){
        if (isFabOpen) {
            fabPlus.startAnimation(rotate_backward);
            fabCamera.startAnimation(fab_close);
            fabGallery.startAnimation(fab_close);
            fabCamera.hide();
            fabGallery.hide();
            this.isFabOpen = false;
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
                        //TODO:
                        // ((NavigationBaseActivity)getActivity()).BaseLogoutListener logoutListener = new ((NavigationBaseActivity)getActivity()).BaseLogoutListener();
                        // CommonsApplication app = (CommonsApplication) getActivity().getApplication();
                        // app.clearApplicationData(getContext(), logoutListener);

                    })
                    .show();
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
    public void recenterMap(fr.free.nrw.commons.location.LatLng curLatLng) {
        CameraPosition position;

        if (ViewUtil.isPortrait(getActivity())){
            position = new CameraPosition.Builder()
                    .target(isBottomListSheetExpanded ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isBottomListSheetExpanded ?
                            ZOOM_LEVEL
                            :mapFragment.getMapboxMap().getCameraPosition().zoom) // Same zoom level
                    .build();
        }else {
            position = new CameraPosition.Builder()
                    .target(isBottomListSheetExpanded ?
                            new LatLng(curLatLng.getLatitude() - CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE,
                                    curLatLng.getLongitude())
                            : new LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
                    .zoom(isBottomListSheetExpanded ?
                            ZOOM_LEVEL
                            :mapFragment.getMapboxMap().getCameraPosition().zoom) // Same zoom level
                    .build();
        }

        mapFragment.getMapboxMap().animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
    }

    @Override
    public void initViewPositions() {

    }

    @Override
    public void hideBottomSheet() {
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void displayBottomSheetWithInfo(Marker marker) {
        this.selectedMarker = marker;
        NearbyMarker nearbyMarker = (NearbyMarker) marker;
        Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
        passInfoToSheet(place);
        bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus
     * If nearby details bottom sheet state is expanded: show fab plus
     * If nearby details bottom sheet state is hidden: hide all fabs
     * @param bottomSheetState
     */
    public void prepareViewsForSheetPosition(int bottomSheetState) {

        switch (bottomSheetState) {
            case (BottomSheetBehavior.STATE_COLLAPSED):
                closeFABs(isFabOpen);
                if (!fabPlus.isShown()) showFABs();
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_EXPANDED):
                this.getView().requestFocus();
                break;
            case (BottomSheetBehavior.STATE_HIDDEN):
                mapFragment.getMapboxMap().deselectMarkers();
                transparentView.setClickable(false);
                transparentView.setAlpha(0);
                closeFABs(isFabOpen);
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
            mapFragment.updateMarker(isBookmarked, this.selectedPlace, locationManager.getLastLocation());
        });


        //TODO move all this buttons into a custom bottom sheet
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
                // TODO storeSharedPrefs();
                controller.initiateCameraPick(getActivity());
            }
        });

        fabGallery.setOnClickListener(view -> {
            if (fabGallery.isShown()) {
                Timber.d("Gallery button tapped. Place: %s", this.selectedPlace.toString());
                //TODO storeSharedPrefs();
                controller.initiateGalleryPick(getActivity(), false);
            }
        });
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
}
