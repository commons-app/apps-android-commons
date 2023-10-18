package fr.free.nrw.commons.explore.depictions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment;
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment;
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.structure.depictions.DepictModel;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import fr.free.nrw.commons.wikidata.WikidataConstants;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * Activity to show depiction media, parent classes and child classes of depicted items in Explore
 */
public class WikidataItemDetailsActivity extends BaseActivity implements MediaDetailPagerFragment.MediaDetailProvider,
    CategoryImagesCallback {
    private FragmentManager supportFragmentManager;
    private DepictedImagesFragment depictionImagesListFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;

    /**
     * Name of the depicted item
     * Ex: Rabbit
     */

    @Inject BookmarkItemsDao bookmarkItemsDao;
    private CompositeDisposable compositeDisposable;
    @Inject
    DepictModel depictModel;
    private String wikidataItemName;
    @BindView(R.id.mediaContainer)
    FrameLayout mediaContainer;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    ViewPagerAdapter viewPagerAdapter;
    private DepictedItem wikidataItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wikidata_item_details);
        ButterKnife.bind(this);
        compositeDisposable = new CompositeDisposable();
        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);

        final DepictedItem depictedItem = getIntent().getParcelableExtra(
            WikidataConstants.BOOKMARKS_ITEMS);
        wikidataItem = depictedItem;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTabs();
        setPageTitle();
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
    @Override
    public void onMediaClicked(int position) {
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        mediaContainer.setVisibility(View.VISIBLE);
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true);
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.mediaContainer, mediaDetailPagerFragment)
                    .addToBackStack(null)
                    .commit();
            supportFragmentManager.executePendingTransactions();
        }
        mediaDetailPagerFragment.showImage(position);
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

    @Override
    public Integer getContributionStateAt(int position) {
        return null;
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    @Override
    public void refreshNominatedMedia(int index) {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            onBackPressed();
            onMediaClicked(index);
        }
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
        intent.putExtra(WikidataConstants.BOOKMARKS_ITEMS, depictedItem);
        context.startActivity(intent);
    }

    /**
     * This function inflates the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu_wikidata_item,menu);

        updateBookmarkState(menu.findItem(R.id.menu_bookmark_current_item));

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This method handles the logic on item select in toolbar menu
     * Currently only 1 choice is available to open Wikidata item details page in browser
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.browser_actions_menu_items:
                String entityId=getIntent().getStringExtra("entityId");
                Uri uri = Uri.parse("https://www.wikidata.org/wiki/" + entityId);
                Utils.handleWebUrl(this, uri);
                return true;
            case R.id.menu_bookmark_current_item:

                if(getIntent().getStringExtra("fragment") != null) {
                    compositeDisposable.add(depictModel.getDepictions(
                        getIntent().getStringExtra("entityId")
                    ).subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(depictedItems -> {
                         final boolean bookmarkExists = bookmarkItemsDao.updateBookmarkItem(
                             depictedItems.get(0));
                         final Snackbar snackbar
                             = bookmarkExists ? Snackbar.make(findViewById(R.id.toolbar_layout),
                             R.string.add_bookmark, Snackbar.LENGTH_LONG)
                             : Snackbar.make(findViewById(R.id.toolbar_layout),
                                 R.string.remove_bookmark,
                                 Snackbar.LENGTH_LONG);

                         snackbar.show();
                         updateBookmarkState(item);
                     }));

                } else {
                    final boolean bookmarkExists
                        = bookmarkItemsDao.updateBookmarkItem(wikidataItem);
                    final Snackbar snackbar
                        = bookmarkExists ? Snackbar.make(findViewById(R.id.toolbar_layout),
                        R.string.add_bookmark, Snackbar.LENGTH_LONG)
                        : Snackbar.make(findViewById(R.id.toolbar_layout), R.string.remove_bookmark,
                            Snackbar.LENGTH_LONG);

                    snackbar.show();
                    updateBookmarkState(item);
                }
                return true;
            case  android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateBookmarkState(final MenuItem item) {
        final boolean isBookmarked;
        if(getIntent().getStringExtra("fragment") != null) {
            isBookmarked
                = bookmarkItemsDao.findBookmarkItem(getIntent().getStringExtra("entityId"));
        } else {
            isBookmarked = bookmarkItemsDao.findBookmarkItem(wikidataItem.getId());
        }
        final int icon
            = isBookmarked ? R.drawable.menu_ic_round_star_filled_24px
            : R.drawable.menu_ic_round_star_border_24px;
        item.setIcon(icon);
    }
}
