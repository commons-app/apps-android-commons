package fr.free.nrw.commons.bookmarks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.tabs.TabLayout;

import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.ParentViewPager;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.theme.BaseActivity;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionController;
import javax.inject.Named;

public class BookmarkFragment extends CommonsDaggerSupportFragment {

  private FragmentManager supportFragmentManager;
  private BookmarksPagerAdapter adapter;
  @BindView(R.id.viewPagerBookmarks)
  ParentViewPager viewPager;
  @BindView(R.id.tab_layout)
  TabLayout tabLayout;
  @BindView(R.id.fragmentContainer)
  FrameLayout fragmentContainer;

  @Inject
  ContributionController controller;
  /**
   * To check if the user is loggedIn or not.
   */
  @Inject
  @Named("default_preferences")
  public
  JsonKvStore applicationKvStore;

  @NonNull
  public static BookmarkFragment newInstance() {
    BookmarkFragment fragment = new BookmarkFragment();
    fragment.setRetainInstance(true);
    return fragment;
  }

  public void setScroll(boolean canScroll){
    viewPager.setCanScroll(canScroll);
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull final LayoutInflater inflater,
      @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View view = inflater.inflate(R.layout.fragment_bookmarks, container, false);
    ButterKnife.bind(this, view);

    // Activity can call methods in the fragment by acquiring a
    // reference to the Fragment from FragmentManager, using findFragmentById()
    supportFragmentManager = getChildFragmentManager();

    adapter = new BookmarksPagerAdapter(supportFragmentManager, getContext(),
                                        applicationKvStore.getBoolean("login_skipped"));
    viewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(viewPager);

    ((MainActivity)getActivity()).showTabs();
    ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

    setupTabLayout();
    return view;
  }

  /**
   * This method sets up the tab layout.
   * If the adapter has only one element it sets the visibility of tabLayout to gone.
   */
  public void setupTabLayout(){
    tabLayout.setVisibility(View.VISIBLE);
    if (adapter.getCount() == 1) {
      tabLayout.setVisibility(View.GONE);
    }
  }


  public void onBackPressed() {
    ((BookmarkListRootFragment) (adapter.getItem(tabLayout.getSelectedTabPosition())))
        .backPressed();
    ((BaseActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
  }
}
