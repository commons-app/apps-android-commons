package fr.free.nrw.commons.upload.categories;

import android.content.Context;
import fr.free.nrw.commons.Media;
import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.category.CategoryItem;

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

        void goBackToPreviousScreen();

        void showNoCategorySelected();

        void showProgressDialog();

        void dismissProgressDialog();

        Context getFragmentContext();

        void updateList(List<String> categories);

        List<CategoryItem> getExistingCategories();

    }

    interface UserActionListener extends BasePresenter<View> {

        void searchForCategories(String query);

        void verifyCategories();

        void updateCategories(Media media);

        void onCategoryItemClicked(CategoryItem categoryItem);

        void clearPreviousSelection();
    }


}
