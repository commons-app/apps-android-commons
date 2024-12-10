package fr.free.nrw.commons.category

import android.content.Context
import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Named
import timber.log.Timber


class CategoryEditHelper @Inject constructor(
    private val notificationHelper: NotificationHelper,
    @Named("commons-page-edit") val pageEditClient: PageEditClient,
    private val viewUtil: ViewUtilWrapper,
    @Named("username") private val username: String
) {

    /**
     * Public interface to edit categories
     * @param context
     * @param media
     * @param categories
     * @return
     */
    fun makeCategoryEdit(
        context: Context,
        media: Media,
        categories: List<String>,
        wikiText: String
    ): Single<Boolean> {
        viewUtil.showShortToast(
            context,
            context.getString(R.string.category_edit_helper_make_edit_toast)
        )
        return addCategory(media, categories, wikiText)
            .flatMapSingle { result ->
                Single.just(showCategoryEditNotification(context, media, result))
            }
            .firstOrError()
    }

    /**
     * Rebuilds the WikiText with new categories and post it on server
     *
     * @param media
     * @param categories to be added
     * @return
     */
    private fun addCategory(
        media: Media,
        categories: List<String>?,
        wikiText: String
    ): Observable<Boolean> {
        Timber.d("thread is category adding %s", Thread.currentThread().name)
        val summary = "Adding categories"
        val buffer = StringBuilder()

        // If the picture was uploaded without a category, the wikitext will contain "Uncategorized" instead of "[[Category"
        val wikiTextWithoutCategory: String = when {
            wikiText.contains("Uncategorized") -> wikiText.substring(0, wikiText.indexOf("Uncategorized"))
            wikiText.contains("[[Category") -> wikiText.substring(0, wikiText.indexOf("[[Category"))
            else -> ""
        }

        if (!categories.isNullOrEmpty()) {
            // If the categories list is empty, when reading the categories of a picture,
            // the code will add "None selected" to categories list in order to see in picture's categories with "None selected".
            // So that after selecting some category, "None selected" should be removed from list
            for (category in categories) {
                if (category != "None selected" || !wikiText.contains("Uncategorized")) {
                    buffer.append("[[Category:").append(category).append("]]\n")
                }
            }
            categories.dropWhile {
                it == "None selected"
            }
        } else {
            buffer.append("{{subst:unc}}")
        }

        val appendText = wikiTextWithoutCategory + buffer
        return pageEditClient.edit(media.filename!!, "$appendText\n", summary)
    }

    private fun showCategoryEditNotification(
        context: Context,
        media: Media,
        result: Boolean
    ): Boolean {
        val title: String
        val message: String

        if (result) {
            title = context.getString(R.string.category_edit_helper_show_edit_title) + ": " +
                    context.getString(R.string.category_edit_helper_show_edit_title_success)

            val categoriesInMessage = StringBuilder()
            val mediaCategoryList = media.categories
            for ((index, category) in mediaCategoryList?.withIndex()!!) {
                categoriesInMessage.append(category)
                if (index != mediaCategoryList.size - 1) {
                    categoriesInMessage.append(",")
                }
            }

            message = context.resources.getQuantityString(
                R.plurals.category_edit_helper_show_edit_message_if,
                mediaCategoryList.size,
                categoriesInMessage.toString()
            )
        } else {
            title = context.getString(R.string.category_edit_helper_show_edit_title) + ": " +
                    context.getString(R.string.category_edit_helper_show_edit_title)
            message = context.getString(R.string.category_edit_helper_edit_message_else)
        }

        val urlForFile = "${BuildConfig.COMMONS_URL}/wiki/${media.filename}"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForFile))
        notificationHelper.showNotification(
            context,
            title,
            message,
            NOTIFICATION_EDIT_CATEGORY,
            browserIntent
        )
        return result
    }

    interface Callback {
        fun updateCategoryDisplay(categories: List<String>?): Boolean
    }

    companion object {
        const val NOTIFICATION_EDIT_CATEGORY = 1
    }
}
