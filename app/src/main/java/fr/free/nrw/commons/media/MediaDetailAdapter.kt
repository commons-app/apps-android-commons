package fr.free.nrw.commons.media

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.forMedia
import timber.log.Timber

// FragmentStatePagerAdapter allows user to swipe across collection of images (no. of images undetermined)
internal class MediaDetailAdapter(
    val mediaDetailPagerFragment: MediaDetailPagerFragment,
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {
    /**
     * Keeps track of the current displayed fragment.
     */
    private var currentFragment: Fragment? = null

    override fun getItem(i: Int): Fragment {
        if (i == 0) {
            // See bug https://code.google.com/p/android/issues/detail?id=27526
            if (mediaDetailPagerFragment.activity == null) {
                Timber.d("Skipping getItem. Returning as activity is destroyed!")
                return Fragment()
            }
            mediaDetailPagerFragment.binding.mediaDetailsPager.postDelayed(
                { mediaDetailPagerFragment.requireActivity().invalidateOptionsMenu() }, 5
            )
        }
        return if (mediaDetailPagerFragment.isFromFeaturedRootFragment) {
            forMedia(
                mediaDetailPagerFragment.position + i,
                mediaDetailPagerFragment.editable, mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed
            )
        } else {
            forMedia(
                i, mediaDetailPagerFragment.editable,
                mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed
            )
        }
    }

    override fun getCount(): Int {
        if (mediaDetailPagerFragment.activity == null) {
            Timber.d("Skipping getCount. Returning as activity is destroyed!")
            return 0
        }
        return mediaDetailPagerFragment.provider.getTotalMediaCount()
    }

    /**
     * If current fragment is of type MediaDetailFragment, return it, otherwise return null.
     *
     * @return MediaDetailFragment
     */
    val currentMediaDetailFragment: MediaDetailFragment?
        get() = currentFragment as? MediaDetailFragment

    /**
     * Called to inform the adapter of which item is currently considered to be the "primary", that
     * is the one show to the user as the current page.
     */
    override fun setPrimaryItem(
        container: ViewGroup, position: Int,
        obj: Any
    ) {
        // Update the current fragment if changed
        if (currentFragment !== obj) {
            currentFragment = (obj as Fragment)
        }
        super.setPrimaryItem(container, position, obj)
    }
}
