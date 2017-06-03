package fr.free.nrw.commons.explore;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import fr.free.nrw.commons.nearby.Place;


public class ExploreAdapter extends ArrayAdapter<Place> {

    public ExploreAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }
}

