package fr.free.nrw.commons.nearby;


import androidx.fragment.app.Fragment;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.contributions.ContributionController;

public class NearbyAdapterFactory {

    private Fragment fragment;
    private ContributionController controller;

    public NearbyAdapterFactory(Fragment fragment, ContributionController controller) {
        this.fragment = fragment;
        this.controller = controller;
    }

    public RVRendererAdapter<Place> create(List<Place> placeList) {
        return create(placeList, null);
    }

    public RVRendererAdapter<Place> create(
            List<Place> placeList,
            PlaceRenderer.OnBookmarkClick onBookmarkClick
    ) {
        RendererBuilder<Place> builder = new RendererBuilder<Place>()
                .bind(Place.class, new PlaceRenderer(fragment, controller, onBookmarkClick));
        ListAdapteeCollection<Place> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }

    public void updateAdapterData(List<Place> newPlaceList, RVRendererAdapter<Place> rendererAdapter) {
        rendererAdapter.notifyDataSetChanged();
        rendererAdapter.diffUpdate(newPlaceList);
    }

    public void clear(RVRendererAdapter<Place> rendererAdapter){
        rendererAdapter.clear();
    }

    public void add(Place place, RVRendererAdapter<Place> rendererAdapter){
        rendererAdapter.add(place);
    }

    public void update(RVRendererAdapter<Place> rendererAdapter){
        rendererAdapter.notifyDataSetChanged();
    }

}
