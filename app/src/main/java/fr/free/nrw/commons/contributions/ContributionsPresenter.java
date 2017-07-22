package fr.free.nrw.commons.contributions;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

class ContributionsPresenter {
    private final MediaWikiApi mwApi;
    private Disposable uploadCountDisposable = null;

    ContributionsPresenter(MediaWikiApi mwApi) {
        this.mwApi = mwApi;
    }

    void dispose() {
        if (uploadCountDisposable != null) {
            uploadCountDisposable.dispose();
        }
    }

    void getUploadCount(ContributionsView view, String username) {
        uploadCountDisposable = mwApi.getUploadCount(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(view::showUpdateCount);
    }
}