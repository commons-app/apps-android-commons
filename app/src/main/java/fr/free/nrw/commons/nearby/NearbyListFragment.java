package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

public class NearbyListFragment extends DaggerFragment {
    private Bundle bundleForUpdates; // Carry information from activity about changed nearby places and current location

    private static final Type LIST_TYPE = new TypeToken<List<Place>>() {
    }.getType();
    private static final Type CUR_LAT_LNG_TYPE = new TypeToken<LatLng>() {
    }.getType();

    private NearbyAdapterFactory adapterFactory;
    private RecyclerView recyclerView;

    @Inject ContributionController controller;
    @Inject @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore;
    @Inject @Named("default_preferences") BasicKvStore defaultKvStore;
    @Inject Gson gson;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
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
     * Updates nearby list elements all together
     */
    public void updateNearbyListSignificantly() {
        try {
            adapterFactory.updateAdapterData(getPlaceListFromBundle(bundleForUpdates), (RVRendererAdapter<Place>) recyclerView.getAdapter());
        } catch (NullPointerException e) {
            Timber.e("Null pointer exception from calling recyclerView.getAdapter()");
        }
    }

    /**
     * While nearby updates for current location held with bundle, automatically, custom updates are
     * done by calling this method, triggered by search this are button input from user.
     * @param placeList List of nearby places to be added list fragment
     */
    public void updateNearbyListSignificantlyForCustomLocation(List<Place> placeList) {
        try {
            adapterFactory.updateAdapterData(placeList, (RVRendererAdapter<Place>) recyclerView.getAdapter());
        } catch (NullPointerException e) {
            Timber.e("Null pointer exception from calling recyclerView.getAdapter()");
        }
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

    /**
     * Sets bundles for updates in map. Ie. user is moved too much so we need to update nearby markers.
     * @param bundleForUpdates includes new calculated nearby places.
     */
    public void setBundleForUpdates(Bundle bundleForUpdates) {
        this.bundleForUpdates = bundleForUpdates;
    }

}
