package fr.free.nrw.commons.upload.depicts;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
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


/**
 * Fragment for showing depicted items list in Upload activity after media details
 */
public class DepictsFragment extends UploadBaseFragment implements DepictsContract.View {

    @BindView(R.id.depicts_title)
    TextView depictsTitle;
    @BindView(R.id.depicts_subtitle)
    TextView depictsSubTitle;
    @BindView(R.id.depicts_search_container)
    TextInputLayout depictsSearchContainer;
    @BindView(R.id.depicts_search)
    PasteSensitiveTextInputEditText depictsSearch;
    @BindView(R.id.depictsSearchInProgress)
    ProgressBar depictsSearchInProgress;
    @BindView(R.id.depicts_recycler_view)
    RecyclerView depictsRecyclerView;
    @BindView(R.id.tooltip)
    ImageView tooltip;
    @BindView(R.id.depicts_next)
    Button btnNext;
    @BindView(R.id.depicts_previous)
    Button btnPrevious;

    @Inject
    DepictsContract.UserActionListener presenter;
    private UploadDepictsAdapter adapter;
    private Disposable subscribe;
    private Media media;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_depicts_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        Bundle bundle = getArguments();
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Depicts");
        }

        init();
        presenter.getDepictedItems().observe(getViewLifecycleOwner(), this::setDepictsList);
    }

    /**
     * Initialize presenter and views
     */
    private void init() {

        if(media == null) {
            depictsTitle
                .setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                    callback.getTotalNumberOfSteps(), getString(R.string.depicts_step_title)));
        } else {
            depictsTitle.setText(R.string.edit_depicts);
            depictsSubTitle.setVisibility(View.GONE);
            btnNext.setText(R.string.menu_save_categories);
            btnPrevious.setText(R.string.menu_cancel_upload);
        }

        setDepictsSubTitle();
        tooltip.setOnClickListener(v -> DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.depicts_step_title),
                getString(R.string.depicts_tooltip), getString(android.R.string.ok), null, true));
        presenter.onAttachView(this);
        initRecyclerView();
        addTextChangeListenerToSearchBox();
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
                depictsSubTitle.setVisibility(View.GONE);
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
            }, new ArrayList<>());
        } else {
            adapter = new UploadDepictsAdapter(item -> {
                presenter.onDepictItemClicked(item);
                return Unit.INSTANCE;
            }, media.getDepictionIds());
        }
        depictsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        depictsRecyclerView.setAdapter(adapter);
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
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.no_depictions_selected),
            getString(R.string.no_depictions_selected_warning_desc),
            getString(R.string.continue_message),
            getString(R.string.cancel),
            this::goToNextScreen,
            null
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
        subscribe.dispose();
    }

    @Override
    public void showProgress(boolean shouldShow) {
        depictsSearchInProgress.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showError(Boolean value) {
        if (value) {
            depictsSearchContainer.setError(getString(R.string.no_depiction_found));
        } else {
            depictsSearchContainer.setErrorEnabled(false);
        }
    }

    @Override
    public void setDepictsList(List<DepictedItem> depictedItemList) {
        adapter.setItems(depictedItemList);
        depictsRecyclerView.smoothScrollToPosition(0);
    }

    @Override
    public Context getFragmentContext(){
        return requireContext();
    }

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
    @OnClick(R.id.depicts_next)
    public void onNextButtonClicked() {
        if(media != null){
            presenter.updateDepicts(media);
        } else {
            presenter.verifyDepictions();
        }
    }

    /**
     * Determines the calling fragment by media nullability and act accordingly
     */
    @OnClick(R.id.depicts_previous)
    public void onPreviousButtonClicked() {
        if(media != null){
            assert getFragmentManager() != null;
            getFragmentManager().popBackStack();
        } else {
            callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
        }
    }

    /**
     * Text change listener for the edit text view of depicts
     */
    private void addTextChangeListenerToSearchBox() {
        subscribe = RxTextView.textChanges(depictsSearch)
                .doOnEach(v -> depictsSearchContainer.setError(null))
                .takeUntil(RxView.detaches(depictsSearch))
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
            Objects.requireNonNull(
                ((AppCompatActivity) Objects.requireNonNull(getActivity())).getSupportActionBar())
                .hide();
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
