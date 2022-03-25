package fr.free.nrw.commons.upload.categories;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import kotlin.Unit;
import timber.log.Timber;

public class UploadCategoriesFragment extends UploadBaseFragment implements CategoriesContract.View {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.tv_subtitle)
    TextView tvSubTitle;
    @BindView(R.id.til_container_search)
    TextInputLayout tilContainerEtSearch;
    @BindView(R.id.et_search)
    PasteSensitiveTextInputEditText etSearch;
    @BindView(R.id.pb_categories)
    ProgressBar pbCategories;
    @BindView(R.id.rv_categories)
    RecyclerView rvCategories;
    @BindView(R.id.tooltip)
    ImageView tooltip;
    @BindView(R.id.btn_next)
    AppCompatButton btnNext;
    @BindView(R.id.btn_previous)
    AppCompatButton btnPrevious;

    @Inject
    CategoriesContract.UserActionListener presenter;
    private UploadCategoryAdapter adapter;
    private Disposable subscribe;
    /**
     * media, passed from MediaFragment
     */
    private Media media;
    /**
     * Shows and dismisses the ProgressBar
     */
    ProgressDialog progressDialog;

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

        Bundle bundle = getArguments();
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Categories");
        }

        if (media != null) {
            handleBackEvent(view);
        }

        init();
    }

    private void init() {
        if(media == null) {
            tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                callback.getTotalNumberOfSteps(), getString(R.string.categories_activity_title)));
        } else {
            tvTitle.setText("Edit Categories");
            tvSubTitle.setVisibility(View.GONE);
            btnNext.setText("Save");
            btnPrevious.setText("Cancel");
        }
        setTvSubTitle();
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

    /**
     * Removes  the tv subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    private void setTvSubTitle() {
        final Activity activity = getActivity();
        if (activity instanceof UploadActivity) {
            final boolean isMultipleFileSelected = ((UploadActivity) activity).getIsMultipleFilesSelected();
            if (!isMultipleFileSelected) {
                tvSubTitle.setVisibility(View.GONE);
            }
        }
    }

    private void searchForCategory(String query) {
        presenter.searchForCategories(query);
    }

    private void initRecyclerView() {
        if (media == null) {
            adapter = new UploadCategoryAdapter(categoryItem -> {
                presenter.onCategoryItemClicked(categoryItem);
                return Unit.INSTANCE;
            }, new ArrayList<>());
        } else {
            for (String s :
                media.getCategories()) {
                Log.d("haha", "initRecyclerView: "+s);
            }
            adapter = new UploadCategoryAdapter(categoryItem -> {
                presenter.onCategoryItemClicked(categoryItem);
                return Unit.INSTANCE;
            }, media.getCategories());
        }
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
    public void goBackToPreviousScreen() {
        getFragmentManager().popBackStack();
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
        if (media != null) {
            presenter.updateCategories(media);
        } else {
            presenter.verifyCategories();
        }
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        if (media != null) {
            getFragmentManager().popBackStack();
            presenter.clearPreviousSelection();
        } else {
            callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
        }
    }

    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        final Editable text = etSearch.getText();
        if (text != null) {
            presenter.searchForCategories(text.toString());
        }
    }

    /**
     * Hides the action bar while opening editing fragment
     */
    @Override
    public void onResume() {
        super.onResume();
        if (media != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        }
    }

    /**
     * Shows the action bar while closing editing fragment
     */
    @Override
    public void onStop() {
        super.onStop();
        if (media != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        }
    }

    /**
     * Shows the progress dialog
     */
    @Override
    public void showProgressDialog() {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.show();
    }

    /**
     * Hides the progress dialog
     */
    @Override
    public void dismissProgressDialog() {
        progressDialog.dismiss();
    }

    /**
     * Gets context
     * @return
     */
    @Override
    public Context getFragmentContext() {
        return requireContext();
    }

    /**
     * update all categories in category layout of MediaDetailFragment
     *
     * @param categories all categories
     */
    @Override
    public void updateList(final List<String> categories) {
        final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
        assert mediaDetailFragment != null;
        mediaDetailFragment.rebuildCatList(categories);
    }

    @Override
    public List<CategoryItem> getExistingCategories() {
        if (media != null) {
            final List<CategoryItem> categoryItems = new ArrayList<>();
            for (final String name :
                Objects.requireNonNull(media.getCategories())) {
                categoryItems.add(new CategoryItem(name, false));
            }
            return categoryItems;
        }
        return null;
    }

    private void handleBackEvent(View view) {
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((view1, keycode, keyEvent) -> {
            if (keycode == KeyEvent.KEYCODE_BACK) {
                assert getFragmentManager() != null;
                getFragmentManager().popBackStack();
                presenter.clearPreviousSelection();
                return true;
            }
            return false;
        });
    }
}

