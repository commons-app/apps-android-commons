package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.graphics.BitmapCompat;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Contains utility methods for the Zoom function in ShareActivity.
 */
public class Zoom {

    private View thumbView;
    private ContentResolver contentResolver;
    private FrameLayout flContainer;

    Zoom(View thumbView, FrameLayout flContainer, ContentResolver contentResolver) {
        this.thumbView = thumbView;
        this.contentResolver = contentResolver;
        this.flContainer = flContainer;
    }

    /**
     * Create a scaled bitmap to display the zoomed-in image
     * @param input the input stream corresponding to the uploaded image
     * @param imageUri the uploaded image's URI
     * @return a zoomable bitmap
     */
    Bitmap createScaledImage(InputStream input, Uri imageUri) {

        Bitmap scaled = null;
        BitmapRegionDecoder decoder = null;
        Bitmap bitmap = null;

        try {
            decoder = BitmapRegionDecoder.newInstance(input, false);
            bitmap = decoder.decodeRegion(new Rect(10, 10, 50, 50), null);
        } catch (IOException e) {
            Timber.e(e);
        } catch (NullPointerException e) {
            Timber.e(e);
        }
        try {
            //Compress the Image
            System.gc();
            Runtime rt = Runtime.getRuntime();
            long maxMemory = rt.freeMemory();
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
            int bitmapByteCount = BitmapCompat.getAllocationByteCount(bitmap);
            long height = bitmap.getHeight();
            long width = bitmap.getWidth();
            long calHeight = (long) ((height * maxMemory) / (bitmapByteCount * 1.1));
            long calWidth = (long) ((width * maxMemory) / (bitmapByteCount * 1.1));
            scaled = Bitmap.createScaledBitmap(bitmap, (int) Math.min(width, calWidth), (int) Math.min(height, calHeight), true);
        } catch (IOException e) {
            Timber.e(e);
        } catch (NullPointerException e) {
            Timber.e(e);
            scaled = bitmap;
        }
        return scaled;
    }

    /**
     *  Calculate the starting and ending bounds for the zoomed-in image.
     *  Also set the container view's offset as the origin for the
     * bounds, since that's the origin for the positioning animation
     * properties (X, Y).
     * @param startBounds the global visible rectangle of the thumbnail
     * @param finalBounds the global visible rectangle of the container view
     * @param globalOffset the container view's offset
     * @return scaled start bounds
     */
    float adjustStartEndBounds(Rect startBounds, Rect finalBounds, Point globalOffset) {

        thumbView.getGlobalVisibleRect(startBounds);
        flContainer.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }
        return startScale;
    }
}
