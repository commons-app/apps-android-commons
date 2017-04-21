package fr.free.nrw.commons.nearby;

import static fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween;
import static fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class NearbyListFragment extends ListFragment  {

    private static final int MAX_RESULTS = 1000;
    private NearbyAsyncTask nearbyAsyncTask;

    @BindView(R.id.listView) ListView listview;
    @BindView(R.id.progressBar) ProgressBar progressBar;

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
        if (savedInstanceState == null) {
            adapter.clear();
            nearbyAsyncTask = new NearbyAsyncTask();
            nearbyAsyncTask.execute();
            progressBar.setVisibility(View.VISIBLE);
            Timber.d("Saved instance state is null, populating ListView");
        } else {
            progressBar.setVisibility(View.GONE);
        }

        // If we are returning here from a screen orientation and the AsyncTask is still working,
        // re-create and display the progress dialog.
        if (isTaskRunning()) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    private boolean isTaskRunning() {
        return nearbyAsyncTask != null && nearbyAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (progressBar != null && progressBar.isShown()) {
            progressBar.setVisibility(View.GONE);
        }
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // See http://stackoverflow.com/questions/18264408/incomplete-asynctask-crashes-my-app
        if (isTaskRunning()) {
            nearbyAsyncTask.cancel(true);
        }
    }

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
            return loadAttractionsFromLocation(
                    ((NearbyActivity)getActivity()).getLocationManager().getLatestLocation()
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

    private List<Place> loadAttractionsFromLocation(LatLng curLatLng) {
        Timber.d("Loading attractions near %s", curLatLng);
        if (curLatLng == null) {
            return Collections.emptyList();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        List<Place> places = prefs.getBoolean("useWikidata", true)
                ? NearbyPlaces.getInstance().getFromWikidataQuery(
                        curLatLng, Locale.getDefault().getLanguage())
                : NearbyPlaces.getInstance().getFromWikiNeedsPictures();
        if (curLatLng != null) {
            Timber.d("Sorting places by distance...");
            final Map<Place, Double> distances = new HashMap<>();
            for (Place place: places) {
                distances.put(place, computeDistanceBetween(place.location, curLatLng));
            }
            Collections.sort(places,
                    new Comparator<Place>() {
                        @Override
                        public int compare(Place lhs, Place rhs) {
                            double lhsDistance = distances.get(lhs);
                            double rhsDistance = distances.get(rhs);
                            return (int) (lhsDistance - rhsDistance);
                        }
                    }
            );
        }

        places = places.subList(0, Math.min(places.size(), MAX_RESULTS));
        for (Place place: places) {
            String distance = formatDistanceBetween(curLatLng, place.location);
            place.setDistance(distance);
        }
        return places;
    }
}
