package fr.free.nrw.commons.nearby.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapFragment;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.utils.MapFragmentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.UiUtils;
import timber.log.Timber;

import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

/**
 * Support Fragment wrapper around a map view.
 * <p>
 * A Map component in an app. This fragment is the simplest way to place a map in an application.
 * It's a wrapper around a view of a map to automatically handle the necessary life cycle needs.
 * Being a fragment, this component can be added to an activity's layout or can dynamically be added
 * using a FragmentManager.
 * </p>
 * <p>
 * To get a reference to the MapView, use {@link #getMapAsync(OnMapReadyCallback)}}
 * </p>
 *
 * @see #getMapAsync(OnMapReadyCallback)
 */
public class NearbyMapFragment extends CommonsDaggerSupportFragment
        implements OnMapReadyCallback, NearbyMapContract.View{

    @Inject
    BookmarkLocationsDao bookmarkLocationDao;

    private final List<OnMapReadyCallback> mapReadyCallbackList = new ArrayList<>();
    private MapFragment.OnMapViewReadyCallback mapViewReadyCallback;
    private MapboxMap mapboxMap;
    private MapView map;
    private Marker currentLocationMarker;
    private Polygon currentLocationPolygon;
    private List<NearbyBaseMarker> customBaseMarkerOptions;

    private final double CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005;
    private final double CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004;
    private static final double ZOOM_LEVEL = 14f;

    /**
     * Creates a MapFragment instance
     *
     * @param mapboxMapOptions The configuration options to be used.
     * @return MapFragment created.
     */
    @NonNull
    public static NearbyMapFragment newInstance(@Nullable MapboxMapOptions mapboxMapOptions) {
        NearbyMapFragment mapFragment = new NearbyMapFragment();
        mapFragment.setArguments(MapFragmentUtils.createFragmentArgs(mapboxMapOptions));
        return mapFragment;
    }

    /**
     * Called when the context attaches to this fragment.
     *
     * @param context the context attaching
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapFragment.OnMapViewReadyCallback) {
            mapViewReadyCallback = (MapFragment.OnMapViewReadyCallback) context;
        }
    }

    /**
     * Called when this fragment is inflated, parses XML tag attributes.
     *
     * @param context            The context inflating this fragment.
     * @param attrs              The XML tag attributes.
     * @param savedInstanceState The saved instance state for the map fragment.
     */
    @Override
    public void onInflate(@NonNull Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);
        setArguments(MapFragmentUtils.createFragmentArgs(MapboxMapOptions.createFromAttributes(context, attrs)));
    }

    /**
     * Creates the fragment view hierarchy.
     *
     * @param inflater           Inflater used to inflate content.
     * @param container          The parent layout for the map fragment.
     * @param savedInstanceState The saved instance state for the map fragment.
     * @return The view created
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Context context = inflater.getContext();
        map = new MapView(context, MapFragmentUtils.resolveArgs(context, getArguments()));
        return map;
    }

    /**
     * Called when the fragment view hierarchy is created.
     *
     * @param view               The content view of the fragment
     * @param savedInstanceState THe saved instance state of the framgnt
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        map.onCreate(savedInstanceState);
        map.getMapAsync(this);

        // notify listeners about MapView creation
        if (mapViewReadyCallback != null) {
            mapViewReadyCallback.onMapViewReady(map);
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        for (OnMapReadyCallback onMapReadyCallback : mapReadyCallbackList) {
            onMapReadyCallback.onMapReady(mapboxMap);
        }
    }

    /**
     * Called when the fragment is visible for the users.
     */
    @Override
    public void onStart() {
        super.onStart();
        map.onStart();
    }

    /**
     * Called when the fragment is ready to be interacted with.
     */
    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    /**
     * Called when the fragment is pausing.
     */
    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    /**
     * Called when the fragment state needs to be saved.
     *
     * @param outState The saved state
     */
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (map != null) {
            map.onSaveInstanceState(outState);
        }
    }

    /**
     * Called when the fragment is no longer visible for the user.
     */
    @Override
    public void onStop() {
        super.onStop();
        map.onStop();
    }

    /**
     * Called when the fragment receives onLowMemory call from the hosting Activity.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (map != null) {
            map.onLowMemory();
        }
    }

    /**
     * Called when the fragment is view hierarchy is being destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map.onDestroy();
    }

    /**
     * Called when the fragment is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mapReadyCallbackList.clear();
    }

    /**
     * Sets a callback object which will be triggered when the MapboxMap instance is ready to be used.
     *
     * @param onMapReadyCallback The callback to be invoked.
     */
    public void getMapAsync(@NonNull final OnMapReadyCallback onMapReadyCallback) {
        if (mapboxMap == null) {
            mapReadyCallbackList.add(onMapReadyCallback);
        } else {
            onMapReadyCallback.onMapReady(mapboxMap);
        }
    }

    /**
     * Clears map, adds nearby markers to map
     * @param latLng current location
     * @param placeList all places will be displayed on the map
     * @param selectedMarker clicked marker by user
     * @param nearbyParentFragmentPresenter presenter
     */
    @Override
    public void updateMapMarkers(LatLng latLng, List<Place> placeList
                                                , Marker selectedMarker
                                                , NearbyParentFragmentPresenter nearbyParentFragmentPresenter) {
        Timber.d("Updates map markers");
        customBaseMarkerOptions =  NearbyController
                .loadAttractionsFromLocationToBaseMarkerOptions(latLng, // Curlatlang will be used to calculate distances
                        placeList,
                        getActivity(),
                        bookmarkLocationDao.getAllBookmarksLocations());
        mapboxMap.clear();
        addNearbyMarkersToMapBoxMap(customBaseMarkerOptions, selectedMarker, nearbyParentFragmentPresenter);
        // Re-enable mapbox gestures on custom location markers load
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
    }

    /**
     * Makes map camera follow users location with animation
     * @param curLatLng current location of user
     */
    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {
        Timber.d("Updates map camera to track user position");
        CameraPosition cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        mapboxMap.setCameraPosition(cameraPosition);
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition), 1000);
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
    public void addCurrentLocationMarker(LatLng curLatLng) {
        if (null != curLatLng) {
            removeCurrentLocationMarker();
            Timber.d("Adds current location marker");

            Icon icon = IconFactory.getInstance(getContext())
                    .fromResource(R.drawable.current_location_marker);

            MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                    .position(new com.mapbox.mapboxsdk.geometry.LatLng(curLatLng.getLatitude(),
                            curLatLng.getLongitude()));
            currentLocationMarkerOptions.setIcon(icon); // Set custom icon
            currentLocationMarker = mapboxMap.addMarker(currentLocationMarkerOptions);

            List<com.mapbox.mapboxsdk.geometry.LatLng> circle = UiUtils
                    .createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                            curLatLng.getAccuracy() * 2, 100);

            PolygonOptions currentLocationPolygonOptions = new PolygonOptions()
                    .addAll(circle)
                    .strokeColor(getResources().getColor(R.color.current_marker_stroke))
                    .fillColor(getResources().getColor(R.color.current_marker_fill));
            currentLocationPolygon = mapboxMap.addPolygon(currentLocationPolygonOptions);
        } else {
            Timber.d("not adding current location marker..current location is null");
        }
    }

    @Override
    public void removeCurrentLocationMarker() {
        if (currentLocationMarker != null) {
            mapboxMap.removeMarker(currentLocationMarker);
            mapboxMap.removePolygon(currentLocationPolygon);
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
    public void filterMarkersByLabels(List<Label> selectedLabels,
                                      boolean displayExists,
                                      boolean displayNeedsPhoto,
                                      boolean filterForPlaceState,
                                      boolean filterForAllNoneType) {

        if (selectedLabels.size() == 0 && filterForPlaceState) { // If nothing is selected, display all
            greyOutAllMarkers();
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

                //updateMarker(markerPlaceGroup.getIsBookmarked(), markerPlaceGroup.getPlace(), NearbyController.currentLocation);
            }
        } else {
            // First greyed out all markers
            greyOutAllMarkers();
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

    /**
     * Greys out all markers
     */
    @Override
    public void filterOutAllMarkers() {
        greyOutAllMarkers();
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

    @Override
    public List<NearbyBaseMarker> getBaseMarkerOptions() {
        return customBaseMarkerOptions;
    }

    /**
     * Adds markers to map
     * @param baseMarkerList is markers will be added
     * @param selectedMarker is selected marker by user
     * @param nearbyParentFragmentPresenter presenter
     */
    @Override
    public void addNearbyMarkersToMapBoxMap(@Nullable List<NearbyBaseMarker> baseMarkerList
                                                        , Marker selectedMarker
                                                        , NearbyParentFragmentPresenter nearbyParentFragmentPresenter) {
        List<Marker> markers = mapboxMap.addMarkers(baseMarkerList);
        NearbyController.markerExistsMap = new HashMap<Boolean, Marker>();
        NearbyController.markerNeedPicMap = new HashMap<Boolean, Marker>();

        NearbyController.markerLabelList.clear();

        for (int i = 0; i < baseMarkerList.size(); i++) {

            NearbyBaseMarker nearbyBaseMarker = baseMarkerList.get(i);
            NearbyController.markerLabelList.add(
                    new MarkerPlaceGroup(markers.get(i), bookmarkLocationDao.findBookmarkLocation(baseMarkerList.get(i).getPlace()), nearbyBaseMarker.getPlace()));
            //TODO: fix bookmark location
            NearbyController.markerExistsMap.put((baseMarkerList.get(i).getPlace().hasWikidataLink()), markers.get(i));
            NearbyController.markerNeedPicMap.put(((baseMarkerList.get(i).getPlace().pic == null) ? true : false), markers.get(i));
        }

        map.getMapAsync(mapboxMap -> {
            mapboxMap.addMarkers(baseMarkerList);
            setMapMarkerActions(selectedMarker, nearbyParentFragmentPresenter);
        });
    }

    @Override
    public LatLng getCameraTarget() {
        return LocationUtils
                .mapBoxLatLngToCommonsLatLng(mapboxMap.getCameraPosition().target);
    }

    @Override
    public MapboxMap getMapboxMap() {
        return mapboxMap;
    }

    @Override
    public void viewsAreAssignedToPresenter(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback) {

    }

    @Override
    public void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener) {

    }

    void setMapMarkerActions(Marker selected, NearbyParentFragmentPresenter nearbyParentFragmentPresenter) {
        getMapboxMap().setOnInfoWindowCloseListener(marker -> {
            if (marker == selected) {
                nearbyParentFragmentPresenter.markerUnselected();
            }
        });

        getMapboxMap().setOnMarkerClickListener(marker -> {

            if (marker instanceof NearbyMarker) {
                nearbyParentFragmentPresenter.markerSelected(marker);
            }
            return false;
        });
    }

    /**
     * Sets marker icon according to marker status. Sets title and distance.
     * @param isBookmarked true if place is bookmarked
     * @param place
     * @param curLatLng current location
     */
    public void updateMarker(boolean isBookmarked, Place place, @Nullable LatLng curLatLng) {

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
        for (Marker marker : mapboxMap.getMarkers()) {
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
     * Greys out all markers except current location marker
     */
    public void greyOutAllMarkers() {
        VectorDrawableCompat vectorDrawable;
            vectorDrawable = VectorDrawableCompat.create(
                    getContext().getResources(), R.drawable.ic_custom_greyed_out_marker, getContext().getTheme());
        Bitmap icon = UiUtils.getBitmap(vectorDrawable);
        for (Marker marker : mapboxMap.getMarkers()) {
            if (currentLocationMarker.getTitle() != marker.getTitle()) {
                marker.setIcon(IconFactory.getInstance(getContext()).fromBitmap(icon));
            }
        }
        addCurrentLocationMarker(NearbyController.currentLocation);
    }

    /**
     * Centers the map in nearby fragment to a given place
     * @param place is new center of the map
     */
    @Override
    public void centerMapToPlace(Place place, boolean isPortraitMode) {
        Timber.d("Map is centered to place");
        double cameraShift;
        if (isPortraitMode) {
            cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
        } else {
            cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
        }
        CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(
                        new LatLng(place.location.getLatitude()-cameraShift,
                                place.getLocation().getLongitude(),
                                0))) // Sets the new camera position
                .zoom(ZOOM_LEVEL) // Same zoom level
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
    }
}

