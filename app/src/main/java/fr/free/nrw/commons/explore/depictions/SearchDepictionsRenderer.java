package fr.free.nrw.commons.explore.depictions;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.pedrogomez.renderers.Renderer;
import com.squareup.picasso.Picasso;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

/**
 * Renderer for DepictedItem
 */

public class SearchDepictionsRenderer extends Renderer<DepictedItem> {

    @BindView(R.id.depicts_label)
    TextView tvDepictionLabel;

    @BindView(R.id.description)
    TextView tvDepictionDesc;

    @BindView(R.id.depicts_image)
    ImageView imageView;

    private DepictCallback listener;

    int size = 0;

    public SearchDepictionsRenderer(DepictCallback listener) {
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
            if (listener != null) {
                listener.depictsClicked(item);
            }
        });
    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.item_depictions, parent, false);
    }

    @Override
    public void render() {
        DepictedItem item = getContent();
        tvDepictionLabel.setText(item.getDepictsLabel());
        tvDepictionDesc.setText(item.getDescription());

        if (!TextUtils.isEmpty(item.getImageUrl())) {
            Picasso.with(imageView.getContext()).load(item.getImageUrl()).into(imageView);
        }else{
            listener.fetchThumbnailUrlForEntity(item.getEntityId(),item.getPosition());
        }
    }

    public interface DepictCallback {
        void depictsClicked(DepictedItem item);

        void fetchThumbnailUrlForEntity(String entityId,int position);
    }
}
