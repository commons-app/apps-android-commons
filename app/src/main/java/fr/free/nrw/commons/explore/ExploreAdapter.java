package fr.free.nrw.commons.explore;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.nearby.NearbyViewHolder;
import fr.free.nrw.commons.nearby.Place;
import timber.log.Timber;


public class ExploreAdapter extends ArrayAdapter<Place> {


    public ExploreAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Place place = getItem(position);
        Timber.v(String.valueOf(place));

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_explore, parent, false);
        }

        ExploreViewHolder viewHolder = new ExploreViewHolder(convertView, position);
        viewHolder.bindModel(getContext(), place);
        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        // TODO: use Wikidata Q-ID instead?
        return position;
    }
}

