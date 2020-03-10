package fr.free.nrw.commons.explore.depictions;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

/**
 * Adapter factory for Items in Explore
 */

public class SearchDepictionsAdapterFactory {
    private final SearchDepictionsRenderer.DepictCallback listener;

    public SearchDepictionsAdapterFactory(SearchDepictionsRenderer.DepictCallback listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<DepictedItem> create() {
        List<DepictedItem> searchImageItemList = new ArrayList<>();
        RendererBuilder<DepictedItem> builder = new RendererBuilder<DepictedItem>().bind(DepictedItem.class, new SearchDepictionsRenderer(listener));
        ListAdapteeCollection<DepictedItem> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<DepictedItem>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
