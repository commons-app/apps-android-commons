package fr.free.nrw.commons.explore.categories;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.explore.images.SearchImagesRenderer;


class SearchCategoriesAdapterFactory {
    private final SearchImagesRenderer.ImageClickedListener listener;

    SearchCategoriesAdapterFactory(SearchImagesRenderer.ImageClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<Media> create(List<Media> searchImageItemList) {
        RendererBuilder<Media> builder = new RendererBuilder<Media>()
                .bind(Media.class, new SearchImagesRenderer(listener));
        ListAdapteeCollection<Media> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<Media>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
