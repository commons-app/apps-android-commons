package fr.free.nrw.commons.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.profile.achievements.AchievementsFragment;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * This activity will set two tabs, achievements and
 * each tab will have their own fragments
 */
public class ProfileActivity extends BaseActivity {

    private FragmentManager supportFragmentManager;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    SessionManager sessionManager;

    private ViewPagerAdapter viewPagerAdapter;
    private AchievementsFragment achievementsFragment;
    private LeaderboardFragment leaderboardFragment;

    public static final String KEY_USERNAME ="username";
    public static final String KEY_SHOULD_SHOW_CONTRIBUTIONS ="shouldShowContributions";

    String userName;
    private boolean  shouldShowContributions;

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            userName = savedInstanceState.getString(KEY_USERNAME);
            shouldShowContributions = savedInstanceState.getBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userName = getIntent().getStringExtra(KEY_USERNAME);
        setTitle(userName);
        shouldShowContributions = getIntent().getBooleanExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, false);

        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
    }

    /**
     * Navigate up event
     * @return boolean
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     * @param context
     */
    public static void startYourself(Context context, String userName, boolean shouldShowContributions) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KEY_USERNAME, userName);
        intent.putExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions);
        context.startActivity(intent);
    }

    /**
     * Set the tabs for the fragments
     */
    private void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        achievementsFragment = new AchievementsFragment();
        Bundle achievementsBundle = new Bundle();
        achievementsBundle.putString(KEY_USERNAME, userName);
        achievementsFragment.setArguments(achievementsBundle);
        fragmentList.add(achievementsFragment);

        titleList.add(getResources().getString(R.string.achievements_tab_title).toUpperCase());
        leaderboardFragment = new LeaderboardFragment();
        Bundle leaderBoardBundle = new Bundle();
        leaderBoardBundle.putString(KEY_USERNAME, userName);
        leaderboardFragment.setArguments(leaderBoardBundle);

        fragmentList.add(leaderboardFragment);
        titleList.add(getResources().getString(R.string.leaderboard_tab_title).toUpperCase());

        if (shouldShowContributions) {
            ContributionsListFragment contributionsListFragment = new ContributionsListFragment();
            Bundle contributionsListBundle = new Bundle();
            contributionsListBundle.putString(KEY_USERNAME, userName);
            contributionsListFragment.setArguments(contributionsListBundle);
            fragmentList.add(contributionsListFragment);
            titleList.add(getString(R.string.contributions_fragment).toUpperCase());
        }

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(KEY_USERNAME, userName);
        super.onSaveInstanceState(outState);
    }
}