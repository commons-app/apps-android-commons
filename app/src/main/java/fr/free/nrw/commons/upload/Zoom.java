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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Zoom {

    private View thumbView;
    private Rect startBounds;
    private InputStream input;
    private Uri imageUri;
    private ContentResolver contentResolver;
    private FrameLayout flContainer;

    public Zoom(View thumbView, Rect startBounds, InputStream input, Uri imageUri, ContentResolver contentResolver) {
        this.thumbView = thumbView;
        this.startBounds = startBounds;
        this.input = input;
        this.imageUri = imageUri;
        this.contentResolver = contentResolver;
    }

    Bitmap createScaledImage() {

        Bitmap scaled = null;
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(input, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = decoder.decodeRegion(new Rect(10, 10, 50, 50), null);
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
        } catch (NullPointerException e) {
            scaled = bitmap;
        }
        return scaled;
    }


    float adjustStartEndBounds(Rect finalBounds, Point globalOffset) {
        // Calculate the starting and ending bounds for the zoomed-in image.
        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
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
