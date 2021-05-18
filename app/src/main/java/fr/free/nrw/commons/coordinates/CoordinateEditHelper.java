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
     * @return
     */
    public Single<Boolean> makeLatitudeEdit(Context context, Media media, String Latitude, Callback callback) {
        viewUtil.showShortToast(context, context.getString(R.string.category_edit_helper_make_edit_toast));
        return addLatitude(media, Latitude)
            .flatMapSingle(result -> Single.just(showCoordinatesEditNotification(context, media, result)))
            .firstOrError();
    }
    public Single<Boolean> makeLongitudeEdit(Context context, Media media, String Longitude, Callback callback) {
        viewUtil.showShortToast(context, context.getString(R.string.category_edit_helper_make_edit_toast));
        return addLongitude(media, Longitude)
            .flatMapSingle(result -> Single.just(showCoordinatesEditNotification(context, media, result)))
            .firstOrError();
    }

    /**
     * Appends new Latitude
     * @param media
     * @param Latitude to be added
     * @return
     */
    private Observable<Boolean> addLatitude(Media media, String Latitude) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Adding Latitude";

        StringBuilder buffer = new StringBuilder();

        if (Latitude != null) {

            buffer.append("\n[[Latitude:").append(Latitude).append("]]");

        } else {
            buffer.append("{{subst:unc}}");
        }
        String appendText = buffer.toString();
        return pageEditClient.edit(media.getFilename(), appendText + "\n", summary);
    }
    /**
     * Appends new Longitude
     * @param media
     * @param Longitude to be added
     * @return
     */
    private Observable<Boolean> addLongitude(Media media, String Longitude) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Adding Longitude";

        StringBuilder buffer = new StringBuilder();

        if (Longitude != null) {

            buffer.append("\n[[Longitude:").append(Longitude).append("]]");

        } else {
            buffer.append("{{subst:unc}}");
        }
        String appendText = buffer.toString();
        return pageEditClient.edit(media.getFilename(), appendText + "\n", summary);
    }

    private boolean showCoordinatesEditNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getString(R.string.category_edit_helper_show_edit_title);

        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title_success);
            StringBuilder categoriesInMessage = new StringBuilder();
            String mediaCoordinate = String.valueOf(media.getCoordinates());

                categoriesInMessage.append(mediaCoordinate);

            message = categoriesInMessage.toString();
        } else {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title);
            message = context.getString(R.string.category_edit_helper_edit_message_else) ;
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
