package fr.free.nrw.commons.category;

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

public class CategoryEditHelper {
    private final NotificationHelper notificationHelper;
    public final PageEditClient pageEditClient;
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
    public Single<Boolean> makeCategoryEdit(Context context, Media media, List<String> categories,
        final String wikiText) {
        viewUtil.showShortToast(context, context.getString(R.string.category_edit_helper_make_edit_toast));
        return addCategory(media, categories, wikiText)
            .flatMapSingle(result -> Single.just(showCategoryEditNotification(context, media, result)))
            .firstOrError();
    }

    /**
     * Rebuilds the WikiText with new categpries and post it on server
     *
     * @param media
     * @param categories to be added
     * @return
     */
    private Observable<Boolean> addCategory(Media media, List<String> categories,
        final String wikiText) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Adding categories";
        final StringBuilder buffer = new StringBuilder();
        final String wikiTextWithoutCategory;
        //If the picture was uploaded without a category, the wikitext will contain "Uncategorized" instead of "[[Category"
        if (wikiText.contains("Uncategorized")) {
            wikiTextWithoutCategory = wikiText.substring(0, wikiText.indexOf("Uncategorized"));
        } else if (wikiText.contains("[[Category")) {
            wikiTextWithoutCategory = wikiText.substring(0, wikiText.indexOf("[[Category"));
        } else {
            wikiTextWithoutCategory = "";
        }
        if (categories != null && !categories.isEmpty()) {
            //If the categories list is empty, when reading the categories of a picture,
            // the code will add "None selected" to categories list in order to see in picture's categories with "None selected".
            // So that after selected some category,"None selected" should be removed from list
            for (int i = 0; i < categories.size(); i++) {
                if (!categories.get(i).equals("None selected")//Not to add "None selected" as category to wikiText
                    || !wikiText.contains("Uncategorized")) {
                        buffer.append("[[Category:").append(categories.get(i)).append("]]\n");
                    }
            }
            categories.remove("None selected");
        } else {
            buffer.append("{{subst:unc}}");
        }
        final String appendText = wikiTextWithoutCategory + buffer;
        return pageEditClient.edit(media.getFilename(), appendText + "\n", summary);
    }

    private boolean showCategoryEditNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getString(R.string.category_edit_helper_show_edit_title);

        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_edit_title_success);
            StringBuilder categoriesInMessage = new StringBuilder();
            List<String> mediaCategoryList = media.getCategories();
            for (String category : mediaCategoryList) {
                categoriesInMessage.append(category);
                if (category.equals(mediaCategoryList.get(mediaCategoryList.size()-1))) {
                    continue;
                }
                categoriesInMessage.append(",");
            }

            message = context.getResources().getQuantityString(R.plurals.category_edit_helper_show_edit_message_if, mediaCategoryList.size(), categoriesInMessage.toString());
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
