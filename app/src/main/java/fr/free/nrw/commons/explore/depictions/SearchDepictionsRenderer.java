package fr.free.nrw.commons.explore.depictions;


import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.pedrogomez.renderers.Renderer;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

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

    /**
     * Render value to all the items in the search depictions list
     */
    @Override
    public void render() {
        DepictedItem item = getContent();
        tvDepictionLabel.setText(item.getName());
        tvDepictionDesc.setText(item.getDescription());
        imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_wikidata_logo_24dp));

        if (!TextUtils.isEmpty(item.getImageUrl())) {
                ImageRequest imageRequest = ImageRequestBuilder
                        .newBuilderWithSource(Uri.parse(item.getImageUrl()))
                        .setAutoRotateEnabled(true)
                        .build();

                ImagePipeline imagePipeline = Fresco.getImagePipeline();
                final DataSource<CloseableReference<CloseableImage>>
                        dataSource = imagePipeline.fetchDecodedImage(imageRequest, getContext());

                dataSource.subscribe(new BaseBitmapDataSubscriber() {

                    @Override
                    public void onNewResultImpl(@Nullable Bitmap bitmap) {
                        if (dataSource.isFinished() && bitmap != null) {
                            //imageView.setImageBitmap(Bitmap.createBitmap(bitmap));
                            imageView.post(() -> imageView.setImageBitmap(Bitmap.createBitmap(bitmap)));
                            dataSource.close();
                        }
                    }

                    @Override
                    public void onFailureImpl(DataSource dataSource) {
                        if (dataSource != null) {
                            dataSource.close();
                        }
                    }
                }, CallerThreadExecutor.getInstance());
        }
    }

    public interface DepictCallback {
        void depictsClicked(DepictedItem item);
    }
}
