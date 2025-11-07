package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.work.ExistingWorkPolicy
import com.google.android.material.bottomnavigation.BottomNavigationView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.contributions.ContributionsFragment.Companion.newInstance
import fr.free.nrw.commons.databinding.MainBinding
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.navtab.NavTabLoggedOut
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback
import fr.free.nrw.commons.notification.NotificationActivity.Companion.startYourself
import fr.free.nrw.commons.notification.NotificationController
import fr.free.nrw.commons.quiz.QuizChecker
import fr.free.nrw.commons.settings.SettingsFragment
import fr.free.nrw.commons.startWelcome
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.upload.UploadProgressActivity
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.Companion.makeOneTimeWorkRequest
import fr.free.nrw.commons.utils.ViewUtilWrapper
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener {
    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    @JvmField
    @Inject
    var controller: ContributionController? = null

    @JvmField
    @Inject
    var contributionDao: ContributionDao? = null

    private var contributionsFragment: ContributionsFragment? = null
    private var nearbyParentFragment: NearbyParentFragment? = null
    private var exploreFragment: ExploreFragment? = null
    private var bookmarkFragment: BookmarkFragment? = null
    @JvmField
    var activeFragment: ActiveFragment? = null
    private val mediaDetailPagerFragment: MediaDetailPagerFragment? = null
    var navListener: BottomNavigationView.OnNavigationItemSelectedListener? = null
        private set

    @JvmField
    @Inject
    var locationManager: LocationServiceManager? = null

    @JvmField
    @Inject
    var notificationController: NotificationController? = null

    @JvmField
    @Inject
    var quizChecker: QuizChecker? = null

    @JvmField
    @Inject
    @Named("default_preferences")
    var applicationKvStore: JsonKvStore? = null

    @JvmField
    @Inject
    var viewUtilWrapper: ViewUtilWrapper? = null

    var menu: Menu? = null

    @JvmField
    var binding: MainBinding? = null

    var tabLayout: NavTabLayout? = null


    override fun onSupportNavigateUp(): Boolean {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            if (!contributionsFragment!!.backButtonClicked()) {
                return false
            }
        } else {
            onBackPressed()
            showTabs()
        }
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding!!.root)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        tabLayout = binding!!.fragmentMainNavTabLayout
        loadLocale()

        binding!!.toolbarBinding.toolbar.setNavigationOnClickListener { view: View? ->
            onSupportNavigateUp()
        }
        /*
"first_edit_depict" is a key for getting information about opening the depiction editor
screen for the first time after opening the app.

Getting true by the key means the depiction editor screen is opened for the first time
after opening the app.
Getting false by the key means the depiction editor screen is not opened for the first time
after opening the app.
 */
        applicationKvStore!!.putBoolean("first_edit_depict", true)
        if (applicationKvStore!!.getBoolean("login_skipped") == true) {
            title = getString(R.string.navigation_item_explore)
            setUpLoggedOutPager()
        } else {
            if (applicationKvStore!!.getBoolean("firstrun", true)) {
                applicationKvStore!!.putBoolean("hasAlreadyLaunchedBigMultiupload", false)
                applicationKvStore!!.putBoolean("hasAlreadyLaunchedCategoriesDialog", false)
            }
            if (savedInstanceState == null) {
                //starting a fresh fragment.
                // Open Last opened screen if it is Contributions or Nearby, otherwise Contributions
                if (applicationKvStore!!.getBoolean("last_opened_nearby")) {
                    title = getString(R.string.nearby_fragment)
                    showNearby()
                    loadFragment(NearbyParentFragment.newInstance(), false)
                } else {
                    title = getString(R.string.contributions_fragment)
                    loadFragment(newInstance(), false)
                }
            }
            setUpPager()

            checkAndResumeStuckUploads()
        }
    }

    fun setSelectedItemId(id: Int) {
        binding!!.fragmentMainNavTabLayout.selectedItemId = id
    }

    private fun setUpPager() {
        binding!!.fragmentMainNavTabLayout.setOnNavigationItemSelectedListener(
            BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
                if (item.title != getString(R.string.more)) {
                    // do not change title for more fragment
                    title = item.title
                }
                // set last_opened_nearby true if item is nearby screen else set false
                applicationKvStore!!.putBoolean(
                    "last_opened_nearby",
                    item.title == getString(R.string.nearby_fragment)
                )
                val fragment = NavTab.of(item.order).newInstance()
                loadFragment(fragment, true)
            }.also { navListener = it })
    }

    private fun setUpLoggedOutPager() {
        loadFragment(ExploreFragment.newInstance(), false)
        binding!!.fragmentMainNavTabLayout.setOnNavigationItemSelectedListener { item: MenuItem ->
            if (item.title != getString(R.string.more)) {
                // do not change title for more fragment
                title = item.title
            }
            val fragment =
                NavTabLoggedOut.of(item.order).newInstance()
            loadFragment(fragment, true)
        }
    }

    private fun loadFragment(fragment: Fragment?, showBottom: Boolean): Boolean {
        //showBottom so that we do not show the bottom tray again when constructing
        //from the saved instance state.

        freeUpFragments();

        if (fragment is ContributionsFragment) {
            if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
                // scroll to top if already on the Contributions tab
                contributionsFragment!!.scrollToTop()
                return true
            }
            contributionsFragment = fragment
            activeFragment = ActiveFragment.CONTRIBUTIONS
        } else if (fragment is NearbyParentFragment) {
            if (activeFragment == ActiveFragment.NEARBY) { // Do nothing if same tab
                return true
            }
            nearbyParentFragment = fragment
            activeFragment = ActiveFragment.NEARBY
        } else if (fragment is ExploreFragment) {
            if (activeFragment == ActiveFragment.EXPLORE) { // Do nothing if same tab
                return true
            }
            exploreFragment = fragment
            activeFragment = ActiveFragment.EXPLORE
        } else if (fragment is BookmarkFragment) {
            if (activeFragment == ActiveFragment.BOOKMARK) { // Do nothing if same tab
                return true
            }
            bookmarkFragment = fragment
            activeFragment = ActiveFragment.BOOKMARK
        } else if (fragment == null && showBottom) {
            if (applicationKvStore!!.getBoolean("login_skipped")
                == true
            ) { // If logged out, more sheet is different
                val bottomSheet = MoreBottomSheetLoggedOutFragment()
                bottomSheet.show(
                    supportFragmentManager,
                    "MoreBottomSheetLoggedOut"
                )
            } else {
                val bottomSheet = MoreBottomSheetFragment()
                bottomSheet.show(
                    supportFragmentManager,
                    "MoreBottomSheet"
                )
            }
        }

        if (fragment != null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            return true
        }
        return false
    }

    /**
     * loadFragment() overload that supports passing extras to fragments
     */
    private fun loadFragment(fragment: Fragment?, showBottom: Boolean, args: Bundle?): Boolean {
        if (fragment != null && args != null) {
            fragment.arguments = args
        }

        return loadFragment(fragment, showBottom)
    }

    /**
     * Old implementation of loadFragment() was causing memory leaks, due to MainActivity holding
     * references to cleared fragments. This function frees up all fragment references.
     *
     *
     * Called in loadFragment() before doing the actual loading.
     */
    fun freeUpFragments() {
        // free all fragments except contributionsFragment because several tests depend on it.
        // hence, contributionsFragment is probably still a leak
        nearbyParentFragment = null
        exploreFragment = null
        bookmarkFragment = null
    }


    fun hideTabs() {
        binding!!.fragmentMainNavTabLayout.visibility = View.GONE
    }

    fun showTabs() {
        binding!!.fragmentMainNavTabLayout.visibility = View.VISIBLE
    }

    /**
     * Adds number of uploads next to tab text "Contributions" then it will look like "Contributions
     * (NUMBER)"
     *
     * @param uploadCount
     */
    fun setNumOfUploads(uploadCount: Int) {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            title =
                resources.getString(R.string.contributions_fragment) + " " + (if (uploadCount != 0)
                    resources
                        .getQuantityString(
                            R.plurals.contributions_subtitle,
                            uploadCount, uploadCount
                        )
                else
                    getString(R.string.contributions_subtitle_zero))
        }
    }

    /**
     * Resume the uploads that got stuck because of the app being killed or the device being
     * rebooted.
     *
     *
     * When the app is terminated or the device is restarted, contributions remain in the
     * 'STATE_IN_PROGRESS' state. This status persists and doesn't change during these events. So,
     * retrieving contributions labeled as 'STATE_IN_PROGRESS' from the database will provide the
     * list of uploads that appear as stuck on opening the app again
     */
    @SuppressLint("CheckResult")
    private fun checkAndResumeStuckUploads() {
        val stuckUploads = contributionDao!!.getContribution(
            listOf(Contribution.STATE_IN_PROGRESS)
        )
            .subscribeOn(Schedulers.io())
            .blockingGet()
        Timber.d("Resuming %d uploads...", stuckUploads.size)
        if (!stuckUploads.isEmpty()) {
            for (contribution in stuckUploads) {
                contribution.state = Contribution.STATE_QUEUED
                contribution.dateUploadStarted = Calendar.getInstance().time
                Completable.fromAction { contributionDao!!.saveSynchronous(contribution) }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
            }
            makeOneTimeWorkRequest(
                this, ExistingWorkPolicy.APPEND_OR_REPLACE
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        //quizChecker.initQuizCheck(this);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("viewPagerCurrentItem", binding!!.pager.currentItem)
        outState.putString("activeFragment", activeFragment!!.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val activeFragmentName = savedInstanceState.getString("activeFragment")
        if (activeFragmentName != null) {
            restoreActiveFragment(activeFragmentName)
        }
    }

    private fun restoreActiveFragment(fragmentName: String) {
        if (fragmentName == ActiveFragment.CONTRIBUTIONS.name) {
            title = getString(R.string.contributions_fragment)
            loadFragment(newInstance(), false)
        } else if (fragmentName == ActiveFragment.NEARBY.name) {
            title = getString(R.string.nearby_fragment)
            loadFragment(NearbyParentFragment.newInstance(), false)
        } else if (fragmentName == ActiveFragment.EXPLORE.name) {
            title = getString(R.string.navigation_item_explore)
            loadFragment(ExploreFragment.newInstance(), false)
        } else if (fragmentName == ActiveFragment.BOOKMARK.name) {
            title = getString(R.string.bookmarks)
            loadFragment(BookmarkFragment.newInstance(), false)
        }
    }

    override fun onBackPressed() {
        when (activeFragment) {
            ActiveFragment.CONTRIBUTIONS -> {
            // Means that contribution fragment is visible
            if (contributionsFragment?.backButtonClicked() != true) { //If this one does not want to handle
                // the back press, let the activity do so
                super.onBackPressed()
                }
            }
        ActiveFragment.NEARBY -> {
            // Means that nearby fragment is visible
            if (nearbyParentFragment?.backButtonClicked() != true) {
            nearbyParentFragment?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
                    }
                setSelectedItemId(NavTab.CONTRIBUTIONS.code())
                }
            }
         ActiveFragment.EXPLORE -> {
            // Explore Fragment is visible
            if (exploreFragment?.onBackPressed() != true) {
                if (applicationKvStore?.getBoolean("login_skipped") == true) {
                    super.onBackPressed()
                } else {
                    setSelectedItemId(NavTab.CONTRIBUTIONS.code())
                    }
                }
            }
         ActiveFragment.BOOKMARK -> {
            // Means that bookmark fragment is visible
            bookmarkFragment?.onBackPressed()
            }
         else -> {
            super.onBackPressed()
            }
        }
    }

    override fun onBackStackChanged() {
        //initBackButton();
    }

    /**
     * Retry all failed uploads as soon as the user returns to the app
     */
    @SuppressLint("CheckResult")
    private fun retryAllFailedUploads() {
        contributionDao
            ?.getContribution(listOf(Contribution.STATE_FAILED))
            ?.subscribeOn(Schedulers.io())
            ?.subscribe { failedUploads ->
                failedUploads.forEach { contribution ->
                    contributionsFragment?.retryUpload(contribution)
                }
            }
    }

    /**
     * Handles item selection in the options menu. This method is called when a user interacts with
     * the options menu in the Top Bar.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.upload_tab -> {
                startActivity(Intent(this, UploadProgressActivity::class.java))
                return true
            }

            R.id.notifications -> {
                // Starts notification activity on click to notification icon
                startYourself(this, "unread")
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun centerMapToPlace(place: Place?) {
        setSelectedItemId(NavTab.NEARBY.code())
        nearbyParentFragment!!.setNearbyParentFragmentInstanceReadyCallback(
            object : NearbyParentFragmentInstanceReadyCallback {
                override fun onReady() {
                    nearbyParentFragment!!.centerMapToPlace(place)
                }
            })
    }

    /**
     * Launch the Explore fragment from Nearby fragment. This method is called when a user clicks
     * the 'Show in Explore' option in the 3-dots menu in Nearby.
     *
     * @param zoom      current zoom of Nearby map
     * @param latitude  current latitude of Nearby map
     * @param longitude current longitude of Nearby map
     */
    fun loadExploreMapFromNearby(zoom: Double, latitude: Double, longitude: Double) {
        val bundle = Bundle()
        bundle.putDouble("prev_zoom", zoom)
        bundle.putDouble("prev_latitude", latitude)
        bundle.putDouble("prev_longitude", longitude)

        loadFragment(ExploreFragment.newInstance(), false, bundle)
        setSelectedItemId(NavTab.EXPLORE.code())
    }

    /**
     * Launch the Nearby fragment from Explore fragment. This method is called when a user clicks
     * the 'Show in Nearby' option in the 3-dots menu in Explore.
     *
     * @param zoom      current zoom of Explore map
     * @param latitude  current latitude of Explore map
     * @param longitude current longitude of Explore map
     */
    fun loadNearbyMapFromExplore(zoom: Double, latitude: Double, longitude: Double) {
        val bundle = Bundle()
        bundle.putDouble("prev_zoom", zoom)
        bundle.putDouble("prev_latitude", latitude)
        bundle.putDouble("prev_longitude", longitude)

        loadFragment(NearbyParentFragment.newInstance(), false, bundle)
        setSelectedItemId(NavTab.NEARBY.code())
    }

    override fun onResume() {
        super.onResume()

        if ((applicationKvStore!!.getBoolean("firstrun", true)) &&
            (!applicationKvStore!!.getBoolean("login_skipped"))
        ) {
            defaultKvStore.putBoolean("inAppCameraFirstRun", true)
            startWelcome()
        }

        retryAllFailedUploads()
    }

    override fun onDestroy() {
        quizChecker!!.cleanup()
        locationManager!!.unregisterLocationManager()
        // Remove ourself from hashmap to prevent memory leaks
        locationManager = null
        super.onDestroy()
    }

    /**
     * Public method to show nearby from the reference of this.
     */
    fun showNearby() {
        binding!!.fragmentMainNavTabLayout.selectedItemId = NavTab.NEARBY.code()
    }

    enum class ActiveFragment {
        CONTRIBUTIONS,
        NEARBY,
        EXPLORE,
        BOOKMARK,
        MORE
    }

    /**
     * Load default language in onCreate from SharedPreferences
     */
    private fun loadLocale() {
        val preferences = getSharedPreferences(
            "Settings",
            MODE_PRIVATE
        )
        val language = preferences.getString("language", "")!!
        val settingsFragment = SettingsFragment()
        settingsFragment.setLocale(this, language)
    }

    companion object {
        /**
         * Consumers should be simply using this method to use this activity.
         *
         * @param context A Context of the application package implementing this class.
         */
        fun startYourself(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(intent)
        }
    }
}
