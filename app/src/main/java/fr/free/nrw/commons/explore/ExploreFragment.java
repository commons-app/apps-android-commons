package fr.free.nrw.commons.explore;


import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Provides;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesListFragment;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class ExploreFragment
        extends CommonsDaggerSupportFragment
        implements MediaDetailPagerFragment.MediaDetailProvider,
        AdapterView.OnItemClickListener {

    private static final String FEATURED_IMAGES_CATEGORY = "Category:Featured_pictures_on_Wikimedia_Commons";
    private static final String MOBILE_UPLOADS_CATEGORY = "Category:Uploaded_with_Mobile/Android";


    @BindView(R.id.mediaContainer)
    FrameLayout mediaContainer;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    ViewPagerAdapter viewPagerAdapter;
    private FragmentManager supportFragmentManager;
    private MediaDetailPagerFragment mediaDetails;
    private CategoryImagesListFragment mobileImagesListFragment;
    private CategoryImagesListFragment featuredImagesListFragment;

    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.activity_explore, container, false);
        ButterKnife.bind(this, v);
        viewPagerAdapter = new ViewPagerAdapter(requireActivity().getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Todo: after setContentView(R.layout.activity_explore);
        ButterKnife.bind(requireActivity());
        //Todo:after setTitle(getString(R.string.title_activity_explore));
        supportFragmentManager = requireActivity().getSupportFragmentManager();


    }

    /**
     * Sets the titles in the tabLayout and fragments in the viewPager
     */
    public void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();

        featuredImagesListFragment = new CategoryImagesListFragment(this);
        Bundle featuredArguments = new Bundle();
        featuredArguments.putString("categoryName", FEATURED_IMAGES_CATEGORY);
        featuredImagesListFragment.setArguments(featuredArguments);
        fragmentList.add(featuredImagesListFragment);
        titleList.add(getString(R.string.explore_tab_title_featured));

        mobileImagesListFragment = new CategoryImagesListFragment();
        Bundle mobileArguments = new Bundle();
        mobileArguments.putString("categoryName", MOBILE_UPLOADS_CATEGORY);
        mobileImagesListFragment.setArguments(mobileArguments);
        fragmentList.add(mobileImagesListFragment);
        titleList.add(getString(R.string.explore_tab_title_mobile));

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     *
     * @param i It is the index of which media object is to be returned which is same as
     *          current index of viewPager.
     * @return Media Object
     */
    @Override
    public Media getMediaAtPosition(int i) {
        if (mobileImagesListFragment.getAdapter() != null && tabLayout.getSelectedTabPosition() == 1) {
            return (Media) mobileImagesListFragment.getAdapter().getItem(i);
        } else if (featuredImagesListFragment.getAdapter() != null && tabLayout.getSelectedTabPosition() == 0) {
            return (Media) featuredImagesListFragment.getAdapter().getItem(i);
        } else {
            return null;
        }
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     *
     * @return Total Media count in the adapter
     */
    @Override
    public int getTotalMediaCount() {
        if (mobileImagesListFragment.getAdapter() != null && tabLayout.getSelectedTabPosition() == 1) {
            return mobileImagesListFragment.getAdapter().getCount();
        } else if (featuredImagesListFragment.getAdapter() != null && tabLayout.getSelectedTabPosition() == 0) {
            return featuredImagesListFragment.getAdapter().getCount();
        } else {
            return 0;
        }
    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void notifyDatasetChanged() {
    }

    /**
     * This method is called on success of API call for featured images or mobile uploads.
     * The viewpager will notified that number of items have changed.
     */
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetails != null) {
            mediaDetails.notifyDataSetChanged();
        }
    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    /**
     * This method is never called but it was in MediaDetailProvider Interface
     * so it needs to be overrided.
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    /** Todo: after
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     *//*
    @Override
    public void onBackPressed() {
        if (supportFragmentManager.getBackStackEntryCount() == 1) {
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }*/


    /**
     * This method is called when viewPager has reached its end.
     * Fetches more images and adds them to the recycler view and viewpager adapter
     */
    public void requestMoreImages() {
        if (mobileImagesListFragment != null && tabLayout.getSelectedTabPosition() == 1) {
            mobileImagesListFragment.fetchMoreImagesViewPager();
        } else if (featuredImagesListFragment != null && tabLayout.getSelectedTabPosition() == 0) {
            featuredImagesListFragment.fetchMoreImagesViewPager();
        }
    }

    /**
     * This method is called onClick of media inside category featured images or mobile uploads.
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        if (mediaDetails == null || !mediaDetails.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = new MediaDetailPagerFragment(false, true, this);
            FragmentManager supportFragmentManager = requireActivity().getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
                    .add(R.id.mediaContainer, mediaDetails)
                    .addToBackStack(null)
                    .commit();
            // Reason for using hide, add instead of replace is to maintain scroll position after
            // coming back to the explore activity. See https://github.com/commons-app/apps-android-commons/issues/1631
            // https://stackoverflow.com/questions/11353075/how-can-i-maintain-fragment-state-when-added-to-the-back-stack/19022550#19022550            supportFragmentManager.executePendingTransactions();
        }
        mediaDetails.showImage(i);
        //Todo: after forceInitBackButton();
    }

    /*Todo: after
     *//**
     * This method inflates the menu in the toolbar
     *//*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    *//**
     * This method handles the logic on ItemSelect in toolbar menu
     * Currently only 1 choice is available to open search page of the app
     *//*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_search:
                NavigationBaseActivity.startActivityWithFlags(this, SearchActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

}

