package fr.free.nrw.commons.explore.categories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

/**
 * presentation logic of individual category in search is handled here
 */

class SearchCategoriesRenderer extends Renderer<String> {
    @BindView(R.id.textView1) TextView tvCategoryName;

    private final ImageClickedListener listener;

    SearchCategoriesRenderer(ImageClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.item_recent_searches, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {
        view.setOnClickListener(v -> {
            String item = getContent();
            if (listener != null) {
                listener.imageClicked(item);
            }
        });
    }

    @Override
    public void render() {
        String item = getContent();
        tvCategoryName.setText(item);
    }

    interface ImageClickedListener {
        void imageClicked(String item);
    }

}
