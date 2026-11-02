package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.work.ExistingWorkPolicy
import com.google.android.material.bottomnavigation.BottomNavigationView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionsFragment.Companion.newInstance
import fr.free.nrw.commons.databinding.MainBinding
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.notification.NotificationActivity.Companion.startYourself
import fr.free.nrw.commons.notification.NotificationController
import fr.free.nrw.commons.quiz.QuizChecker
import fr.free.nrw.commons.settings.SettingsFragment
import fr.free.nrw.commons.startWelcome
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.UploadProgressActivity
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.makeOneTimeWorkRequest
import fr.free.nrw.commons.utils.ViewUtilWrapper
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class MainActivity : BaseActivity() {
    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    @JvmField
    @Inject
    var controller: ContributionController? = null

    @JvmField
    @Inject
    var contributionDao: ContributionDao? = null

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

    lateinit var tabHandler: MainTabHandler
    lateinit var navigationHelper: MainNavigationHelper

    var activeFragment: ActiveFragment?
        get() = if (::navigationHelper.isInitialized) navigationHelper.activeFragment else null
        set(value) {
            if (::navigationHelper.isInitialized) {
                navigationHelper.activeFragment = value
            }
        }

    val navListener: BottomNavigationView.OnNavigationItemSelectedListener?
        get() = if (::tabHandler.isInitialized) tabHandler.navListener else null

    override fun onSupportNavigateUp(): Boolean = navigationHelper.onSupportNavigateUp()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding!!.root)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        tabLayout = binding!!.fragmentMainNavTabLayout

        val preferences = getSharedPreferences("Settings", MODE_PRIVATE)
        val language = preferences.getString("language", "")!!
        SettingsFragment().setLocale(this, language)

        tabHandler = MainTabHandler(this, tabLayout!!)
        navigationHelper = MainNavigationHelper(this, tabHandler)

        binding!!.toolbarBinding.toolbar.setNavigationOnClickListener {
            onSupportNavigateUp()
        }

        applicationKvStore!!.putBoolean("first_edit_depict", true)
        if (applicationKvStore!!.getBoolean("login_skipped") == true) {
            title = getString(R.string.navigation_item_explore)
            navigationHelper.loadFragment(ExploreFragment.newInstance(), false)
            tabHandler.setUpLoggedOutPager { fragment, showBottom ->
                navigationHelper.loadFragment(fragment, showBottom)
            }
        } else {
            if (applicationKvStore!!.getBoolean("firstrun", true)) {
                applicationKvStore!!.putBoolean("hasAlreadyLaunchedBigMultiupload", false)
                applicationKvStore!!.putBoolean("hasAlreadyLaunchedCategoriesDialog", false)
            }
            if (savedInstanceState == null) {
                if (applicationKvStore!!.getBoolean("last_opened_nearby")) {
                    title = getString(R.string.nearby_fragment)
                    tabHandler.showNearby()
                    navigationHelper.loadFragment(NearbyParentFragment.newInstance(), false)
                } else {
                    title = getString(R.string.contributions_fragment)
                    navigationHelper.loadFragment(newInstance(), false)
                }
            }
            tabHandler.setUpPager { fragment, showBottom ->
                navigationHelper.loadFragment(fragment, showBottom)
            }

            checkAndResumeStuckUploads()
        }

        navigationHelper.setupBackNavigation()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("viewPagerCurrentItem", binding!!.pager.currentItem)
        outState.putString("activeFragment", activeFragment?.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val activeFragmentName = savedInstanceState.getString("activeFragment")
        if (activeFragmentName != null) {
            navigationHelper.restoreActiveFragment(activeFragmentName)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.upload_tab -> {
                startActivity(Intent(this, UploadProgressActivity::class.java))
                return true
            }

            R.id.notifications -> {
                startYourself(this, "unread")
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Centers the Nearby map on the specified place.
     *
     * @param place the place to center on
     */
    fun centerMapToPlace(place: Place?) {
        navigationHelper.centerMapToPlace(place)
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
        locationManager = null
        super.onDestroy()
    }

    /**
     * Makes the bottom navigation tabs visible.
     */
    fun showTabs() {
        tabHandler.showTabs()
    }

    /**
     * Selects the tab item with the specified ID.
     *
     * @param id the item ID to select
     */
    fun setSelectedItemId(id: Int) {
        tabHandler.setSelectedItemId(id)
    }

    enum class ActiveFragment {
        CONTRIBUTIONS,
        NEARBY,
        EXPLORE,
        BOOKMARK,
        MORE,
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

/**
 * Resume uploads that got stuck in progress because of process termination or reboot.
 */
@SuppressLint("CheckResult")
private fun MainActivity.checkAndResumeStuckUploads() {
    val stuckUploads =
        contributionDao!!
            .getContribution(
                listOf(Contribution.STATE_IN_PROGRESS),
            ).subscribeOn(Schedulers.io())
            .blockingGet()
    Timber.d("Resuming %d uploads...", stuckUploads.size)
    if (!stuckUploads.isEmpty()) {
        for (contribution in stuckUploads) {
            contribution.state = Contribution.STATE_QUEUED
            contribution.dateUploadStarted = Calendar.getInstance().time
            Completable
                .fromAction { contributionDao!!.saveSynchronous(contribution) }
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
        makeOneTimeWorkRequest(
            this,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
        )
    }
}

/**
 * Retry all failed uploads as soon as the user returns to the app.
 */
@SuppressLint("CheckResult")
private fun MainActivity.retryAllFailedUploads() {
    contributionDao
        ?.getContribution(listOf(Contribution.STATE_FAILED))
        ?.subscribeOn(Schedulers.io())
        ?.subscribe { failedUploads ->
            failedUploads.forEach { contribution ->
                navigationHelper.contributionsFragment?.retryUpload(contribution)
            }
        }
}

fun MainActivity.hideTabs() {
    tabHandler.hideTabs()
}

fun MainActivity.showNearby() {
    tabHandler.showNearby()
}

fun MainActivity.setNumOfUploads(uploadCount: Int) {
    tabHandler.setNumOfUploads(uploadCount, activeFragment)
}

fun MainActivity.loadExploreMapFromNearby(
    zoom: Double,
    latitude: Double,
    longitude: Double,
) {
    navigationHelper.loadExploreMapFromNearby(zoom, latitude, longitude)
}

fun MainActivity.loadNearbyMapFromExplore(
    zoom: Double,
    latitude: Double,
    longitude: Double,
) {
    navigationHelper.loadNearbyMapFromExplore(zoom, latitude, longitude)
}
