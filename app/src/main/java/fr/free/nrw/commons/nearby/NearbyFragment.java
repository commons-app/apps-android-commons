package fr.free.nrw.commons.nearby;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.UriSerializer;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.PERMISSION_JUST_GRANTED;

public class NearbyFragment extends CommonsDaggerSupportFragment
        implements LocationUpdateListener,
                    WikidataEditListener.WikidataP18EditListener {

    private static final int LOCATION_REQUEST = 1;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.bottom_sheet)
    LinearLayout bottomSheet;
    @BindView(R.id.bottom_sheet_details)
    LinearLayout bottomSheetDetails;
    @BindView(R.id.transparentView)
    View transparentView;
    @BindView(R.id.fab_recenter)
    View fabRecenter;

    @Inject
    LocationServiceManager locationManager;
    @Inject
    NearbyController nearbyController;
    @Inject
    WikidataEditListener wikidataEditListener;
    @Inject
    @Named("application_preferences")
    SharedPreferences applicationPrefs;

    public NearbyMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();
    private Bundle bundle;
    private BottomSheetBehavior bottomSheetBehavior; // Behavior for list bottom sheet
    private BottomSheetBehavior bottomSheetBehaviorForDetails; // Behavior for details bottom sheet

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);

        // Resume the fragment if exist
        resumeFragment();
        bundle = new Bundle();
        initBottomSheetBehaviour();
        wikidataEditListener.setAuthenticationStateListener(this);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    public void listOptionMenuIteClicked() {
        if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

    }

    private void resumeFragment() {
        // Find the retained fragment on activity restarts
        nearbyMapFragment = getMapFragment();
        nearbyListFragment = getListFragment();
    }

    private NearbyMapFragment getMapFragment() {
        return (NearbyMapFragment) getFragmentManager().findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
    }

    private void removeMapFragment() {
        if (nearbyMapFragment != null) {
            android.support.v4.app.FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(nearbyMapFragment).commit();
            nearbyMapFragment = null;
        }
    }

    private NearbyListFragment getListFragment() {
        return (NearbyListFragment) getFragmentManager().findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
    }

    private void removeListFragment() {
        if (nearbyListFragment != null) {
            android.support.v4.app.FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(nearbyListFragment).commit();
            nearbyListFragment = null;
        }
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
        // TODO initProperBottomSheetBehavior();
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehaviorForDetails = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public void prepareViewsForSheetPosition(int bottomSheetState) {
        // TODO
    }



    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        //refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        //refreshView(LOCATION_SLIGHTLY_CHANGED);
    }

    @Override
    public void onWikidataEditSuccessful() {
        //refreshView(MAP_UPDATED);
    }

    /**
     * This method should be the single point to load/refresh nearby places
     *
     * @param locationChangeType defines if location shanged significantly or slightly
     */
   /* private void refreshView(LocationServiceManager.LocationChangeType locationChangeType) {
        if (lockNearbyView) {
            return;
        }

        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            hideProgressBar();
            return;
        }

        registerLocationUpdates();
        LatLng lastLocation = locationManager.getLastLocation();

        if (curLatLng != null && curLatLng.equals(lastLocation)
                && !locationChangeType.equals(MAP_UPDATED)) { //refresh view only if location has changed
            return;
        }
        curLatLng = lastLocation;

        if (locationChangeType.equals(PERMISSION_JUST_GRANTED)) {
            curLatLng = lastKnownLocation;
        }

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
                || locationChangeType.equals(PERMISSION_JUST_GRANTED)
                || locationChangeType.equals(MAP_UPDATED)) {
            progressBar.setVisibility(View.VISIBLE);

            //TODO: This hack inserts curLatLng before populatePlaces is called (see #1440). Ideally a proper fix should be found
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonCurLatLng = gson.toJson(curLatLng);
            bundle.clear();
            bundle.putString("CurLatLng", gsonCurLatLng);

            placesDisposable = Observable.fromCallable(() -> nearbyController
                    .loadAttractionsFromLocation(curLatLng, false))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::populatePlaces,
                            throwable -> {
                                Timber.d(throwable);
                                showErrorMessage(getString(R.string.error_fetching_nearby_places));
                                progressBar.setVisibility(View.GONE);
                            });
        } else if (locationChangeType
                .equals(LOCATION_SLIGHTLY_CHANGED)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonCurLatLng = gson.toJson(curLatLng);
            bundle.putString("CurLatLng", gsonCurLatLng);
            updateMapFragment(true);
        }
    }
*/

}


