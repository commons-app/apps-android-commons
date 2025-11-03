package fr.free.nrw.commons.upload.categories

import android.content.Context
import androidx.lifecycle.LiveData
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.category.CategoryItem

/**
 * The contract with with UploadCategoriesFragment and its presenter would talk to each other
 */
interface CategoriesContract {
    interface View {
        fun showProgress(shouldShow: Boolean)

        fun showError(error: String?)

        fun showError(stringResourceId: Int)

        /**
         * Show a cancelable AlertDialog with a given message.
         */
        fun showErrorDialog(message: String)

        fun setCategories(categories: List<CategoryItem>?)

        fun goToNextScreen()

        fun showNoCategorySelected()

        /**
         * Gets existing category names from media
         */
        fun getExistingCategories(): List<String>?

        /**
         * Returns required context
         */
        fun getFragmentContext(): Context

        /**
         * Returns to previous fragment
         */
        fun goBackToPreviousScreen()

        /**
         * Shows the progress dialog
         */
        fun showProgressDialog()

        /**
         * Hides the progress dialog
         */
        fun dismissProgressDialog()

        /**
         * Refreshes the categories
         */
        fun refreshCategories()

        /**
         * Navigate the user to Login Activity
         */
        fun navigateToLoginScreen()
    }

    interface UserActionListener : BasePresenter<View> {
        fun searchForCategories(query: String)

        fun verifyCategories()

        fun onCategoryItemClicked(categoryItem: CategoryItem)

        /**
         * Attaches view and media
         */
        fun onAttachViewWithMedia(view: View, media: Media)

        /**
         * Clears previous selections
         */
        fun clearPreviousSelection()

        /**
         * Update the categories
         */
        fun updateCategories(media: Media, wikiText: String)

        fun getCategories(): LiveData<List<CategoryItem>>

        fun selectCategories()
    }
}
