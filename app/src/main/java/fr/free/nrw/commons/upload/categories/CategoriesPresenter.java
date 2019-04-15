package fr.free.nrw.commons.upload.categories;

import android.text.TextUtils;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.category.CategoryClickedListener;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.upload.UploadModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class CategoriesPresenter implements ICategories.UserActionListener {

    private final CategoriesModel categoriesModel;
    private final UploadModel uploadModel;
    ICategories.View view;

    @Inject
    public CategoriesPresenter(CategoriesModel categoriesModel, UploadModel uploadModel) {
        this.categoriesModel = categoriesModel;
        this.uploadModel = uploadModel;
    }

    @Override
    public void onAttachView(ICategories.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = null;
    }

    @Override
    public void searchForCategories(String query, List<String> imageTitleList) {
        List<CategoryItem> categoryItems = new ArrayList<>();
        Observable.fromIterable(categoriesModel.getSelectedCategories())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    view.showProgress(true);
                    view.showError(null);
                    view.setCategories(null);
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        categoriesModel.searchAll(query, imageTitleList)
                                .mergeWith(categoriesModel.searchCategories(query, imageTitleList))
                                .concatWith(TextUtils.isEmpty(query)
                                        ? categoriesModel.defaultCategories(imageTitleList)
                                        : Observable.empty())
                )
                .filter(categoryItem -> !categoriesModel.containsYear(categoryItem.getName()))
                .distinct()
                .sorted(categoriesModel.sortBySimilarity(query))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoryItems.add(s),
                        Timber::e,
                        () -> {
                            view.setCategories(categoryItems);
                            view.showProgress(false);

                            if (categoryItems.isEmpty()) {
                                view.showError(R.string.no_categories_found);
                            }
                        }
                );
    }

    @Override
    public void verifyCategories() {
        List<CategoryItem> selectedCategories = categoriesModel.getSelectedCategories();
        if (selectedCategories != null && !selectedCategories.isEmpty()) {
            uploadModel.setSelectedCategories(categoriesModel.getCategoryStringList());
            view.goToNextScreen();
        } else {
            view.showNoCategorySelected();
        }
    }

    @Override
    public CategoryClickedListener getCategoriesModel() {
        return categoriesModel;
    }
}
