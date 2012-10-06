package org.wikimedia.commons;

import java.net.*;
import java.io.*;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;

class ImageLoaderTask extends AsyncTask<Uri, String, Bitmap> {
    ImageView view;

    private Bitmap getBitmap(Uri url) throws MalformedURLException, IOException {
        if(url.getScheme().equals("content")) {
            return MediaStore.Images.Media.getBitmap(view.getContext().getContentResolver(), url);
        } else {
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(url.toString()).getContent());
            return bitmap;
        }
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
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
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
