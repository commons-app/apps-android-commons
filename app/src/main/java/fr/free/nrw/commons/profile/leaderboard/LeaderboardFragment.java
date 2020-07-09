package fr.free.nrw.commons.profile.leaderboard;

import android.accounts.Account;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import timber.log.Timber;

public class LeaderboardFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.avatar)
    SimpleDraweeView avatar;

    @BindView(R.id.username)
    TextView username;

    @BindView(R.id.rank)
    TextView rank;

    @BindView(R.id.count)
    TextView count;

    @BindView(R.id.leaderboard_list)
    RecyclerView leaderboardListRecyclerView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    private String avatarSourceURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/%s/1024px-%s.png";

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        ButterKnife.bind(this, rootView);
        progressBar.setVisibility(View.VISIBLE);
        hideLayouts();
        setLeaderboard();
        return rootView;
    }

    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */
    private void setLeaderboard() {
        if (checkAccount()) {
            try{
                compositeDisposable.add(okHttpJsonApiClient
                    .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                        "all_time", "upload", null, null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        response -> {
                            if (response != null && response.getStatus() == 200) {
                                setLeaderboardUser(response);
                                setLeaderboardList(response.getLeaderboardList());
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

    private void setLeaderboardUser(LeaderboardResponse response) {
        hideProgressBar();
        avatar.setImageURI(
            Uri.parse(String.format(avatarSourceURL, response.getAvatar(), response.getAvatar())));
        username.setText(response.getUsername());
        rank.setText(String.format("%s %d", getString(R.string.rank_prefix), response.getRank()));
        count.setText(String.format("%s %d", getString(R.string.count_prefix), response.getCategoryCount()));
    }

    private void setLeaderboardList(List<LeaderboardList> leaderboardList) {
        LeaderboardListAdapter leaderboardListAdapter = new LeaderboardListAdapter(leaderboardList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        leaderboardListRecyclerView.setLayoutManager(linearLayoutManager);
        leaderboardListRecyclerView.setAdapter(leaderboardListAdapter);
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            avatar.setVisibility(View.VISIBLE);
            username.setVisibility(View.VISIBLE);
            rank.setVisibility(View.VISIBLE);
            leaderboardListRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * used to hide the layouts while fetching results from api
     */
    private void hideLayouts(){
        avatar.setVisibility(View.INVISIBLE);
        username.setVisibility(View.INVISIBLE);
        rank.setVisibility(View.INVISIBLE);
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
