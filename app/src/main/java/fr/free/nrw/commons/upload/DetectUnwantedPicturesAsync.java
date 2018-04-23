package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
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

    private final String imageMediaFilePath;
    public final WeakReference<Activity> activityWeakReference;

    DetectUnwantedPicturesAsync(WeakReference<Activity> activityWeakReference, String imageMediaFilePath) {
        //this.callback = callback;
        this.imageMediaFilePath = imageMediaFilePath;
        this.activityWeakReference = activityWeakReference;
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
        Activity activity = activityWeakReference.get();

        if (result != ImageUtils.Result.IMAGE_OK) {
            //show appropriate error message
            String errorMessage = result == ImageUtils.Result.IMAGE_DARK ? activity.getString(R.string.upload_image_too_dark) : activity.getString(R.string.upload_image_blurry);
            AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(activity);
            errorDialogBuilder.setMessage(errorMessage);
            errorDialogBuilder.setTitle(activity.getString(R.string.warning));
            errorDialogBuilder.setPositiveButton(activity.getString(R.string.no), (dialogInterface, i) -> {
                //user does not wish to upload the picture, take them back to ContributionsActivity
                Intent intent = new Intent(activity, ContributionsActivity.class);
                dialogInterface.dismiss();
                activity.startActivity(intent);
            });
            errorDialogBuilder.setNegativeButton(activity.getString(R.string.yes), (dialogInterface, i) -> {
                //user wishes to go ahead with the upload of this picture, just dismiss this dialog
                dialogInterface.dismiss();
            });

            AlertDialog errorDialog = errorDialogBuilder.create();
            if (!activity.isFinishing()) {
                errorDialog.show();
            }
        }
    }
}
