package fr.free.nrw.commons.explore.categories;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

class SearchCategoriesAdapterFactory {
    private final SearchCategoriesRenderer.CategoryClickedListener listener;

    SearchCategoriesAdapterFactory(SearchCategoriesRenderer.CategoryClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<String> create(List<String> searchImageItemList) {
        RendererBuilder<String> builder = new RendererBuilder<String>().bind(String.class, new SearchCategoriesRenderer(listener));
        ListAdapteeCollection<String> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<String>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
