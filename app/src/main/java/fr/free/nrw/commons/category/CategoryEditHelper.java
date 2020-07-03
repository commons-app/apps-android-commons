package fr.free.nrw.commons.category;

import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_DELETE;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.media.MwParseResponse;
import fr.free.nrw.commons.media.MwParseResult;
import fr.free.nrw.commons.notification.NotificationHelper;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import timber.log.Timber;

public class CategoryEditHelper {
    private final NotificationHelper notificationHelper;
    private final PageEditClient pageEditClient;
    private final ViewUtilWrapper viewUtil;
    private final CategoryEditInterface categoryEditInterface;
    private final String username;

    @Inject
    public CategoryEditHelper(NotificationHelper notificationHelper,
        @Named("commons-page-edit") PageEditClient pageEditClient,
        ViewUtilWrapper viewUtil,
        @Named("username") String username,
        CategoryEditInterface categoryEditInterface) {
        this.notificationHelper = notificationHelper;
        this.pageEditClient = pageEditClient;
        this.viewUtil = viewUtil;
        this.username = username;
        this.categoryEditInterface = categoryEditInterface;
    }

    /**
     * Public interface to edit categories
     * @param context
     * @param media
     * @param categories
     * @return
     */
    public Single<Boolean> makeCategoryEdit(Context context, Media media, List<String> categories) {
        viewUtil.showShortToast(context, "Trying to add categories");

        return addCategory(media, categories)
            .flatMapSingle(result -> Single.just(showCategoryEditNotification(context, media, result)))
            .firstOrError();
    }
/*
    private Observable<Boolean> updateCategories(Media media, List<String> categories) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Updating categories";
        Log.d("deneme8","pagetitle:"+media.getFilename());
        try {
        return categoryEditInterface.getContentOfFile(media.getFilename())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .map(mediaDetailResponse -> {
                    Log.d("deneme8", "content:");
                    Pattern pattern = Pattern.compile("(?=\\[\\[Category\\:)(.*?)\\]\\]");
                    String content = mediaDetailResponse.query().firstPage().revisions().get(0)
                        .content();
                    Matcher matcher = pattern.matcher(content);
                    Log.d("deneme8", "content:"+content);
                    StringBuilder builder = new StringBuilder();
                    int i = 0;
                    while (matcher.find()) {
                        builder.append(content.substring(i, matcher.start()));
                        i = matcher.end();
                    }

                    String pageContentCategoriesRemoved = builder.toString();
                    Log.d("deneme8", "pageContentRemoved:"+pageContentCategoriesRemoved);
                    // TODO burada kaldın

                    StringBuilder buffer = new StringBuilder();

                    if (categories != null && categories.size() != 0) {

                        for (int k = 0; k < categories.size(); k++) {
                            buffer.append("\n[[Category:").append(categories.get(i)).append("]]");
                        }
                    } else {
                        buffer.append("{{subst:unc}}");
                    }
                    String categoryWikitext = buffer.toString();
                    Log.d("deneme8", "categoryWikitext:"+categoryWikitext);
                    pageContentCategoriesRemoved += categoryWikitext;
                    return pageEditClient.edit(media.getFilename(), pageContentCategoriesRemoved + "\n", summary);
                }).blockingSingle();
        }catch (Throwable throwable) {
            return Observable.just(false);
        }
    }
*/
    /**
     * Appends new categories
     * @param media
     * @param categories to be added
     * @return
     */
    private Observable<Boolean> addCategory(Media media, List<String> categories) {
        Timber.d("thread is category adding %s", Thread.currentThread().getName());
        String summary = "Adding categories";

        StringBuilder buffer = new StringBuilder();

        if (categories != null && categories.size() != 0) {

            for (int i = 0; i < categories.size(); i++) {
                buffer.append("\n[[Category:").append(categories.get(i)).append("]]");
            }
        } else {
            buffer.append("{{subst:unc}}");
        }
        String appendText = buffer.toString();
        return pageEditClient.appendEdit(media.getFilename(), appendText + "\n", summary);
    }

    private boolean showCategoryEditNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getString(R.string.category_edit_helper_show_deletion_title);

        if (result) {
            title += ": " + context.getString(R.string.category_edit_helper_show_deletion_title_success);
            StringBuilder categoriesInMessage = new StringBuilder();
            List<String> mediaCategoryList = media.getCategories();
            for (String category : mediaCategoryList) {
                categoriesInMessage.append(category);
                if (category.equals(mediaCategoryList.get(mediaCategoryList.size()-1))) {
                    continue;
                }
                categoriesInMessage.append(",");
            }

            message = context.getResources().getQuantityString(R.plurals.category_edit_helper_show_deletion_message_if, mediaCategoryList.size(), categoriesInMessage.toString());
        } else {
            title += ": " + context.getString(R.string.category_edit_helper_show_deletion_title);
            message = context.getString(R.string.delete_helper_show_deletion_message_else) ;
        }

        String urlForFile = BuildConfig.COMMONS_URL + "/wiki/" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_DELETE, browserIntent);
        return result;
    }
}
