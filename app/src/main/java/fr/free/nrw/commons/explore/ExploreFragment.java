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
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends CommonsDaggerSupportFragment
    implements MediaDetailPagerFragment.MediaDetailProvider, CategoryImagesCallback {

  private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
  private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";


  @BindView(R.id.mediaContainer)
  FrameLayout mediaContainer;
  @BindView(R.id.tab_layout)
  TabLayout tabLayout;
  @BindView(R.id.viewPager)
  ViewPager viewPager;
  ViewPagerAdapter viewPagerAdapter;
  private FragmentManager supportFragmentManager;
  private MediaDetailPagerFragment mediaDetails;
  private CategoriesMediaFragment mobileImagesListFragment;
  private CategoriesMediaFragment featuredImagesListFragment;

  /**
   * Consumers should be simply using this method to use this activity.
   *
   * @param context A Context of the application package implementing this class.
   */
  public static void startYourself(Context context) {
    Intent intent = new Intent(context, ExploreActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    context.startActivity(intent);
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
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    View view = inflater.inflate(R.layout.activity_explore, container, false);
    ButterKnife.bind(this, view);
    supportFragmentManager = getChildFragmentManager();
    viewPagerAdapter = new ViewPagerAdapter(getChildFragmentManager());
    viewPager.setAdapter(viewPagerAdapter);
    tabLayout.setupWithViewPager(viewPager);
    setTabs();
    return view;
  }

  /**
   * Sets the titles in the tabLayout and fragments in the viewPager
   */
  public void setTabs() {
    List<Fragment> fragmentList = new ArrayList<>();
    List<String> titleList = new ArrayList<>();

    featuredImagesListFragment = new CategoriesMediaFragment();
    Bundle featuredArguments = new Bundle();
    featuredArguments.putString("categoryName", FEATURED_IMAGES_CATEGORY);
    featuredImagesListFragment.setArguments(featuredArguments);
    fragmentList.add(featuredImagesListFragment);
    titleList.add(getString(R.string.explore_tab_title_featured).toUpperCase());

    mobileImagesListFragment = new CategoriesMediaFragment();
    Bundle mobileArguments = new Bundle();
    mobileArguments.putString("categoryName", MOBILE_UPLOADS_CATEGORY);
    mobileImagesListFragment.setArguments(mobileArguments);
    fragmentList.add(mobileImagesListFragment);
    titleList.add(getString(R.string.explore_tab_title_mobile).toUpperCase());

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
    if (tabLayout.getSelectedTabPosition() == 1) {
      return mobileImagesListFragment.getMediaAtPosition(i);
    } else if (tabLayout.getSelectedTabPosition() == 0) {
      return featuredImagesListFragment.getMediaAtPosition(i);
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
    if (tabLayout.getSelectedTabPosition() == 1) {
      return mobileImagesListFragment.getTotalMediaCount();
    } else if (tabLayout.getSelectedTabPosition() == 0) {
      return featuredImagesListFragment.getTotalMediaCount();
    } else {
      return 0;
    }
  }

  @Override
  public Integer getContributionStateAt(int position) {
    return null;
  }

  /**
   * This method is called on success of API call for featured images or mobile uploads.
   * The viewpager will notified that number of items have changed.
   */
  @Override
  public void viewPagerNotifyDataSetChanged() {
    if (mediaDetails != null) {
      mediaDetails.notifyDataSetChanged();
    }
  }


  /**
   * This method is called on backPressed of anyFragment in the activity.
   * If condition is called when mediaDetailFragment is opened.
   */
  /*@Override
  public void onBackPressed() {
    if (supportFragmentManager.getBackStackEntryCount() == 1) {
      tabLayout.setVisibility(View.VISIBLE);
      viewPager.setVisibility(View.VISIBLE);
      mediaContainer.setVisibility(View.GONE);
    }
    initDrawer();
    super.onBackPressed();
  }*/

  /**
   * This method is called onClick of media inside category featured images or mobile uploads.
   */
  @Override
  public void onMediaClicked( int position) {
    tabLayout.setVisibility(View.GONE);
    viewPager.setVisibility(View.GONE);
    mediaContainer.setVisibility(View.VISIBLE);
    if (mediaDetails == null || !mediaDetails.isVisible()) {
      // set isFeaturedImage true for featured images, to include author field on media detail
      mediaDetails = new MediaDetailPagerFragment(false, true);
      FragmentManager supportFragmentManager = getChildFragmentManager();
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
    mediaDetails.showImage(position);
    //forceInitBackButton();
  }

  /**
   * This method inflates the menu in the toolbar
   */
  /*@Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_search, menu);
    return super.onCreateOptionsMenu(menu);
  }

  /**
   * This method handles the logic on ItemSelect in toolbar menu
   * Currently only 1 choice is available to open search page of the app
   */
  /*@Override
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


