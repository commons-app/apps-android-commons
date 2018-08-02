package fr.free.nrw.commons.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

class CategoriesRenderer extends Renderer<CategoryItem> {
    @BindView(R.id.tvName)
    TextView tvName;
    @BindView(R.id.categoryCheckbox)
    CheckBox categoryCheckbox;
    @BindView(R.id.viewMoreIcon)
    ImageView viewMoreIcon;
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
        tvName.setOnClickListener(v -> {
            CategoryItem item = getContent();
            if (listener != null) {
                listener.categoryViewMoreClicked(item);
            }
        });
        viewMoreIcon.setOnClickListener(v -> {
            CategoryItem item = getContent();
            if (listener != null) {
                listener.categoryViewMoreClicked(item);
            }
        });
        categoryCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CategoryItem item = getContent();
            item.setSelected(isChecked);
            if (listener != null) {
                listener.categoryClicked(item);
            }
        });
    }

    @Override
    public void render() {
        CategoryItem item = getContent();
        categoryCheckbox.setChecked(item.isSelected());
        tvName.setText(item.getName());
    }

    interface CategoryClickedListener {
        void categoryClicked(CategoryItem item);
        void categoryViewMoreClicked(CategoryItem item);
    }
}
