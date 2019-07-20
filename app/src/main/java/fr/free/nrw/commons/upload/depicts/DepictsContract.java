package fr.free.nrw.commons.upload.depicts;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

/**
 * The contract with with DepictsFragment and its presenter would talk to each other
 */

public interface DepictsContract {

    interface View {
        void goToNextScreen();

        void goToPreviousScreen();

        void noDepictionSelected();

        void showProgress(boolean shouldShow);

        void showError();

        void setDepictsList(List<DepictedItem> depictedItemList);
    }

    interface UserActionListener extends BasePresenter<View> {

        void onPreviousButtonClicked();

        void onDepictItemClicked(DepictedItem depictedItem);

        void searchForDepictions(String query);

        void verifyDepictions();
    }
}
