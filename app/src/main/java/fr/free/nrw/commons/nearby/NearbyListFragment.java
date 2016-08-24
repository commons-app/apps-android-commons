package fr.free.nrw.commons.nearby;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyListFragment extends ListFragment {

    private LatLng mLatestLocation;
    private int mImageSize;
    private boolean mItemClicked;
    Adapter mAdapter;

    private static final String TAG = "NearbyListFragment";

    public NearbyListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "NearbyListFragment created");

        View view = inflater.inflate(R.layout.fragment_nearby, container, false);


        // Create a progress bar to display while the list loads
        ProgressBar progressBar = new ProgressBar(getActivity());
        progressBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);
        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) view.getRootView();
        root.addView(progressBar);

        //TODO: Original is an AttractionAdapter. Not a CursorAdapter.
        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new NearbyAdapter(getActivity(), );
        setListAdapter(mAdapter);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
        return view;
    }





    private class NearbyAdapter extends ArrayAdapter {

        public List<Place> placesList;
        private Context mContext;

        public NearbyAdapter(Context context, List<Place> places) {
            super(context, 0);
            mContext = context;
            placesList = places;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Place place = (Place) getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_place, parent, false);
            }

            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tvDesc);

            // Populate the data into the template view using the data object
            tvName.setText(place.name);
            tvDesc.setText(place.description);

            // Return the completed view to render on screen
            return convertView;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View view = inflater.inflate(R.layout.list_row, parent, false);
            return new RecyclerView.ViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Attraction attraction = placesList.get(position);

            holder.mTitleTextView.setText(attraction.name);
            holder.mDescriptionTextView.setText(attraction.description);
            Glide.with(mContext)
                    .load(attraction.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .placeholder(R.drawable.empty_photo)
                    .override(mImageSize, mImageSize)
                    .into(holder.mImageView);

            String distance =
                    Utils.formatDistanceBetween(mLatestLocation, attraction.location);
            if (TextUtils.isEmpty(distance)) {
                holder.mOverlayTextView.setVisibility(View.GONE);
            } else {
                holder.mOverlayTextView.setVisibility(View.VISIBLE);
                holder.mOverlayTextView.setText(distance);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return placesList == null ? 0 : placesList.size();
        }

        @Override
        public void onItemClick(View view, int position) {
            if (!mItemClicked) {
                mItemClicked = true;
                View heroView = view.findViewById(android.R.id.icon);
                DetailActivity.launch(
                        getActivity(), mAdapter.mAttractionList.get(position).name, heroView);
            }
        }
    }
}
