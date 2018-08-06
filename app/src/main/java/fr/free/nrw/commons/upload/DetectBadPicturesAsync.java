package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

/**
 * Created by bluesir9 on 16/9/17.
 * <p>
 * <p>Responsible for checking if the picture that the user is trying to upload is useful or not. Will attempt to filter
 * away completely black,fuzzy/blurry pictures(for now).
 * <p>
 * <p>todo: Detect selfies?
 */

public class DetectBadPicturesAsync extends AsyncTask<Void, Void, ImageUtils.Result> {

    public final WeakReference<MediaWikiApi> mwApiWeakReference;
    private final Uri mediaUri;
    public final WeakReference<BehaviorSubject<ImageUtils.Result>> subjectWeakReference;
    public final WeakReference<Context> contextWeakReference;

    DetectBadPicturesAsync(WeakReference<BehaviorSubject<ImageUtils.Result>> subjectWeakReference, WeakReference<Context> contextWeakReference, WeakReference<MediaWikiApi> mwApiWeakReference, Uri mediaUri) {
        //this.callback = callback;
        this.mediaUri = mediaUri;
        this.subjectWeakReference = subjectWeakReference;
        this.contextWeakReference = contextWeakReference;
        this.mwApiWeakReference = mwApiWeakReference;
    }

    @Override
    protected ImageUtils.Result doInBackground(Void... voids) {
        try {
            InputStream inputStream = contextWeakReference.get().getContentResolver().openInputStream(mediaUri);
            String fileSha1 = FileUtils.getSHA1(inputStream);
            // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
            if (mwApiWeakReference.get().existingFile(fileSha1)) {
                Timber.d("File %s already exists in Commons", mediaUri);
                return ImageUtils.Result.IMAGE_DUPLICATE;
            }

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(inputStream, false);

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
