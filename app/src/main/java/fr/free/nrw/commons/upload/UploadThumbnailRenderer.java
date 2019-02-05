package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.pedrogomez.renderers.Renderer;

import java.io.File;

import fr.free.nrw.commons.R;

class UploadThumbnailRenderer extends Renderer<UploadModel.UploadItem> {
    private ThumbnailClickedListener listener;
    private SimpleDraweeView background;
    private View space;
    private ImageView error;

    public UploadThumbnailRenderer(ThumbnailClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.item_upload_thumbnail, parent, false);
    }

    @Override
    protected void setUpView(View rootView) {
        error = rootView.findViewById(R.id.error);
        space = rootView.findViewById(R.id.left_space);
        background = rootView.findViewById(R.id.thumbnail);
    }

    @Override
    protected void hookListeners(View rootView) {
        background.setOnClickListener(v -> listener.thumbnailClicked(getContent()));
    }

    @Override
    public void render() {
        UploadModel.UploadItem content = getContent();
        Uri uri = Uri.parse(content.getMediaUri().toString());
        background.setImageURI(Uri.fromFile(new File(String.valueOf(uri))));
        background.setAlpha(content.isSelected() ? 1.0f : 0.5f);
        space.setVisibility(content.isFirst() ? View.VISIBLE : View.GONE);
        error.setVisibility(content.isVisited() && content.isError() ? View.VISIBLE : View.GONE);
    }

}
