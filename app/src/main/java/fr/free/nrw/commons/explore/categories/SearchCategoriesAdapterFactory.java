package fr.free.nrw.commons.explore.categories;

import android.content.Context;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

/**
 * This class helps in creating adapter for categoriesRecyclerView in SearchCategoryFragment,
 * implementing onClicks on categoriesRecyclerView Items
 **/
public class SearchCategoriesAdapterFactory {
    private final SearchCategoriesRenderer.CategoryClickedListener listener;

    public SearchCategoriesAdapterFactory(SearchCategoriesRenderer.CategoryClickedListener listener) {
        this.listener = listener;
    }

    /**
     * This method creates a recyclerViewAdapter for Categories.
     * @param searchImageItemList List of category name to be displayed
     * @return categoriesAdapter
     **/
    public RVRendererAdapter<String> create(List<String> searchImageItemList, Context context) {
        SearchCategoriesRenderer searchCategoriesRenderer = new SearchCategoriesRenderer(listener, context);
        RendererBuilder<String> builder = new RendererBuilder<String>().bind(String.class, searchCategoriesRenderer);
        ListAdapteeCollection<String> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<String>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
