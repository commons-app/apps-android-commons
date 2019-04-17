package fr.free.nrw.commons.upload.categories;

import android.text.TextUtils;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.repository.UploadRepository;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class CategoriesPresenter implements CategoriesContract.UserActionListener {

    private static final CategoriesContract.View DUMMY = (CategoriesContract.View) Proxy
            .newProxyInstance(
                    CategoriesContract.View.class.getClassLoader(),
                    new Class[]{CategoriesContract.View.class},
                    (proxy, method, methodArgs) -> null);

    CategoriesContract.View view = DUMMY;
    private UploadRepository repository;

    private CompositeDisposable compositeDisposable;

    @Inject
    public CategoriesPresenter(UploadRepository repository) {
        this.repository = repository;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onAttachView(CategoriesContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
        compositeDisposable.clear();
    }

    @Override
    public void searchForCategories(String query, List<String> imageTitleList) {
        List<CategoryItem> categoryItems = new ArrayList<>();
        Disposable searchCategoriesDisposable = Observable
                .fromIterable(repository.getSelectedCategories())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    view.showProgress(true);
                    view.showError(null);
                    view.setCategories(null);
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        repository.searchAll(query, imageTitleList)
                                .mergeWith(repository.searchCategories(query, imageTitleList))
                                .concatWith(TextUtils.isEmpty(query)
                                        ? repository.defaultCategories(imageTitleList)
                                        : Observable.empty())
                )
                .filter(categoryItem -> !repository.containsYear(categoryItem.getName()))
                .distinct()
                .sorted(repository.sortBySimilarity(query))
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

        compositeDisposable.add(searchCategoriesDisposable);
    }

    @Override
    public void verifyCategories() {
        List<CategoryItem> selectedCategories = repository.getSelectedCategories();
        if (selectedCategories != null && !selectedCategories.isEmpty()) {
            repository.setSelectedCategories(repository.getCategoryStringList());
            view.goToNextScreen();
        } else {
            view.showNoCategorySelected();
        }
    }

    @Override
    public void onCategoryItemClicked(CategoryItem categoryItem) {
        repository.onCategoryClicked(categoryItem);
    }
}
