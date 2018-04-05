package fr.free.nrw.commons.category;


import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.MwVolleyApi;
import fr.free.nrw.commons.upload.SingleUploadFragment;
import fr.free.nrw.commons.utils.StringSortingUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.KeyEvent.ACTION_UP;
import static android.view.KeyEvent.KEYCODE_BACK;

/**
 * Displays the category suggestion and selection screen. Category search is initiated here.
 */
public class CategorizationFragment extends CommonsDaggerSupportFragment {

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

    @Inject MediaWikiApi mwApi;
    @Inject @Named("default_preferences") SharedPreferences prefs;
    @Inject @Named("prefs") SharedPreferences prefsPrefs;
    @Inject @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs;
    @Inject CategoryDao categoryDao;

    private RVRendererAdapter<CategoryItem> categoriesAdapter;
    private OnCategoriesSaveHandler onCategoriesSaveHandler;
    private HashMap<String, ArrayList<String>> categoriesCache;
    private List<CategoryItem> selectedCategories = new ArrayList<>();
    private TitleTextWatcher textWatcher = new TitleTextWatcher();
    private boolean hasDirectCategories = false;

    private final CategoriesAdapterFactory adapterFactory = new CategoriesAdapterFactory(item -> {
        if (item.isSelected()) {
            selectedCategories.add(item);
            updateCategoryCount(item);
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

        ArrayList<CategoryItem> items = new ArrayList<>();
        categoriesCache = new HashMap<>();
        if (savedInstanceState != null) {
            items.addAll(savedInstanceState.getParcelableArrayList("currentCategories"));
            //noinspection unchecked
            categoriesCache.putAll((HashMap<String, ArrayList<String>>) savedInstanceState
                    .getSerializable("categoriesCache"));
        }

        categoriesAdapter = adapterFactory.create(items);
        categoriesList.setAdapter(categoriesAdapter);


        categoriesFilter.addTextChangedListener(textWatcher);

        categoriesFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });

        RxTextView.textChanges(categoriesFilter)
                .takeUntil(RxView.detaches(categoriesFilter))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> updateCategoryList(filter.toString()));
        return rootView;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        categoriesFilter.removeTextChangedListener(textWatcher);
        super.onDestroyView();
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
                                .concatWith(TextUtils.isEmpty(filter)
                                        ? defaultCategories() : Observable.empty())
                )
                .filter(categoryItem -> !containsYear(categoryItem.getName()))
                .distinct()
                .sorted(sortBySimilarity(filter))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        Timber::e,
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

    private Comparator<CategoryItem> sortBySimilarity(final String filter) {
        Comparator<String> stringSimilarityComparator = StringSortingUtils.sortBySimilarity(filter);
        return (firstItem, secondItem) -> stringSimilarityComparator
                .compare(firstItem.getName(), secondItem.getName());
    }

    private List<String> getStringList(List<CategoryItem> input) {
        List<String> output = new ArrayList<>();
        for (CategoryItem item : input) {
            output.add(item.getName());
        }
        return output;
    }

    private Observable<CategoryItem> defaultCategories() {

        Observable<CategoryItem> directCat = directCategories();
        if (hasDirectCategories) {
            Timber.d("Image has direct Cat");
            return directCat
                    .concatWith(gpsCategories())
                    .concatWith(titleCategories())
                    .concatWith(recentCategories());
        }
        else {
            Timber.d("Image has no direct Cat");
            return gpsCategories()
                    .concatWith(titleCategories())
                    .concatWith(recentCategories());
        }
    }

    private Observable<CategoryItem> directCategories() {
        String directCategory = directPrefs.getString("Category", "");
        // Strip newlines to prevent blank categories, and to tidy existing categories
        directCategory = directCategory.replace("\n", "");

        List<String> categoryList = new ArrayList<>();
        Timber.d("Direct category found: " + "'" + directCategory + "'");

        if (!directCategory.equals("")) {
            hasDirectCategories = true;
            categoryList.add(directCategory);
            Timber.d("DirectCat does not equal emptyString. Direct Cat list has " + categoryList);
        }
        return Observable.fromIterable(categoryList).map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(
                MwVolleyApi.GpsCatExists.getGpsCatExists()
                        ? MwVolleyApi.getGpsCat() : new ArrayList<>())
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> titleCategories() {
        //Retrieve the title that was saved when user tapped submit icon
        String title = prefs.getString("Title", "");

        return mwApi
                .searchTitles(title, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(categoryDao.recentCategories(SEARCH_CATS_LIMIT))
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
        return mwApi
                .allCategories(term, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> searchCategories(String term) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return Observable.empty();
        }

        return mwApi
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
        //Check if the year in the form of XX(X)0s is relevant, i.e. in the 2000s or 2010s as stated in Issue #1029
        return ((item.matches(".*(19|20)\\d{2}.*") && !item.contains(yearInString) && !item.contains(prevYearInString))
                || item.matches("(.*)needing(.*)") || item.matches("(.*)taken on(.*)")
                || (item.matches(".*0s.*") && !item.matches(".*(200|201)0s.*")));
    }

    private void updateCategoryCount(CategoryItem item) {
        Category category = categoryDao.find(item.getName());

        // Newly used category...
        if (category == null) {
            category = new Category(null, item.getName(), new Date(), 0);
        }

        category.incTimesUsed();
        categoryDao.save(category);
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

    private class TitleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        }
    }
}
