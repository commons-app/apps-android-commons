package fr.free.nrw.commons.description;

import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_EDIT_COORDINATES;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.notification.NotificationHelper;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

public class DescriptionEditHelper {

    /**
     * notificationHelper: helps creating notification
     */
    private final NotificationHelper notificationHelper;
    /**
     * * pageEditClient: methods provided by this member posts the edited coordinates
     * to the Media wiki api
     */
    public final PageEditClient pageEditClient;
    /**
     * viewUtil: helps to show Toast
     */
    private final ViewUtilWrapper viewUtil;

    @Inject
    public DescriptionEditHelper(final NotificationHelper notificationHelper,
        @Named("commons-page-edit") final PageEditClient pageEditClient,
        final ViewUtilWrapper viewUtil) {
        this.notificationHelper = notificationHelper;
        this.pageEditClient = pageEditClient;
        this.viewUtil = viewUtil;
    }

    /**
     * Public interface to edit coordinates
     * @param context to be added
     * @param media to be added
     * @param Accuracy to be added
     * @return Single<Boolean>
     */
    public Single<Boolean> makeDescriptionEdit(final Context context, final Media media,
        final String Latitude, final String Longitude, final String Accuracy) {
        viewUtil.showShortToast(context,
            context.getString(R.string.coordinates_edit_helper_make_edit_toast));
        return addDescription(media, Latitude, Longitude, Accuracy)
            .flatMapSingle(result -> Single.just(showDescriptionEditNotification(context, media,
                Latitude, Longitude, Accuracy, result)))
            .firstOrError();
    }

    /**
     * Replaces new coordinates
     * @param media to be added
     * @param Latitude to be added
     * @param Longitude to be added
     * @param Accuracy to be added
     * @return Observable<Boolean>
     */
    private Observable<Boolean> addDescription(final Media media, final String Latitude,
        final String Longitude, final String Accuracy) {
        Timber.d("thread is coordinates adding %s", Thread.currentThread().getName());
        final String summary = "Adding Coordinates";

        final StringBuilder buffer = new StringBuilder();

        final String wikiText = pageEditClient.getCurrentWikiText(media.getFilename())
            .subscribeOn(Schedulers.io())
            .blockingGet();

        if (Latitude != null) {
            buffer.append("\n{{Location|").append(Latitude).append("|").append(Longitude)
                .append("|").append(Accuracy).append("}}");
        }

        final String editedLocation = buffer.toString();
        final String appendText = getFormattedWikiText(wikiText, editedLocation);

        return pageEditClient.edit(Objects.requireNonNull(media.getFilename())
            , appendText, summary);
    }

    /**
     * Helps to get formatted wikitext with upgraded location
     * @param wikiText current wikitext
     * @param editedLocation new location
     * @return String
     */
    private String getFormattedWikiText(final String wikiText, final String editedLocation){

        if (wikiText.contains("filedesc") && wikiText.contains("Location")) {

            final String fromLocationToEnd = wikiText.substring(wikiText.indexOf("{{Location"));
            final String firstHalf = wikiText.substring(0, wikiText.indexOf("{{Location"));
            final String lastHalf = fromLocationToEnd.substring(
                fromLocationToEnd.indexOf("}}") + 2);

            final int startOfSecondSection = StringUtils.ordinalIndexOf(wikiText,
                "==", 3);
            final StringBuilder buffer = new StringBuilder();
            if (wikiText.charAt(wikiText.indexOf("{{Location")-1) == '\n') {
                buffer.append(editedLocation.substring(1));
            } else {
                buffer.append(editedLocation);
            }
            if (startOfSecondSection != -1 && wikiText.charAt(startOfSecondSection-1)!= '\n') {
                buffer.append("\n");
            }

            return firstHalf + buffer + lastHalf;

        }
        if (wikiText.contains("filedesc") && !wikiText.contains("Location")) {

            final int startOfSecondSection = StringUtils.ordinalIndexOf(wikiText,
                "==", 3);

            if (startOfSecondSection != -1) {
                final String firstHalf = wikiText.substring(0, startOfSecondSection);
                final String lastHalf = wikiText.substring(startOfSecondSection);
                final String buffer = editedLocation.substring(1)
                    + "\n";
                return firstHalf + buffer + lastHalf;
            }

            return wikiText + editedLocation;
        }
        if (!wikiText.contains("filedesc") && !wikiText.contains("Location")) {

            return "== {{int:filedesc}} ==" + editedLocation + wikiText;

        }
        if (!wikiText.contains("filedesc") && wikiText.contains("Location")) {

            return "== {{int:filedesc}} ==" + editedLocation + wikiText;

        }
        return null;
    }

    /**
     * Update coordinates and shows notification about coordinates update
     * @param context to be added
     * @param media to be added
     * @param latitude to be added
     * @param longitude to be added
     * @param Accuracy to be added
     * @param result to be added
     * @return boolean
     */
    private boolean showDescriptionEditNotification(final Context context, final Media media,
        final String latitude, final String longitude, final String Accuracy,
        final boolean result) {
        final String message;
        String title = context.getString(R.string.coordinates_edit_helper_show_edit_title);

        if (result) {
            media.setCoordinates(
                new fr.free.nrw.commons.location.LatLng(Double.parseDouble(latitude),
                    Double.parseDouble(longitude),
                    Float.parseFloat(Accuracy)));
            title += ": " + context
                .getString(R.string.coordinates_edit_helper_show_edit_title_success);
            final StringBuilder coordinatesInMessage = new StringBuilder();
            final String mediaCoordinate = String.valueOf(media.getCoordinates());
            coordinatesInMessage.append(mediaCoordinate);
            message = context.getString(R.string.coordinates_edit_helper_show_edit_message,
                coordinatesInMessage.toString());
        } else {
            title += ": " + context.getString(R.string.coordinates_edit_helper_show_edit_title);
            message = context.getString(R.string.coordinates_edit_helper_edit_message_else) ;
        }

        final String urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.getFilename();
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_EDIT_COORDINATES,
            browserIntent);
        return result;
    }
}
