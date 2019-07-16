package fr.free.nrw.commons.upload;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.upload.structure.depicts.DepictsClickedListener;

/**
 * Depicts Renderer for setting up inflating layout,
 * and setting views for the layout of each depicted Item
 */

public class UploadDepictsRenderer extends Renderer<DepictedItem> {
    private final DepictsClickedListener listener;
    @BindView(R.id.tvName)
    CheckBox checkedView;
    @BindView(R.id.thumbnail)
    ImageView thumbnail;
    @BindView(R.id.depicts_label)
    TextView depictsLabel;
    @BindView(R.id.description) TextView description;

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
        depictsLabel.setText(item.getDepictsLabel());
        description.setText(item.getDescription());
        thumbnail.setImageResource(R.drawable.empty_photo);
    }
}
