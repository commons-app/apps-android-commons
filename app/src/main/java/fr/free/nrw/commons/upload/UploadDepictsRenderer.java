package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.view.SimpleDraweeView;
import com.pedrogomez.renderers.Renderer;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.upload.structure.depictions.UploadDepictsCallback;

/**
 * Depicts Renderer for setting up inflating layout,
 * and setting views for the layout of each depicted Item
 */
public class UploadDepictsRenderer extends Renderer<DepictedItem> {
    private final UploadDepictsCallback listener;
    @BindView(R.id.depict_checkbox)
    CheckBox checkedView;
    @BindView(R.id.depicts_label)
    TextView depictsLabel;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.depicted_image)
    SimpleDraweeView imageView;
    private final static String NO_IMAGE_FOR_DEPICTION="No Image for Depiction";

    public UploadDepictsRenderer(UploadDepictsCallback listener) {
        this.listener = listener;
    }

    @Override
    protected void setUpView(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    /**
     * Setup OnClicklisteners on the views
     */
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
        checkedView.setOnClickListener(v -> {
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

    /**
     * initialise views for every item in the adapter
     */
    @Override
    public void render() {
        DepictedItem item = getContent();
        checkedView.setChecked(item.isSelected());
        depictsLabel.setText(item.getName());
        description.setText(item.getDescription());
        if (!TextUtils.isEmpty(item.getImageUrl())) {
          if (!item.getImageUrl().equals(NO_IMAGE_FOR_DEPICTION)) {
            imageView.setImageURI(Uri.parse(item.getImageUrl()));
          }
        } else {
          imageView.setImageURI(UriUtil.getUriForResourceId(R.drawable.ic_wikidata_logo_24dp));
          listener.fetchThumbnailUrlForEntity(item);
        }
    }

}
