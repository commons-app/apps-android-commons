package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADED;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADING;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.START_OFFSET;

import android.accounts.Account;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.MergeAdapter;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Objects;
import javax.inject.Inject;
import timber.log.Timber;

public class LeaderboardFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.leaderboard_list)
    RecyclerView leaderboardListRecyclerView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.category_spinner)
    Spinner categorySpinner;

    @BindView(R.id.duration_spinner)
    Spinner durationSpinner;

    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    ViewModelFactory viewModelFactory;

    LeaderboardListViewModel viewModel;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    String duration;
    String category;
    int limit = PAGE_SIZE;
    int offset = START_OFFSET;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        ButterKnife.bind(this, rootView);

        progressBar.setVisibility(View.VISIBLE);
        hideLayouts();
        setSpinners();

        String[] durationValues = getContext().getResources().getStringArray(R.array.leaderboard_duration_values);
        String[] categoryValues = getContext().getResources().getStringArray(R.array.leaderboard_category_values);

        duration = durationValues[0];
        category = categoryValues[0];

        setLeaderboard(duration, category, limit, offset);

        durationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                duration = durationValues[durationSpinner.getSelectedItemPosition()];
                refreshLeaderboard();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        categorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = categoryValues[categorySpinner.getSelectedItemPosition()];
                refreshLeaderboard();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        return rootView;
    }

    private void refreshLeaderboard() {
        if (viewModel != null) {
            viewModel.refresh(duration, category, limit, offset);
            setLeaderboard(duration, category, limit, offset);
        }
    }

    private void setSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.leaderboard_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> durationAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.leaderboard_durations, android.R.layout.simple_spinner_item);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durationAdapter);
    }

    /**
     * To call the API to get results
     * which then sets the views using setLeaderboardUser method
     */
    private void setLeaderboard(String duration, String category, int limit, int offset) {
        if (checkAccount()) {
            try {
                compositeDisposable.add(okHttpJsonApiClient
                    .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                        duration, category, null, null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        response -> {
                            if (response != null && response.getStatus() == 200) {
                                setViews(response, duration, category, limit, offset);
                            }
                        },
                        t -> {
                            Timber.e(t, "Fetching leaderboard statistics failed");
                            onError();
                        }
                    ));
            }
            catch (Exception e){
                Timber.d(e+"success");
            }
        }
    }

    /**
     * Set the views
     * @param response Leaderboard Response Object
     */
    private void setViews(LeaderboardResponse response, String duration, String category, int limit, int offset) {
        viewModel = new ViewModelProvider(this, viewModelFactory).get(LeaderboardListViewModel.class);
        viewModel.setParams(duration, category, limit, offset);
        LeaderboardListAdapter leaderboardListAdapter = new LeaderboardListAdapter();
        UserDetailAdapter userDetailAdapter= new UserDetailAdapter(response);
        MergeAdapter mergeAdapter = new MergeAdapter(userDetailAdapter, leaderboardListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        leaderboardListRecyclerView.setLayoutManager(linearLayoutManager);
        leaderboardListRecyclerView.setAdapter(mergeAdapter);
        viewModel.getListLiveData().observe(getViewLifecycleOwner(), leaderboardListAdapter::submitList);
        viewModel.getProgressLoadStatus().observe(getViewLifecycleOwner(), status -> {
            if (Objects.requireNonNull(status).equalsIgnoreCase(LOADING)) {
                showProgressBar();
            } else if (status.equalsIgnoreCase(LOADED)) {
                hideProgressBar();
            }
        });
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            categorySpinner.setVisibility(View.VISIBLE);
            durationSpinner.setVisibility(View.VISIBLE);
            leaderboardListRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * to show progressbar
     */
    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * used to hide the layouts while fetching results from api
     */
    private void hideLayouts(){
        categorySpinner.setVisibility(View.INVISIBLE);
        durationSpinner.setVisibility(View.INVISIBLE);
        leaderboardListRecyclerView.setVisibility(View.INVISIBLE);
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        Account currentAccount = sessionManager.getCurrentAccount();
        if (currentAccount == null) {
            Timber.d("Current account is null");
            ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(getActivity());
            return false;
        }
        return true;
    }

    /**
     * Shows a generic error toast when error occurs while loading leaderboard
     */
    private void onError() {
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.error_occurred));
        progressBar.setVisibility(View.GONE);
    }

}
