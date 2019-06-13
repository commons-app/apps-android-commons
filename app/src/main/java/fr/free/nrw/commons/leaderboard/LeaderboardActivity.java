package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LeaderboardActivity extends NavigationBaseActivity {


    private FragmentManager supportFragmentManager;
    private ViewPagerAdapter adapter;
    private LeaderboardUploadFragment leaderboardUploadFragment;
    private LeaderboardNearbyFragment leaderboardNearbyFragment;
    private LeaderboardUsedFragment leaderboardUsedFragment;
    @BindView(R.id.user_rank)
    TextView user_rank;
    @BindView(R.id.viewPagerLeaderboard)
    ViewPager viewPager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @Inject
    SessionManager sessionManager;
    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    /**
     * This method helps in the creation Leaderboard screen
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        ButterKnife.bind(this);

        initDrawer();

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();

        setUserRank();
    }

    @SuppressLint("CheckResult")
    private void setUserRank() {
        String userName = sessionManager.getUserName();
        if (StringUtils.isBlank(userName)) {
            return;
        }
        compositeDisposable.add(okHttpJsonApiClient.getUserRank(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(edits -> user_rank.setText(String.valueOf(edits)), e -> {
                    Timber.e("Error:" + e);
                }));
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        leaderboardUploadFragment = new LeaderboardUploadFragment();
        leaderboardNearbyFragment = new LeaderboardNearbyFragment();
        leaderboardUsedFragment = new LeaderboardUsedFragment();
        fragmentList.add(leaderboardUploadFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_upload_title));
        fragmentList.add(leaderboardNearbyFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_nearby_title));
        fragmentList.add(leaderboardUsedFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_used_title));

        adapter.setTabData(fragmentList, titleList);
        adapter.notifyDataSetChanged();
    }

}
