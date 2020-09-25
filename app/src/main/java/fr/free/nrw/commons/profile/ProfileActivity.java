package fr.free.nrw.commons.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.profile.achievements.AchievementsFragment;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * This activity will set two tabs, achievements and
 * each tab will have their own fragments
 */
public class ProfileActivity extends NavigationBaseActivity {

    private FragmentManager supportFragmentManager;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    private ViewPagerAdapter viewPagerAdapter;
    private AchievementsFragment achievementsFragment;
    private LeaderboardFragment leaderboardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        initDrawer();
        setTitle(R.string.Profile);

        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     * @param context
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    /**
     * Set the tabs for the fragments
     */
    private void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        achievementsFragment = new AchievementsFragment();
        fragmentList.add(achievementsFragment);
        titleList.add(getResources().getString(R.string.achievements_tab_title).toUpperCase());
        leaderboardFragment = new LeaderboardFragment();
        fragmentList.add(leaderboardFragment);
        titleList.add(getResources().getString(R.string.leaderboard_tab_title).toUpperCase());
        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

}