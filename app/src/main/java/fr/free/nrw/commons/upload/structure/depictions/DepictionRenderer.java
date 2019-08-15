package fr.free.nrw.commons.upload.structure.depictions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

public class DepictionRenderer extends Renderer<DepictedItem> {
    @BindView(R.id.depict_checkbox)
    CheckedTextView checkedView;
    private final UploadDepictsCallback listener;
    @BindView(R.id.depicts_label)
    TextView depictsLabel;
    @BindView(R.id.description) TextView description;

    public DepictionRenderer(UploadDepictsCallback listener) {
        this.listener = listener;
    }

    @Override
    protected void setUpView(View rootView) {
            ButterKnife.bind(this, rootView);
    }

    @Override
    protected void hookListeners(View rootView) {
        rootView.setOnClickListener( v -> {
            DepictedItem item = getContent();
            item.setSelected(true);
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
    }
}
