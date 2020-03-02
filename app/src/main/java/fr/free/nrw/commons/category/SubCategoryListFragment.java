package fr.free.nrw.commons.category;


import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.categories.SearchCategoriesAdapterFactory;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Displays the category search screen.
 */

public class SubCategoryListFragment extends CommonsDaggerSupportFragment {

    private static int TIMEOUT_SECONDS = 15;

    @BindView(R.id.imagesListBox)
    RecyclerView categoriesRecyclerView;
    @BindView(R.id.imageSearchInProgress)
    ProgressBar progressBar;
    @BindView(R.id.imagesNotFound)
    TextView categoriesNotFoundView;

    private String categoryName = null;
    @Inject CategoryClient categoryClient;

    private RVRendererAdapter<String> categoriesAdapter;
    private boolean isParentCategory = true;

    private final SearchCategoriesAdapterFactory adapterFactory = new SearchCategoriesAdapterFactory(item -> {
        // Open SubCategory Details page
        Intent intent = new Intent(getContext(), CategoryDetailsActivity.class);
        intent.putExtra("categoryName", item);
        getContext().startActivity(intent);

    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browse_image, container, false);
        ButterKnife.bind(this, rootView);
        categoryName = getArguments().getString("categoryName");
        isParentCategory = getArguments().getBoolean("isParentCategory");
        initSubCategoryList();
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        else{
            categoriesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        }
        ArrayList<String> items = new ArrayList<>();
        categoriesAdapter = adapterFactory.create(items);
        categoriesRecyclerView.setAdapter(categoriesAdapter);
        return rootView;
    }

    /**
     * Checks for internet connection and then initializes the recycler view with all(max 500) categories of the searched query
     * Clearing categoryAdapter every time new keyword is searched so that user can see only new results
     */
    public void initSubCategoryList() {
        categoriesNotFoundView.setVisibility(GONE);
        if (!NetworkUtils.isInternetConnectionEstablished(getContext())) {
            handleNoInternet();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        if (isParentCategory) {
            compositeDisposable.add(categoryClient.getParentCategoryList("Category:"+categoryName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .collect(ArrayList<String>::new, ArrayList::add)
                    .subscribe(this::handleSuccess, this::handleError));
        } else {
            compositeDisposable.add(categoryClient.getSubCategoryList("Category:"+categoryName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .collect(ArrayList<String>::new, ArrayList::add)
                    .subscribe(this::handleSuccess, this::handleError));
        }
    }


    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     * @param subCategoryList
     */
    private void handleSuccess(List<String> subCategoryList) {
        if (subCategoryList == null || subCategoryList.isEmpty()) {
            initEmptyView();
        }
        else {
            progressBar.setVisibility(View.GONE);
            categoriesAdapter.addAll(subCategoryList);
            categoriesAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Logs and handles API error scenario
     * @param throwable
     */
    private void handleError(Throwable throwable) {
        if (!isParentCategory){
            Timber.e(throwable, "Error occurred while loading queried subcategories");
            ViewUtil.showShortSnackbar(categoriesRecyclerView,R.string.error_loading_categories);
        }else {
            Timber.e(throwable, "Error occurred while loading queried parentcategories");
            ViewUtil.showShortSnackbar(categoriesRecyclerView,R.string.error_loading_categories);
        }
    }

    /**
     * Handles the UI updates for a empty results scenario
     */
    private void initEmptyView() {
        progressBar.setVisibility(GONE);
        categoriesNotFoundView.setVisibility(VISIBLE);
        if (!isParentCategory){
            categoriesNotFoundView.setText(getString(R.string.no_subcategory_found));
        }else {
            categoriesNotFoundView.setText(getString(R.string.no_parentcategory_found));
        }

    }

    /**
     * Handles the UI updates for no internet scenario
     */
    private void handleNoInternet() {
        progressBar.setVisibility(GONE);
        ViewUtil.showShortSnackbar(categoriesRecyclerView, R.string.no_internet);
    }
}
