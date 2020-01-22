package fr.free.nrw.commons.upload.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.pedrogomez.renderers.RVRendererAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryClickedListener;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadCategoriesAdapterFactory;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class UploadCategoriesFragment extends UploadBaseFragment implements CategoriesContract.View,
        CategoryClickedListener {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.til_container_search)
    TextInputLayout tilContainerEtSearch;
    @BindView(R.id.et_search)
    TextInputEditText etSearch;
    @BindView(R.id.pb_categories)
    ProgressBar pbCategories;
    @BindView(R.id.rv_categories)
    RecyclerView rvCategories;

    @Inject
    CategoriesContract.UserActionListener presenter;
    private RVRendererAdapter<CategoryItem> adapter;
    private List<String> mediaTitleList=new ArrayList<>();
    private Disposable subscribe;
    private List<CategoryItem> categories;
    private boolean isVisible;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setMediaTitleList(List<String> mediaTitleList) {
        this.mediaTitleList = mediaTitleList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_categories_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        init();
    }

    private void init() {
        tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                callback.getTotalNumberOfSteps()));
        presenter.onAttachView(this);
        initRecyclerView();
        addTextChangeListenerToEtSearch();
        //get default categories for empty query
    }

    @Override
    public void onResume() {
        super.onResume();
        if (presenter != null && isVisible && (categories == null || categories.isEmpty())) {
            presenter.searchForCategories(null);
        }
    }

    private void addTextChangeListenerToEtSearch() {
        subscribe = RxTextView.textChanges(etSearch)
                .doOnEach(v -> tilContainerEtSearch.setError(null))
                .takeUntil(RxView.detaches(etSearch))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> searchForCategory(filter.toString()), Timber::e);
    }

    private void searchForCategory(String query) {
        presenter.searchForCategories(query);
    }

    private void initRecyclerView() {
        adapter = new UploadCategoriesAdapterFactory(this)
                .create(new ArrayList<>());
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
        subscribe.dispose();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        pbCategories.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showError(String error) {
        tilContainerEtSearch.setError(error);
    }

    @Override
    public void showError(int stringResourceId) {
        tilContainerEtSearch.setError(getString(stringResourceId));
    }

    @Override
    public void setCategories(List<CategoryItem> categories) {
        adapter.clear();
        if (categories != null) {
            this.categories = categories;
            adapter.addAll(categories);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void addCategory(CategoryItem category) {
        adapter.add(category);
        adapter.notifyItemInserted(adapter.getItemCount());
    }

    @Override
    public void goToNextScreen() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void showNoCategorySelected() {
        DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.no_categories_selected),
                getString(R.string.no_categories_selected_warning_desc),
                getString(R.string.no_go_back),
                getString(R.string.yes_submit),
                null,
                () -> goToNextScreen());
    }

    @Override
    public void setSelectedCategories(List<CategoryItem> selectedCategories) {

    }

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        presenter.verifyCategories();
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void categoryClicked(CategoryItem item) {
        presenter.onCategoryItemClicked(item);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;

        if (presenter != null && isResumed() && (categories == null || categories.isEmpty())) {
            presenter.searchForCategories(null);
        }
    }
}
