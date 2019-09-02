package fr.free.nrw.commons.nearby;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import javax.inject.Inject;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.mvp.presenter.NearbyParentFragmentPresenter;
import fr.free.nrw.commons.utils.PermissionUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.ContributionsFragment.CONTRIBUTION_LIST_FRAGMENT_TAG;
import static fr.free.nrw.commons.nearby.NearbyTestFragmentLayersActivity.CONTRIBUTIONS_TAB_POSITION;


public class NearbyTestLayersFragment extends CommonsDaggerSupportFragment implements NearbyParentFragmentContract.View {

    @Inject
    LocationServiceManager locationManager;

    @Inject
    NearbyController nearbyController;

    NearbyParentFragmentPresenter nearbyParentFragmentPresenter;
    SupportMapFragment mapFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple, container, false);
        ButterKnife.bind(this, view);
        // Inflate the layout for this fragment
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(getActivity(), getString(R.string.mapbox_commons_app_token));

        // Create supportMapFragment
        if (savedInstanceState == null) {

            // Create fragment
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();

            // Build mapboxMap
            MapboxMapOptions options = MapboxMapOptions.createFromAttributes(getActivity(), null);
            options.camera(new CameraPosition.Builder()
                    .target(new LatLng(-52.6885, -70.1395))
                    .zoom(9)
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

                mapboxMap.setStyle(Style.SATELLITE, new Style.OnStyleLoaded() {
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
        //checkPermissionsAndPerformAction(this::registerLocationUpdates);
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
        return false;
    }

    @Override
    public void addNetworkBroadcastReceiver() {

    }

    @Override
    public void listOptionMenuItemClicked() {

    }

    @Override
    public void populatePlaces(fr.free.nrw.commons.location.LatLng curlatLng, fr.free.nrw.commons.location.LatLng searchLatLng) {
        compositeDisposable.add(Observable.fromCallable(() -> nearbyController
                .loadAttractionsFromLocation(curlatLng, searchLatLng, false, true))
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
        nearbyParentFragmentPresenter.updateMapMarkers(nearbyPlacesInfo);
    }

    @Override
    public boolean isBottomSheetExpanded() {
        return false;
    }

    @Override
    public void addSearchThisAreaButtonAction() {

    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {

    }

    @Override
    public void setSearchThisAreaProgressVisibility(boolean isVisible) {

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
}
