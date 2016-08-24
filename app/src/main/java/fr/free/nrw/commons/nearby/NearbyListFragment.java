package fr.free.nrw.commons.nearby;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.R;

public class NearbyListFragment extends ListFragment {

    private LatLng mLatestLocation;
    private int mImageSize;
    private boolean mItemClicked;

    private static final String TAG = "NearbyListFragment";

    public NearbyListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "NearbyListFragment created");

        View view = inflater.inflate(R.layout.fragment_nearby, container, false);


        return view;
    }
}
