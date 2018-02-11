package fr.free.nrw.commons.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

class CategoriesRenderer extends Renderer<CategoryItem> {
    @BindView(R.id.tvName) CheckedTextView checkedView;
    private final CategoryClickedListener listener;

    CategoriesRenderer(CategoryClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.layout_categories_item, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void hookListeners(View view) {
        view.setOnClickListener(v -> {
            CategoryItem item = getContent();
            item.setSelected(!item.isSelected());
            checkedView.setChecked(item.isSelected());
            if (listener != null) {
                listener.categoryClicked(item);
            }
        });
    }

    @Override
    public void render() {
        CategoryItem item = getContent();
        checkedView.setChecked(item.isSelected());
        checkedView.setText(item.getName());
    }

    interface CategoryClickedListener {
        void categoryClicked(CategoryItem item);
    }
}
