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
import fr.free.nrw.commons.upload.MwVolleyApi;
import io.reactivex.Observable;
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
                    showBackButtonDialog();
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
                if (selectedCategories.size() > 0) {
                    //Some categories selected, proceed to submission
                    onCategoriesSaveHandler.onCategoriesSave(getStringList(selectedCategories));
                } else {
                    //No categories selected, prompt the user to select some
                    showConfirmationDialog();
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
                        searchAll(filter)
                                .mergeWith(searchCategories(filter))
                                .concatWith( TextUtils.isEmpty(filter)
                                        ? defaultCategories() : Observable.empty())
                )
                .filter(categoryItem -> !containsYear(categoryItem.getName()))
                .distinct()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        throwable -> Timber.e(throwable),
                        () -> {
                            categoriesAdapter.notifyDataSetChanged();
                            categoriesSearchInProgress.setVisibility(View.GONE);

                            if (categoriesAdapter.getItemCount() == selectedCategories.size()) {
                                // There are no suggestions
                                if (TextUtils.isEmpty(filter)) {
                                    // Allow to send image with no categories
                                    categoriesSkip.setVisibility(View.VISIBLE);
                                } else {
                                    // Inform the user that the searched term matches  no category
                                    categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
                                    categoriesNotFoundView.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                );
    }

    private List<String> getStringList(List<CategoryItem> input) {
        List<String> output = new ArrayList<>();
        for (CategoryItem item : input) {
            output.add(item.getName());
        }
        return output;
    }

    private Observable<CategoryItem> defaultCategories() {
        return gpsCategories()
                .concatWith(titleCategories())
                .concatWith(recentCategories());
    }

    private Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(
                MwVolleyApi.GpsCatExists.getGpsCatExists()
                        ? MwVolleyApi.getGpsCat() : new ArrayList<>())
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> titleCategories() {
        //Retrieve the title that was saved when user tapped submit icon
        SharedPreferences titleDesc = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String title = titleDesc.getString("Title", "");

        return CommonsApplication.getInstance().getMWApi()
                .searchTitles(title, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(Category.recentCategories(databaseClient, SEARCH_CATS_LIMIT))
                .map(s -> new CategoryItem(s, false));
    }

    private Observable<CategoryItem> searchAll(String term) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return Observable.empty();
        }

        //if user types in something that is in cache, return cached category
        if (categoriesCache.containsKey(term)) {
            return Observable.fromIterable(categoriesCache.get(term))
                    .map(name -> new CategoryItem(name, false));
        }

        //otherwise, search API for matching categories
        return CommonsApplication.getInstance().getMWApi()
                .allCategories(term, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> searchCategories(String term) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return Observable.empty();
        }

        return CommonsApplication.getInstance().getMWApi()
                .searchCategories(term, SEARCH_CATS_LIMIT)
                .map(s -> new CategoryItem(s, false));
    }

    private boolean containsYear(String item) {
        //Check for current and previous year to exclude these categories from removal
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String yearInString = String.valueOf(year);

        int prevYear = year - 1;
        String prevYearInString = String.valueOf(prevYear);
        Timber.d("Previous year: %s", prevYearInString);

        //Check if item contains a 4-digit word anywhere within the string (.* is wildcard)
        //And that item does not equal the current year or previous year
        //And if it is an irrelevant category such as Media_needing_categories_as_of_16_June_2017(Issue #750)
        return ((item.matches(".*(19|20)\\d{2}.*") && !item.contains(yearInString) && !item.contains(prevYearInString))
                || item.matches("(.*)needing(.*)") || item.matches("(.*)taken on(.*)"));
    }

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

    public int getCurrentSelectedCount() {
        return selectedCategories.size();
    }

    /**
     * Show dialog asking for confirmation to leave without saving categories.
     */
    public void showBackButtonDialog() {
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

    private void showConfirmationDialog() {
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
    }
}
