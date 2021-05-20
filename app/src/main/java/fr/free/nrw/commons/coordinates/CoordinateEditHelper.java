package fr.free.nrw.commons.coordinates;

import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_EDIT_CATEGORY;

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
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class CoordinateEditHelper {
    private final NotificationHelper notificationHelper;
    public final PageEditClient pageEditClient;
    private final ViewUtilWrapper viewUtil;
    private final String username;
    private Callback callback;

    @Inject
    public CoordinateEditHelper(NotificationHelper notificationHelper,
        @Named("commons-page-edit") PageEditClient pageEditClient,
        ViewUtilWrapper viewUtil,
        @Named("username") String username) {
        this.notificationHelper = notificationHelper;
        this.pageEditClient = pageEditClient;
        this.viewUtil = viewUtil;
        this.username = username;
    }

    /**
     * Public interface to edit coordinates
     * @param context
     * @param media
     * @param Accuracy
     * @return
     */
    public Single<Boolean> makeCoordinatesEdit(Context context, Media media, String Latitude,
        String Longitude, String Accuracy, Callback callback) {
        viewUtil.showShortToast(context, "Trying to update coordinates");
        return addCoordinates(media, Latitude, Longitude, Accuracy)
            .flatMapSingle(result -> Single.just(showCoordinatesEditNotification(context, media, result)))
            .firstOrError();
    }

    /**
     * Appends new Latitude
     * @param media
     * @param Latitude to be added
     * @param Longitude
     * @param Accuracy
     * @return
     */
    private Observable<Boolean> addCoordinates(Media media, String Latitude,
        String Longitude, String Accuracy) {
        Timber.d("thread is coordinates adding %s", Thread.currentThread().getName());
        String summary = "Adding Coordinates";

        StringBuilder buffer = new StringBuilder();

        if (Latitude != null) {

            // {{Location|55.755826|37.6173}}
            buffer.append("\n{{Location|").append(Latitude).append("|").append(Longitude)
                .append("|").append(Accuracy).append("}}");

        } else {
            buffer.append("{{subst:unc}}");
        }
        String appendText = buffer.toString();
        return pageEditClient.edit(media.getFilename(), appendText , summary);
    }

    private boolean showCoordinatesEditNotification(Context context, Media media, boolean result) {
        String message;
        String title = "Coordinates Update";

        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title_success);
            StringBuilder categoriesInMessage = new StringBuilder();
            String mediaCoordinate = String.valueOf(media.getCoordinates());

                categoriesInMessage.append(mediaCoordinate);

            message = categoriesInMessage.toString();
        } else {
            title += ": " + "Coordinates Update";
            message = "Could not update coordinates" ;
        }

        String urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_EDIT_CATEGORY, browserIntent);
        return result;
    }

    public interface  Callback {
        boolean updateCategoryDisplay(List<String> categories);
    }
}
