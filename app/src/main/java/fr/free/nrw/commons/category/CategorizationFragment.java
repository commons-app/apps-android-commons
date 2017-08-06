package fr.free.nrw.commons.category;

import android.content.ContentProviderClient;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.data.Category;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.MwVolleyApi;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_BACK;
import static fr.free.nrw.commons.category.CategoryContentProvider.AUTHORITY;

/**
 * Displays the category suggestion and selection screen. Category search is initiated here.
 */
public class CategorizationFragment extends Fragment {

    public static final int SEARCH_CATS_LIMIT = 25;

    @BindView(R.id.categoriesListBox)
    RecyclerView categoriesList;
    @BindView(R.id.categoriesSearchBox)
    EditText categoriesFilter;
    @BindView(R.id.categoriesSearchInProgress)
    ProgressBar categoriesSearchInProgress;
    @BindView(R.id.categoriesNotFound)
    TextView categoriesNotFoundView;
    @BindView(R.id.categoriesExplanation)
    TextView categoriesSkip;

    private RVRendererAdapter<CategoryItem> categoriesAdapter;
    private OnCategoriesSaveHandler onCategoriesSaveHandler;
    private HashMap<String, ArrayList<String>> categoriesCache;
    private List<CategoryItem> selectedCategories = new ArrayList<>();
    private ContentProviderClient databaseClient;
    private final CategoriesAdapterFactory adapterFactory = new CategoriesAdapterFactory(item -> {
        if (item.isSelected()) {
            selectedCategories.add(item);
            updateCategoryCount(item, databaseClient);
        } else {
            selectedCategories.remove(item);
        }
    });

    private void updateCategoryCount(CategoryItem item, ContentProviderClient client) {
        Category cat = lookupCategory(item.getName());
        cat.incTimesUsed();
        cat.save(client);
    }

    private Category lookupCategory(String name) {
        Category cat = Category.find(databaseClient, name);

        if (cat == null) {
            // Newly used category...
            cat = new Category();
            cat.setName(name);
            cat.setLastUsed(new Date());
            cat.setTimesUsed(0);
        }

        return cat;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_categorization, container, false);
        ButterKnife.bind(this, rootView);

        categoriesList.setLayoutManager(new LinearLayoutManager(getContext()));

        RxView.clicks(categoriesSkip)
                .takeUntil(RxView.detaches(categoriesSkip))
                .subscribe(o -> {
                    getActivity().onBackPressed();
                    getActivity().finish();
                });

        ArrayList<CategoryItem> items = new ArrayList<>();
        categoriesCache = new HashMap<>();
        if (savedInstanceState != null) {
            items.addAll(savedInstanceState.getParcelableArrayList("currentCategories"));
            categoriesCache.putAll((HashMap<String, ArrayList<String>>) savedInstanceState
                    .getSerializable("categoriesCache"));
        }

        categoriesAdapter = adapterFactory.create(items);
        categoriesList.setAdapter(categoriesAdapter);

        RxTextView.textChanges(categoriesFilter)
                .takeUntil(RxView.detaches(categoriesFilter))
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> updateCategoryList(filter.toString()));
        return rootView;
    }

    private void updateCategoryList(String filter) {
        Observable.fromIterable(selectedCategories)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> {
                    categoriesSearchInProgress.setVisibility(View.VISIBLE);
                    categoriesNotFoundView.setVisibility(View.GONE);
                    categoriesSkip.setVisibility(View.GONE);
                    categoriesAdapter.clear();
                })
                .observeOn(Schedulers.io())
                .concatWith(
                        search(filter)
                                .mergeWith(search2(filter))
                                .filter(categoryItem -> !selectedCategories.contains(categoryItem))
                                .switchIfEmpty(
                                        gpsCategories()
                                                .concatWith(titleCategories())
                                                .concatWith(recentCategories())
                                                .filter(categoryItem -> !selectedCategories.contains(categoryItem))
                                )
                )
                .filter(categoryItem -> !containsYear(categoryItem.getName()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        throwable -> Timber.e(throwable),
                        () -> {
                            categoriesAdapter.notifyDataSetChanged();
                            categoriesSearchInProgress.setVisibility(View.GONE);

                            if (categoriesAdapter.getItemCount() == 0) {
                                if (TextUtils.isEmpty(filter)) {
                                    // If we found no recent cats, show the skip message!
                                    categoriesSkip.setVisibility(View.VISIBLE);
                                } else {
                                    categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
                                    categoriesNotFoundView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                );
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.fragment_categorization, menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        View rootView = getView();
        if (rootView != null) {
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
            rootView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == ACTION_UP && keyCode == KEYCODE_BACK) {
                    backButtonDialog();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseClient.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int itemCount = categoriesAdapter.getItemCount();
        ArrayList<CategoryItem> items = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            items.add(categoriesAdapter.getItem(i));
        }
        outState.putParcelableArrayList("currentCategories", items);
        outState.putSerializable("categoriesCache", categoriesCache);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_save_categories:
                //If no categories selected, display warning to user
                if (selectedCategories.size() == 0) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage("Images without categories are rarely usable. "
                                    + "Are you sure you want to submit without selecting "
                                    + "categories?")
                            .setTitle("No Categories Selected")
                            .setPositiveButton("No, go back", (dialog, id) -> {
                                //Exit menuItem so user can select their categories
                            })
                            .setNegativeButton("Yes, submit", (dialog, id) -> {
                                //Proceed to submission
                                onCategoriesSaveHandler.onCategoriesSave(getStringList(selectedCategories));
                            })
                            .create()
                            .show();
                } else {
                    //Proceed to submission
                    onCategoriesSaveHandler.onCategoriesSave(getStringList(selectedCategories));
                }
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        onCategoriesSaveHandler = (OnCategoriesSaveHandler) getActivity();
        getActivity().setTitle(R.string.categories_activity_title);
        databaseClient = getActivity().getContentResolver().acquireContentProviderClient(AUTHORITY);
    }

    private List<String> getStringList(List<CategoryItem> input) {
        List<String> output = new ArrayList<>();
        for (CategoryItem item : input) {
            output.add(item.getName());
        }
        return output;
    }

    private Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(
                MwVolleyApi.GpsCatExists.getGpsCatExists() ?
                        MwVolleyApi.getGpsCat() : new ArrayList<>())
                .map(s -> new CategoryItem(s, false));
    }

    private Observable<CategoryItem> titleCategories() {
        //Retrieve the title that was saved when user tapped submit icon
        SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String title = titleDesc.getString("Title", "");

        return Observable.just(title)
                .observeOn(Schedulers.io())
                .flatMapIterable(s -> titleCatQuery(s))
                .map(s -> new CategoryItem(s, false));
    }

    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(Category.recentCategories(databaseClient, SEARCH_CATS_LIMIT))
                .map(s -> new CategoryItem(s, false));
    }

    private Observable<CategoryItem> search(String term) {
        return Single.just(term)
                .map(s -> {
                    //If user hasn't typed anything in yet, get GPS and recent items
                    if (TextUtils.isEmpty(s)) {
                        return new ArrayList<String>();
                    }

                    //if user types in something that is in cache, return cached category
                    if (categoriesCache.containsKey(s)) {
                        return categoriesCache.get(s);
                    }

                    //otherwise if user has typed something in that isn't in cache, search API for matching categories
                    //URL: https://commons.wikimedia.org/w/api.php?action=query&list=allcategories&acprefix=filter&aclimit=25
                    MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
                    List<String> categories = new ArrayList<>();
                    try {
                        categories = api.allCategories(SEARCH_CATS_LIMIT, s);
                        Timber.d("Prefix URL filter %s", categories);
                    } catch (IOException e) {
                        Timber.e(e, "IO Exception: ");
                        //Return empty arraylist
                        return categories;
                    }

                    Timber.d("Found categories from Prefix search, waiting for filter");
                    return categories;
                })
                .flatMapObservable(Observable::fromIterable)
                .map(s -> new CategoryItem(s, false));
    }

    private Observable<CategoryItem> search2(String term) {
        return Single.just(term)
                .map(s -> {
                    //If user hasn't typed anything in yet, get GPS and recent items
                    if (TextUtils.isEmpty(s)) {
                        return new ArrayList<String>();
                    }

                    MediaWikiApi api = CommonsApplication.getInstance().getMWApi();

                    //URL https://commons.wikimedia.org/w/api.php?action=query&format=xml&list=search&srwhat=text&srenablerewrites=1&srnamespace=14&srlimit=10&srsearch=
                    try {
                        return api.searchCategories(SEARCH_CATS_LIMIT, term);
                    } catch (IOException e) {
                        Timber.e(e, "IO Exception: ");
                        return new ArrayList<String>();
                    }
                })
                .flatMapObservable(Observable::fromIterable)
                .map(s -> new CategoryItem(s, false));
    }

    private boolean containsYear(String items) {

        //Check for current and previous year to exclude these categories from removal
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String yearInString = String.valueOf(year);

        int prevYear = year - 1;
        String prevYearInString = String.valueOf(prevYear);
        Timber.d("Previous year: %s", prevYearInString);


        //Check if s contains a 4-digit word anywhere within the string (.* is wildcard)
        //And that s does not equal the current year or previous year
        //And if it is an irrelevant category such as Media_needing_categories_as_of_16_June_2017(Issue #750)
        return ((items.matches(".*(19|20)\\d{2}.*") && !items.contains(yearInString) && !items.contains(prevYearInString))
                || items.matches("(.*)needing(.*)") || items.matches("(.*)taken on(.*)"));
    }

    public int getCurrentSelectedCount() {
        return selectedCategories.size();
    }

    public void backButtonDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you want to go back? The image will not "
                        + "have any categories saved.")
                .setTitle("Warning")
                .setPositiveButton("No", (dialog, id) -> {
                    //No need to do anything, user remains on categorization screen
                })
                .setNegativeButton("Yes", (dialog, id) -> getActivity().finish())
                .create()
                .show();
    }
}
