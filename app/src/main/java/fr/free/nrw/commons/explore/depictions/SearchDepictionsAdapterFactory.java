package fr.free.nrw.commons.explore.depictions;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

public class SearchDepictionsAdapterFactory {
    private final SearchDepictionsRenderer.DepictsClickedListener listener;

    public SearchDepictionsAdapterFactory(SearchDepictionsRenderer.DepictsClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<DepictedItem> create(List<DepictedItem> searchImageItemList) {
        RendererBuilder<DepictedItem> builder = new RendererBuilder<DepictedItem>().bind(DepictedItem.class, new SearchDepictionsRenderer(listener));
        ListAdapteeCollection<DepictedItem> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<DepictedItem>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
