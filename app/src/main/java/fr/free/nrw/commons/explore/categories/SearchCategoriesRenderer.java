package fr.free.nrw.commons.explore.categories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

/**
 * presentation logic of individual category in search is handled here
 **/
class SearchCategoriesRenderer extends Renderer<String> {
    TextView tvCategoryName;

    private final CategoryClickedListener listener;
    private boolean currentThemeIsDark;

    SearchCategoriesRenderer(CategoryClickedListener listener, boolean currentThemeIsDark) {
        this.listener = listener;
        this.currentThemeIsDark = currentThemeIsDark;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (currentThemeIsDark) {
            return layoutInflater.inflate(R.layout.item_recent_searches_dark_theme, viewGroup, false);
        }
        return layoutInflater.inflate(R.layout.item_recent_searches, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        if (currentThemeIsDark) {
            tvCategoryName = view.findViewById(R.id.textView2);
        } else {
            tvCategoryName = view.findViewById(R.id.textView1);
        }
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {
        view.setOnClickListener(v -> {
            String item = getContent();
            if (listener != null) {
                listener.categoryClicked(item);
            }
        });
    }

    @Override
    public void render() {
        String item = getContent();
        tvCategoryName.setText(item.replaceFirst("^Category:", ""));
    }

    public interface CategoryClickedListener {
        void categoryClicked(String item);
    }

}
