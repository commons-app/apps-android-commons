package fr.free.nrw.commons.nearby.mvp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.gson.Gson;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyListFragment;
import fr.free.nrw.commons.nearby.NearbyMapFragment;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;

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
    LocationServiceManager locationManager;
    @Inject
    NearbyController nearbyController;
    @Inject
    WikidataEditListener wikidataEditListener;
    @Inject
    Gson gson;

    public fr.free.nrw.commons.nearby.NearbyMapFragment nearbyMapFragment;
    private fr.free.nrw.commons.nearby.NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();

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
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeFragment();
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

    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {

    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {

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
}
