package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryClickedListener;
import fr.free.nrw.commons.category.CategoryItem;

public class UploadCategoriesRenderer extends Renderer<CategoryItem> {
    @BindView(R.id.tvName) CheckBox checkedView;
    private final CategoryClickedListener listener;

    UploadCategoriesRenderer(CategoryClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.layout_upload_categories_item, viewGroup, false);
    }

    @Override
    protected void setUpView(View view) {
        ButterKnife.bind(this, view);
        Configuration config = getContext().getResources().getConfiguration();
        if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            checkedView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }

    @SuppressLint("WrongConstant")
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
}
