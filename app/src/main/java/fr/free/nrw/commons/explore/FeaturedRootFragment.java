package fr.free.nrw.commons.explore;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.ContributionsListFragment;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.NavTab;
import fr.free.nrw.commons.settings.SettingsFragment;

public class FeaturedRootFragment extends CommonsDaggerSupportFragment implements
    MediaDetailPagerFragment.MediaDetailProvider, CategoryImagesCallback {
  private static final String FEATURED_IMAGES_CATEGORY = "Featured_pictures_on_Wikimedia_Commons";
  private static final String MOBILE_UPLOADS_CATEGORY = "Uploaded_with_Mobile/Android";
  private static final String MEDIA_DETAILS_FRAGMENT_TAG = "MediaDetailsFragment";

  private MediaDetailPagerFragment mediaDetails;
  private CategoriesMediaFragment listFragment;

  @BindView(R.id.explore_container)
  FrameLayout container;

  @Nullable
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    View view = inflater.inflate(R.layout.fragment_featured_root, container, false);
    ButterKnife.bind(this, view);
    String title = FEATURED_IMAGES_CATEGORY;
    if (savedInstanceState != null) {
      title = savedInstanceState.getString("categoryName");
    }
    listFragment = new CategoriesMediaFragment();
    Bundle featuredArguments = new Bundle();
    featuredArguments.putString("categoryName", title);
    listFragment.setArguments(featuredArguments);

    return view;
  }

  @Override
  public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setFragment(listFragment, mediaDetails);
  }

  public void setFragment(Fragment fragment, Fragment otherFragment) {
    if (fragment.isAdded() && otherFragment != null) {
      getChildFragmentManager()
          .beginTransaction()
          .hide(otherFragment)
          .show( fragment)
          .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
          .commit();
      getChildFragmentManager().executePendingTransactions();
    } else if (fragment.isAdded() && otherFragment == null) {
      getChildFragmentManager()
          .beginTransaction()
          .show( fragment)
          .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
          .commit();
      getChildFragmentManager().executePendingTransactions();
    }else if (!fragment.isAdded() && otherFragment != null ) {
      getChildFragmentManager()
          .beginTransaction()
          .hide(otherFragment)
          .add(R.id.explore_container, fragment)
          .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
          .commit();
      getChildFragmentManager().executePendingTransactions();
    } else if (!fragment.isAdded()) {
      getChildFragmentManager()
          .beginTransaction()
          .replace(R.id.explore_container, fragment)
          .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
          .commit();
      getChildFragmentManager().executePendingTransactions();
    }
  }

  public void removeFragment(Fragment fragment) {
      getChildFragmentManager()
          .beginTransaction()
          .remove(fragment)
          .commit();
      getChildFragmentManager().executePendingTransactions();
  }

  @Override
  public void onAttach(final Context context) {
    super.onAttach(context);
    //setFragment(listFragment);
  }

  @Override
  public void onMediaClicked(int position) {
    Log.d("deneme8","on media clicked");
    container.setVisibility(View.VISIBLE);
    ((ExploreFragment)getParentFragment()).tabLayout.setVisibility(View.GONE);
    //setFragment(new SettingsFragment()); show that problem is not because of fragment replace
    mediaDetails = new MediaDetailPagerFragment(false, true);
    setFragment(mediaDetails, listFragment);
    //mediaDetails.showImage(position);
  }

  /**
   * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
   *
   * @param i It is the index of which media object is to be returned which is same as current
   *          index of viewPager.
   * @return Media Object
   */
  @Override
  public Media getMediaAtPosition(int i) {
    if (listFragment != null) {
      return listFragment.getMediaAtPosition(i);
    } else {
      return null;
    }
  }

  /**
   * This method is called on from getCount of MediaDetailPagerFragment The viewpager will contain
   * same number of media items as that of media elements in adapter.
   *
   * @return Total Media count in the adapter
   */
  @Override
  public int getTotalMediaCount() {
    if (listFragment!=null) {
      return listFragment.getTotalMediaCount();
    } else {
      return 0;
    }
  }

  @Override
  public Integer getContributionStateAt(int position) {
    return null;
  }

  /**
   * This method is called on success of API call for featured images or mobile uploads. The
   * viewpager will notified that number of items have changed.
   */
  @Override
  public void viewPagerNotifyDataSetChanged() {
    if (mediaDetails != null) {
      mediaDetails.notifyDataSetChanged();
    }
  }

  public void backPressed() {
    if (mediaDetails.isVisible()) {
      // todo add get list fragment
      ((ExploreFragment)getParentFragment()).tabLayout.setVisibility(View.VISIBLE);
      removeFragment(mediaDetails);
      setFragment(listFragment, mediaDetails);
    } else {
      ((MainActivity) getActivity()).setSelectedItemId(NavTab.CONTRIBUTIONS.code());
    }
  }
}
