package fr.free.nrw.commons.explore.categories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;

/**
 * presentation logic of individual image in search is handled here
 */

class SearchCategoriesRenderer extends Renderer<Media> {
    @BindView(R.id.categoryImageTitle) TextView tvImageName;

    private final ImageClickedListener listener;

    SearchCategoriesRenderer(ImageClickedListener listener) {
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
        tvImageName.setText(item.getFilename());
    }

    interface ImageClickedListener {
        void imageClicked(Media item);
    }

}
