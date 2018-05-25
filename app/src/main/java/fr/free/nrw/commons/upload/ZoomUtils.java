package fr.free.nrw.commons.upload;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.provider.MediaStore;
import android.support.v4.graphics.BitmapCompat;
import android.view.View;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ZoomUtils {

    static void zoomImageUtil(View thumbView, Rect startBounds, InputStream input) {

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
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageuri);
            int bitmapByteCount= BitmapCompat.getAllocationByteCount(bitmap);
            long height = bitmap.getHeight();
            long width = bitmap.getWidth();
            long calHeight = (long) ((height * maxMemory)/(bitmapByteCount * 1.1));
            long calWidth = (long) ((width * maxMemory)/(bitmapByteCount * 1.1));
            scaled = Bitmap.createScaledBitmap(bitmap,(int) Math.min(width,calWidth), (int) Math.min(height,calHeight), true);
        } catch (IOException e) {
        } catch (NullPointerException e){
            scaled = bitmap;
        }
        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageBitmap(scaled);



        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
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

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);
        zoomOutButton.setVisibility(View.VISIBLE);
        zoomInButton.setVisibility(View.GONE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

    }
}
