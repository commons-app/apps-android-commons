package fr.free.nrw.commons.bookmarks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.ContributionController;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

public class BookmarkFragment extends CommonsDaggerSupportFragment {

  private FragmentManager supportFragmentManager;
  private BookmarksPagerAdapter adapter;
  @BindView(R.id.viewPagerBookmarks)
  ViewPager viewPager;
  @BindView(R.id.tab_layout)
  TabLayout tabLayout;
  @BindView(R.id.fragmentContainer)
  FrameLayout fragmentContainer;

  @Inject
  ContributionController controller;

  @NonNull
  public static BookmarkFragment newInstance() {
    BookmarkFragment fragment = new BookmarkFragment();
    fragment.setRetainInstance(true);
    return fragment;
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

    adapter = new BookmarksPagerAdapter(supportFragmentManager, getContext());
    viewPager.setAdapter(adapter);
    tabLayout.setupWithViewPager(viewPager);
    return view;
  }

  public void onBackPressed() {
    ((BookmarkListRootFragment) (adapter.getItem(tabLayout.getSelectedTabPosition())))
        .backPressed();
    ((BaseActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
  }
}
