package fr.free.nrw.commons.bookmarks;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsFragment;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsFragment;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.category.GridViewAdapter;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.NavTab;
import java.util.ArrayList;
import java.util.Iterator;

public class BookmarkListRootFragment extends CommonsDaggerSupportFragment implements
    FragmentManager.OnBackStackChangedListener,
    MediaDetailPagerFragment.MediaDetailProvider,
    AdapterView.OnItemClickListener, CategoryImagesCallback {

    private MediaDetailPagerFragment mediaDetails;
    //private BookmarkPicturesFragment bookmarkPicturesFragment;
    private BookmarkLocationsFragment bookmarkLocationsFragment;
    public Fragment listFragment;
    private BookmarksPagerAdapter bookmarksPagerAdapter;

    @BindView(R.id.explore_container)
    FrameLayout container;

    public BookmarkListRootFragment() {
        //empty constructor necessary otherwise crashes on recreate
    }

    public BookmarkListRootFragment(Bundle bundle, BookmarksPagerAdapter bookmarksPagerAdapter) {
        String title = bundle.getString("categoryName");
        int order = bundle.getInt("order");
        final int orderItem = bundle.getInt("orderItem");
        if (order == 0) {
            listFragment = new BookmarkPicturesFragment();
        } else {
            listFragment = new BookmarkLocationsFragment();
            if(orderItem == 2) {
                listFragment = new BookmarkItemsFragment();
            }
        }
        Bundle featuredArguments = new Bundle();
        featuredArguments.putString("categoryName", title);
        listFragment.setArguments(featuredArguments);
        this.bookmarksPagerAdapter = bookmarksPagerAdapter;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
        @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_featured_root, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            setFragment(listFragment, mediaDetails);
        }
    }

    public void setFragment(Fragment fragment, Fragment otherFragment) {
        if (fragment.isAdded() && otherFragment != null) {
            getChildFragmentManager()
                .beginTransaction()
                .hide(otherFragment)
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (fragment.isAdded() && otherFragment == null) {
            getChildFragmentManager()
                .beginTransaction()
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit();
            getChildFragmentManager().executePendingTransactions();
        } else if (!fragment.isAdded() && otherFragment != null) {
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
    }

    @Override
    public void onMediaClicked(int position) {
        Log.d("deneme8", "on media clicked");
    /*container.setVisibility(View.VISIBLE);
    ((BookmarkFragment)getParentFragment()).tabLayout.setVisibility(View.GONE);
    mediaDetails = new MediaDetailPagerFragment(false, true, position);
    setFragment(mediaDetails, bookmarkPicturesFragment);*/
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
        if (bookmarksPagerAdapter.getMediaAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return (Media) bookmarksPagerAdapter.getMediaAdapter().getItem(i);
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
        if (bookmarksPagerAdapter.getMediaAdapter() == null) {
            return 0;
        }
        return bookmarksPagerAdapter.getMediaAdapter().getCount();
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
        if (mediaDetails != null && !listFragment.isVisible()) {
            removeFragment(mediaDetails);
            mediaDetails = MediaDetailPagerFragment.newInstance(false, true);
            ((BookmarkFragment) getParentFragment()).setScroll(false);
            setFragment(mediaDetails, listFragment);
            mediaDetails.showImage(index);
        }
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

    public boolean backPressed() {
        //check mediaDetailPage fragment is not null then we check mediaDetail.is Visible or not to avoid NullPointerException
        if (mediaDetails != null) {
            if (mediaDetails.isVisible()) {
                // todo add get list fragment
                ((BookmarkFragment) getParentFragment()).setupTabLayout();
                ArrayList<Integer> removed = mediaDetails.getRemovedItems();
                removeFragment(mediaDetails);
                ((BookmarkFragment) getParentFragment()).setScroll(true);
                setFragment(listFragment, mediaDetails);
                ((MainActivity) getActivity()).showTabs();
                if (listFragment instanceof BookmarkPicturesFragment) {
                    GridViewAdapter adapter = ((GridViewAdapter) ((BookmarkPicturesFragment) listFragment)
                        .getAdapter());
                    Iterator i = removed.iterator();
                    while (i.hasNext()) {
                        adapter.remove(adapter.getItem((int) i.next()));
                    }
                    mediaDetails.clearRemoved();

                }
            } else {
                moveToContributionsFragment();
            }
        } else {
            moveToContributionsFragment();
        }
        // notify mediaDetails did not handled the backPressed further actions required.
        return false;
    }

    void moveToContributionsFragment() {
        ((MainActivity) getActivity()).setSelectedItemId(NavTab.CONTRIBUTIONS.code());
        ((MainActivity) getActivity()).showTabs();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("deneme8", "on media clicked");
        container.setVisibility(View.VISIBLE);
        ((BookmarkFragment) getParentFragment()).tabLayout.setVisibility(View.GONE);
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true);
        ((BookmarkFragment) getParentFragment()).setScroll(false);
        setFragment(mediaDetails, listFragment);
        mediaDetails.showImage(position);
    }

    @Override
    public void onBackStackChanged() {

    }
}
