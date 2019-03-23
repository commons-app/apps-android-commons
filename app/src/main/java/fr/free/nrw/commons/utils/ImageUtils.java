package fr.free.nrw.commons.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import androidx.annotation.IntDef;
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

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.exifinterface.media.ExifInterface;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import timber.log.Timber;

/**
 * Created by bluesir9 on 3/10/17.
 */

public class ImageUtils {

    /**
     * Set 0th bit as 1 for dark image ie. 0001
     */
    public static final int IMAGE_DARK = 1 << 0; // 1
    /**
     * Set 1st bit as 1 for blurry image ie. 0010
     */
    static final int IMAGE_BLURRY = 1 << 1; // 2
    /**
     * Set 2nd bit as 1 for duplicate image ie. 0100
     */
    public static final int IMAGE_DUPLICATE = 1 << 2; //4
    /**
     * Set 3rd bit as 1 for image with different geo location ie. 1000
     */
    public static final int IMAGE_GEOLOCATION_DIFFERENT = 1 << 3; //8
    /**
     * The parameter FILE_FBMD is returned from the class ReadFBMD if the uploaded image contains FBMD data else returns IMAGE_OK
     * ie. 10000
     */
    public static final int FILE_FBMD = 1 << 4;
    /**
    * The parameter FILE_NO_EXIF is returned from the class EXIFReader if the uploaded image does not contains EXIF data else returns IMAGE_OK
    * ie. 100000
    */
    public static final int FILE_NO_EXIF = 1 << 5;
    public static final int IMAGE_OK = 0;
    public static final int IMAGE_KEEP = -1;
    public static final int IMAGE_WAIT = -2;
    public static final int EMPTY_TITLE = -3;
    public static final int FILE_NAME_EXISTS = -4;
    static final int NO_CATEGORY_SELECTED = -5;

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
     * @return IMAGE_OK if image is not too dark
     * IMAGE_DARK if image is too dark
     */
    static @Result int checkIfImageIsTooDark(String imagePath) {
        long millis = System.currentTimeMillis();
        try {
            Bitmap bmp = new ExifInterface(imagePath).getThumbnailBitmap();
            if (bmp == null) {
                bmp = BitmapFactory.decodeFile(imagePath);
            }

            if (checkIfImageIsDark(bmp)) {
                return IMAGE_DARK;
            }

        } catch (Exception e) {
            Timber.d(e, "Error while checking image darkness.");
        } finally {
            Timber.d("Checking image darkness took " + (System.currentTimeMillis() - millis) + " ms.");
        }
        return IMAGE_OK;
    }

    /**
     * @param geolocationOfFileString Geolocation of image. If geotag doesn't exists, then this will be an empty string
     * @param latLng Location of wikidata item will be edited after upload
     * @return false if image is neither dark nor blurry or if the input bitmapRegionDecoder provided is null
     * true if geolocation of the image and wikidata item are different
     */
    static boolean checkImageGeolocationIsDifferent(String geolocationOfFileString, LatLng latLng) {
        Timber.d("Comparing geolocation of file with nearby place location");
        if (latLng == null) { // Means that geolocation for this image is not given
            return false; // Since we don't know geolocation of file, we choose letting upload
        }

        String[] geolocationOfFile = geolocationOfFileString.split("\\|");
        Double distance = LengthUtils.computeDistanceBetween(
                new LatLng(Double.parseDouble(geolocationOfFile[0]),Double.parseDouble(geolocationOfFile[1]),0)
                , latLng);
        // Distance is more than 1 km, means that geolocation is wrong
        return distance >= 1000;
    }

    private static boolean checkIfImageIsDark(Bitmap bitmap) {
        if (bitmap == null) {
            Timber.e("Expected bitmap was null");
            return true;
        }

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int allPixelsCount = bitmapWidth * bitmapHeight;
        int numberOfBrightPixels = 0;
        int numberOfMediumBrightnessPixels = 0;
        double brightPixelThreshold = 0.025 * allPixelsCount;
        double mediumBrightPixelThreshold = 0.3 * allPixelsCount;

        for (int x = 0; x < bitmapWidth; x++) {
            for (int y = 0; y < bitmapHeight; y++) {
                int pixel = bitmap.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                int secondMax = r > g ? r : g;
                double max = (secondMax > b ? secondMax : b) / 255.0;

                int secondMin = r < g ? r : g;
                double min = (secondMin < b ? secondMin : b) / 255.0;

                double luminance = ((max + min) / 2.0) * 100;

                int highBrightnessLuminance = 40;
                int mediumBrightnessLuminance = 26;

                if (luminance < highBrightnessLuminance) {
                    if (luminance > mediumBrightnessLuminance) {
                        numberOfMediumBrightnessPixels++;
                    }
                } else {
                    numberOfBrightPixels++;
                }

                if (numberOfBrightPixels >= brightPixelThreshold || numberOfMediumBrightnessPixels >= mediumBrightPixelThreshold) {
                    return false;
                }
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

    /**
     * Result variable is a result of an or operation of all possible problems. Ie. if result
     * is 0001 means IMAGE_DARK
     * if result is 1100 IMAGE_DUPLICATE and IMAGE_GEOLOCATION_DIFFERENT
     */
    public static String getErrorMessageForResult(Context context, @Result int result) {
        StringBuilder errorMessage = new StringBuilder();
        if (result <= 0 ) {
            Timber.d("No issues to warn user is found");
        } else {
            Timber.d("Issues found to warn user");

            errorMessage.append(context.getResources().getString(R.string.upload_problem_exist));

            if ((IMAGE_DARK & result) != 0 ) { // We are checking image dark bit to see if that bit is set or not
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.upload_problem_image_dark));
            }

            if ((IMAGE_BLURRY & result) != 0 ) {
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.upload_problem_image_blurry));
            }

            if ((IMAGE_DUPLICATE & result) != 0 ) {
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.upload_problem_image_duplicate));
            }

            if ((IMAGE_GEOLOCATION_DIFFERENT & result) != 0 ) {
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.upload_problem_different_geolocation));
            }

            if ((FILE_FBMD & result) != 0) {
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.upload_problem_fbmd));
            }

            if ((FILE_NO_EXIF & result) != 0){
                errorMessage.append("\n - ").append(context.getResources().getString(R.string.internet_downloaded));
            }

            errorMessage.append("\n\n").append(context.getResources().getString(R.string.upload_problem_do_you_continue));
        }

        return errorMessage.toString();
    }
}
