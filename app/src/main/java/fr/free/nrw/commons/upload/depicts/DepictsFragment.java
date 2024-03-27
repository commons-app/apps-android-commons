package fr.free.nrw.commons.upload.depicts;

import static fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.databinding.UploadDepictsFragmentBinding;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import timber.log.Timber;


/**
 * Fragment for showing depicted items list in Upload activity after media details
 */
public class DepictsFragment extends UploadBaseFragment implements DepictsContract.View {

    @Inject
    @Named("default_preferences")
    public
    JsonKvStore applicationKvStore;

    @Inject
    DepictsContract.UserActionListener presenter;
    private UploadDepictsAdapter adapter;
    private Disposable subscribe;
    private Media media;
    private ProgressDialog progressDialog;
    /**
     * Determines each encounter of edit depicts
     */
    private int count;
    private Place nearbyPlace;

    private UploadDepictsFragmentBinding binding;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        binding = UploadDepictsFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Depicts");
            nearbyPlace = bundle.getParcelable(SELECTED_NEARBY_PLACE);
        }

        if(callback!=null || media!=null){
            init();
            presenter.getDepictedItems().observe(getViewLifecycleOwner(), this::setDepictsList);
        }
    }

    /**
     * Initialize presenter and views
     */
    private void init() {

        if (binding == null) {
            return;
        }

        if (media == null) {
            binding.depictsTitle.setText(String.format(getString(R.string.step_count), callback.getIndexInViewFlipper(this) + 1,
                    callback.getTotalNumberOfSteps(), getString(R.string.depicts_step_title)));
        } else {
            binding.depictsTitle.setText(R.string.edit_depictions);
            binding.depictsSubtitle.setVisibility(View.GONE);
            binding.depictsNext.setText(R.string.menu_save_categories);
            binding.depictsPrevious.setText(R.string.menu_cancel_upload);
        }

        setDepictsSubTitle();
        binding.tooltip.setOnClickListener(v -> DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.depicts_step_title),
                getString(R.string.depicts_tooltip), getString(android.R.string.ok), null, true));
        if (media == null) {
            presenter.onAttachView(this);
        } else {
            presenter.onAttachViewWithMedia(this, media);
        }
        initRecyclerView();
        addTextChangeListenerToSearchBox();

        binding.depictsNext.setOnClickListener(v->onNextButtonClicked());
        binding.depictsPrevious.setOnClickListener(v->onPreviousButtonClicked());
    }

    /**
     * Removes the depicts subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    private void setDepictsSubTitle() {
        final Activity activity = getActivity();
        if (activity instanceof UploadActivity) {
            final boolean isMultipleFileSelected = ((UploadActivity) activity).getIsMultipleFilesSelected();
            if (!isMultipleFileSelected) {
                binding.depictsSubtitle.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Initialise recyclerView and set adapter
     */
    private void initRecyclerView() {
        if (media == null) {
            adapter = new UploadDepictsAdapter(categoryItem -> {
                presenter.onDepictItemClicked(categoryItem);
                return Unit.INSTANCE;
            }, nearbyPlace);
        } else {
            adapter = new UploadDepictsAdapter(item -> {
                presenter.onDepictItemClicked(item);
                return Unit.INSTANCE;
            }, nearbyPlace);
        }
        if (binding == null) {
            return;
        }
        binding.depictsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.depictsRecyclerView.setAdapter(adapter);
    }

    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        // Select Place depiction as the fragment becomes visible to ensure that the most up to date
        // Place is used (i.e. if the user accepts a nearby place dialog)
        presenter.selectPlaceDepictions();
    }

    @Override
    public void goToNextScreen() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void goToPreviousScreen() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void noDepictionSelected() {
        if (media == null) {
            DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.no_depictions_selected),
                getString(R.string.no_depictions_selected_warning_desc),
                getString(R.string.continue_message),
                getString(R.string.cancel),
                this::goToNextScreen,
                null
            );
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_depictions_selected),
                Toast.LENGTH_SHORT).show();
            presenter.clearPreviousSelection();
            updateDepicts();
            goBackToPreviousScreen();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        media = null;
        presenter.onDetachView();
        subscribe.dispose();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        if (binding == null) {
            return;
        }
        binding.depictsSearchInProgress.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showError(Boolean value) {
        if (binding == null) {
            return;
        }
        if (value) {
            binding.depictsSearchContainer.setError(getString(R.string.no_depiction_found));
        } else {
            binding.depictsSearchContainer.setErrorEnabled(false);
        }
    }

    @Override
    public void setDepictsList(List<DepictedItem> depictedItemList) {

        if (applicationKvStore.getBoolean("first_edit_depict")) {
            count = 1;
            applicationKvStore.putBoolean("first_edit_depict", false);
            adapter.setItems(depictedItemList);
        } else {
            if ((count == 0) && (!depictedItemList.isEmpty())) {
                adapter.setItems(null);
                count = 1;
            } else {
                adapter.setItems(depictedItemList);
            }
        }

        if (binding == null) {
            return;
        }
        // Nested waiting for search result data to load into the depicted item
        // list and smoothly scroll to the top of the search result list.
        binding.depictsRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                binding.depictsRecyclerView.smoothScrollToPosition(0);
                binding.depictsRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.depictsRecyclerView.smoothScrollToPosition(0);
                    }
                });
            }
        });
    }

    /**
     * Returns required context
     */
    @Override
    public Context getFragmentContext(){
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
     * Gets existing depictions IDs from media
     */
    @Override
    public List<String> getExistingDepictions(){
        return (media == null) ? null : media.getDepictionIds();
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
     * Update the depicts
     */
    @Override
    public void updateDepicts() {
        final MediaDetailFragment mediaDetailFragment = (MediaDetailFragment) getParentFragment();
        assert mediaDetailFragment != null;
        mediaDetailFragment.onResume();
    }

    /**
     * Determines the calling fragment by media nullability and act accordingly
     */
    public void onNextButtonClicked() {
        if(media != null){
            presenter.updateDepictions(media);
        } else {
            presenter.verifyDepictions();
        }
    }

    /**
     * Determines the calling fragment by media nullability and act accordingly
     */
    public void onPreviousButtonClicked() {
        if(media != null){
            presenter.clearPreviousSelection();
            updateDepicts();
            goBackToPreviousScreen();
        } else {
            callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
        }
    }

    /**
     * Text change listener for the edit text view of depicts
     */
    private void addTextChangeListenerToSearchBox() {
        subscribe = RxTextView.textChanges(binding.depictsSearch)
                .doOnEach(v -> binding.depictsSearchContainer.setError(null))
                .takeUntil(RxView.detaches(binding.depictsSearch))
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(filter -> searchForDepictions(filter.toString()), Timber::e);
    }

    /**
     * Search for depictions for the following query
     *
     * @param query query string
     */
    private void searchForDepictions(final String query) {
        presenter.searchForDepictions(query);
    }



    /**
     * Hides the action bar while opening editing fragment
     */
    @Override
    public void onResume() {
        super.onResume();

        if (media != null) {
            binding.depictsSearch.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    binding.depictsSearch.clearFocus();
                    presenter.clearPreviousSelection();
                    updateDepicts();
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
                    updateDepicts();
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
