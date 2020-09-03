package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

public class BookmarkFragment extends CommonsDaggerSupportFragment
    implements FragmentManager.OnBackStackChangedListener,
    MediaDetailPagerFragment.MediaDetailProvider,
    AdapterView.OnItemClickListener, CategoryImagesCallback {

  private FragmentManager supportFragmentManager;
  private BookmarksPagerAdapter adapter;
  private MediaDetailPagerFragment mediaDetails;
  @BindView(R.id.viewPagerBookmarks)
  ViewPager viewPager;
  @BindView(R.id.tab_layout)
  TabLayout tabLayout;

  @Inject
  ContributionController controller;

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

  @Nullable
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.fragment_bookmarks, container, false);
    ButterKnife.bind(this, view);

    // Activity can call methods in the fragment by acquiring a
    // reference to the Fragment from FragmentManager, using findFragmentById()
    supportFragmentManager = getChildFragmentManager();
    supportFragmentManager.addOnBackStackChangedListener(this);

    adapter = new BookmarksPagerAdapter(supportFragmentManager, getContext());
    viewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(viewPager);
    return view;
  }

  /**
   * Consumers should be simply using this method to use this activity.
   * @param context A Context of the application package implementing this class.
   */
  public static void startYourself(Context context) {
    Intent intent = new Intent(context, BookmarksActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    context.startActivity(intent);
  }

  @Override
  public void onBackStackChanged() {
    if (supportFragmentManager.getBackStackEntryCount() == 0) {
      // The activity has the focus
      adapter.requestPictureListUpdate();
    }
  }

  /*@Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    controller.handleActivityResult(this, requestCode, resultCode, data);
  }*/

  /**
   * This method is called onClick of media inside category details (CategoryImageListFragment).
   */
  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
    if (mediaDetails == null || !mediaDetails.isVisible()) {
      mediaDetails = new MediaDetailPagerFragment(false, true);
      supportFragmentManager
          .beginTransaction()
          .hide(supportFragmentManager.getFragments().get(supportFragmentManager.getBackStackEntryCount()))
          .add(R.id.fragmentContainer, mediaDetails)
          .addToBackStack(null)
          .commit();
      supportFragmentManager.executePendingTransactions();
    }
    mediaDetails.showImage(i);
  }

  /**
   * This method is called on success of API call for featured Images.
   * The viewpager will notified that number of items have changed.
   */
  @Override
  public void viewPagerNotifyDataSetChanged() {
    if (mediaDetails!=null){
      mediaDetails.notifyDataSetChanged();
    }
  }


  /**
   * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
   * @param i It is the index of which media object is to be returned which is same as
   *          current index of viewPager.
   * @return Media Object
   */
  @Override
  public Media getMediaAtPosition(int i) {
    if (adapter.getMediaAdapter() == null) {
      // not yet ready to return data
      return null;
    } else {
      return (Media) adapter.getMediaAdapter().getItem(i);
    }
  }

  /**
   * This method is called on from getCount of MediaDetailPagerFragment
   * The viewpager will contain same number of media items as that of media elements in adapter.
   * @return Total Media count in the adapter
   */
  @Override
  public int getTotalMediaCount() {
    if (adapter.getMediaAdapter() == null) {
      return 0;
    }
    return adapter.getMediaAdapter().getCount();
  }

  @Override
  public void onMediaClicked(int position) {
    //TODO use with pagination
  }

  @Override
  public Integer getContributionStateAt(int position) {
    return null;
  }
}
