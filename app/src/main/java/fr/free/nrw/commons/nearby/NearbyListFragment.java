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
import android.util.Log;
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

public class NearbyListFragment extends ListFragment implements TaskListener {

    private static final int MAX_RESULTS = 1000;
    private NearbyAsyncTask nearbyAsyncTask;

    @BindView(R.id.listview) ListView listview;
    @BindView(R.id.progressBar) ProgressBar progressBar;

    private boolean isTaskRunning = false;

    private List<Place> places;

    private static final String TAG = NearbyListFragment.class.getName();

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

        Log.d(TAG, "NearbyListFragment created");
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        //Check that this is the first time view is created, to avoid double list when screen orientation changed
        if (savedInstanceState == null) {
            nearbyAsyncTask = new NearbyAsyncTask(this);
            nearbyAsyncTask.execute();
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Saved instance state is null, populating ListView");
        } else {
            progressBar.setVisibility(View.GONE);
        }

        // If we are returning here from a screen orientation and the AsyncTask is still working,
        // re-create and display the progress dialog.
        if (isTaskRunning) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outInstanceState) {
        // See http://stackoverflow.com/questions/8942135/listview-added-dublicate-item-in-list-when-screen-orientation-changes
        outInstanceState.putInt("value", 1);
    }

    @Override
    public void onTaskStarted() {
        isTaskRunning = true;
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTaskFinished(List<Place> result) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        isTaskRunning = false;
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
        if (nearbyAsyncTask != null && nearbyAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            nearbyAsyncTask.cancel(true);
        }
    }

    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        private final TaskListener listener;

        public NearbyAsyncTask(TaskListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            listener.onTaskStarted();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            places = loadAttractionsFromLocation(
                    ((NearbyActivity)getActivity()).getLocationManager().getLatestLocation()
            );
            return places;
        }

        @Override
        protected void onPostExecute(List<Place> result) {
            super.onPostExecute(result);

            if (isCancelled()) {
                return;
            }

            progressBar.setVisibility(View.GONE);
            NearbyAdapter adapter = new NearbyAdapter(getActivity(), places);

            listview.setAdapter(adapter);

            listener.onTaskFinished(result);
            adapter.notifyDataSetChanged();
        }
    }

    @OnItemClick(R.id.listview)
    void onItemClicked(int position) {
        Place place = places.get(position);
        LatLng placeLatLng = place.location;

        double latitude = placeLatLng.latitude;
        double longitude = placeLatLng.longitude;

        Log.d(TAG, "Item at position "
                + position + " has coords: Lat: "
                + latitude + " Long: "
                + longitude);

        //Open map app at given position
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + latitude + "," + longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    private List<Place> loadAttractionsFromLocation(LatLng curLatLng) {
        Log.d(TAG, "Loading attractions near " + curLatLng);
        if (curLatLng == null) {
            return Collections.emptyList();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        List<Place> places = prefs.getBoolean("useWikidata", true)
                ? NearbyPlaces.getInstance().getFromWikidataQuery(
                        curLatLng, Locale.getDefault().getLanguage())
                : NearbyPlaces.getInstance().getFromWikiNeedsPictures();
        if (curLatLng != null) {
            Log.d(TAG, "Sorting places by distance...");
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
