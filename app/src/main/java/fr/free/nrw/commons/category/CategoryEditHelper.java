package fr.free.nrw.commons.category;

import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_DELETE;

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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class CategoryEditHelper {
    private final NotificationHelper notificationHelper;
    private final PageEditClient pageEditClient;
    private final ViewUtilWrapper viewUtil;
    private final String username;

    @Inject
    public CategoryEditHelper(NotificationHelper notificationHelper,
        @Named("commons-page-edit") PageEditClient pageEditClient,
        ViewUtilWrapper viewUtil,
        @Named("username") String username) {
        this.notificationHelper = notificationHelper;
        this.pageEditClient = pageEditClient;
        this.viewUtil = viewUtil;
        this.username = username;
    }

    /**
     * Public interface to edit categories
     * @param context
     * @param media
     * @param categories
     * @return
     */
    public Single<Boolean> makeCategoryEdit(Context context, Media media, List<Category> categories) {
        viewUtil.showShortToast(context, "Trying to add categories");

        return addCategory(media, categories)
            .flatMapSingle(result -> Single.just(showCategoryEditNotification(context, media, result)))
            .firstOrError();
    }

    /**
     * Makes several API calls to nominate the file for deletion
     * @param media
     * @param categories to be added
     * @return
     */
    private Observable<Boolean> addCategory(Media media, List<Category> categories) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Adding categories";

        StringBuilder buffer = new StringBuilder();

        if (categories != null && categories.size() != 0) {

            for (int i = 0; i < categories.size(); i++) {
                buffer.append("\n[[Category:").append(categories.get(i).getName()).append("]]");
            }
        } else {
            buffer.append("{{subst:unc}}");
        }
        String appendText = buffer.toString();
        return pageEditClient.appendEdit("File:Birds,_pidgeons_and_a_dove.jpg", appendText + "\n", summary);
    }

    private boolean showCategoryEditNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getString(R.string.delete_helper_show_deletion_title);

        if (result) {
            title += ": " + context.getString(R.string.delete_helper_show_deletion_title_success);
            message = context.getString((R.string.delete_helper_show_deletion_message_if),media.getDisplayTitle());
        } else {
            title += ": " + context.getString(R.string.delete_helper_show_deletion_title_failed);
            message = context.getString(R.string.delete_helper_show_deletion_message_else) ;
        }

        String urlForDelete = BuildConfig.COMMONS_URL + "/wiki/Commons:Deletion_requests/" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForDelete));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_DELETE, browserIntent);
        return result;
    }
}
