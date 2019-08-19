package fr.free.nrw.commons.upload.depicts;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;

/**
 * The contract with which DepictsFragment and its presenter would talk to each other
 */
public interface DepictsContract {

    interface View {
        void goToNextScreen();

        void goToPreviousScreen();

        void noDepictionSelected();

        void showProgress(boolean shouldShow);

        void showError(Boolean value);

        void setDepictsList(List<DepictedItem> depictedItemList);

        void onImageUrlFetched(String response, int position);
    }

    interface UserActionListener extends BasePresenter<View> {

        void onPreviousButtonClicked();

        void onDepictItemClicked(DepictedItem depictedItem);

        void searchForDepictions(String query);

        void verifyDepictions();

        void fetchThumbnailForEntityId(String entityId, int position);
    }
}
