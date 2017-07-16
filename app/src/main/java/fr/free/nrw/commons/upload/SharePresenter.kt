package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.net.Uri
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.mwapi.MediaWikiApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SharePresenter(private val mwApi: MediaWikiApi) {
    fun checkIfFileExists(contentResolver: ContentResolver, mediaUrl: Uri, view: ShareView) {
        Observable.just(contentResolver.openInputStream(mediaUrl))
                .map { stream -> Utils.getSHA1(stream) }
                .flatMap { sha -> mwApi.existingFile(sha) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ exists ->
                    if (exists) {
                        view.fileIsDuplicate()
                    } else {
                        view.fileIsNotDuplicate()
                    }
                })
    }
}