package fr.free.nrw.commons.explore.images;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;

/**
 * presentation logic of individual image in search is handled here
 **/
class SearchImagesRenderer extends Renderer<Media> {
    @BindView(R.id.categoryImageTitle) TextView tvImageName;
    @BindView(R.id.categoryImageAuthor) TextView categoryImageAuthor;
    @BindView(R.id.categoryImageView) SimpleDraweeView browseImage;

    private final ImageClickedListener listener;

    SearchImagesRenderer(ImageClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.layout_category_images, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {
        view.setOnClickListener(v -> {
            Media item = getContent();
            if (listener != null) {
                listener.imageClicked(item);
            }
        });
    }

    @Override
    public void render() {
        Media item = getContent();
        tvImageName.setText(item.getDisplayTitle());
        browseImage.setImageURI(item.getThumbUrl());
        setAuthorView(item, categoryImageAuthor);
    }

    interface ImageClickedListener {
        void imageClicked(Media item);
    }

    /**
     * formats author name as "Uploaded by: authorName" and sets it in textview
     */
    private void setAuthorView(Media item, TextView author) {
        if (item.getCreator() != null && !item.getCreator().equals("")) {
            author.setVisibility(View.VISIBLE);
            String uploadedByTemplate = getContext().getString(R.string.image_uploaded_by);
            author.setText(String.format(uploadedByTemplate, item.getCreator()));
        } else {
            author.setVisibility(View.GONE);
        }
    }
}
