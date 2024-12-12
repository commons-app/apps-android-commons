package fr.free.nrw.commons.upload.depicts

import android.content.Context
import androidx.lifecycle.LiveData
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem

/**
 * The contract with which DepictsFragment and its presenter would talk to each other
 */
interface DepictsContract {
    interface View {
        /**
         * Go to category screen
         */
        fun goToNextScreen()

        /**
         * Go to media detail screen
         */
        fun goToPreviousScreen()

        /**
         * show error in case of no depiction selected
         */
        fun noDepictionSelected()

        /**
         * Show progress/Hide progress depending on the boolean value
         */
        fun showProgress(shouldShow: Boolean)

        /**
         * decides whether to show error values or not depending on the boolean value
         */
        fun showError(value: Boolean)

        /**
         * add depictions to list
         */
        fun setDepictsList(depictedItemList: List<DepictedItem>)

        /**
         * Returns required context
         */
        fun getFragmentContext(): Context

        /**
         * Returns to previous fragment
         */
        fun goBackToPreviousScreen()

        /**
         * Gets existing depictions IDs from media
         */
        fun getExistingDepictions(): List<String>?

        /**
         * Shows the progress dialog
         */
        fun showProgressDialog()

        /**
         * Hides the progress dialog
         */
        fun dismissProgressDialog()

        /**
         * Update the depictions
         */
        fun updateDepicts()

        /**
         * Navigate the user to Login Activity
         */
        fun navigateToLoginScreen()
    }

    interface UserActionListener : BasePresenter<View> {
        /**
         * Takes to previous screen
         */
        fun onPreviousButtonClicked()

        /**
         * Listener for the depicted items selected from the list
         */
        fun onDepictItemClicked(depictedItem: DepictedItem)

        /**
         * asks the repository to fetch depictions for the query
         * @param query
         */
        fun searchForDepictions(query: String)

        /**
         * Selects all associated places (if any) as depictions
         */
        fun selectPlaceDepictions()

        /**
         * Check if depictions were selected
         * from the depiction list
         */
        fun verifyDepictions()

        /**
         * Clears previous selections
         */
        fun clearPreviousSelection()

        fun getDepictedItems(): LiveData<List<DepictedItem>>

        /**
         * Update the depictions
         */
        fun updateDepictions(media: Media)

        /**
         * Attaches view and media
         */
        fun onAttachViewWithMedia(view: View, media: Media)
    }
}
