package fr.free.nrw.commons.upload;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;
import fr.free.nrw.commons.filepicker.UploadableFile;
import java.util.Collections;
import java.util.List;

public class UploadThumbnailsAdapterFactory {
    private ThumbnailClickedListener listener;

    UploadThumbnailsAdapterFactory(ThumbnailClickedListener listener) {
        this.listener = listener;
    }

    public RVRendererAdapter<UploadableFile> create(List<UploadableFile> placeList) {
        RendererBuilder<UploadableFile> builder = new RendererBuilder<UploadableFile>()
                .bind(UploadableFile.class, new UploadThumbnailRenderer(listener));
        ListAdapteeCollection<UploadableFile> collection = new ListAdapteeCollection<>(
                placeList != null ? placeList : Collections.emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
