package fr.free.nrw.commons.contributions

import fr.free.nrw.commons.mwapi.MediaWikiApi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ContributionsPresenter(private val mwApi: MediaWikiApi) {
    private var uploadCountDisposable: Disposable? = null

    fun dispose() {
        uploadCountDisposable?.dispose()
    }

    fun getUploadCount(view: ContributionsView, username: String) {
        uploadCountDisposable = mwApi.getUploadCount(username)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ count -> view.showUpdateCount(count) })
    }
}