package fr.free.nrw.commons.media;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import timber.log.Timber;

//FragmentStatePagerAdapter allows user to swipe across collection of images (no. of images undetermined)
class MediaDetailAdapter extends FragmentStatePagerAdapter {

    final MediaDetailPagerFragment mediaDetailPagerFragment;
    /**
     * Keeps track of the current displayed fragment.
     */
    private Fragment mCurrentFragment;

    public MediaDetailAdapter(MediaDetailPagerFragment mediaDetailPagerFragment,
        FragmentManager fm) {
        super(fm);
        this.mediaDetailPagerFragment = mediaDetailPagerFragment;
    }

    @Override
    public Fragment getItem(int i) {
        if (i == 0) {
            // See bug https://code.google.com/p/android/issues/detail?id=27526
            if (mediaDetailPagerFragment.getActivity() == null) {
                Timber.d("Skipping getItem. Returning as activity is destroyed!");
                return null;
            }
            mediaDetailPagerFragment.binding.mediaDetailsPager.postDelayed(
                () -> mediaDetailPagerFragment.getActivity().invalidateOptionsMenu(), 5);
        }
        if (mediaDetailPagerFragment.isFromFeaturedRootFragment) {
            return MediaDetailFragment.forMedia(mediaDetailPagerFragment.position + i,
                mediaDetailPagerFragment.editable, mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed);
        } else {
            return MediaDetailFragment.forMedia(i, mediaDetailPagerFragment.editable,
                mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed);
        }
    }

    @Override
    public int getCount() {
        if (mediaDetailPagerFragment.getActivity() == null) {
            Timber.d("Skipping getCount. Returning as activity is destroyed!");
            return 0;
        }
        return mediaDetailPagerFragment.provider.getTotalMediaCount();
    }

    /**
     * Get the currently displayed fragment.
     *
     * @return
     */
    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }

    /**
     * If current fragment is of type MediaDetailFragment, return it, otherwise return null.
     *
     * @return MediaDetailFragment
     */
    public MediaDetailFragment getCurrentMediaDetailFragment() {
        if (mCurrentFragment instanceof MediaDetailFragment) {
            return (MediaDetailFragment) mCurrentFragment;
        }

        return null;
    }

    /**
     * Called to inform the adapter of which item is currently considered to be the "primary", that
     * is the one show to the user as the current page.
     *
     * @param container
     * @param position
     * @param object
     */
    @Override
    public void setPrimaryItem(@NonNull final ViewGroup container, final int position,
        @NonNull final Object object) {
        // Update the current fragment if changed
        if (getCurrentFragment() != object) {
            mCurrentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }
}
