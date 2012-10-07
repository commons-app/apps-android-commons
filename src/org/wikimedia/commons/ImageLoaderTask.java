package org.wikimedia.commons;

import java.net.*;
import java.io.*;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.*;

class ImageLoaderTask extends AsyncTask<Uri, String, Bitmap> {
    ImageView view;

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    public Bitmap getBitmap(Uri imageUri) throws FileNotFoundException {

        // FIXME: Use proper window width, not device width. But should do for now!
        WindowManager wm = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
        int reqHeight = wm.getDefaultDisplay().getHeight();
        int reqWidth = wm.getDefaultDisplay().getWidth();
        
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream bitmapStream = view.getContext().getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(bitmapStream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        
        // Re-get the InputStream!
        bitmapStream = view.getContext().getContentResolver().openInputStream(imageUri);
        return BitmapFactory.decodeStream(bitmapStream, null, options);
    }

    ImageLoaderTask(ImageView view) {
        this.view = view;
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        Uri url = params[0];
        Bitmap bitmap;
        try {
            bitmap = getBitmap(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        view.setImageBitmap(result);
    }
}
