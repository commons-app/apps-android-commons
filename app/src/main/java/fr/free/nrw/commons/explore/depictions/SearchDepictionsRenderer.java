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
import com.squareup.picasso.Picasso;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import timber.log.Timber;

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
            if (!item.getImageUrl().equals(getContext().getString(R.string.depictions_image_not_found)))
            setImageView(Uri.parse(item.getImageUrl()), imageView);
        }else{
            listener.fetchThumbnailUrlForEntity(item.getEntityId(),item.getPosition());
        }
    }

    private void setImageView(Uri imageUrl, ImageView imageView) {
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(imageUrl)
                .setAutoRotateEnabled(true)
                .build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        final DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(imageRequest, getContext());

        dataSource.subscribe(new BaseBitmapDataSubscriber() {

            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if (dataSource.isFinished() && bitmap != null) {
                    Timber.d("Bitmap loaded from url %s", imageUrl.toString());
                    imageView.setImageBitmap(Bitmap.createBitmap(bitmap));
                    dataSource.close();
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                Timber.d("Error getting bitmap from image url %s", imageUrl.toString());
                if (dataSource != null) {
                    dataSource.close();
                }
            }
        }, CallerThreadExecutor.getInstance());
    }

    public interface DepictCallback {
        void depictsClicked(DepictedItem item);

        void fetchThumbnailUrlForEntity(String entityId,int position);
    }
}
