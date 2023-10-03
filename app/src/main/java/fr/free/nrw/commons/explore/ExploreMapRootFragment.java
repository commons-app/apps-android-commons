package fr.free.nrw.commons.explore;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.map.ExploreMapFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.NavTab;

public class ExploreMapRootFragment extends CommonsDaggerSupportFragment implements
    MediaDetailPagerFragment.MediaDetailProvider, CategoryImagesCallback {

    private MediaDetailPagerFragment mediaDetails;
    private ExploreMapFragment mapFragment;

    @BindView(R.id.explore_container)
    FrameLayout container;

    public ExploreMapRootFragment() {
        //empty constructor necessary otherwise crashes on recreate
    }

    @NonNull
    public static ExploreMapRootFragment newInstance() {
        ExploreMapRootFragment fragment = new ExploreMapRootFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    public ExploreMapRootFragment(Bundle bundle) {
        String title = bundle.getString("categoryName");
        mapFragment = new ExploreMapFragment();
        Bundle featuredArguments = new Bundle();
        featuredArguments.putString("categoryName", title);
        mapFragment.setArguments(featuredArguments);
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
            setFragment(mapFragment, mediaDetails);
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
        container.setVisibility(View.VISIBLE);
        ((ExploreFragment) getParentFragment()).tabLayout.setVisibility(View.GONE);
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true);
        ((ExploreFragment) getParentFragment()).setScroll(false);
        setFragment(mediaDetails, mapFragment);
        mediaDetails.showImage(position);
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
        if (mapFragment != null && mapFragment.mediaList != null) {
            return mapFragment.mediaList.get(i);
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
        if (mapFragment != null && mapFragment.mediaList != null) {
            return mapFragment.mediaList.size();
        } else {
            return 0;
        }
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
        if (mediaDetails != null && !mapFragment.isVisible()) {
            removeFragment(mediaDetails);
            onMediaClicked(index);
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

    /**
     * Performs back pressed action on the fragment. Return true if the event was handled by the
     * mediaDetails otherwise returns false.
     *
     * @return
     */
    public boolean backPressed() {
        if (null != mediaDetails && mediaDetails.isVisible()) {
            ((ExploreFragment) getParentFragment()).tabLayout.setVisibility(View.VISIBLE);
            removeFragment(mediaDetails);
            ((ExploreFragment) getParentFragment()).setScroll(true);
            setFragment(mapFragment, mediaDetails);
            ((MainActivity) getActivity()).showTabs();
            return true;

        } if (mapFragment != null && mapFragment.isVisible()) {
            if (mapFragment.backButtonClicked()) {
                // Explore map fragment handled the event no further action required.
                return true;
            } else {
                ((MainActivity) getActivity()).showTabs();
                return false;
            }
        } else {
            ((MainActivity) getActivity()).setSelectedItemId(NavTab.CONTRIBUTIONS.code());
        }
        ((MainActivity) getActivity()).showTabs();
        return false;
    }
}
