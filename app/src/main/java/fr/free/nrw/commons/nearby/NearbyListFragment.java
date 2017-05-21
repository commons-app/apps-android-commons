package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

public class NearbyListFragment extends ListFragment  {
    private List<Place> placeList;

    @BindView(R.id.listView) ListView listview;


    private NearbyAdapter adapter;

    public NearbyListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Timber.d("NearbyListFragment created");
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);
        adapter = new NearbyAdapter(getActivity());
        listview.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        // Check that this is the first time view is created,
        // to avoid double list when screen orientation changed
        Bundle bundle = this.getArguments();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null) {
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<LatLng>() {}.getType();
            LatLng curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            placeList = NearbyController.loadAttractionsFromLocationToPlaces(curLatLng, placeList);
        }
        if (savedInstanceState == null) {
            adapter.clear();
            Timber.d("Saved instance state is null, populating ListView");
        }

        adapter.clear();
        adapter.addAll(placeList);
        adapter.notifyDataSetChanged();
    }

    @OnItemClick(R.id.listView)
    void onItemClicked(int position) {
        Place place = (Place) listview.getItemAtPosition(position);
        LatLng placeLatLng = place.location;

        double latitude = placeLatLng.latitude;
        double longitude = placeLatLng.longitude;

        Timber.d("Item at position %d has coords: Lat: %f Long: %f", position, latitude, longitude);

        NearbyInfoDialog.showYourself(getActivity(), place);
    }
}
