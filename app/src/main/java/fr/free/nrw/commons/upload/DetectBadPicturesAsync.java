package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Created by bluesir9 on 16/9/17.
 *
 * <p>Responsible for checking if the picture that the user is trying to upload is useful or not. Will attempt to filter
 * away completely black,fuzzy/blurry pictures(for now).
 *
 * <p>todo: Detect selfies?
 */

public class DetectBadPicturesAsync extends AsyncTask<Void, Void, ImageUtils.Result> {

    private final String imageMediaFilePath;
    public final WeakReference<BehaviorSubject<ImageUtils.Result>> subjectWeakReference;

    DetectBadPicturesAsync(WeakReference<BehaviorSubject<ImageUtils.Result>> subjectWeakReference, String imageMediaFilePath) {
        //this.callback = callback;
        this.imageMediaFilePath = imageMediaFilePath;
        this.subjectWeakReference = subjectWeakReference;
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
        subjectWeakReference.get().onNext(result);
    }
}
