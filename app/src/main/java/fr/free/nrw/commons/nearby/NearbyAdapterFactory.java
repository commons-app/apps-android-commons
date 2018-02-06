package fr.free.nrw.commons.nearby;

import android.support.v4.app.Fragment;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

class NearbyAdapterFactory {

    private Fragment fragment;

    NearbyAdapterFactory(){

    }

    NearbyAdapterFactory(Fragment fragment) {
        this.fragment = fragment;
    }

    public RVRendererAdapter<Place> create(List<Place> placeList) {
        RendererBuilder<Place> builder = new RendererBuilder<Place>()
                .bind(Place.class, new PlaceRenderer(fragment));
        ListAdapteeCollection<Place> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}