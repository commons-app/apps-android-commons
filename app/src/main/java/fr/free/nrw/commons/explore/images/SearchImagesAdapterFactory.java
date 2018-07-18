package fr.free.nrw.commons.explore.images;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.Media;

/**
 * This class helps in creating adapter for imagesRecyclerView in SearchImagesFragment,
 * implementing onClicks on imagesRecyclerView Items
 **/
class SearchImagesAdapterFactory {
    private final SearchImagesRenderer.ImageClickedListener listener;

    SearchImagesAdapterFactory(SearchImagesRenderer.ImageClickedListener listener) {
        this.listener = listener;
    }

    /**
     * This method creates a recyclerViewAdapter for Media.
     * @param searchImageItemList List of Media objects to be displayed
     * @return imagesAdapter
     **/
    public RVRendererAdapter<Media> create(List<Media> searchImageItemList) {
        RendererBuilder<Media> builder = new RendererBuilder<Media>()
                .bind(Media.class, new SearchImagesRenderer(listener));
        ListAdapteeCollection<Media> collection = new ListAdapteeCollection<>(
                searchImageItemList != null ? searchImageItemList : Collections.<Media>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
