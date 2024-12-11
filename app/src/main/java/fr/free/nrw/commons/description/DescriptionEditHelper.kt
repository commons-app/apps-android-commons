package fr.free.nrw.commons.description


import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.notification.NotificationHelper.Companion.NOTIFICATION_EDIT_DESCRIPTION
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import timber.log.Timber

/**
 * Helper class for edit and update given descriptions and showing notification upgradation
 */
class DescriptionEditHelper @Inject constructor(
    /**
     * notificationHelper: helps creating notification
     */
    private val notificationHelper: NotificationHelper,

    /**
     * pageEditClient: methods provided by this member posts the edited descriptions
     * to the Media wiki api
     */
    @Named("commons-page-edit") val pageEditClient: PageEditClient
) {

    /**
     * Replaces new descriptions
     *
     * @param context context
     * @param media to be added
     * @param appendText to be added
     * @return Single<Boolean>
     */
    fun addDescription(context: Context, media: Media, appendText: String): Single<Boolean> {
        Timber.d("thread is description adding %s", Thread.currentThread().name)
        val summary = "Updating Description"

        return pageEditClient.edit(
            requireNotNull(media.filename),
            appendText,
            summary
        ).flatMapSingle { result ->
            Single.just(showDescriptionEditNotification(context, media, result))
        }.firstOrError()
    }

    /**
     * Adds new captions
     *
     * @param context context
     * @param media to be added
     * @param language to be added
     * @param value to be added
     * @return Single<Boolean>
     */
    fun addCaption(
        context: Context,
        media: Media,
        language: String,
        value: String
    ): Single<Boolean> {
        Timber.d("thread is caption adding %s", Thread.currentThread().name)
        val summary = "Updating Caption"

        return pageEditClient.setCaptions(
            summary,
            requireNotNull(media.filename),
            language,
            value
        ).flatMapSingle { result ->
            Single.just(showCaptionEditNotification(context, media, result))
        }.firstOrError()
    }

    /**
     * Update captions and shows notification about captions update
     * @param context to be added
     * @param media to be added
     * @param result to be added
     * @return boolean
     */
    private fun showCaptionEditNotification(context: Context, media: Media, result: Int): Boolean {
        val message: String
        var title = context.getString(R.string.caption_edit_helper_show_edit_title)

        if (result == 1) {
            title += ": " + context.getString(
                R.string.coordinates_edit_helper_show_edit_title_success
            )
            message = context.getString(R.string.caption_edit_helper_show_edit_message)
        } else {
            title += ": " + context.getString(R.string.caption_edit_helper_show_edit_title)
            message = context.getString(R.string.caption_edit_helper_edit_message_else)
        }

        val urlForFile = "${BuildConfig.COMMONS_URL}/wiki/${media.filename}"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NOTIFICATION_EDIT_DESCRIPTION,
            browserIntent
        )
        return result == 1
    }

    /**
     * Update descriptions and shows notification about descriptions update
     * @param context to be added
     * @param media to be added
     * @param result to be added
     * @return boolean
     */
    private fun showDescriptionEditNotification(
        context: Context,
        media: Media,
        result: Boolean
    ): Boolean {
        val message: String
        var title=  context.getString(
            R.string.description_edit_helper_show_edit_title
        )

        if (result) {
            title += ": " + context.getString(
                R.string.coordinates_edit_helper_show_edit_title_success
            )
            message = context.getString(R.string.description_edit_helper_show_edit_message)
        } else {
            title += ": " + context.getString(R.string.description_edit_helper_show_edit_title)
            message = context.getString(R.string.description_edit_helper_edit_message_else)
        }

        val urlForFile = "${BuildConfig.COMMONS_URL}/wiki/${media.filename}"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NOTIFICATION_EDIT_DESCRIPTION,
            browserIntent
        )
        return result
    }
}
