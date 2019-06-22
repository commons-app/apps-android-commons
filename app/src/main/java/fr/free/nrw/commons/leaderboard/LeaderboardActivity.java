package fr.free.nrw.commons.leaderboard;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.disposables.CompositeDisposable;

public class LeaderboardActivity extends NavigationBaseActivity {

    private FragmentManager supportFragmentManager;
    private ViewPagerAdapter adapter;
    private LeaderboardFragment leaderboardUploadFragment;
    private LeaderboardFragment leaderboardNearbyFragment;
    private LeaderboardFragment leaderboardUsedFragment;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    @BindView(R.id.viewPagerLeaderboard)
    ViewPager viewPager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
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
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        leaderboardUploadFragment = new LeaderboardFragment();
        Bundle uploadArguments = new Bundle();
        uploadArguments.putString("FragmentName","upload");
        leaderboardUploadFragment.setArguments(uploadArguments);
        fragmentList.add(leaderboardUploadFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_upload_title));

        leaderboardNearbyFragment = new LeaderboardFragment();
        Bundle nearbyArguments = new Bundle();
        nearbyArguments.putString("FragmentName","nearby");
        leaderboardNearbyFragment.setArguments(nearbyArguments);
        fragmentList.add(leaderboardNearbyFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_nearby_title));

        leaderboardUsedFragment = new LeaderboardFragment();
        Bundle usedArguments = new Bundle();
        usedArguments.putString("FragmentName","used");
        leaderboardUsedFragment.setArguments(usedArguments);
        fragmentList.add(leaderboardUsedFragment);
        titleList.add(getResources().getString(R.string.leaderBoard_used_title));

        adapter.setTabData(fragmentList, titleList);
        adapter.notifyDataSetChanged();
    }
}
