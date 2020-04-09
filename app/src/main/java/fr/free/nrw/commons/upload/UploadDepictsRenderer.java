package fr.free.nrw.commons.upload;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.upload.structure.depictions.UploadDepictsCallback;
import timber.log.Timber;

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
    ImageView imageView;
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
            if (!item.getImageUrl().equals(NO_IMAGE_FOR_DEPICTION))
                setImageView(Uri.parse(item.getImageUrl()), imageView);
        }else{
            listener.fetchThumbnailUrlForEntity(item.getId(),item.getPosition());
        }
    }

    /**
     * Set thumbnail for the depicted item
     */
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
                    imageView.post(() -> imageView.setImageBitmap(Bitmap.createBitmap(bitmap)));
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
}
