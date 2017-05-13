package fr.free.nrw.commons.nearby;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.utils.UriDeserializer;
import timber.log.Timber;

public class NearbyListFragment extends ListFragment  {

    //private NearbyAsyncTask nearbyAsyncTask;
    private Gson gson;
    private List<Place> placeList;
    private LatLng curLatLng;

    @BindView(R.id.listView) ListView listview;


    private NearbyAdapter adapter;

    public NearbyListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriDeserializer())
                .create();
        if (bundle != null){
            String gsonPlaceList = bundle.getString("PlaceList");
            String gsonLatLng = bundle.getString("CurLatLng");
            Type listType = new TypeToken<List<Place>>() {}.getType();
            placeList = gson.fromJson(gsonPlaceList, listType);
            Type curLatLngType = new TypeToken<LatLng>() {}.getType();
            curLatLng = gson.fromJson(gsonLatLng, curLatLngType);
            NearbyController.loadAttractionsFromLocationToPlaces(curLatLng, placeList);
        }
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
        if (savedInstanceState == null) {
            adapter.clear();
            //nearbyAsyncTask = new NearbyAsyncTask();
            //nearbyAsyncTask.execute();
            //progressBar.setVisibility(View.VISIBLE);
            Timber.d("Saved instance state is null, populating ListView");
        } else {
            //progressBar.setVisibility(View.GONE);
        }

        // If we are returning here from a screen orientation and the AsyncTask is still working,
        // re-create and display the progress dialog.
        if (isTaskRunning()) {
            //progressBar.setVisibility(View.VISIBLE);
        }

        adapter.clear();
        adapter.addAll(placeList);
        adapter.notifyDataSetChanged();
    }

    private boolean isTaskRunning() {
        //return nearbyAsyncTask != null && nearbyAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
        return false;
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
       /* if (progressBar != null && progressBar.isShown()) {
            progressBar.setVisibility(View.GONE);
        }*/
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // See http://stackoverflow.com/questions/18264408/incomplete-asynctask-crashes-my-app
        if (isTaskRunning()) {
            //nearbyAsyncTask.cancel(true);
        }
    }
/*
    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            return NearbyController.loadAttractionsFromLocationToPlaces(
                    ((NearbyActivity)getActivity())
                            .getLocationManager()
                            .getLatestLocation(), getActivity()
            );
        }

        @Override
        protected void onPostExecute(List<Place> places) {
            super.onPostExecute(places);

            if (isCancelled()) {
                return;
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            adapter.clear();
            adapter.addAll(places);
            adapter.notifyDataSetChanged();
        }
    }
    */

    @OnItemClick(R.id.listView)
    void onItemClicked(int position) {
        Place place = (Place) listview.getItemAtPosition(position);
        LatLng placeLatLng = place.location;

        double latitude = placeLatLng.latitude;
        double longitude = placeLatLng.longitude;

        Timber.d("Item at position %d has coords: Lat: %f Long: %f", position, latitude, longitude);

        //Open map app at given position
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }
}
