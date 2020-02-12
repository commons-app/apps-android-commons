package fr.free.nrw.commons.upload;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;
import fr.free.nrw.commons.category.CategoryClickedListener;
import fr.free.nrw.commons.category.CategoryItem;
import java.util.Collections;
import java.util.List;

public class UploadCategoriesAdapterFactory {
  private final CategoryClickedListener listener;

  public UploadCategoriesAdapterFactory(CategoryClickedListener listener) {
    this.listener = listener;
  }

  public RVRendererAdapter<CategoryItem> create(List<CategoryItem> placeList) {
    RendererBuilder<CategoryItem> builder =
        new RendererBuilder<CategoryItem>()
            .bind(CategoryItem.class, new UploadCategoriesRenderer(listener));
    ListAdapteeCollection<CategoryItem> collection =
        new ListAdapteeCollection<>(placeList != null ? placeList : Collections.emptyList());
    return new RVRendererAdapter<>(builder, collection);
  }
}
