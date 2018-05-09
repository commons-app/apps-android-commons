package fr.free.nrw.commons.category;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.MwVolleyApi;
import fr.free.nrw.commons.utils.StringSortingUtils;
import fr.free.nrw.commons.utils.ViewUtil;
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

    @Inject CategoriesModel categoriesModel;

    private CategoryRendererAdapter categoriesAdapter;
    private OnCategoriesSaveHandler onCategoriesSaveHandler;
    private TitleTextWatcher textWatcher = new TitleTextWatcher();

    @SuppressLint("CheckResult")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_categorization, container, false);
        ButterKnife.bind(this, rootView);

        categoriesList.setLayoutManager(new LinearLayoutManager(getContext()));

        ArrayList<CategoryItem> items = new ArrayList<>();
        if (savedInstanceState != null) {
            items.addAll(savedInstanceState.getParcelableArrayList("currentCategories"));
            //noinspection unchecked
            categoriesModel.cacheAll((HashMap<String, ArrayList<String>>) savedInstanceState
                    .getSerializable("categoriesCache"));
        }

        CategoriesAdapterFactory adapterFactory = new CategoriesAdapterFactory(item -> {
            if (item.isSelected()) {
                categoriesModel.selectCategory(item);
                categoriesModel.updateCategoryCount(item);
            } else {
                categoriesModel.unselectCategory(item);
            }
        });
        categoriesAdapter = adapterFactory.create(items);
        categoriesList.setAdapter(categoriesAdapter);

        categoriesFilter.addTextChangedListener(textWatcher);

        categoriesFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ViewUtil.hideKeyboard(v);
            }
        });

        RxTextView.textChanges(categoriesFilter)
                .takeUntil(RxView.detaches(categoriesFilter))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> updateCategoryList(filter.toString()));
        return rootView;
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
        outState.putParcelableArrayList("currentCategories", categoriesAdapter.allItems());
        outState.putSerializable("categoriesCache", categoriesModel.getCategoriesCache());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_save_categories:
                if (categoriesModel.selectedCategoriesCount() > 0) {
                    //Some categories selected, proceed to submission
                    onCategoriesSaveHandler.onCategoriesSave(categoriesModel.getCategoryStringList());
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

    @SuppressLint({"StringFormatInvalid", "CheckResult"})
    private void updateCategoryList(String filter) {
        Observable.fromIterable(categoriesModel.getSelectedCategories())
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
                        categoriesModel.searchAll(filter)
                                .mergeWith(categoriesModel.searchCategories(filter))
                                .concatWith(TextUtils.isEmpty(filter)
                                        ? categoriesModel.defaultCategories() : Observable.empty())
                )
                .filter(categoryItem -> !categoriesModel.containsYear(categoryItem.getName()))
                .distinct()
                .sorted(categoriesModel.sortBySimilarity(filter))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        s -> categoriesAdapter.add(s),
                        Timber::e,
                        () -> {
                            categoriesAdapter.notifyDataSetChanged();
                            categoriesSearchInProgress.setVisibility(View.GONE);

                            if (categoriesAdapter.getItemCount() == categoriesModel.selectedCategoriesCount()) {
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
                    onCategoriesSaveHandler.onCategoriesSave(categoriesModel.getCategoryStringList());
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
