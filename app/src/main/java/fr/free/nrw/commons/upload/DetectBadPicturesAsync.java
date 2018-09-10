package fr.free.nrw.commons.upload;

import android.graphics.BitmapRegionDecoder;
import android.os.AsyncTask;

import java.io.FileInputStream;
import java.io.IOException;
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

public class DetectBadPicturesAsync extends AsyncTask<Void, Void, Integer> {

    private final WeakReference<MediaWikiApi> mwApiWeakReference;
    private final String mediaPath;
    private final WeakReference<BehaviorSubject<Integer>> subjectWeakReference;

    DetectBadPicturesAsync(WeakReference<BehaviorSubject<Integer>> subjectWeakReference, WeakReference<MediaWikiApi> mwApiWeakReference, String mediaPath) {
        //this.callback = callback;
        this.mediaPath = mediaPath;
        this.subjectWeakReference = subjectWeakReference;
        this.mwApiWeakReference = mwApiWeakReference;
    }

    @Override
    protected @ImageUtils.Result Integer doInBackground(Void... voids) {
        try {
            String fileSha1 = FileUtils.getSHA1(new FileInputStream(mediaPath));
            // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
            if (mwApiWeakReference.get().existingFile(fileSha1)) {
                Timber.d("File %s already exists in Commons", mediaPath);
                return ImageUtils.IMAGE_DUPLICATE;
            }

            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(new FileInputStream(mediaPath), false);

            return ImageUtils.checkIfImageIsTooDark(decoder);
        } catch (IOException ioe) {
            Timber.e(ioe, "IO Exception with file:"+mediaPath);
            return ImageUtils.IMAGE_OK;
        }
    }

    @Override
    protected void onPostExecute(@ImageUtils.Result Integer result) {
        super.onPostExecute(result);
        subjectWeakReference.get().onNext(result);
    }
}
