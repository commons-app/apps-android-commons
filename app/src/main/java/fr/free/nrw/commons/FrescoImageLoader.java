package fr.free.nrw.commons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import androidx.annotation.Nullable;

public class FrescoImageLoader {

    /**
     * Uses Fresco to load an image from Url
     * @param imageUrl
     * @param context
     */
    public static Bitmap loadImageFromUrl(String imageUrl,
                                        Context context) {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imageUrl)).build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource
                = imagePipeline.fetchDecodedImage(request, context);
        final Bitmap[] bitmap = {null};
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(@Nullable Bitmap tempBitmap) {
                if (tempBitmap != null) {
                    bitmap[0] = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap[0]);
                    canvas.drawBitmap(tempBitmap, 0f, 0f, new Paint());
                }
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                // Ignore failure for now.
            }
        }, CallerThreadExecutor.getInstance());

        return bitmap[0];
    }
}
