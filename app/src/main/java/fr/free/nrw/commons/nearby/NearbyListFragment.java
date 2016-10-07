package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyListFragment extends ListFragment implements TaskListener {

    private NearbyAsyncTask nearbyAsyncTask;
    private NearbyAdapter mAdapter;
    private ListView listview;

    private ProgressBar progressBar;
    private boolean isTaskRunning = false;

    private List<Place> places;
    private LatLng mLatestLocation;

    private static final String TAG = "NearbyListFragment";

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
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        //Check that this is the first time view is created, to avoid double list when screen orientation changed
        if(savedInstanceState == null) {
            mLatestLocation = ((NearbyActivity) getActivity()).getmLatestLocation();
            listview = (ListView) getView().findViewById(R.id.listview);
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
        if(nearbyAsyncTask != null && nearbyAsyncTask.getStatus() != AsyncTask.Status.FINISHED) {
            nearbyAsyncTask.cancel(true);
        }
    }

    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        private final TaskListener listener;

        public NearbyAsyncTask (TaskListener listener) {
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
            places = loadAttractionsFromLocation(mLatestLocation);
            return places;
        }

        @Override
        protected void onPostExecute(List<Place> result) {
            super.onPostExecute(result);

            if(isCancelled()) {
                return;
            }

            progressBar.setVisibility(View.GONE);

            mAdapter = new NearbyAdapter(getActivity(), places);

            listview.setAdapter(mAdapter);

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Place place = places.get(position);
                    LatLng placeLatLng = place.location;

                    double latitude = placeLatLng.latitude;
                    double longitude = placeLatLng.longitude;

                    Log.d(TAG, "Item at position " + position + " has coords: Lat: " + latitude + " Long: " + longitude);

                    //Open map app at given position
                    Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + latitude + "," + longitude);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                    if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(mapIntent);
                    }
                }
            });
            listener.onTaskFinished(result);
            mAdapter.notifyDataSetChanged();
        }
    }

    private class NearbyAdapter extends ArrayAdapter<Place> {

        public List<Place> placesList;
        private Context mContext;

        public NearbyAdapter(Context context, List<Place> places) {
            super(context, R.layout.item_place, places);
            mContext = context;
            placesList = places;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Place place = (Place) getItem(position);
            Log.d(TAG, "Place " + place.name);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
            }

            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tvDesc);
            TextView distance = (TextView) convertView.findViewById(R.id.distance);
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);

            String quotelessName = place.name.replaceAll("^\"|\"$", "");

            // Populate the data into the template view using the data object
            tvName.setText(quotelessName);
            tvDesc.setText(place.description);
            distance.setText(place.distance);

            // See https://github.com/commons-app/apps-android-commons/issues/250
            // Most common types of desc: building, house, cottage, farmhouse, village, civil parish, church, railway station,
            // gatehouse, milestone, inn, secondary school, hotel
            switch(place.description) {
                case "building":
                    icon.setImageResource(R.drawable.round_icon_generic_building);
                    break;
                case "house":
                    icon.setImageResource(R.drawable.round_icon_house);
                    break;
                case "cottage":
                    icon.setImageResource(R.drawable.round_icon_house);
                    break;
                case "farmhouse":
                    icon.setImageResource(R.drawable.round_icon_house);
                    break;
                case "church":
                    icon.setImageResource(R.drawable.round_icon_church);
                    break;
                case "railway station":
                    icon.setImageResource(R.drawable.round_icon_railway_station);
                    break;
                case "gatehouse":
                    icon.setImageResource(R.drawable.round_icon_gatehouse);
                    break;
                case "milestone":
                    icon.setImageResource(R.drawable.round_icon_milestone);
                    break;
                case "inn":
                    icon.setImageResource(R.drawable.round_icon_house);
                    break;
                case "city":
                    icon.setImageResource(R.drawable.round_icon_city);
                    break;
                case "secondary school":
                    icon.setImageResource(R.drawable.round_icon_school);
                    break;
                case "edu":
                    icon.setImageResource(R.drawable.round_icon_school);
                    break;
                case "isle":
                    icon.setImageResource(R.drawable.round_icon_island);
                    break;
                case "mountain":
                    icon.setImageResource(R.drawable.round_icon_mountain);
                    break;
                case "airport":
                    icon.setImageResource(R.drawable.round_icon_airport);
                    break;
                case "bridge":
                    icon.setImageResource(R.drawable.round_icon_bridge);
                    break;
                case "road":
                    icon.setImageResource(R.drawable.round_icon_road);
                    break;
                case "forest":
                    icon.setImageResource(R.drawable.round_icon_forest);
                    break;
                case "park":
                    icon.setImageResource(R.drawable.round_icon_park);
                    break;
                case "river":
                    icon.setImageResource(R.drawable.round_icon_river);
                    break;
                case "waterfall":
                    icon.setImageResource(R.drawable.round_icon_waterfall);
                    break;
                default:
                    icon.setImageResource(R.drawable.round_icon_unknown);
            }

            // Return the completed view to render on screen
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private List<Place> loadAttractionsFromLocation(final LatLng curLatLng) {

        List<Place> places = NearbyPlaces.get();
        if (curLatLng != null) {
            Log.d(TAG, "Sorting places by distance...");
            Collections.sort(places,
                    new Comparator<Place>() {
                        @Override
                        public int compare(Place lhs, Place rhs) {
                            double lhsDistance = computeDistanceBetween(
                                    lhs.location, curLatLng);
                            double rhsDistance = computeDistanceBetween(
                                    rhs.location, curLatLng);
                            return (int) (lhsDistance - rhsDistance);
                        }
                    }
            );
        }

        if (places.size() > 0) {
            for (int i = 0; i < 100; i++) {
                Place place = places.get(i);
                String distance = formatDistanceBetween(mLatestLocation, place.location);
                place.setDistance(distance);
            }
        }
        return places;
    }

    private String formatDistanceBetween(LatLng point1, LatLng point2) {
        if (point1 == null || point2 == null) {
            return null;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        double distance = Math.round(computeDistanceBetween(point1, point2));

        // Adjust to KM if M goes over 1000 (see javadoc of method for note
        // on only supporting metric)
        if (distance >= 1000) {
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(distance / 1000) + "km";
        }
        return numberFormat.format(distance) + "m";
    }

    private static double computeDistanceBetween(LatLng from, LatLng to) {
        return computeAngleBetween(from, to) * 6371009.0D;
    }

    private static double computeAngleBetween(LatLng from, LatLng to) {
        return distanceRadians(Math.toRadians(from.latitude), Math.toRadians(from.longitude), Math.toRadians(to.latitude), Math.toRadians(to.longitude));
    }


    private static double distanceRadians(double lat1, double lng1, double lat2, double lng2) {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2));
    }

    private static double arcHav(double x) {
        return 2.0D * Math.asin(Math.sqrt(x));
    }

    private static double havDistance(double lat1, double lat2, double dLng) {
        return hav(lat1 - lat2) + hav(dLng) * Math.cos(lat1) * Math.cos(lat2);
    }

    private static double hav(double x) {
        double sinHalf = Math.sin(x * 0.5D);
        return sinHalf * sinHalf;
    }

}
