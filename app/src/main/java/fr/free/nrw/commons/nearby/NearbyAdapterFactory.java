package fr.free.nrw.commons.nearby;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

class NearbyAdapterFactory {

    public RVRendererAdapter<Place> create(List<Place> placeList) {
        RendererBuilder<Place> builder = new RendererBuilder<Place>()
                .bind(Place.class, new PlaceRenderer());
        ListAdapteeCollection<Place> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }

    public void updateAdapterData(List<Place> newPlaceList, RVRendererAdapter<Place> rendererAdapter) {
        rendererAdapter.notifyDataSetChanged();
        rendererAdapter.diffUpdate(newPlaceList);
    }
}