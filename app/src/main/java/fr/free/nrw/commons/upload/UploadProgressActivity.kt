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
        menuInflater.inflate(R.menu.menu_uploads,menu)
        this.menu = menu
        updateMenuItems(0)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun updateMenuItems(currentPosition: Int) {
        menu!!.clear()
        if (currentPosition == 0) {
            if (isPendingIconsVisible){
                if (!isPaused){
                    if (menu!!.findItem(R.id.pause_icon) == null) {
                        menu!!.add(Menu.NONE, R.id.pause_icon, Menu.NONE, getString(R.string.pause))
                            .setIcon(android.R.drawable.ic_media_pause).setOnMenuItemClickListener {
                                pendingUploadsFragment!!.pauseUploads()
                                setPausedIcon(true)
                                true
                            }
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                    if (menu!!.findItem(R.id.cancel_icon) == null) {
                        menu!!.add(Menu.NONE, R.id.cancel_icon, Menu.NONE, getString(R.string.cancel))
                            .setIcon(android.R.drawable.ic_menu_close_clear_cancel).setOnMenuItemClickListener {
                                pendingUploadsFragment!!.deleteUploads()
                                true
                            }
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                }else{
                    if (menu!!.findItem(R.id.resume_icon) == null) {
                        menu!!.add(Menu.NONE, R.id.resume_icon, Menu.NONE, getString(R.string.resume))
                            .setIcon(android.R.drawable.ic_media_play).setOnMenuItemClickListener {
                                pendingUploadsFragment!!.restartUploads()
                                setPausedIcon(false)
                                true
                            }
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    }
                }
            }
        } else if (currentPosition == 1) {
            if (isErrorIconsVisisble){
                if (menu!!.findItem(R.id.retry_icon) == null) {
                    menu!!.add(Menu.NONE, R.id.retry_icon, Menu.NONE, getString(R.string.retry))
                        .setIcon(R.drawable.ic_refresh_white_24dp).setOnMenuItemClickListener {
                            failedUploadsFragment!!.restartUploads()
                            true
                        }
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
                if (menu!!.findItem(R.id.cancel_icon) == null) {
                    menu!!.add(Menu.NONE, R.id.cancel_icon, Menu.NONE, getString(R.string.cancel))
                        .setIcon(android.R.drawable.ic_menu_close_clear_cancel).setOnMenuItemClickListener {
                            failedUploadsFragment!!.deleteUploads()
                            true
                        }
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
        }
    }

    fun hidePendingIcons() {
        isPendingIconsVisible = false
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

    fun setPausedIcon(paused : Boolean){
        isPaused = paused
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

    fun setErrorIconsVisibility(visible : Boolean){
        isErrorIconsVisisble = visible
        updateMenuItems(binding.uploadProgressViewPager.currentItem)
    }

    fun resetProgressBar(){
        pendingUploadsFragment!!.resetProgressBar()
    }

}
