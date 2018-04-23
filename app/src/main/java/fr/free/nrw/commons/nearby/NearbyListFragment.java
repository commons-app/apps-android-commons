package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.DaggerFragment;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class NearbyListFragment extends DaggerFragment {
    private Bundle bundleForUpdates; // Carry information from activity about changed nearby places and current location

    private static final Type LIST_TYPE = new TypeToken<List<Place>>() {
    }.getType();
    private static final Type CUR_LAT_LNG_TYPE = new TypeToken<LatLng>() {
    }.getType();
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Uri.class, new UriDeserializer())
            .create();

    private NearbyAdapterFactory adapterFactory;
    private RecyclerView recyclerView;
    private ContributionController controller;

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
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        recyclerView = view.findViewById(R.id.listView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        controller = new ContributionController(this);
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

    public void updateNearbyListSignificantly() {
        adapterFactory.updateAdapterData(getPlaceListFromBundle(bundleForUpdates),
                (RVRendererAdapter<Place>) recyclerView.getAdapter());
    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult: req code = " + " perm = " + permissions + " grant =" + grantResults);

        switch (requestCode) {
            // 4 = "Read external storage" allowed when gallery selected
            case 4: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;

            // 5 = "Write external storage" allowed when camera selected
            case 5: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startCameraCapture()");
                    controller.startCameraCapture();
                }
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            controller.handleImagePicked(requestCode, data, true);
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    public void setBundleForUpdates(Bundle bundleForUpdates) {
        this.bundleForUpdates = bundleForUpdates;
    }

}