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
import io.reactivex.Single
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
     * Public interface to edit depicts
     *
     * @param context context
     * @param media media
     * @param depicts selected depicts to be added
     * @return Single<Boolean>
     */
    fun makeDepictEdit(
        context: Context,
        media: Media,
        depicts: List<String>
    ): Single<Boolean> {
        viewUtilWrapper.showShortToast(
            context,
            context.getString(R.string.depict_edit_helper_make_edit_toast)
        )
        return addCategory(media, depicts)
            .flatMapSingle { result: Boolean ->
                Single.just(
                    showCategoryEditNotification(context, media, result)
                )
            }
            .firstOrError()
    }

    /**
     * Appends new depicts
     *
     * @param media media
     * @param depicts to be added
     * @return Observable<Boolean>
     */
    private fun addCategory(media: Media, depicts: List<String>): Observable<Boolean> {
        Timber.d("thread is depict adding %s", Thread.currentThread().name)
        return wikidataEditService.editDepiction(depicts, media.filename)
    }

    /**
     * Helps to create notification about condition of editing depicts
     *
     * @param context context
     * @param media media
     * @param result response of result
     * @return Single<Boolean>
     */
    private fun showCategoryEditNotification(
        context: Context,
        media: Media,
        result: Boolean
    ): Boolean {
        val message: String
        var title = context.getString(R.string.depict_edit_helper_show_edit_title)
        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title_success)
            val categoriesInMessage = StringBuilder()
            val depictIdList = media.depictionIds
            for (category in depictIdList) {
                categoriesInMessage.append(category)
                if (category == depictIdList[depictIdList.size - 1]) {
                    continue
                }
                categoriesInMessage.append(",")
            }
            message = context.resources.getQuantityString(
                R.plurals.depict_edit_helper_show_edit_message_if,
                depictIdList.size,
                categoriesInMessage.toString()
            )
        } else {
            title += ": " + context.getString(R.string.depict_edit_helper_show_edit_title)
            message = context.getString(R.string.depict_edit_helper_edit_message_else)
        }
        val urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.filename
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NotificationHelper.NOTIFICATION_EDIT_DEPICT,
            browserIntent
        )
        return result
    }
}
