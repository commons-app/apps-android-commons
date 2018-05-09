package fr.free.nrw.commons.upload;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class UploadThumbnailsAdapterFactory {
    private ThumbnailClickedListener listener;

    UploadThumbnailsAdapterFactory(ThumbnailClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<UploadModel.UploadItem> create(List<UploadModel.UploadItem> placeList) {
        RendererBuilder<UploadModel.UploadItem> builder = new RendererBuilder<UploadModel.UploadItem>()
                .bind(UploadModel.UploadItem.class, new UploadThumbnailRenderer(listener));
        ListAdapteeCollection<UploadModel.UploadItem> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
