package fr.free.nrw.commons.upload;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.upload.structure.depicts.DepictsClickedListener;

/**
 * Adapter Factory for DepictsClicked Listener
 */

public class UploadDepictsAdapterFactory {
    private final DepictsClickedListener listener;

    public UploadDepictsAdapterFactory(DepictsClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<DepictedItem> create(List<DepictedItem> itemList) {
        RendererBuilder<DepictedItem> builder = new RendererBuilder<DepictedItem>()
                .bind(DepictedItem.class, new UploadDepictsRenderer(listener));
        ListAdapteeCollection<DepictedItem> collection = new ListAdapteeCollection<>(
                itemList != null ? itemList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
