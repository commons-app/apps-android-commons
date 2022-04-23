package fr.free.nrw.commons.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ActivityUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

public class ExploreFragment extends CommonsDaggerSupportFragment {

    private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
    private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";
    private static final String EXPLORE_MAP = "Map";
    private static final String MEDIA_DETAILS_FRAGMENT_TAG = "MediaDetailsFragment";

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ParentViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;
    private ExploreListRootFragment featuredRootFragment;
    private ExploreListRootFragment mobileRootFragment;
    private ExploreMapRootFragment mapRootFragment;
    @Inject
    @Named("default_preferences")
    public JsonKvStore applicationKvStore;

    public void setScroll(boolean canScroll){
        viewPager.setCanScroll(canScroll);
    }

    @NonNull
    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        ButterKnife.bind(this, view);
        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setId(R.id.viewPager);
        tabLayout.setupWithViewPager(viewPager);
        viewPager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 2) {
                    viewPager.setCanScroll(false);
                } else {
                    viewPager.setCanScroll(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        setTabs();
        setHasOptionsMenu(true);
        return view;
    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        Bundle featuredArguments = new Bundle();
        featuredArguments.putString("categoryName", FEATURED_IMAGES_CATEGORY);

        Bundle mobileArguments = new Bundle();
        mobileArguments.putString("categoryName", MOBILE_UPLOADS_CATEGORY);

        Bundle mapArguments = new Bundle();
        mapArguments.putString("categoryName", EXPLORE_MAP);

        featuredRootFragment = new ExploreListRootFragment(featuredArguments);
        mobileRootFragment = new ExploreListRootFragment(mobileArguments);
        mapRootFragment = new ExploreMapRootFragment(mapArguments);
        fragmentList.add(featuredRootFragment);
        titleList.add(getString(R.string.explore_tab_title_featured).toUpperCase());

        fragmentList.add(mobileRootFragment);
        titleList.add(getString(R.string.explore_tab_title_mobile).toUpperCase());

        fragmentList.add(mapRootFragment);
        titleList.add(getString(R.string.explore_tab_title_map).toUpperCase());

        ((MainActivity)getActivity()).showTabs();
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
    }

    public boolean onBackPressed() {
        if (tabLayout.getSelectedTabPosition() == 0) {
            if (featuredRootFragment.backPressed()) {
                ((BaseActivity) getActivity()).getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
                return true;
            }
        } else if (tabLayout.getSelectedTabPosition() == 1) { //Mobile root fragment
            if (mobileRootFragment.backPressed()) {
                ((BaseActivity) getActivity()).getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
                return true;
            }
        } else { //explore map fragment
            if (mapRootFragment.backPressed()) {
                ((BaseActivity) getActivity()).getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
                return true;
            }
        }
        return false;
    }

    /**
     * This method inflates the menu in the toolbar
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This method handles the logic on ItemSelect in toolbar menu Currently only 1 choice is
     * available to open search page of the app
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_search:
                ActivityUtils.startActivityWithFlags(getActivity(), SearchActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}


