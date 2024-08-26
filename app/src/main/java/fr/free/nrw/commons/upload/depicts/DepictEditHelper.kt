package fr.free.nrw.commons.upload.depicts

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import fr.free.nrw.commons.wikidata.WikidataEditService
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject

class DepictEditHelper @Inject constructor (notificationHelper: NotificationHelper,
                                            wikidataEditService: WikidataEditService,
                                            viewUtilWrapper: ViewUtilWrapper) {

    /**
     * Class for making post operations
     */
    @Inject
    lateinit var wikidataEditService: WikidataEditService

    /**
     * Class for creating notification
     */
    @Inject
    lateinit var notificationHelper: NotificationHelper

    /**
     * Class for showing toast
     */
    @Inject
    lateinit var viewUtilWrapper: ViewUtilWrapper

    /**
     * Public interface to edit depictions
     *
     * @param context context
     * @param media media
     * @param depictions selected depictions to be added ex: ["Q12", "Q234"]
     * @return Single<Boolean>
     */
    fun makeDepictionEdit(
        context: Context,
        media: Media,
        depictions: List<String>
    ): Observable<Boolean> {
        viewUtilWrapper.showShortToast(
            context,
            context.getString(R.string.depictions_edit_helper_make_edit_toast)
        )
        return addDepiction(media, depictions)
            .flatMap { result: Boolean ->
                Observable.just(
                    showDepictionEditNotification(context, media, result)
                )
            }
    }

    /**
     * Appends new depictions
     *
     * @param media media
     * @param depictions to be added
     * @return Observable<Boolean>
     */
     private fun addDepiction(media: Media, depictions: List<String>): Observable<Boolean> {
        Timber.d("thread is adding depiction %s", Thread.currentThread().name)
        return wikidataEditService.updateDepictsProperty(media.pageId, depictions)
    }

    /**
     * Helps to create notification about condition of editing depictions
     *
     * @param context context
     * @param media media
     * @param result response of result
     * @return Single<Boolean>
     */
    private fun showDepictionEditNotification(
        context: Context,
        media: Media,
        result: Boolean
    ): Boolean {
        val message: String
        var title = context.getString(R.string.depictions_edit_helper_show_edit_title)
        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title_success)
            val depictsInMessage = StringBuilder()
            val depictIdList = media.depictionIds
            for (depiction in depictIdList) {
                depictsInMessage.append(depiction)
                if (depiction == depictIdList[depictIdList.size - 1]) {
                    continue
                }
                depictsInMessage.append(",")
            }
            message = context.resources.getQuantityString(
                R.plurals.depictions_edit_helper_show_edit_message_if,
                depictIdList.size,
                depictsInMessage.toString()
            )
        } else {
            title += ": " + context.getString(R.string.depictions_edit_helper_show_edit_title)
            message = context.getString(R.string.depictions_edit_helper_edit_message_else)
        }
        val urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.filename
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NotificationHelper.NOTIFICATION_EDIT_DEPICTIONS,
            browserIntent
        )
        return result
    }
}
