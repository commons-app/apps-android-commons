package fr.free.nrw.commons.category

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.free.nrw.commons.BuildConfig.COMMONS_URL
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.databinding.ActivityCategoryDetailsBinding
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.explore.categories.parent.ParentCategoriesFragment
import fr.free.nrw.commons.explore.categories.sub.SubCategoriesFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.model.WikiSite
import fr.free.nrw.commons.wikidata.model.page.PageTitle
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * This activity displays details of a particular category
 * Its generic and simply takes the name of category name in its start intent to load all images, subcategories in
 * a particular category on wikimedia commons.
 */
class CategoryDetailsActivity : BaseActivity(),
    MediaDetailPagerFragment.MediaDetailProvider,
    CategoryImagesCallback {

    private lateinit var supportFragmentManager: FragmentManager
    private lateinit var categoriesMediaFragment: CategoriesMediaFragment
    private var mediaDetails: MediaDetailPagerFragment? = null
    private var categoryName: String? = null
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private lateinit var binding: ActivityCategoryDetailsBinding

    @Inject
    lateinit var categoryViewModelFactory: CategoryDetailsViewModel.ViewModelFactory

    private val viewModel: CategoryDetailsViewModel by viewModels<CategoryDetailsViewModel> { categoryViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCategoryDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportFragmentManager = getSupportFragmentManager()
        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTabs()
        setPageTitle()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.bookmarkState.collect {
                    invalidateOptionsMenu()
                }
            }
        }

    }

    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    private fun setTabs() {
        val fragmentList = mutableListOf<Fragment>()
        val titleList = mutableListOf<String>()
        categoriesMediaFragment = CategoriesMediaFragment()
        val subCategoryListFragment = SubCategoriesFragment()
        val parentCategoriesFragment = ParentCategoriesFragment()
        categoryName = intent?.getStringExtra("categoryName")
        if (intent != null && categoryName != null) {
            val arguments = Bundle().apply {
                putString("categoryName", categoryName)
            }
            categoriesMediaFragment.arguments = arguments
            subCategoryListFragment.arguments = arguments
            parentCategoriesFragment.arguments = arguments

            viewModel.onCheckIfBookmarked(categoryName!!)
        }
        fragmentList.add(categoriesMediaFragment)
        titleList.add("MEDIA")
        fragmentList.add(subCategoryListFragment)
        titleList.add("SUBCATEGORIES")
        fragmentList.add(parentCategoriesFragment)
        titleList.add("PARENT CATEGORIES")
        viewPagerAdapter.setTabData(fragmentList, titleList)
        viewPagerAdapter.notifyDataSetChanged()
    }

    /**
     * Gets the passed categoryName from the intents and displays it as the page title
     */
    private fun setPageTitle() {
        intent?.getStringExtra("categoryName")?.let {
            title = it
        }
    }

    /**
     * This method is called onClick of media inside category details (CategoryImageListFragment).
     */
    override fun onMediaClicked(position: Int) {
        binding.tabLayout.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.mediaContainer.visibility = View.VISIBLE
        if (mediaDetails == null || mediaDetails?.isVisible == false) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetails = MediaDetailPagerFragment.newInstance(false, true)
            supportFragmentManager.beginTransaction()
                .replace(R.id.mediaContainer, mediaDetails!!)
                .addToBackStack(null)
                .commit()
            supportFragmentManager.executePendingTransactions()
        }
        mediaDetails?.showImage(position)
    }


    companion object {
        /**
         * Consumers should be simply using this method to use this activity.
         * @param context  A Context of the application package implementing this class.
         * @param categoryName Name of the category for displaying its details
         */
        fun startYourself(context: Context?, categoryName: String) {
            val intent = Intent(context, CategoryDetailsActivity::class.java).apply {
                putExtra("categoryName", categoryName)
            }
            context?.startActivity(intent)
        }
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     * @param i It is the index of which media object is to be returned which is same as
     *          current index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? {
        return categoriesMediaFragment.getMediaAtPosition(i)
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int {
        return categoriesMediaFragment.getTotalMediaCount()
    }

    override fun getContributionStateAt(position: Int): Int? {
        return null
    }

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (supportFragmentManager.backStackEntryCount == 1) {
            onBackPressed()
            onMediaClicked(index)
        }
    }

    /**
     * This method inflates the menu in the toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.fragment_category_detail, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * This method handles the logic on ItemSelect in toolbar menu
     * Currently only 1 choice is available to open category details page in browser
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_browser_current_category -> {
                val title = PageTitle(CATEGORY_PREFIX + categoryName, WikiSite(COMMONS_URL))

                handleWebUrl(this, Uri.parse(title.canonicalUri))
                true
            }

            R.id.menu_bookmark_current_category -> {
                categoryName?.let {
                    viewModel.onBookmarkClick(categoryName = it)
                }
                true
            }

            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.run {
          val bookmarkMenuItem = findItem(R.id.menu_bookmark_current_category)
            if (bookmarkMenuItem != null) {
                val icon = if(viewModel.bookmarkState.value){
                    R.drawable.menu_ic_round_star_filled_24px
                } else {
                    R.drawable.menu_ic_round_star_border_24px
                }

                bookmarkMenuItem.setIcon(icon)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    @Deprecated("This method has been deprecated in favor of using the" +
            "{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}." +
            "The OnBackPressedDispatcher controls how back button events are dispatched" +
            "to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            binding.tabLayout.visibility = View.VISIBLE
            binding.viewPager.visibility = View.VISIBLE
            binding.mediaContainer.visibility = View.GONE
        }
        super.onBackPressed()
    }

    /**
     * This method is called on success of API call for Images inside a category.
     * The viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        mediaDetails?.notifyDataSetChanged()
    }
}
