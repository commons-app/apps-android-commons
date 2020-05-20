package fr.free.nrw.commons.upload.depicts;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.pedrogomez.renderers.RVRendererAdapter;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadDepictsAdapterFactory;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.upload.structure.depictions.UploadDepictsCallback;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;


/**
 * Fragment for showing depicted items list in Upload activity after media details
 */
public class DepictsFragment extends UploadBaseFragment implements DepictsContract.View, UploadDepictsCallback {

    @BindView(R.id.depicts_title)
    TextView depictsTitle;
    @BindView(R.id.depicts_search_container)
    TextInputLayout depictsSearchContainer;
    @BindView(R.id.depicts_search)
    TextInputEditText depictsSearch;
    @BindView(R.id.depictsSearchInProgress)
    ProgressBar depictsSearchInProgress;
    @BindView(R.id.depicts_recycler_view)
    RecyclerView depictsRecyclerView;

    @Inject
    DepictsContract.UserActionListener presenter;
    private RVRendererAdapter<DepictedItem> adapter;
    private Disposable subscribe;

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
        init();
        presenter.getDepictedItems().observe(getViewLifecycleOwner(), this::setDepictsList);
    }

    /**
     * Initialize presenter and views
     */
    private void init() {
        depictsTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
                callback.getTotalNumberOfSteps()));
        presenter.onAttachView(this);
        initRecyclerView();
        addTextChangeListenerToSearchBox();
    }

    /**
     * Initialise recyclerView and set adapter
     */
    private void initRecyclerView() {
        adapter = new UploadDepictsAdapterFactory(this)
                .create(new ArrayList<>());
        depictsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        depictsRecyclerView.setAdapter(adapter);
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
        adapter.clear();
        if (depictedItemList != null) {
            adapter.addAll(depictedItemList);
            adapter.notifyDataSetChanged();
        }
    }

    @Nullable
    private Pair<DepictedItem,Integer> returnItemAndPosition(@NotNull DepictedItem depictedItem) {
        for (int i = 0; i < adapter.getItemCount(); i++) {
            final DepictedItem item = adapter.getItem(i);
            if(item.getId().equals(depictedItem.getId())){
                return new Pair<>(item, i);
            }
        }
        return null;
    }

    @OnClick(R.id.depicts_next)
    public void onNextButtonClicked() {
        presenter.verifyDepictions();
    }

    @OnClick(R.id.depicts_previous)
    public void onPreviousButtonClicked() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void depictsClicked(DepictedItem item) {
        presenter.onDepictItemClicked(item);
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
    private void searchForDepictions(String query) {
        presenter.searchForDepictions(query);
    }

}
