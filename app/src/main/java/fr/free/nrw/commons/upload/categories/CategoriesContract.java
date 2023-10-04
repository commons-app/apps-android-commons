package fr.free.nrw.commons.upload.categories;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.category.CategoryItem;
import java.util.List;

/**
 * The contract with with UploadCategoriesFragment and its presenter would talk to each other
 */
public interface CategoriesContract {

    interface View {

        void showProgress(boolean shouldShow);

        void showError(String error);

        void showError(int stringResourceId);

        void setCategories(List<CategoryItem> categories);

        void goToNextScreen();

        void showNoCategorySelected();

        /**
         * Gets existing category names from media
         */
        List<String> getExistingCategories();

        /**
         * Returns required context
         */
        Context getFragmentContext();

        /**
         * Returns to previous fragment
         */
        void goBackToPreviousScreen();

        /**
         * Shows the progress dialog
         */
        void showProgressDialog();

        /**
         * Hides the progress dialog
         */
        void dismissProgressDialog();

        /**
         * Refreshes the categories
         */
        void refreshCategories();
    }

    interface UserActionListener extends BasePresenter<View> {

        void searchForCategories(String query);

        void verifyCategories();

        void onCategoryItemClicked(CategoryItem categoryItem);

        /**
         * Attaches view and media
         */
        void onAttachViewWithMedia(@NonNull CategoriesContract.View view, Media media);

        /**
         * Clears previous selections
         */
        void clearPreviousSelection();

        /**
         * Update the categories
         */
        void updateCategories(Media media, String wikiText);

        LiveData<List<CategoryItem>> getCategories();

        void selectCategories();

    }


}
