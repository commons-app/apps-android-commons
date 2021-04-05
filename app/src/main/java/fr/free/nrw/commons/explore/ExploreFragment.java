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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ActivityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExploreFragment extends CommonsDaggerSupportFragment {

    private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
    private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";
    private static final String MEDIA_DETAILS_FRAGMENT_TAG = "MediaDetailsFragment";

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)

    ParentViewPager viewPager;
    ExploreViewPagerAdapter viewPagerAdapter;

    public void setScroll(boolean canScroll){
        viewPager.setCanScroll(canScroll);
    }

    @NonNull
    public static ExploreFragment newInstance() {
        final ExploreFragment fragment = new ExploreFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("viewPagerAdapter", viewPagerAdapter.saveState());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        ButterKnife.bind(this, view);
        viewPagerAdapter = new ExploreViewPagerAdapter(getChildFragmentManager());
        if (savedInstanceState != null) {
            viewPagerAdapter.restoreState(savedInstanceState.getParcelable("viewPagerAdapter"), ClassLoader.getSystemClassLoader());
        }
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setId(R.id.viewPager);
        tabLayout.setupWithViewPager(viewPager);
        setHasOptionsMenu(true);
        return view;
    }

    /**
     * Adapter that sets the titles in the tabLayout and fragments in the viewPager
     */
    private class ExploreViewPagerAdapter extends FragmentPagerAdapter {

        public static final int FEATURED_IMAGES_POSITION = 0;
        public static final int MOBILE_UPLOADS_POSITION = 1;

        private final List<Bundle> mBundleList = new ArrayList<>();
        private final List<String> mTitlesList = new ArrayList<>();

        private final HashMap<Integer, Fragment> mFragments = new HashMap<>();

        public ExploreViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);

            final Bundle featuredArguments = new Bundle();
            featuredArguments.putString("categoryName", FEATURED_IMAGES_CATEGORY);
            mBundleList.add(featuredArguments);

            final Bundle mobileArguments = new Bundle();
            mobileArguments.putString("categoryName", MOBILE_UPLOADS_CATEGORY);
            mBundleList.add(mobileArguments);

            mTitlesList.add(getString(R.string.explore_tab_title_featured).toUpperCase());
            mTitlesList.add(getString(R.string.explore_tab_title_mobile).toUpperCase());
        }

        @NonNull
        @Override
        public Fragment getItem(final int position) {
            final ExploreListRootFragment fragment = new ExploreListRootFragment();
            if (position < mBundleList.size()) {
                fragment.setArguments(mBundleList.get(position));
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return mBundleList.size();
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(final int position) {
            return mTitlesList.get(position);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            final Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, fragment);
            return fragment;
        }

        /***
         * Returns the fragment at the specified position. If there are no fragments or if the
         * position is invalid, it returns null.
         *
         * @param position the position in the adapter.
         * @return the {@link Fragment} at the specified position in the adapter.
         */
        public Fragment getFragment(int position) {
            return mFragments.size() == 0 ? null : mFragments.get(position);
        }
    }

    public void onBackPressed() {
        final Fragment fragment;
        if (tabLayout.getSelectedTabPosition() == 0) {
            fragment =
                viewPagerAdapter.getFragment(ExploreViewPagerAdapter.FEATURED_IMAGES_POSITION);
        } else {
            fragment =
                viewPagerAdapter.getFragment(ExploreViewPagerAdapter.MOBILE_UPLOADS_POSITION);
        }

        if (fragment instanceof ExploreListRootFragment) {
            final ExploreListRootFragment exploreListRootFragment = (ExploreListRootFragment) fragment;
            exploreListRootFragment.backPressed();
            ((BaseActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
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


