package fr.free.nrw.commons.explore;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.utils.ActivityUtils;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends CommonsDaggerSupportFragment {

    private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
    private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";
    private static final String MEDIA_DETAILS_FRAGMENT_TAG = "MediaDetailsFragment";


    @BindView(R.id.mediaContainer)
    FrameLayout mediaContainer;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;
    /*private MediaDetailPagerFragment mediaDetails;
    private CategoriesMediaFragment mobileImagesListFragment;
    private CategoriesMediaFragment featuredImagesListFragment;*/
    private FeaturedRootFragment featuredRootFragment;
    private FeaturedRootFragment mobileRootFragment;

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

        featuredRootFragment = new FeaturedRootFragment();
        featuredRootFragment.setArguments(featuredArguments);
        mobileRootFragment = new FeaturedRootFragment();
        mobileRootFragment.setArguments(mobileArguments);
        fragmentList.add(featuredRootFragment);
        titleList.add(getString(R.string.explore_tab_title_featured).toUpperCase());

        fragmentList.add(mobileRootFragment);
        titleList.add(getString(R.string.explore_tab_title_mobile).toUpperCase());

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     *
     //* @param i It is the index of which media object is to be returned which is same as current
     *          index of viewPager.
     * @return Media Object
     */
    /*@Override
    public Media getMediaAtPosition(int i) {
        if (tabLayout.getSelectedTabPosition() == 1) {
            return mobileImagesListFragment.getMediaAtPosition(i);
        } else if (tabLayout.getSelectedTabPosition() == 0) {
            return featuredImagesListFragment.getMediaAtPosition(i);
        } else {
            return null;
        }
    }*/

    public void onBackPressed() {
        /*if (mediaContainer.getVisibility() == View.VISIBLE) {
            supportFragmentManager
                .beginTransaction()
                .show(mobileImagesListFragment)
                .show(featuredImagesListFragment)
                .remove(mediaDetails)
                .commit();
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
            ((MainActivity)getActivity()).showTabs();
        }*/
    }

    /**
     * This method is called onClick of media inside category featured images or mobile uploads.
     */
    /*@Override
    public void onMediaClicked(int position) {
        mediaContainer.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.GONE);
        mediaDetails = new MediaDetailPagerFragment(false, true);

        if (tabLayout.getSelectedTabPosition() == 0) {
            featuredRootFragment.setFragment(mediaDetails);
        } else if (tabLayout.getSelectedTabPosition() == 1) {
            //mediaDetails = mobileImagesListFragment.onMediaItemClicked(position);
        }*/
        //if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            //mediaDetails = new MediaDetailPagerFragment(false, true);
            //supportFragmentManager
                //.beginTransaction()
                //.hide(featuredImagesListFragment)
                //.hide(mobileImagesListFragment)
                //.add(R.id.mediaContainer, mediaDetails)
                //.replace(R.id.mediaContainer, mediaDetails)
                //.addToBackStack(MEDIA_DETAILS_FRAGMENT_TAG)
                //.commit();
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the explore activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550            supportFragmentManager.executePendingTransactions();
        //}

    //}

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


