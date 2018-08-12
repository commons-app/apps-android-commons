package fr.free.nrw.commons.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.IOException;

import fr.free.nrw.commons.R;
import timber.log.Timber;

/**
 * Created by bluesir9 on 3/10/17.
 */

public class ImageUtils {

    public enum Result {
        IMAGE_DARK,
        IMAGE_OK
    }

    /**
     * @param bitmapRegionDecoder BitmapRegionDecoder for the image we wish to process
     * @return Result.IMAGE_OK if image is neither dark nor blurry or if the input bitmapRegionDecoder provided is null
     *         Result.IMAGE_DARK if image is too dark
     */
    public static Result checkIfImageIsTooDark(BitmapRegionDecoder bitmapRegionDecoder) {
        if (bitmapRegionDecoder == null) {
            Timber.e("Expected bitmapRegionDecoder was null");
            return Result.IMAGE_OK;
        }

        int loadImageHeight = bitmapRegionDecoder.getHeight();
        int loadImageWidth = bitmapRegionDecoder.getWidth();

        int checkImageTopPosition = 0;
        int checkImageLeftPosition = 0;

        Timber.v("left: " + checkImageLeftPosition + " right: " + loadImageWidth + " top: " + checkImageTopPosition + " bottom: " + loadImageHeight);

        Rect rect = new Rect(checkImageLeftPosition,checkImageTopPosition, loadImageWidth, loadImageHeight);

        Bitmap processBitmap = bitmapRegionDecoder.decodeRegion(rect,null);

        if (checkIfImageIsDark(processBitmap)) {
            return Result.IMAGE_DARK;
        }

        return Result.IMAGE_OK;
    }

    /**
     * Pulls the pixels into an array and then runs through it while checking the brightness of each pixel.
     * The calculation of brightness of each pixel is done by extracting the RGB constituents of the pixel
     * and then applying the formula to calculate its "Luminance".
     * Pixels with luminance greater than 40% are considered to be bright pixels while the ones with luminance
     * greater than 26% but less than 40% are considered to be pixels with medium brightness. The rest are
     * dark pixels.
     * If the number of bright pixels is more than 2.5% or the number of pixels with medium brightness is
     * more than 30% of the total number of pixels then the image is considered to be OK else dark.
     * @param bitmap The bitmap that needs to be checked.
     * @return true if bitmap is dark or null, false if bitmap is bright
     */
    private static boolean checkIfImageIsDark(Bitmap bitmap) {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null");
            return true;
        }

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int allPixelsCount = bitmapWidth * bitmapHeight;
        int[] bitmapPixels = new int[allPixelsCount];
        Log.e("total", Integer.toString(allPixelsCount));

        bitmap.getPixels(bitmapPixels,0,bitmapWidth,0,0,bitmapWidth,bitmapHeight);
        int numberOfBrightPixels = 0;
        int numberOfMediumBrightnessPixels = 0;
        double brightPixelThreshold = 0.025*allPixelsCount;
        double mediumBrightPixelThreshold = 0.3*allPixelsCount;

        for (int pixel : bitmapPixels) {
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);

            int secondMax = r>g ? r:g;
            double max = (secondMax>b ? secondMax:b)/255.0;

            int secondMin = r<g ? r:g;
            double min = (secondMin<b ? secondMin:b)/255.0;

            double luminance = ((max+min)/2.0)*100;

            int highBrightnessLuminance = 40;
            int mediumBrightnessLuminance = 26;

            if (luminance<highBrightnessLuminance){
                if (luminance>mediumBrightnessLuminance){
                    numberOfMediumBrightnessPixels++;
                }
            }
            else {
                numberOfBrightPixels++;
            }

            if (numberOfBrightPixels>=brightPixelThreshold || numberOfMediumBrightnessPixels>=mediumBrightPixelThreshold){
                return false;
            }

        }
        return true;
    }

    /**
     * Downloads the image from the URL and sets it as the phone's wallpaper
     * Fails silently if download or setting wallpaper fails.
     * @param context
     * @param imageUrl
     */
    public static void setWallpaperFromImageUrl(Context context, Uri imageUrl) {
        Timber.d("Trying to set wallpaper from url %s", imageUrl.toString());
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(imageUrl)
                .setAutoRotateEnabled(true)
                .build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        final DataSource<CloseableReference<CloseableImage>>
                dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);

        dataSource.subscribe(new BaseBitmapDataSubscriber() {

            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if (dataSource.isFinished() && bitmap != null){
                    Timber.d("Bitmap loaded from url %s", imageUrl.toString());
                    setWallpaper(context, Bitmap.createBitmap(bitmap));
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

    private static void setWallpaper(Context context, Bitmap bitmap) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        try {
            wallpaperManager.setBitmap(bitmap);
            ViewUtil.showLongToast(context, context.getString(R.string.wallpaper_set_successfully));
        } catch (IOException e) {
            Timber.e(e,"Error setting wallpaper");
        }
    }
}
