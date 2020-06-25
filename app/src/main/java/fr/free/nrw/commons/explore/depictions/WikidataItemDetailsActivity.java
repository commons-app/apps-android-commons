package fr.free.nrw.commons.explore.depictions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment;
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment;
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment;
import fr.free.nrw.commons.explore.ViewPagerAdapter;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to show depiction media, parent classes and child classes of depicted items in Explore
 */
public class WikidataItemDetailsActivity extends NavigationBaseActivity implements MediaDetailPagerFragment.MediaDetailProvider,
    CategoryImagesCallback {
    private FragmentManager supportFragmentManager;
    private DepictedImagesFragment depictionImagesListFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    /**
     * Name of the depicted item
     * Ex: Rabbit
     */
    private String wikidataItemName;
    @BindView(R.id.mediaContainer)
    FrameLayout mediaContainer;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    ViewPagerAdapter viewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wikidata_item_details);
        ButterKnife.bind(this);
        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
        setPageTitle();
        initDrawer();
        forceInitBackButton();
    }

    /**
     * Gets the passed wikidataItemName from the intents and displays it as the page title
     */
    private void setPageTitle() {
        if (getIntent() != null && getIntent().getStringExtra("wikidataItemName") != null) {
            setTitle(getIntent().getStringExtra("wikidataItemName"));
        }
    }

    /**
     * This method is called on success of API call for featured Images.
     * The viewpager will notified that number of items have changed.
     */
    @Override
    public void viewPagerNotifyDataSetChanged() {
        if (mediaDetailPagerFragment !=null){
            mediaDetailPagerFragment.notifyDataSetChanged();
        }
    }

    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    private void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        depictionImagesListFragment = new DepictedImagesFragment();
        ChildDepictionsFragment childDepictionsFragment = new ChildDepictionsFragment();
        ParentDepictionsFragment parentDepictionsFragment = new ParentDepictionsFragment();
        wikidataItemName = getIntent().getStringExtra("wikidataItemName");
        String entityId = getIntent().getStringExtra("entityId");
        if (getIntent() != null && wikidataItemName != null) {
            Bundle arguments = new Bundle();
            arguments.putString("wikidataItemName", wikidataItemName);
            arguments.putString("entityId", entityId);
            depictionImagesListFragment.setArguments(arguments);
            parentDepictionsFragment.setArguments(arguments);
            childDepictionsFragment.setArguments(arguments);
        }
        fragmentList.add(depictionImagesListFragment);
        titleList.add(getResources().getString(R.string.title_for_media));
        fragmentList.add(childDepictionsFragment);
        titleList.add(getResources().getString(R.string.title_for_child_classes));
        fragmentList.add(parentDepictionsFragment);
        titleList.add(getResources().getString(R.string.title_for_parent_classes));
        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPager.setOffscreenPageLimit(2);
        viewPagerAdapter.notifyDataSetChanged();

    }


    /**
     * Shows media detail fragment when user clicks on any image in the list
     */
    public void onMediaClicked(int position) {
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetailPagerFragment = new MediaDetailPagerFragment(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.mediaContainer, mediaDetailPagerFragment)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetailPagerFragment.showImage(position);
        forceInitBackButton();
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     * @param i It is the index of which media object is to be returned which is same as
     *          current index of viewPager.
     * @return Media Object
     */
    @Override
    public Media getMediaAtPosition(int i) {
        return depictionImagesListFragment.getMediaAtPosition(i);
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    @Override
    public void onBackPressed() {
        if (supportFragmentManager.getBackStackEntryCount() == 1){
            // back to search so show search toolbar and hide navigation toolbar
            tabLayout.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            mediaContainer.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     * @return Total Media count in the adapter
     */
    @Override
    public int getTotalMediaCount() {
        return depictionImagesListFragment.getTotalMediaCount();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context      A Context of the application package implementing this class.
     * @param depictedItem Name of the depicts for displaying its details
     */
    public static void startYourself(Context context, DepictedItem depictedItem) {
        Intent intent = new Intent(context, WikidataItemDetailsActivity.class);
        intent.putExtra("wikidataItemName", depictedItem.getName());
        intent.putExtra("entityId", depictedItem.getId());
        context.startActivity(intent);
    }
}
