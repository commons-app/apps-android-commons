package fr.free.nrw.commons.media

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import fr.free.nrw.commons.media.MediaDetailFragment.Companion.forMedia
import timber.log.Timber

class MediaDetailAdapter(
    fm: FragmentManager,
    private val mediaDetailPagerFragment: MediaDetailPagerFragment,
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
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
            mediaDetailPagerFragment.binding?.mediaDetailsPager?.postDelayed(
                {
                    if (mediaDetailPagerFragment.activity != null) {
                        mediaDetailPagerFragment.requireActivity().invalidateOptionsMenu()
                    }
                },
                5,
            )
        }
        return if (mediaDetailPagerFragment.isFromFeaturedRootFragment) {
            forMedia(
                mediaDetailPagerFragment.position + i,
                mediaDetailPagerFragment.editable,
                mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed,
            )
        } else {
            forMedia(
                mediaDetailPagerFragment.position + i,
                mediaDetailPagerFragment.editable,
                mediaDetailPagerFragment.isFeaturedImage,
                mediaDetailPagerFragment.isWikipediaButtonDisplayed,
            )
        }
    }

    override fun getCount(): Int =
        if (mediaDetailPagerFragment.isFromFeaturedRootFragment) {
            mediaDetailPagerFragment.mediaDetailProvider.totalMediaCount
        } else {
            mediaDetailPagerFragment.totalMediaCount
        }

    /**
     * Gives the reference to the current fragment
     *
     * @return Fragment
     */
    val primaryItem: Fragment?
        get() = currentFragment

    override fun setPrimaryItem(
        container: android.view.ViewGroup,
        position: Int,
        `object`: Any,
    ) {
        super.setPrimaryItem(container, position, `object`)
        currentFragment = `object` as Fragment
    }
}
