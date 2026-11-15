package fr.free.nrw.commons.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.FragmentFeaturedRootBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider

/**
 * Keeps Explore's list and map shells on the same media-detail navigation path, including the
 * hide/show transactions that preserve the underlying content while a picture is open.
 */
abstract class ExploreMediaRootFragment :
    CommonsDaggerSupportFragment(),
    MediaDetailProvider,
    CategoryImagesCallback {
    protected var mediaDetails: MediaDetailPagerFragment? = null
    protected var binding: FragmentFeaturedRootBinding? = null
    protected abstract val contentFragment: Fragment?

    /**
     * Creates the container shared by the content fragment and its media-detail pager.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreate(savedInstanceState)

        binding = FragmentFeaturedRootBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    /**
     * Preserves the existing hide/show back-stack transitions so returning from media details does
     * not replace a content fragment that is already added.
     */
    fun setFragment(
        fragment: Fragment,
        otherFragment: Fragment?,
    ) {
        if (fragment.isAdded && otherFragment != null) {
            childFragmentManager
                .beginTransaction()
                .hide(otherFragment)
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (fragment.isAdded && otherFragment == null) {
            childFragmentManager
                .beginTransaction()
                .show(fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded && otherFragment != null) {
            childFragmentManager
                .beginTransaction()
                .hide(otherFragment)
                .add(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        } else if (!fragment.isAdded) {
            childFragmentManager
                .beginTransaction()
                .replace(R.id.explore_container, fragment)
                .addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
                .commit()
            childFragmentManager.executePendingTransactions()
        }
    }

    /**
     * Completes removal before the shell reopens content or another media-detail pager.
     */
    protected fun removeFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        childFragmentManager.executePendingTransactions()
    }

    /**
     * Opens media details over the current content and disables swiping between Explore tabs until
     * the shell handles the return to content.
     */
    override fun onMediaClicked(position: Int) {
        showMediaContainer()
        setExploreTabsVisibility(View.GONE)
        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
        (parentFragment as ExploreFragment).setScroll(false)
        setFragment(mediaDetails!!, contentFragment)
        mediaDetails!!.showImage(position)
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (mediaDetails != null && !contentFragment!!.isVisible) {
            removeFragment(mediaDetails!!)
            onMediaClicked(index)
        }
    }

    /**
     * This method is called on success of API call for featured images or mobile uploads. The
     * viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        mediaDetails?.notifyDataSetChanged()
    }

    /**
     * Exposes the media container; list shells retain their existing allowance for a missing view.
     */
    protected open fun showMediaContainer() {
        binding!!.exploreContainer.visibility = View.VISIBLE
    }

    /**
     * Updates the enclosing tab strip while keeping the map shell's existing binding requirement.
     */
    protected open fun setExploreTabsVisibility(visibility: Int) {
        (parentFragment as ExploreFragment).binding!!.tabLayout.visibility = visibility
    }

    /**
     * Releases the shared container binding at the same lifecycle point as the original shells.
     */
    override fun onDestroy() {
        super.onDestroy()

        binding = null
    }
}
