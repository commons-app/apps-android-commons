package fr.free.nrw.commons.upload.depicts;

import androidx.lifecycle.LiveData;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem;
import java.util.List;

/**
 * The contract with which DepictsFragment and its presenter would talk to each other
 */
public interface DepictsContract {

    interface View {
        /**
         * Go to category screen
         */
        void goToNextScreen();

        /**
         * Go to media detail screen
         */
        void goToPreviousScreen();

        /**
         * show error in case of no depiction selected
         */
        void noDepictionSelected();

        /**
         * Show progress/Hide progress depending on the boolean value
         */
        void showProgress(boolean shouldShow);

        /**
         * decides whether to show error values or not depending on the boolean value
         */
        void showError(Boolean value);

        /**
         * add depictions to list
         */
        void setDepictsList(List<DepictedItem> depictedItemList);
    }

    interface UserActionListener extends BasePresenter<View> {

        /**
         * Takes to previous screen
         */
        void onPreviousButtonClicked();

        /**
         * Listener for the depicted items selected from the list
         */
        void onDepictItemClicked(DepictedItem depictedItem);

        /**
         * asks the repository to fetch depictions for the query
         *  @param query
         */
        void searchForDepictions(String query);

        /**
         * Selects all associated places (if any) as depictions
         */
        void selectPlaceDepictions();

        /**
         * Check if depictions were selected
         * from the depiction list
         */
        void verifyDepictions();

        LiveData<List<DepictedItem>> getDepictedItems();
    }
}
