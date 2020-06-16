package fr.free.nrw.commons.upload.categories;

import fr.free.nrw.commons.BasePresenter;
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

  }

  interface UserActionListener extends BasePresenter<View> {

    void searchForCategories(String query);

    void verifyCategories();

    void onCategoryItemClicked(CategoryItem categoryItem);
  }


}
