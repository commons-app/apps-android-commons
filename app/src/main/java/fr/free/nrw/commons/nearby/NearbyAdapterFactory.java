package fr.free.nrw.commons.nearby;

import android.support.v4.app.Fragment;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.contributions.ContributionController;

class NearbyAdapterFactory {

    private Fragment fragment;
    private ContributionController controller;

    NearbyAdapterFactory(){

    }

    NearbyAdapterFactory(Fragment fragment, ContributionController controller) {
        this.fragment = fragment;
        this.controller = controller;
    }

    public RVRendererAdapter<Place> create(List<Place> placeList) {
        RendererBuilder<Place> builder = new RendererBuilder<Place>()
                .bind(Place.class, new PlaceRenderer(fragment, controller));
        ListAdapteeCollection<Place> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}