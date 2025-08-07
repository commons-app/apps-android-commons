package fr.free.nrw.commons.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentBookmarksBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.theme.BaseActivity
import javax.inject.Inject
import javax.inject.Named

class BookmarkFragment : CommonsDaggerSupportFragment() {
    private var adapter: BookmarksPagerAdapter? = null

    @JvmField
    var binding: FragmentBookmarksBinding? = null

    @JvmField
    @Inject
    var controller: ContributionController? = null

    /**
     * To check if the user is loggedIn or not.
     */
    @JvmField
    @Inject
    @Named("default_preferences")
    var applicationKvStore: JsonKvStore? = null

    fun setScroll(canScroll: Boolean) {
        binding?.let {
            it.viewPagerBookmarks.canScroll = canScroll
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentBookmarksBinding.inflate(inflater, container, false)

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        val supportFragmentManager = childFragmentManager

        adapter = BookmarksPagerAdapter(
            supportFragmentManager, requireContext(),
            applicationKvStore!!.getBoolean("login_skipped")
        )
        binding!!.viewPagerBookmarks.adapter = adapter
        binding!!.tabLayout.setupWithViewPager(binding!!.viewPagerBookmarks)

        (requireActivity() as MainActivity).showTabs()
        (requireActivity() as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)

        setupTabLayout()
        return binding!!.root
    }

    /**
     * This method sets up the tab layout. If the adapter has only one element it sets the
     * visibility of tabLayout to gone.
     */
    fun setupTabLayout() {
        binding!!.tabLayout.visibility = View.VISIBLE
        if (adapter!!.count == 1) {
            binding!!.tabLayout.visibility = View.GONE
        }
    }


    fun onBackPressed() {
        if (((adapter!!.getItem(binding!!.tabLayout.selectedTabPosition)) as BookmarkListRootFragment).backPressed()) {
            // The event is handled internally by the adapter , no further action required.
            return
        }

        // Event is not handled by the adapter ( performed back action ) change action bar.
        (requireActivity() as BaseActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        fun newInstance(): BookmarkFragment = BookmarkFragment().apply {
            retainInstance = true
        }
    }
}
