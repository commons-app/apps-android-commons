package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
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
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.mvp.presenter.NearbyParentFragmentPresenter;
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
public class SupportMapFragment extends CommonsDaggerSupportFragment
                                implements OnMapReadyCallback,
                                            NearbyMapContract.View{

    @Inject
    BookmarkLocationsDao bookmarkLocationDao;

    private final List<OnMapReadyCallback> mapReadyCallbackList = new ArrayList<>();
    private MapFragment.OnMapViewReadyCallback mapViewReadyCallback;
    private MapboxMap mapboxMap;
    private MapView map;

    /**
     * Creates a default MapFragment instance
     *
     * @return MapFragment created
     */
    public static SupportMapFragment newInstance() {
        return new SupportMapFragment();
    }

    /**
     * Creates a MapFragment instance
     *
     * @param mapboxMapOptions The configuration options to be used.
     * @return MapFragment created.
     */
    @NonNull
    public static SupportMapFragment newInstance(@Nullable MapboxMapOptions mapboxMapOptions) {
        SupportMapFragment mapFragment = new SupportMapFragment();
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

    @Override
    public void updateMapMarkers(LatLng latLng, List<Place> placeList
                                                , Marker selectedMarker
                                                , NearbyParentFragmentPresenter nearbyParentFragmentPresenter) {
        Log.d("denemeTest","updateMapMarkers, curLatng:"+latLng);
        List<NearbyBaseMarker> customBaseMarkerOptions =  NearbyController
                .loadAttractionsFromLocationToBaseMarkerOptions(latLng, // Curlatlang will be used to calculate distances
                        placeList,
                        getActivity(),
                        bookmarkLocationDao.getAllBookmarksLocations());
        mapboxMap.clear();
        // TODO: set search latlang here

        /*mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition), 1000);*/
        // TODO: set position depening to botom sheet position heere
        // We are trying to find nearby places around our custom searched area, thus custom parameter is nonnull
        addNearbyMarkersToMapBoxMap(customBaseMarkerOptions, selectedMarker, nearbyParentFragmentPresenter);
        // Re-enable mapbox gestures on custom location markers load
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
    }

    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {
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
     */
    @Override
    public void addCurrentLocationMarker(LatLng curLatLng) {
        Log.d("denemeTest","addCurrentLocationMarker");
        Timber.d("addCurrentLocationMarker is called");

        Icon icon = IconFactory.getInstance(getContext()).fromResource(R.drawable.current_location_marker);

        MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                .position(new com.mapbox.mapboxsdk.geometry.LatLng(curLatLng.getLatitude(), curLatLng.getLongitude()));
        currentLocationMarkerOptions.setIcon(icon); // Set custom icon

        Marker currentLocationMarker = mapboxMap.addMarker(currentLocationMarkerOptions);

        List<com.mapbox.mapboxsdk.geometry.LatLng> circle = createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                curLatLng.getAccuracy() * 2, 100);

        PolygonOptions currentLocationPolygonOptions = new PolygonOptions()
                .addAll(circle)
                .strokeColor(Color.parseColor("#55000000"))
                .fillColor(Color.parseColor("#11000000"));
        mapboxMap.addPolygon(currentLocationPolygonOptions);
    }

    //TODO: go to util
    /**
     * Creates a series of points that create a circle on the map.
     * Takes the center latitude, center longitude of the circle,
     * the radius in meter and the number of nodes of the circle.
     *
     * @return List List of LatLng points of the circle.
     */
    private List<com.mapbox.mapboxsdk.geometry.LatLng> createCircleArray(
            double centerLat, double centerLong, float radius, int nodes) {
        List<com.mapbox.mapboxsdk.geometry.LatLng> circle = new ArrayList<>();
        float radiusKilometer = radius / 1000;
        double radiusLong = radiusKilometer
                / (111.320 * Math.cos(centerLat * Math.PI / 180));
        double radiusLat = radiusKilometer / 110.574;

        for (int i = 0; i < nodes; i++) {
            double theta = ((double) i / (double) nodes) * (2 * Math.PI);
            double nodeLongitude = centerLong + radiusLong * Math.cos(theta);
            double nodeLatitude = centerLat + radiusLat * Math.sin(theta);
            circle.add(new com.mapbox.mapboxsdk.geometry.LatLng(nodeLatitude, nodeLongitude));
        }
        return circle;
    }

    @Override
    public void addNearbyMarkersToMapBoxMap(@Nullable List<NearbyBaseMarker> baseMarkerList
                                                        , Marker selectedMarker
                                                        , NearbyParentFragmentPresenter nearbyParentFragmentPresenter) {
        Log.d("denemeTest","add markers to map");
        mapboxMap.addMarkers(baseMarkerList);
        mapboxMap.setOnInfoWindowCloseListener(marker -> {
            /*if (marker == selected) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }*/
        });
        map.getMapAsync(mapboxMap -> {
            mapboxMap.addMarkers(baseMarkerList);
            //fabRecenter.setVisibility(View.VISIBLE);
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

    public void updateMarker(boolean isBookmarked, Place place, LatLng curLatLng) {

        VectorDrawableCompat vectorDrawable;
        if (isBookmarked) {
            vectorDrawable = VectorDrawableCompat.create(
                    getContext().getResources(), R.drawable.ic_custom_bookmark_marker, getContext().getTheme()
            );
        } else {
            vectorDrawable = VectorDrawableCompat.create(
                    getContext().getResources(), R.drawable.ic_custom_map_marker, getContext().getTheme()
            );
        }
        for (Marker marker : mapboxMap.getMarkers()) {
            if (marker.getTitle() != null && marker.getTitle().equals(place.getName())) {

                Bitmap icon = UiUtils.getBitmap(vectorDrawable);
                String distance = formatDistanceBetween(curLatLng, place.location);
                place.setDistance(distance);

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

}

