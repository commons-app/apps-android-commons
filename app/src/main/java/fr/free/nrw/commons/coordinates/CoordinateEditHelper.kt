package fr.free.nrw.commons.coordinates


import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.notification.NotificationHelper.Companion.NOTIFICATION_EDIT_COORDINATES
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.Objects
import javax.inject.Inject
import javax.inject.Named
import org.apache.commons.lang3.StringUtils
import timber.log.Timber


/**
 * Helper class for edit and update given coordinates and showing notification about new coordinates
 * upgradation
 */
class CoordinateEditHelper @Inject constructor(
    private val notificationHelper: NotificationHelper,
    @Named("commons-page-edit") private val pageEditClient: PageEditClient,
    private val viewUtil: ViewUtilWrapper
) {

    /**
     * Public interface to edit coordinates
     * @param context to be added
     * @param media to be added
     * @param latitude to be added
     * @param longitude to be added
     * @param accuracy to be added
     * @return Single<Boolean>
     */
    fun makeCoordinatesEdit(
        context: Context,
        media: Media,
        latitude: String,
        longitude: String,
        accuracy: String
    ): Single<Boolean>? {
        viewUtil.showShortToast(
            context,
            context.getString(R.string.coordinates_edit_helper_make_edit_toast)
        )
        return addCoordinates(media, latitude, longitude, accuracy)
            ?.flatMapSingle { result ->
                Single.just(showCoordinatesEditNotification(context, media, latitude, longitude, accuracy, result))
            }
            ?.firstOrError()
    }

    /**
     * Replaces new coordinates
     * @param media to be added
     * @param latitude to be added
     * @param longitude to be added
     * @param accuracy to be added
     * @return Observable<Boolean>
     */
    private fun addCoordinates(
        media: Media,
        latitude: String,
        longitude: String,
        accuracy: String
    ): Observable<Boolean>? {
        Timber.d("thread is coordinates adding %s", Thread.currentThread().getName())
        val summary = "Adding Coordinates"

        val buffer = StringBuilder()

        val wikiText = media.filename?.let {
            pageEditClient.getCurrentWikiText(it)
                .subscribeOn(Schedulers.io())
                .blockingGet()
        }

        if (latitude != null) {
            buffer.append("\n{{Location|").append(latitude).append("|").append(longitude)
                .append("|").append(accuracy).append("}}")
        }

        val editedLocation = buffer.toString()
        val appendText = wikiText?.let { getFormattedWikiText(it, editedLocation) }

        return Objects.requireNonNull(media.filename)
            ?.let { pageEditClient.edit(it, appendText!!, summary) }
    }

    /**
     * Helps to get formatted wikitext with upgraded location
     * @param wikiText current wikitext
     * @param editedLocation new location
     * @return String
     */
    private fun getFormattedWikiText(wikiText: String, editedLocation: String): String {
        if (wikiText.contains("filedesc") && wikiText.contains("Location")) {
            val fromLocationToEnd = wikiText.substring(wikiText.indexOf("{{Location"))
            val firstHalf = wikiText.substring(0, wikiText.indexOf("{{Location"))
            val lastHalf = fromLocationToEnd.substring(fromLocationToEnd.indexOf("}}") + 2)

            val startOfSecondSection = StringUtils.ordinalIndexOf(wikiText, "==", 3)
            val buffer = StringBuilder()
            if (wikiText[wikiText.indexOf("{{Location") - 1] == '\n') {
                buffer.append(editedLocation.substring(1))
            } else {
                buffer.append(editedLocation)
            }
            if (startOfSecondSection != -1 && wikiText[startOfSecondSection - 1] != '\n') {
                buffer.append("\n")
            }

            return firstHalf + buffer + lastHalf
        }
        if (wikiText.contains("filedesc") && !wikiText.contains("Location")) {
            val startOfSecondSection = StringUtils.ordinalIndexOf(wikiText, "==", 3)

            if (startOfSecondSection != -1) {
                val firstHalf = wikiText.substring(0, startOfSecondSection)
                val lastHalf = wikiText.substring(startOfSecondSection)
                val buffer = editedLocation.substring(1) + "\n"
                return firstHalf + buffer + lastHalf
            }

            return wikiText + editedLocation
        }
        return "== {{int:filedesc}} ==$editedLocation$wikiText"
    }

    /**
     * Update coordinates and shows notification about coordinates update
     * @param context to be added
     * @param media to be added
     * @param latitude to be added
     * @param longitude to be added
     * @param accuracy to be added
     * @param result to be added
     * @return boolean
     */
    private fun showCoordinatesEditNotification(
        context: Context,
        media: Media,
        latitude: String,
        longitude: String,
        accuracy: String,
        result: Boolean
    ): Boolean {
        val message: String
        var title = context.getString(R.string.coordinates_edit_helper_show_edit_title)

        if (result) {
            media.coordinates = fr.free.nrw.commons.location.LatLng(
                latitude.toDouble(),
                longitude.toDouble(),
                accuracy.toFloat()
            )
            title += ": " + context.getString(R.string.coordinates_edit_helper_show_edit_title_success)
            val coordinatesInMessage = StringBuilder()
            val mediaCoordinate = media.coordinates.toString()
            coordinatesInMessage.append(mediaCoordinate)
            message = context.getString(
                R.string.coordinates_edit_helper_show_edit_message,
                coordinatesInMessage.toString()
            )
        } else {
            title += ": " + context.getString(R.string.coordinates_edit_helper_show_edit_title)
            message = context.getString(R.string.coordinates_edit_helper_edit_message_else)
        }

        val urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.filename
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NOTIFICATION_EDIT_COORDINATES,
            browserIntent
        )
        return result
    }
}
