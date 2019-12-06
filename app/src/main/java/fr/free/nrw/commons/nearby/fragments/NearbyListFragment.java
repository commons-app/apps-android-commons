package fr.free.nrw.commons.nearby.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyAdapterFactory;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import timber.log.Timber;

public class NearbyListFragment extends CommonsDaggerSupportFragment implements NearbyParentFragmentContract.NearbyListView {

    private static final Type LIST_TYPE = new TypeToken<List<Place>>() {
    }.getType();
    private static final Type CUR_LAT_LNG_TYPE = new TypeToken<LatLng>() {
    }.getType();

    private NearbyAdapterFactory adapterFactory;
    private RecyclerView recyclerView;

    @Inject ContributionController controller;
    @Inject Gson gson;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("NearbyListFragment created");
        View view = inflater.inflate(R.layout.fragment_nearby_list, container, false);
        recyclerView = view.findViewById(R.id.listView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterFactory = new NearbyAdapterFactory(this, controller);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Check that this is the first time view is created,
        // to avoid double list when screen orientation changed
        Bundle bundle = this.getArguments();
        recyclerView.setAdapter(adapterFactory.create(getPlaceListFromBundle(bundle)));
    }

    /**
     * When user moved too much, we need to update nearby list too. This operation is made by passing
     * a bundle from NearbyFragment to NearbyListFragment and NearbyMapFragment. This method extracts
     * place list from bundle to a list variable.
     * @param bundle Bundle passed from NearbyFragment on users significant moving
     * @return List of new nearby places
     */
    private List<Place> getPlaceListFromBundle(Bundle bundle) {
        List<Place> placeList = Collections.emptyList();

        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList", "[]");
            placeList = gson.fromJson(gsonPlaceList, LIST_TYPE);

            String gsonLatLng = bundle.getString("CurLatLng");
            LatLng curLatLng = gson.fromJson(gsonLatLng, CUR_LAT_LNG_TYPE);

            placeList = NearbyController.loadAttractionsFromLocationToPlaces(curLatLng, placeList);
        }

        return placeList;
    }

    @Override
    public void updateListFragment(List<Place> placeList) {
        Timber.d("Update list fragment");
        adapterFactory.updateAdapterData(placeList, (RVRendererAdapter<Place>) recyclerView.getAdapter());
    }
}
