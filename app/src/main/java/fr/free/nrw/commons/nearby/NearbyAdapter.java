package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import fr.free.nrw.commons.R;

import timber.log.Timber;

public class NearbyAdapter extends ArrayAdapter<Place> {

    /** Accepts activity context and list of places.
     * @param context activity context
     */
    public NearbyAdapter(Context context) {
        super(context, R.layout.item_place);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Place place = getItem(position);
        Timber.v(String.valueOf(place));

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_place, parent, false);
        }

        NearbyViewHolder viewHolder = new NearbyViewHolder(convertView);
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
