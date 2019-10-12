package fr.free.nrw.commons.category;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

class CategoriesAdapterFactory {
    private final CategoryClickedListener listener;

    public CategoriesAdapterFactory(CategoryClickedListener listener) {
        this.listener = listener;
    }

    public CategoryRendererAdapter create(List<CategoryItem> placeList) {
        RendererBuilder<CategoryItem> builder = new RendererBuilder<CategoryItem>()
                .bind(CategoryItem.class, new CategoriesRenderer(listener));
        ListAdapteeCollection<CategoryItem> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new CategoryRendererAdapter(builder, collection);
    }
}
