package fr.free.nrw.commons.bookmarks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.databinding.FragmentBookmarksBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.theme.BaseActivity;
import javax.inject.Inject;
import fr.free.nrw.commons.contributions.ContributionController;
import javax.inject.Named;

public class BookmarkFragment extends CommonsDaggerSupportFragment {

    private FragmentManager supportFragmentManager;
    private BookmarksPagerAdapter adapter;
    FragmentBookmarksBinding binding;

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

    public void setScroll(boolean canScroll) {
        if (binding!=null) {
            binding.viewPagerBookmarks.setCanScroll(canScroll);
        }
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
        binding = FragmentBookmarksBinding.inflate(inflater, container, false);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getChildFragmentManager();

        adapter = new BookmarksPagerAdapter(supportFragmentManager, getContext(),
            applicationKvStore.getBoolean("login_skipped"));
        binding.viewPagerBookmarks.setAdapter(adapter);
        binding.tabLayout.setupWithViewPager(binding.viewPagerBookmarks);

        ((MainActivity) getActivity()).showTabs();
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        setupTabLayout();
        return binding.getRoot();
    }

    /**
     * This method sets up the tab layout. If the adapter has only one element it sets the
     * visibility of tabLayout to gone.
     */
    public void setupTabLayout() {
        binding.tabLayout.setVisibility(View.VISIBLE);
        if (adapter.getCount() == 1) {
            binding.tabLayout.setVisibility(View.GONE);
        }
    }


    public void onBackPressed() {
        if (((BookmarkListRootFragment) (adapter.getItem(binding.tabLayout.getSelectedTabPosition())))
            .backPressed()) {
            // The event is handled internally by the adapter , no further action required.
            return;
        }
        // Event is not handled by the adapter ( performed back action ) change action bar.
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
