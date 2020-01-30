package fr.free.nrw.commons.upload.categories;

import android.text.TextUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

/**
 * The presenter class for UploadCategoriesFragment
 */
@Singleton
public class CategoriesPresenter implements CategoriesContract.UserActionListener {

    private static final CategoriesContract.View DUMMY = (CategoriesContract.View) Proxy
            .newProxyInstance(
                    CategoriesContract.View.class.getClassLoader(),
                    new Class[]{CategoriesContract.View.class},
                    (proxy, method, methodArgs) -> null);
    private final Scheduler ioScheduler;
    private final Scheduler mainThreadScheduler;

    CategoriesContract.View view = DUMMY;
    private UploadRepository repository;

    private CompositeDisposable compositeDisposable;

    @Inject
    public CategoriesPresenter(UploadRepository repository, @Named(IO_THREAD) Scheduler ioScheduler,
                               @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.repository = repository;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
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

    /**
     * asks the repository to fetch categories for the query
     *  @param query
     *
     */
    @Override
    public void searchForCategories(String query) {
        List<CategoryItem> categoryItems = new ArrayList<>();
        List<String> imageTitleList = getImageTitleList();
        Observable<CategoryItem> distinctCategoriesObservable = Observable
                .fromIterable(repository.getSelectedCategories())
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .doOnSubscribe(disposable -> {
                    view.showProgress(true);
                    view.showError(null);
                    view.setCategories(null);
                })
                .observeOn(ioScheduler)
                .concatWith(
                        repository.searchAll(query, imageTitleList)
                )
                .filter(categoryItem -> !repository.containsYear(categoryItem.getName()))
                .distinct();
                if(!TextUtils.isEmpty(query)) {
                distinctCategoriesObservable=distinctCategoriesObservable.sorted(repository.sortBySimilarity(query));
                }
        Disposable searchCategoriesDisposable = distinctCategoriesObservable
                .observeOn(mainThreadScheduler)
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

    /**
     * Returns image title list from UploadItem
     * @return
     */
    private List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : repository.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

    /**
     * Verifies the number of categories selected, prompts the user if none selected
     */
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

    /**
     * ask repository to handle category clicked
     *
     * @param categoryItem
     */
    @Override
    public void onCategoryItemClicked(CategoryItem categoryItem) {
        repository.onCategoryClicked(categoryItem);
    }
}
