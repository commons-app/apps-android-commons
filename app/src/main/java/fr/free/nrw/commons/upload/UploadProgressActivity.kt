package fr.free.nrw.commons.upload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.databinding.ActivityUploadProgressBinding
import fr.free.nrw.commons.theme.BaseActivity
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity to manage the progress of uploads. It includes tabs to show pending and failed uploads,
 * and provides menu options to pause, resume, cancel, and retry uploads. Also, it contains ViewPager
 * which holds Pending Uploads Fragment and Failed Uploads Fragment to show list of pending and
 * failed uploads respectively.
 */
class UploadProgressActivity : BaseActivity() {

    private lateinit var binding: ActivityUploadProgressBinding
    private var pendingUploadsFragment: PendingUploadsFragment? = null
    private var failedUploadsFragment: FailedUploadsFragment? = null
    var viewPagerAdapter: ViewPagerAdapter? = null
    var menu: Menu? = null

    @Inject
    lateinit var contributionDao: ContributionDao

    val fragmentList: MutableList<Fragment> = ArrayList()
    val titleList: MutableList<String> = ArrayList()
    var isPaused = true
    var isPendingIconsVisible = true
    var isErrorIconsVisisble = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.uploadProgressViewPager.setAdapter(viewPagerAdapter)
        binding.uploadProgressViewPager.setId(R.id.upload_progress_view_pager)
        binding.uploadProgressTabLayout.setupWithViewPager(binding.uploadProgressViewPager)
        binding.toolbarBinding.toolbar.title = getString(R.string.uploads)
        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.uploadProgressViewPager.addOnPageChangeListener(object :
            ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                updateMenuItems(position)
                if (position == 2) {
                    binding.uploadProgressViewPager.setCanScroll(false)
                } else {
                    binding.uploadProgressViewPager.setCanScroll(true)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        setTabs()
    }

    /**
     * Initializes and sets up the tabs data by creating instances of `PendingUploadsFragment`
     * and `FailedUploadsFragment`, adds them to the `fragmentList`, and assigns corresponding
     * titles from resources to the `titleList`.
     */
    fun setTabs() {
        pendingUploadsFragment = PendingUploadsFragment()
        failedUploadsFragment = FailedUploadsFragment()

        fragmentList.add(pendingUploadsFragment!!)
        titleList.add(getString(R.string.pending))
        fragmentList.add(failedUploadsFragment!!)
        titleList.add(getString(R.string.failed))
        viewPagerAdapter!!.setTabData(fragmentList, titleList)
        viewPagerAdapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_uploads, menu)
        this.menu = menu
        updateMenuItems(0)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Updates the menu items based on the current position in the view pager and the visibility
     * of icons related to pending or failed uploads. This function dynamically modifies the menu
     * to display pause, resume, retry, and cancel options depending on the state of the uploads.
     *
     * @param currentPosition The current position in the view pager. A value of `0` indicates
     * pending uploads, while `1` indicates failed uploads.
     */
    fun updateMenuItems(currentPosition: Int) {
        if (menu != null) {
            menu!!.clear()
            if (currentPosition == 0) {
                if (isPendingIconsVisible) {
                    if (!isPaused) {
                        if (menu!!.findItem(R.id.pause_icon) == null) {
                            menu!!.add(
                                Menu.NONE,
                                R.id.pause_icon,
                                Menu.NONE,
                                getString(R.string.pause)
                            )
                                .setIcon(R.drawable.pause_icon)
                                .setOnMenuItemClickListener {
                                    pendingUploadsFragment!!.pauseUploads()
                                    setPausedIcon(true)
                                    true
                                }
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }
                        if (menu!!.findItem(R.id.cancel_icon) == null) {
                            menu!!.add(
                                Menu.NONE,
                                R.id.cancel_icon,
                                Menu.NONE,
                                getString(R.string.cancel)
                            )
                                .setIcon(R.drawable.ic_cancel_upload)
                                .setOnMenuItemClickListener {
                                    pendingUploadsFragment!!.deleteUploads()
                                    true
                                }
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }
                    } else {
                        if (menu!!.findItem(R.id.resume_icon) == null) {
                            menu!!.add(
                                Menu.NONE,
                                R.id.resume_icon,
                                Menu.NONE,
                                getString(R.string.resume)
                            )
                                .setIcon(R.drawable.play_icon)
                                .setOnMenuItemClickListener {
                                    pendingUploadsFragment!!.restartUploads()
                                    setPausedIcon(false)
                                    true
                                }
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                        }
                    }
                }
            } else if (currentPosition == 1) {
                if (isErrorIconsVisisble) {
                    if (menu!!.findItem(R.id.retry_icon) == null) {
                        menu!!.add(Menu.NONE, R.id.retry_icon, Menu.NONE, getString(R.string.retry))
                            .setIcon(R.drawable.ic_refresh_24dp).setOnMenuItemClickListener {
                                failedUploadsFragment!!.restartUploads()
                                true
                            }
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                    if (menu!!.findItem(R.id.cancel_icon) == null) {
                        menu!!.add(
                            Menu.NONE,
                            R.id.cancel_icon,
                            Menu.NONE,
                            getString(R.string.cancel)
                        )
                            .setIcon(R.drawable.ic_cancel_upload)
                            .setOnMenuItemClickListener {
                                failedUploadsFragment!!.deleteUploads()
                                true
                            }
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                }
            }
        }
    }

    /**
     * Hides the menu icons related to pending uploads.
     */
    fun hidePendingIcons() {
        isPendingIconsVisible = false
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

    /**
     * Sets the paused state and updates the menu items accordingly.
     * @param paused A boolean indicating whether all the uploads are paused.
     */
    fun setPausedIcon(paused: Boolean) {
        isPaused = paused
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

    /**
     * Sets the visibility of the menu icons related to failed uploads.
     * @param visible A boolean indicating whether the error icons should be visible.
     */
    fun setErrorIconsVisibility(visible: Boolean) {
        isErrorIconsVisisble = visible
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

}
