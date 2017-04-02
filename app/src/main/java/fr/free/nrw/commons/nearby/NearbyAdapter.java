package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import fr.free.nrw.commons.R;

public class NearbyAdapter extends ArrayAdapter<Place> {
    private List<Place> placesList;
    private Context context;

    public List<Place> getPlacesList() {
        return placesList;
    }

    /** Accepts activity context and list of places
     * @param context
     * @param places
     */
    public NearbyAdapter(Context context, List<Place> places) {
        super(context, R.layout.item_place, places);
        this.context = context;
        placesList = places;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Place place = getItem(position);
        Log.d("NearbyAdapter", "Place " + place.name);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_place, parent, false);
        }

        NearbyViewHolder viewHolder = new NearbyViewHolder(convertView);
        viewHolder.bindModel(context, place);
        // Return the completed view to render on screen
        return convertView;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}
