package fr.free.nrw.commons.upload;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.upload.structure.depicts.DepictsClickedListener;

public class UploadDepictsRenderer extends Renderer<DepictedItem> {
    private final DepictsClickedListener listener;
    @BindView(R.id.tvName)
    CheckBox checkedView;

    public UploadDepictsRenderer(DepictsClickedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void setUpView(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    @Override
    protected void hookListeners(View rootView) {
        rootView.setOnClickListener(v -> {
            DepictedItem item = getContent();
            item.setSelected(!item.isSelected());
            checkedView.setChecked(item.isSelected());
            if (listener != null) {
                listener.depictsClicked(item);
            }
        });
    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.layout_upload_depicts_item, parent, false);
    }

    @Override
    public void render() {
        DepictedItem item = getContent();
        checkedView.setChecked(item.isSelected());
        checkedView.setText(item.getDepictsLabel());
    }
}
