package fr.free.nrw.commons.explore;

import static androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE;

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
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.databinding.FragmentExploreBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ActivityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

public class ExploreFragment extends CommonsDaggerSupportFragment {

    private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
    private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";
    private static final String EXPLORE_MAP = "Map";
    private static final String MEDIA_DETAILS_FRAGMENT_TAG = "MediaDetailsFragment";


    public FragmentExploreBinding binding;
    ViewPagerAdapter viewPagerAdapter;
    private ExploreListRootFragment featuredRootFragment;
    private ExploreListRootFragment mobileRootFragment;
    private ExploreMapRootFragment mapRootFragment;
    private MenuItem othersMenuItem;
    @Inject
    @Named("default_preferences")
    public JsonKvStore applicationKvStore;

    // Nearby map state (for if we came from Nearby fragment)
    private double prevZoom;
    private double prevLatitude;
    private double prevLongitude;

    public void setScroll(boolean canScroll) {
        if (binding != null) {
            binding.viewPager.setCanScroll(canScroll);
        }
    }

    @NonNull
    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadNearbyMapData();
        binding = FragmentExploreBinding.inflate(inflater, container, false);

        viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager(),
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

        binding.viewPager.setAdapter(viewPagerAdapter);
        binding.viewPager.setId(R.id.viewPager);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.viewPager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                int positionOffsetPixels) {

            }

            @Override
        public void onPageSelected(int position) {
            // Control scrolling behavior
            if (position == 2) {
                binding.viewPager.setCanScroll(false);
                // Request location permission only for the Map tab
                if (mapRootFragment != null) {
                    mapRootFragment.requestLocationPermission();
                }
            } else {
                binding.viewPager.setCanScroll(true);
            }
            // Update menu item visibility
            if (othersMenuItem != null) {
                othersMenuItem.setVisible(position == 2);
            }
        }

            @Override
            public void onPageScrollStateChanged(int state) {
            if (state == SCROLL_STATE_IDLE && binding.viewPager.getCurrentItem() == 2) {
                if (othersMenuItem != null) {
                    othersMenuItem.setVisible(true);
                }
            }
        }
        });
        setTabs();
        setHasOptionsMenu(true);

        // if we came from 'Show in Explore' in Nearby, jump to Map tab
        if (isCameFromNearbyMap()) {
            binding.viewPager.setCurrentItem(2);
        }
        return binding.getRoot();
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

        // if we came from 'Show in Explore' in Nearby, pass on zoom and center to Explore map root
        if (isCameFromNearbyMap()) {
            mapArguments.putDouble("prev_zoom", prevZoom);
            mapArguments.putDouble("prev_latitude", prevLatitude);
            mapArguments.putDouble("prev_longitude", prevLongitude);
        }

        featuredRootFragment = new ExploreListRootFragment(featuredArguments);
        mobileRootFragment = new ExploreListRootFragment(mobileArguments);
        mapRootFragment = new ExploreMapRootFragment(mapArguments);
        fragmentList.add(featuredRootFragment);
        titleList.add(getString(R.string.explore_tab_title_featured).toUpperCase(Locale.ROOT));

        fragmentList.add(mobileRootFragment);
        titleList.add(getString(R.string.explore_tab_title_mobile).toUpperCase(Locale.ROOT));

        fragmentList.add(mapRootFragment);
        titleList.add(getString(R.string.explore_tab_title_map).toUpperCase(Locale.ROOT));

        ((MainActivity) getActivity()).showTabs();
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
    }

    /**
     * Fetch Nearby map camera data from fragment arguments if any.
     */
    public void loadNearbyMapData() {
        // get fragment arguments
        if (getArguments() != null) {
            prevZoom = getArguments().getDouble("prev_zoom");
            prevLatitude = getArguments().getDouble("prev_latitude");
            prevLongitude = getArguments().getDouble("prev_longitude");
        }
    }

    /**
     * Checks if fragment arguments contain data from Nearby map. if present, then the user
     * navigated from Nearby using 'Show in Explore'.
     *
     * @return true if user navigated from Nearby map
     **/
    public boolean isCameFromNearbyMap() {
        return prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0;
    }

    public boolean onBackPressed() {
        if (binding.tabLayout.getSelectedTabPosition() == 0) {
            if (featuredRootFragment.backPressed()) {
                ((BaseActivity) getActivity()).getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(false);
                return true;
            }
        } else if (binding.tabLayout.getSelectedTabPosition() == 1) { //Mobile root fragment
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
        // If logged in, 'Show in Nearby' menu item is visible
        if (applicationKvStore.getBoolean("login_skipped") == false) {
            inflater.inflate(R.menu.explore_fragment_menu, menu);

            othersMenuItem = menu.findItem(R.id.list_item_show_in_nearby);

            // Set initial visibility based on the current tab
            if (binding.viewPager.getCurrentItem() == 2) {
                othersMenuItem.setVisible(true);
            } else {
                othersMenuItem.setVisible(false);
            }
        } else {
            inflater.inflate(R.menu.menu_search, menu);
        }
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
            case R.id.list_item_show_in_nearby:
                mapRootFragment.loadNearbyMapFromExplore();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}


