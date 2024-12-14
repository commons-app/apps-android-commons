package fr.free.nrw.commons.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentBookmarksBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.theme.BaseActivity
import javax.inject.Inject
import fr.free.nrw.commons.contributions.ContributionController
import javax.inject.Named


class BookmarkFragment : CommonsDaggerSupportFragment() {

    var binding: FragmentBookmarksBinding? = null
    private var adapter: BookmarksPagerAdapter? = null

    @Inject
    lateinit var controller: ContributionController

    /**
     * To check if the user is loggedIn or not.
     */
    @Inject
    @field: Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    companion object {
        fun newInstance(): BookmarkFragment {
            val fragment = BookmarkFragment()
            fragment.retainInstance = true
            return fragment
        }
    }

    fun setScroll(canScroll: Boolean) {
        binding?.viewPagerBookmarks?.isCanScroll = canScroll
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentBookmarksBinding.inflate(inflater, container, false)

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        val supportFragmentManager = childFragmentManager

        adapter = BookmarksPagerAdapter(
            supportFragmentManager,
            requireContext(),
            applicationKvStore.getBoolean("login_skipped")
        )
        binding?.apply {
            viewPagerBookmarks.adapter = adapter
            tabLayout.setupWithViewPager(viewPagerBookmarks)
        }

        (requireActivity() as MainActivity).showTabs()
        (requireActivity() as BaseActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        setupTabLayout()
        return binding?.root
    }

    /**
     * This method sets up the tab layout. If the adapter has only one element it sets the
     * visibility of tabLayout to gone.
     */
    fun setupTabLayout() {
        binding?.apply {
            tabLayout.visibility = View.VISIBLE
            if (adapter?.count == 1) {
                tabLayout.visibility = View.GONE
            }
        }
    }

    fun onBackPressed() {
        val selectedFragment = adapter?.getItem(binding?.tabLayout?.selectedTabPosition ?: 0)
                as? BookmarkListRootFragment

        if (selectedFragment?.backPressed() == true) {
            // The event is handled internally by the adapter, no further action required.
            return
        }

        // Event is not handled by the adapter (performed back action) change action bar.
        (requireActivity() as BaseActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
