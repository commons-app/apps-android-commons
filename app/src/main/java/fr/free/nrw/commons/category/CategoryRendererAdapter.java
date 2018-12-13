package fr.free.nrw.commons.category;

import com.pedrogomez.renderers.AdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.ArrayList;

public class CategoryRendererAdapter extends RVRendererAdapter<CategoryItem> {
    CategoryRendererAdapter(RendererBuilder<CategoryItem> rendererBuilder, AdapteeCollection<CategoryItem> collection) {
        super(rendererBuilder, collection);
    }

    protected ArrayList<CategoryItem> allItems() {
        int itemCount = getItemCount();
        ArrayList<CategoryItem> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(getItem(i));
        }
        return items;
    }
}
