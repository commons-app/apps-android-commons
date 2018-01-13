package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;

import fr.free.nrw.commons.utils.ImageUtils;
import timber.log.Timber;

/**
 * Created by bluesir9 on 16/9/17.
 *
 * <p>Responsible for checking if the picture that the user is trying to upload is useful or not. Will attempt to filter
 * away completely black,fuzzy/blurry pictures(for now).
 *
 * <p>todo: Detect selfies?
 */

public class DetectUnwantedPicturesAsync extends AsyncTask<Void, Void, ImageUtils.Result> {

    interface Callback {
        void onResult(ImageUtils.Result result);
    }

    private final Callback callback;
    private final String imageMediaFilePath;

    DetectUnwantedPicturesAsync(String imageMediaFilePath, Callback callback) {
        this.callback = callback;
        this.imageMediaFilePath = imageMediaFilePath;
    }

    @Override
    protected ImageUtils.Result doInBackground(Void... voids) {
        try {
            Timber.d("FilePath: " + imageMediaFilePath);
            if (imageMediaFilePath == null) {
                return ImageUtils.Result.IMAGE_OK;
            }

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imageMediaFilePath,false);

            return ImageUtils.checkIfImageIsTooDark(decoder);
        } catch (IOException ioe) {
            Timber.e(ioe, "IO Exception");
            return ImageUtils.Result.IMAGE_OK;
        }
    }

    @Override
    protected void onPostExecute(ImageUtils.Result result) {
        super.onPostExecute(result);
        //callback to UI so that it can take necessary decision based on the result obtained
        callback.onResult(result);
    }
}
