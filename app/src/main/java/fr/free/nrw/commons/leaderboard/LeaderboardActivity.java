package fr.free.nrw.commons.leaderboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxSearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.BookmarksActivity;
import fr.free.nrw.commons.bookmarks.BookmarksPagerAdapter;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
import fr.free.nrw.commons.explore.categories.SearchCategoryFragment;
import fr.free.nrw.commons.explore.images.SearchImageFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.FragmentUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class LeaderboardActivity extends NavigationBaseActivity {


    private FragmentManager supportFragmentManager;
    private ViewPagerAdapter adapter;
    private LeaderboardUploadFragment leaderboardUploadFragment;
    private LeaderboardNearbyFragment leaderboardNearbyFragment;
    private LeaderboardUsedFragment leaderboardUsedFragment;
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
