package fr.free.nrw.commons.explore.depictions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.ActivityWikidataItemDetailsBinding
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.WikidataConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Activity to show depiction media, parent classes and child classes of depicted items in Explore
 */
class WikidataItemDetailsActivity : BaseActivity(),
    MediaDetailPagerFragment.MediaDetailProvider, CategoryImagesCallback {

    private lateinit var supportFragmentManager: FragmentManager
    private lateinit var depictionImagesListFragment: DepictedImagesFragment
    private var mediaDetailPagerFragment: MediaDetailPagerFragment? = null

    /**
     * Name of the depicted item
     * Ex: Rabbit
     */
    @Inject
    lateinit var bookmarkItemsDao: BookmarkItemsDao

    @Inject
    lateinit var depictModel: DepictModel
    private var wikidataItemName: String? = null
    private lateinit var binding: ActivityWikidataItemDetailsBinding

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var wikidataItem: DepictedItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWikidataItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager = getSupportFragmentManager()
        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.offscreenPageLimit = 2
        binding.tabLayout.setupWithViewPager(binding.viewPager)

        wikidataItem = intent.getParcelableExtra(WikidataConstants.BOOKMARKS_ITEMS)
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setTabs()
        setPageTitle()
    }

    /**
     * Gets the passed wikidataItemName from the intent and displays it as the page title
     */
    private fun setPageTitle() {
        intent.getStringExtra("wikidataItemName")?.let {
            title = it
        }
    }

    /**
     * This method is called on success of API call for featured images.
     * The ViewPager will be notified that the number of items has changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        mediaDetailPagerFragment?.notifyDataSetChanged()
    }

    /**
     * This activity contains 3 tabs and a ViewPager.
     * This method is used to set the titles of tabs and the fragments according to the selected tab
     */
    private fun setTabs() {
        val fragmentList = mutableListOf<Fragment>()
        val titleList = mutableListOf<String>()

        depictionImagesListFragment = DepictedImagesFragment()
        val childDepictionsFragment = ChildDepictionsFragment()
        val parentDepictionsFragment = ParentDepictionsFragment()

        wikidataItemName = intent.getStringExtra("wikidataItemName")
        val entityId = intent.getStringExtra("entityId")

        if (!wikidataItemName.isNullOrEmpty()) {
            val arguments = Bundle().apply {
                putString("wikidataItemName", wikidataItemName)
                putString("entityId", entityId)
            }
            depictionImagesListFragment.arguments = arguments
            parentDepictionsFragment.arguments = arguments
            childDepictionsFragment.arguments = arguments
        }

        fragmentList.apply {
            add(depictionImagesListFragment)
            add(childDepictionsFragment)
            add(parentDepictionsFragment)
        }

        titleList.apply {
            add(getString(R.string.title_for_media))
            add(getString(R.string.title_for_child_classes))
            add(getString(R.string.title_for_parent_classes))
        }

        viewPagerAdapter.setTabData(fragmentList, titleList)
        binding.viewPager.offscreenPageLimit = 2
        viewPagerAdapter.notifyDataSetChanged()
    }

    /**
     * Shows media detail fragment when user clicks on any image in the list
     */
    override fun onMediaClicked(position: Int) {
        binding.apply {
            tabLayout.visibility = View.GONE
            viewPager.visibility = View.GONE
            mediaContainer.visibility = View.VISIBLE
        }

        if (mediaDetailPagerFragment == null || mediaDetailPagerFragment?.isVisible == false) {
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            supportFragmentManager = getSupportFragmentManager()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mediaContainer, mediaDetailPagerFragment!!)
                .addToBackStack(null)
                .commit()
            supportFragmentManager.executePendingTransactions()
        }

        mediaDetailPagerFragment?.showImage(position)
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     * @param i It is the index of which media object is to be returned which is same as
     *          current index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? {
        return depictionImagesListFragment.getMediaAtPosition(i)
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            binding.apply {
                tabLayout.visibility = View.VISIBLE
                viewPager.visibility = View.VISIBLE
                mediaContainer.visibility = View.GONE
            }
        }
        super.onBackPressed()
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int {
        return depictionImagesListFragment.getTotalMediaCount()
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

    companion object {
        /**
         * Consumers should be simply using this method to use this activity.
         *
         * @param context a Context of the application package implementing this class.
         * @param depictedItem Name of the depicts for displaying its details
         */
        fun startYourself(context: Context, depictedItem: DepictedItem) {
            val intent = Intent(context, WikidataItemDetailsActivity::class.java).apply {
                putExtra("wikidataItemName", depictedItem.name)
                putExtra("entityId", depictedItem.id)
                putExtra(WikidataConstants.BOOKMARKS_ITEMS, depictedItem)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Inflates the menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_wikidata_item, menu)
        updateBookmarkState(menu.findItem(R.id.menu_bookmark_current_item))
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * This method handles the logic on item select in toolbar menu
     * Currently only 1 choice is available to open Wikidata item details page in browser
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.browser_actions_menu_items -> {
                val entityId = intent.getStringExtra("entityId")
                val uri = Uri.parse("https://www.wikidata.org/wiki/$entityId")
                Utils.handleWebUrl(this, uri)
                return true
            }
            R.id.menu_bookmark_current_item -> {
                val entityId = intent.getStringExtra("entityId")

                if (intent.getStringExtra("fragment") != null) {
                    compositeDisposable.add(
                        depictModel.getDepictions(entityId!!)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { depictedItems ->
                                val bookmarkExists = bookmarkItemsDao
                                    .updateBookmarkItem(depictedItems[0])
                                val snackbarText = if (bookmarkExists)
                                    R.string.add_bookmark
                                else
                                    R.string.remove_bookmark
                                Snackbar.make(
                                    findViewById(R.id.toolbar_layout),
                                    snackbarText,
                                    Snackbar.LENGTH_LONG
                                ).show()
                                updateBookmarkState(item)
                            }
                    )
                } else {
                    val bookmarkExists = bookmarkItemsDao.updateBookmarkItem(wikidataItem!!)
                    val snackbarText = if (bookmarkExists)
                        R.string.add_bookmark
                    else
                        R.string.remove_bookmark
                    Snackbar.make(
                        findViewById(R.id.toolbar_layout),
                        snackbarText,
                        Snackbar.LENGTH_LONG
                    ).show()
                    updateBookmarkState(item)
                }
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateBookmarkState(item: MenuItem) {
        val isBookmarked = bookmarkItemsDao.findBookmarkItem(
            intent.getStringExtra("entityId") ?: wikidataItem?.id
        )
        val icon = if (isBookmarked)
            R.drawable.menu_ic_round_star_filled_24px
        else
            R.drawable.menu_ic_round_star_border_24px
        item.setIcon(icon)
    }
}
