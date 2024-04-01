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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryItem;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.databinding.UploadCategoriesFragmentBinding;
import fr.free.nrw.commons.media.MediaDetailFragment;
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

    @Inject
    CategoriesContract.UserActionListener presenter;
    @Inject
    SessionManager sessionManager;
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

    private UploadCategoriesFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = UploadCategoriesFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        if (binding == null) {
            return;
        }
        if (media == null) {
            if (callback != null) {
                binding.tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                    callback.getTotalNumberOfSteps(), getString(R.string.categories_activity_title)));
            }
        } else {
            binding.tvTitle.setText(R.string.edit_categories);
            binding.tvSubtitle.setVisibility(View.GONE);
            binding.btnNext.setText(R.string.menu_save_categories);
            binding.btnPrevious.setText(R.string.menu_cancel_upload);
        }

        setTvSubTitle();
        binding.tooltip.setOnClickListener(new OnClickListener() {
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
        binding.btnNext.setOnClickListener(v -> onNextButtonClicked());
        binding.btnPrevious.setOnClickListener(v -> onPreviousButtonClicked());

        initRecyclerView();
        addTextChangeListenerToEtSearch();
    }

    private void addTextChangeListenerToEtSearch() {
        if (binding == null) {
            return;
        }
        subscribe = RxTextView.textChanges(binding.etSearch)
                .doOnEach(v -> binding.tilContainerSearch.setError(null))
                .takeUntil(RxView.detaches(binding.etSearch))
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
                binding.tvSubtitle.setVisibility(View.GONE);
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

        if (binding!=null) {
            binding.rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.rvCategories.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
        subscribe.dispose();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        if (binding != null) {
            binding.pbCategories.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void showError(String error) {
        if (binding != null) {
            binding.tilContainerSearch.setError(error);
        }
    }

    @Override
    public void showError(int stringResourceId) {
        if (binding != null) {
            binding.tilContainerSearch.setError(getString(stringResourceId));
        }
    }

    @Override
    public void setCategories(List<CategoryItem> categories) {
        if (categories == null) {
            adapter.clear();
        } else {
            adapter.setItems(categories);
        }
        adapter.notifyDataSetChanged();


        if (binding == null) {
            return;
        }
        // Nested waiting for search result data to load into the category
        // list and smoothly scroll to the top of the search result list.
        binding.rvCategories.post(new Runnable() {
            @Override
            public void run() {
                binding.rvCategories.smoothScrollToPosition(0);
                binding.rvCategories.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.rvCategories.smoothScrollToPosition(0);
                    }
                });
            }
        });
    }

    @Override
    public void goToNextScreen() {
        if (callback != null){
            callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
        }
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
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
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

    /**
     *
     */
    @Override
    public void navigateToLoginScreen() {
        final String username = sessionManager.getUserName();
        final CommonsApplication.BaseLogoutListener logoutListener = new CommonsApplication.BaseLogoutListener(
            getActivity(),
            requireActivity().getString(R.string.invalid_login_message),
            username
        );

        CommonsApplication.getInstance().clearApplicationData(
            requireActivity(), logoutListener);
    }

    public void onNextButtonClicked() {
        if (media != null) {
            presenter.updateCategories(media, wikiText);
        } else {
            presenter.verifyCategories();
        }
    }

    public void onPreviousButtonClicked() {
        if (media != null) {
            presenter.clearPreviousSelection();
            adapter.setItems(null);
            final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
            assert mediaDetailFragment != null;
            mediaDetailFragment.onResume();
            goBackToPreviousScreen();
        } else {
            if (callback != null) {
                callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
            }
        }
    }

    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        if (binding == null) {
           return;
        }
        presenter.selectCategories();
        final Editable text = binding.etSearch.getText();
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
            binding.etSearch.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    binding.etSearch.clearFocus();
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
                    .getParentFragment().getParentFragment())).binding.cardViewNearby
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
