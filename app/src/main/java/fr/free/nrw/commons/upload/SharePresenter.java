package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

class SharePresenter {
    private final MediaWikiApi mwApi;

    SharePresenter(MediaWikiApi mwApi) {
        this.mwApi = mwApi;
    }

    void checkIfFileExists(ContentResolver contentResolver, Uri mediaUrl, ShareView view) {
        try {
            InputStream inputStream = contentResolver.openInputStream(mediaUrl);
            if (inputStream == null) {
                view.fileIsNotDuplicate();
                return;
            }

            Observable.just(inputStream)
                    .map(Utils::getSHA1)
                    .flatMap(mwApi::existingFile)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(exists -> {
                        if (exists) {
                            view.fileIsDuplicate();
                        } else {
                            view.fileIsNotDuplicate();
                        }
                    });
        } catch (FileNotFoundException e) {
            Timber.e(e, "IO Exception: ");
            view.fileIsNotDuplicate();
        }
    }
}