package fr.free.nrw.commons.coordinates;

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
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class CoordinateEditHelper {
    private final NotificationHelper notificationHelper;
    public final PageEditClient pageEditClient;
    private final ViewUtilWrapper viewUtil;

    @Inject
    public CoordinateEditHelper(final NotificationHelper notificationHelper,
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
    public Single<Boolean> makeCoordinatesEdit(final Context context, final Media media,
        final String Latitude, final String Longitude, final String Accuracy) {
        viewUtil.showShortToast(context,
            context.getString(R.string.coordinates_edit_helper_make_edit_toast));
        return addCoordinates(media, Latitude, Longitude, Accuracy)
            .flatMapSingle(result -> Single.just(showCoordinatesEditNotification(context, media,
                result)))
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
    private Observable<Boolean> addCoordinates(final Media media, final String Latitude,
        final String Longitude, final String Accuracy) {
        Timber.d("thread is coordinates adding %s", Thread.currentThread().getName());
        final String summary = "Adding Coordinates";

        final StringBuilder buffer = new StringBuilder();

        if (Latitude != null) {
            buffer.append("\n{{Location|").append(Latitude).append("|").append(Longitude)
                .append("|").append(Accuracy).append("}}");
        } else {
            buffer.append("{{subst:unc}}");
        }
        final String appendText = buffer.toString();
        return pageEditClient.edit(Objects.requireNonNull(media.getFilename())
               , appendText, summary);
    }

    /**
     * Shows notification about coordinate update
     * @param context to be added
     * @param media to be added
     * @param result to be added
     * @return boolean
     */
    private boolean showCoordinatesEditNotification(final Context context, final Media media,
        final boolean result) {
        final String message;
        String title = context.getString(R.string.coordinates_edit_helper_show_edit_title);

        if (result) {
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

    public interface  Callback {

    }
}
