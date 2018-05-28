package fr.free.nrw.commons.explore.images;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;


class SearchImagesAdapterFactory {
    private final SearchImagesRenderer.ImageClickedListener listener;

    SearchImagesAdapterFactory(SearchImagesRenderer.ImageClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<SearchImageItem> create(List<SearchImageItem> searchImageItemList) {
        RendererBuilder<SearchImageItem> builder = new RendererBuilder<SearchImageItem>()
                .bind(SearchImageItem.class, new SearchImagesRenderer(listener));
        ListAdapteeCollection<SearchImageItem> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<SearchImageItem>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
