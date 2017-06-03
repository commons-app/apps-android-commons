package fr.free.nrw.commons.explore;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.Place;
import timber.log.Timber;

public class ExploreListFragment extends Fragment {

    private LocationServiceManager locationManager;
    private LatLng curLatLang;
    private NearbyAsyncTask nearbyAsyncTask;
    private ExploreAdapter adapter;

    @BindView(R.id.exploreList) GridView exploreList;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.waitingMessage) TextView waitingMessage;
    @BindView(R.id.emptyMessage) TextView emptyMessage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        locationManager = new LocationServiceManager(getActivity());
        locationManager.registerLocationManager();
        curLatLang = locationManager.getLatestLocation();
        adapter = new ExploreAdapter(getActivity(),R.layout.item_place);
        View view = inflater.inflate(R.layout.fragment_explore,null);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nearbyAsyncTask = new NearbyAsyncTask(getActivity());
        nearbyAsyncTask.execute();
        if (savedInstanceState == null) {
            adapter.clear();
            Timber.d("Saved instance state is null, populating ListView");
        }
    }

    /*
    * To get photos from nearby places
    * */
    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        private Context mContext;

        private NearbyAsyncTask (Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            return NearbyController
                    .loadPhotographedAttractionsFromLocation(curLatLang, CommonsApplication.getInstance()
                    );
        }

        @Override
        protected void onPostExecute(List<Place> placeList) {
            super.onPostExecute(placeList);

            if (isCancelled()) {
                return;
            }

            if (placeList.size() == 0) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(mContext, R.string.no_nearby, duration);
                toast.show();
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            exploreList.setAdapter(adapter);
            adapter.clear();
            adapter.addAll(placeList);
            adapter.notifyDataSetChanged();
        }
    }
}
