package fr.free.nrw.commons.upload.categories;

import static fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE_CATEGORY;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
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
    Button btnNext;
    @BindView(R.id.btn_previous)
    Button btnPrevious;

    @Inject
    CategoriesContract.UserActionListener presenter;
    private UploadCategoryAdapter adapter;
    private Disposable subscribe;
    /**
     * Current media
     */
    private Media media;
    /**
     * Progress Dialog for showing background process
     */
    private ProgressDialog progressDialog;
    /**
     * WikiText from the server
     */
    private String wikiText;
    private String nearbyPlaceCategory;

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
        final Bundle bundle = getArguments();
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Categories");
            wikiText = bundle.getString("WikiText");
            nearbyPlaceCategory = bundle.getString(SELECTED_NEARBY_PLACE_CATEGORY);
        }
        init();
        presenter.getCategories().observe(getViewLifecycleOwner(), this::setCategories);
    }

    private void init() {
        if (media == null) {
            tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                callback.getTotalNumberOfSteps(), getString(R.string.categories_activity_title)));
        } else {
            tvTitle.setText(R.string.edit_categories);
            tvSubTitle.setVisibility(View.GONE);
            btnNext.setText(R.string.menu_save_categories);
            btnPrevious.setText(R.string.menu_cancel_upload);
        }

        setTvSubTitle();
        tooltip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtil.showAlertDialog(getActivity(), getString(R.string.categories_activity_title), getString(R.string.categories_tooltip), getString(android.R.string.ok), null, true);
            }
        });
        if (media == null) {
            presenter.onAttachView(this);
        } else {
            presenter.onAttachViewWithMedia(this, media);
        }
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
        adapter = new UploadCategoryAdapter(categoryItem -> {
            presenter.onCategoryItemClicked(categoryItem);
            return Unit.INSTANCE;
        }, nearbyPlaceCategory);
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
        if (categories == null) {
            adapter.clear();
        } else {
            adapter.setItems(categories);
        }
        adapter.notifyDataSetChanged();

        // Nested waiting for search result data to load into the category
        // list and smoothly scroll to the top of the search result list.
        rvCategories.post(new Runnable() {
            @Override
            public void run() {
                rvCategories.smoothScrollToPosition(0);
                rvCategories.post(new Runnable() {
                    @Override
                    public void run() {
                        rvCategories.smoothScrollToPosition(0);
                    }
                });
            }
        });
    }

    @Override
    public void goToNextScreen() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void showNoCategorySelected() {
        if (media == null) {
            DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.no_categories_selected),
                getString(R.string.no_categories_selected_warning_desc),
                getString(R.string.continue_message),
                getString(R.string.cancel),
                () -> goToNextScreen(),
                null);
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_categories_selected),
                Toast.LENGTH_SHORT).show();
            presenter.clearPreviousSelection();
            goBackToPreviousScreen();
        }

    }

    /**
     * Gets existing categories from media
     */
    @Override
    public List<String> getExistingCategories() {
        return (media == null) ? null : media.getCategories();
    }

    /**
     * Returns required context
     */
    @Override
    public Context getFragmentContext() {
        return requireContext();
    }

    /**
     * Returns to previous fragment
     */
    @Override
    public void goBackToPreviousScreen() {
        getFragmentManager().popBackStack();
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
     * Refreshes the categories
     */
    @Override
    public void refreshCategories() {
        final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
        assert mediaDetailFragment != null;
        mediaDetailFragment.updateCategories();
    }

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        if (media != null) {
            presenter.updateCategories(media, wikiText);
        } else {
            presenter.verifyCategories();
        }
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        if (media != null) {
            presenter.clearPreviousSelection();
            adapter.setItems(null);
            final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
            assert mediaDetailFragment != null;
            mediaDetailFragment.onResume();
            goBackToPreviousScreen();
        } else {
            callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
        }
    }

    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        presenter.selectCategories();
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
            etSearch.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    etSearch.clearFocus();
                    presenter.clearPreviousSelection();
                    final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
                    assert mediaDetailFragment != null;
                    mediaDetailFragment.onResume();
                    goBackToPreviousScreen();
                    return true;
                }
                return false;
            });

            Objects.requireNonNull(getView()).setFocusableInTouchMode(true);
            getView().requestFocus();
            getView().setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    presenter.clearPreviousSelection();
                    final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
                    assert mediaDetailFragment != null;
                    mediaDetailFragment.onResume();
                    goBackToPreviousScreen();
                    return true;
                }
                return false;
            });

            Objects.requireNonNull(
                ((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar())
                .hide();

            if (getParentFragment().getParentFragment().getParentFragment()
                instanceof ContributionsFragment) {
                ((ContributionsFragment) (getParentFragment()
                    .getParentFragment().getParentFragment())).nearbyNotificationCardView
                    .setVisibility(View.GONE);
            }
        }
    }

    /**
     * Shows the action bar while closing editing fragment
     */
    @Override
    public void onStop() {
        super.onStop();
        if (media != null) {
            Objects.requireNonNull(
                ((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar())
                .show();
        }
    }
}
