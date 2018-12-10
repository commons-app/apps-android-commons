package fr.free.nrw.commons.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fr.free.nrw.commons.R;
import timber.log.Timber;

/**
 * Created by bluesir9 on 3/10/17.
 */

public class ImageUtils {

    public static final int IMAGE_DARK = 1;
    public static final int IMAGE_BLURRY = 1 << 1;
    public static final int IMAGE_DUPLICATE = 1 << 2;
    public static final int IMAGE_OK = 0;
    public static final int IMAGE_KEEP = -1;
    public static final int IMAGE_WAIT = -2;
    public static final int EMPTY_TITLE = -3;
    public static final int FILE_NAME_EXISTS = -4;
    public static final int NO_CATEGORY_SELECTED = -5;
    public static final int IMAGE_GEOLOCATION_DIFFERENT = -6;

    @IntDef(
            flag = true,
            value = {
                    IMAGE_DARK,
                    IMAGE_BLURRY,
                    IMAGE_DUPLICATE,
                    IMAGE_OK,
                    IMAGE_KEEP,
                    IMAGE_WAIT,
                    EMPTY_TITLE,
                    FILE_NAME_EXISTS,
                    NO_CATEGORY_SELECTED,
                    IMAGE_GEOLOCATION_DIFFERENT
            }
    )
    @Retention(RetentionPolicy.SOURCE)
    public @interface Result {
    }

    /**
     * @param bitmapRegionDecoder BitmapRegionDecoder for the image we wish to process
     * @return IMAGE_OK if image is neither dark nor blurry or if the input bitmapRegionDecoder provided is null
     * IMAGE_DARK if image is too dark
     */
    public static @Result
    int checkIfImageIsTooDark(BitmapRegionDecoder bitmapRegionDecoder) {
        if (bitmapRegionDecoder == null) {
            Timber.e("Expected bitmapRegionDecoder was null");
            return IMAGE_OK;
        }

        int loadImageHeight = bitmapRegionDecoder.getHeight();
        int loadImageWidth = bitmapRegionDecoder.getWidth();

        int checkImageTopPosition = 0;
        int checkImageLeftPosition = 0;

        Timber.v("left: " + checkImageLeftPosition + " right: " + loadImageWidth + " top: " + checkImageTopPosition + " bottom: " + loadImageHeight);

        Rect rect = new Rect(checkImageLeftPosition,checkImageTopPosition, loadImageWidth, loadImageHeight);

        Bitmap processBitmap = bitmapRegionDecoder.decodeRegion(rect,null);

        if (checkIfImageIsDark(processBitmap)) {
            return IMAGE_DARK;
        }

        return IMAGE_OK;
    }

    /**
     * @param geolocation Geolocation of image. If geotag doesn't exists, then this will be an empty string
     * @return IMAGE_OK if image is neither dark nor blurry or if the input bitmapRegionDecoder provided is null
     * IMAGE_DARK if image is too dark
     */
    public static boolean checkImageGeolocationIsDifferent(String geolocation) {
        return true;
    }

    private static boolean checkIfImageIsDark(Bitmap bitmap) {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null");
            return true;
        }

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int allPixelsCount = bitmapWidth * bitmapHeight;
        int[] bitmapPixels = new int[allPixelsCount];
        Timber.d("total %s", Integer.toString(allPixelsCount));

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
     *
     * @param context context
     * @param imageUrl Url of the image
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
                if (dataSource.isFinished() && bitmap != null) {
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
            Timber.e(e, "Error setting wallpaper");
        }
    }

    public static String getErrorMessageForResult(Context context, @Result int result) {
        String errorMessage;
        if (result == ImageUtils.IMAGE_DARK)
            errorMessage = context.getString(R.string.upload_image_problem_dark);
        else if (result == ImageUtils.IMAGE_BLURRY)
            errorMessage = context.getString(R.string.upload_image_problem_blurry);
        else if (result == ImageUtils.IMAGE_DUPLICATE)
            errorMessage = context.getString(R.string.upload_image_problem_duplicate);
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_BLURRY))
            errorMessage = context.getString(R.string.upload_image_problem_dark_blurry);
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = context.getString(R.string.upload_image_problem_dark_duplicate);
        else if (result == (ImageUtils.IMAGE_BLURRY|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = context.getString(R.string.upload_image_problem_blurry_duplicate);
        else if (result == (ImageUtils.IMAGE_DARK|ImageUtils.IMAGE_BLURRY|ImageUtils.IMAGE_DUPLICATE))
            errorMessage = context.getString(R.string.upload_image_problem_dark_blurry_duplicate);
        else
            return "";

        return errorMessage;
    }
}
