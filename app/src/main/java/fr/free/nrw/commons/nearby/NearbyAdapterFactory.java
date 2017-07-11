package fr.free.nrw.commons.nearby;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

class NearbyAdapterFactory {
    private PlaceRenderer.PlaceClickedListener listener;

    NearbyAdapterFactory(PlaceRenderer.PlaceClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<Place> create(List<Place> placeList) {
        RendererBuilder<Place> builder = new RendererBuilder<Place>()
                .bind(Place.class, new PlaceRenderer(listener));
        ListAdapteeCollection<Place> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.<Place>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
