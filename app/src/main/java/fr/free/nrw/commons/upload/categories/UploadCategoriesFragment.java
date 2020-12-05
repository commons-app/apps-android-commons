package fr.free.nrw.commons.upload.categories;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import kotlin.Unit;
import timber.log.Timber;

public class UploadCategoriesFragment extends UploadBaseFragment implements CategoriesContract.View {

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
    @BindView(R.id.tooltip)
    ImageView tooltip;

    @Inject
    CategoriesContract.UserActionListener presenter;
    private UploadCategoryAdapter adapter;
    private Disposable subscribe;

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
                callback.getTotalNumberOfSteps(), getString(R.string.categories_activity_title)));
        tooltip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showAlertDialog(getActivity(), getString(R.string.categories_activity_title), getString(R.string.categories_tooltip), getString(android.R.string.ok), null, true);
            }
        });
        presenter.onAttachView(this);
        initRecyclerView();
        addTextChangeListenerToEtSearch();
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
        adapter = new UploadCategoryAdapter(categoryItem -> {
            presenter.onCategoryItemClicked(categoryItem);
            return Unit.INSTANCE;
        });
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
        if(categories==null) {
            adapter.clear();
        }
        else{
            adapter.setItems(categories);
        }
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
                getString(R.string.continue_message),
                getString(R.string.cancel),
                () -> goToNextScreen(),
                null);

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
    protected void onBecameVisible() {
        super.onBecameVisible();
        final Editable text = etSearch.getText();
        if (text != null) {
            presenter.searchForCategories(text.toString());
        }
    }
}
