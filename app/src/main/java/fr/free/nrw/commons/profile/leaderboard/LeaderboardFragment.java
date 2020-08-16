package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADED;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADING;

import android.accounts.Account;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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

    @BindView(R.id.scroll)
    Button scrollButton;

    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    ViewModelFactory viewModelFactory;

    LeaderboardListViewModel viewModel;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        ButterKnife.bind(this, rootView);
        progressBar.setVisibility(View.VISIBLE);
        hideLayouts();
        setLeaderboard("all_time", "upload");
        scrollButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.refresh("weekly", "upload");
                setLeaderboard("weekly", "upload");
            }
        });
        return rootView;
    }

    /**
     * To call the API to get results
     * which then sets the views using setLeaderboardUser method
     */
    private void setLeaderboard(String duration, String category) {
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
                                setViews(response, duration, category);
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
    private void setViews(LeaderboardResponse response, String duration, String category) {
        viewModelFactory.setDuration(duration);
        viewModelFactory.setCategory(category);
        viewModelFactory.setLimit(10);
        viewModelFactory.setOffset(0);

        viewModel = new ViewModelProvider(this, viewModelFactory).get(LeaderboardListViewModel.class);
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
            } });
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
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
