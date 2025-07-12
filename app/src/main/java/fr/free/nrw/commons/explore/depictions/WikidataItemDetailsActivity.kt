package fr.free.nrw.commons.explore.depictions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.ActivityWikidataItemDetailsBinding
import fr.free.nrw.commons.explore.depictions.child.ChildDepictionsFragment
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment
import fr.free.nrw.commons.explore.depictions.parent.ParentDepictionsFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.structure.depictions.DepictModel
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.WikidataConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Activity to show depiction media, parent classes and child classes of depicted items in Explore
 */
class WikidataItemDetailsActivity : BaseActivity(), MediaDetailProvider, CategoryImagesCallback {
    @JvmField
    @Inject
    var bookmarkItemsDao: BookmarkItemsDao? = null

    @JvmField
    @Inject
    var depictModel: DepictModel? = null

    private var supportFragmentManager: FragmentManager? = null
    private var depictionImagesListFragment: DepictedImagesFragment? = null
    private var mediaDetailPagerFragment: MediaDetailPagerFragment? = null
    private var binding: ActivityWikidataItemDetailsBinding? = null

    var viewPagerAdapter: ViewPagerAdapter? = null
    private var wikidataItem: DepictedItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWikidataItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        supportFragmentManager = getSupportFragmentManager()
        viewPagerAdapter = ViewPagerAdapter(this, getSupportFragmentManager())
        binding!!.viewPager.adapter = viewPagerAdapter
        binding!!.viewPager.offscreenPageLimit = 2
        binding!!.tabLayout.setupWithViewPager(binding!!.viewPager)

        wikidataItem = intent.getParcelableExtra(WikidataConstants.BOOKMARKS_ITEMS)
        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setTabs()
        setPageTitle()
    }

    /**
     * Gets the passed wikidataItemName from the intents and displays it as the page title
     */
    private fun setPageTitle() {
        if (intent != null && intent.getStringExtra("wikidataItemName") != null) {
            title = intent.getStringExtra("wikidataItemName")
        }
    }

    /**
     * This method is called on success of API call for featured Images.
     * The viewpager will notified that number of items have changed.
     */
    override fun viewPagerNotifyDataSetChanged() {
        if (mediaDetailPagerFragment != null) {
            mediaDetailPagerFragment!!.notifyDataSetChanged()
        }
    }

    /**
     * This activity contains 3 tabs and a viewpager. This method is used to set the titles of tab,
     * Set the fragments according to the tab selected in the viewPager.
     */
    private fun setTabs() {
        depictionImagesListFragment = DepictedImagesFragment()
        val childDepictionsFragment = ChildDepictionsFragment()
        val parentDepictionsFragment = ParentDepictionsFragment()
        val wikidataItemName = intent.getStringExtra("wikidataItemName")
        val entityId = intent.getStringExtra("entityId")
        if (intent != null && wikidataItemName != null) {
            val arguments = bundleOf(
                "wikidataItemName" to wikidataItemName,
                "entityId" to entityId
            )
            depictionImagesListFragment!!.arguments = arguments
            parentDepictionsFragment.arguments = arguments
            childDepictionsFragment.arguments = arguments
        }

        viewPagerAdapter!!.setTabs(
            R.string.title_for_media to depictionImagesListFragment!!,
            R.string.title_for_subcategories to childDepictionsFragment,
            R.string.title_for_parent_categories to parentDepictionsFragment
        )
        binding!!.viewPager.offscreenPageLimit = 2
        viewPagerAdapter!!.notifyDataSetChanged()
    }


    /**
     * Shows media detail fragment when user clicks on any image in the list
     */
    override fun onMediaClicked(position: Int) {
        binding!!.tabLayout.visibility = View.GONE
        binding!!.viewPager.visibility = View.GONE
        binding!!.mediaContainer.visibility = View.VISIBLE
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment!!.isVisible) {
            // set isFeaturedImage true for featured images, to include author field on media detail
            mediaDetailPagerFragment = MediaDetailPagerFragment.newInstance(false, true)
            val supportFragmentManager = getSupportFragmentManager()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.mediaContainer, mediaDetailPagerFragment!!)
                .addToBackStack(null)
                .commit()
            supportFragmentManager.executePendingTransactions()
        }
        mediaDetailPagerFragment!!.showImage(position)
    }

    /**
     * This method is called mediaDetailPagerFragment. It returns the Media Object at that Index
     * @param i It is the index of which media object is to be returned which is same as
     * current index of viewPager.
     * @return Media Object
     */
    override fun getMediaAtPosition(i: Int): Media? {
        return depictionImagesListFragment!!.getMediaAtPosition(i)
    }

    /**
     * This method is called on backPressed of anyFragment in the activity.
     * If condition is called when mediaDetailFragment is opened.
     */
    override fun onBackPressed() {
        if (supportFragmentManager!!.backStackEntryCount == 1) {
            binding!!.tabLayout.visibility = View.VISIBLE
            binding!!.viewPager.visibility = View.VISIBLE
            binding!!.mediaContainer.visibility = View.GONE
        }
        super.onBackPressed()
    }

    /**
     * This method is called on from getCount of MediaDetailPagerFragment
     * The viewpager will contain same number of media items as that of media elements in adapter.
     * @return Total Media count in the adapter
     */
    override fun getTotalMediaCount(): Int = depictionImagesListFragment!!.getTotalMediaCount()

    override fun getContributionStateAt(position: Int): Int? = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        if (getSupportFragmentManager().backStackEntryCount == 1) {
            onBackPressed()
            onMediaClicked(index)
        }
    }

    /**
     * This function inflates the menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuInflater = menuInflater
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
                handleWebUrl(this, uri)
                return true
            }

            R.id.menu_bookmark_current_item -> {
                if (intent.getStringExtra("fragment") != null) {
                    compositeDisposable!!.add(
                        depictModel!!.getDepictions(
                            intent.getStringExtra("entityId")!!
                        ).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(Consumer<List<DepictedItem?>> { depictedItems: List<DepictedItem?> ->
                                val bookmarkExists = bookmarkItemsDao!!.updateBookmarkItem(
                                    depictedItems[0]!!
                                )
                                val snackbar = if (bookmarkExists)
                                    Snackbar.make(
                                        findViewById(R.id.toolbar_layout),
                                        R.string.add_bookmark, Snackbar.LENGTH_LONG
                                    )
                                else
                                    Snackbar.make(
                                        findViewById(R.id.toolbar_layout),
                                        R.string.remove_bookmark,
                                        Snackbar.LENGTH_LONG
                                    )

                                snackbar.show()
                                updateBookmarkState(item)
                            })
                    )
                } else {
                    val bookmarkExists = bookmarkItemsDao!!.updateBookmarkItem(wikidataItem!!)
                    val snackbar = if (bookmarkExists)
                        Snackbar.make(
                            findViewById(R.id.toolbar_layout),
                            R.string.add_bookmark, Snackbar.LENGTH_LONG
                        )
                    else
                        Snackbar.make(
                            findViewById(R.id.toolbar_layout), R.string.remove_bookmark,
                            Snackbar.LENGTH_LONG
                        )

                    snackbar.show()
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
        val isBookmarked: Boolean = if (intent.getStringExtra("fragment") != null) {
            bookmarkItemsDao!!.findBookmarkItem(intent.getStringExtra("entityId"))
        } else {
            bookmarkItemsDao!!.findBookmarkItem(wikidataItem!!.id)
        }
        item.setIcon(if (isBookmarked) {
            R.drawable.menu_ic_round_star_filled_24px
        } else {
            R.drawable.menu_ic_round_star_border_24px
        })
    }

    companion object {
        /**
         * Consumers should be simply using this method to use this activity.
         *
         * @param context      A Context of the application package implementing this class.
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
}
