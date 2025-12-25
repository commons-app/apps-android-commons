package fr.free.nrw.commons.nearby

import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.notification.NotificationHelper.Companion.NOTIFICATION_DELETE
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


/**
 * Singleton class for making edits to wiki pages asynchronously using RxJava.
 */
@Singleton
class PageEditHelper @Inject constructor(
    private val notificationHelper: NotificationHelper,
    @param:Named("wikidata-page-edit") private val pageEditClient: PageEditClient,
    private val viewUtil: ViewUtilWrapper
) {
    /**
     * Public interface to make a page edit request asynchronously.
     *
     * @param context     The context for displaying messages.
     * @param title       The title of the page to edit.
     * @param preText     The existing content of the page.
     * @param description The description of the issue to be fixed.
     * @param details     Additional details about the issue.
     * @param lat         The latitude of the location related to the page.
     * @param lng         The longitude of the location related to the page.
     * @return A Single emitting true if the edit was successful, false otherwise.
     */
    fun makePageEdit(
        context: Context, title: String, preText: String,
        description: String?,
        details: String?, lat: Double?, lng: Double?
    ): Single<Boolean?> {
        viewUtil.showShortToast(context, "Trying to edit " + title)

        return editPage(title, preText, description, details, lat, lng)
            .flatMapSingle<Boolean?>(Function { result: Boolean? ->
                Single.just<Boolean?>(
                    showNotification(context, title, result!!)
                )
            })
            .firstOrError()
            .onErrorResumeNext(Function { exception: Throwable? -> Single.error(exception) })
    }

    /**
     * Creates the text content for the page edit based on provided parameters.
     *
     * @param title       The title of the page to edit.
     * @param preText     The existing content of the page.
     * @param description The description of the issue to be fixed.
     * @param details     Additional details about the issue.
     * @param lat         The latitude of the location related to the page.
     * @param lng         The longitude of the location related to the page.
     * @return An Observable emitting true if the edit was successful, false otherwise.
     */
    @VisibleForTesting
    fun editPage(
        title: String, preText: String, description: String?,
        details: String?, lat: Double?, lng: Double?
    ): Observable<Boolean> {
        val summary = "Please fix this item"
        val marker =
            "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!"
        val markerIndex = preText.indexOf(marker)
        val text = if (preText.isEmpty() || markerIndex == -1) {
            """
                ==Please fix this item==
                Someone using the [[Commons:Mobile_app|Commons Android app]] went to this item's geographical location ($lat,$lng) and noted the following problem(s):
                * <i><nowiki>$description</nowiki></i>
                
                Details: <i><nowiki>$details</nowiki></i>
                
                Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!
                
                ~~~~
                """.trimIndent()
        } else {
            preText.substring(0, markerIndex) +
                    """
            * <i><nowiki>$description</nowiki></i>

            Details: <i><nowiki>$details</nowiki></i>
            
            Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!
            
            ~~~~
            """.trimIndent()
        }

        return pageEditClient.postCreate(title, text, summary)
    }

    /**
     * Displays a notification based on the result of the page edit.
     *
     * @param context The context for displaying the notification.
     * @param title   The title of the page edited.
     * @param result  The result of the edit operation.
     * @return true if the edit was successful, false otherwise.
     */
    private fun showNotification(context: Context, title: String, result: Boolean): Boolean {
        val message = if (result) {
            "$title Edited Successfully"
        } else {
            context.getString(R.string.delete_helper_show_deletion_message_else)
        }

        notificationHelper.showNotification(
            context,
            title,
            message,
            NOTIFICATION_DELETE,
            Intent(
                Intent.ACTION_VIEW,
                "${BuildConfig.WIKIDATA_URL}/wiki/$title".toUri()
            )
        )
        return result
    }
}
