package fr.free.nrw.commons.explore.depictions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import fr.free.nrw.commons.utils.ImageUtils;

public class SearchDepictionsRenderer extends Renderer<DepictedItem> {

    @BindView(R.id.depicts_label)
    TextView tvDepictionLabel;

    @BindView(R.id.description)
    TextView tvDepictionDesc;

    @BindView(R.id.depicts_image)
    ImageView imageView;

    private final DepictsClickedListener listener;

    public SearchDepictionsRenderer(DepictsClickedListener listener) {
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
        if (!item.getImageView().isEmpty()) {
            Picasso.with(getContext()).load(item.getImageView()).into(imageView, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError() {
                    imageView.setVisibility(View.GONE);
                }
            });
        } else imageView.setVisibility(View.GONE);
    }

    public interface DepictsClickedListener {
        void depictsClicked(DepictedItem item);
    }
}
