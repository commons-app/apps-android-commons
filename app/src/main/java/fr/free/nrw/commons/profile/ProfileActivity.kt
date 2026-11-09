package fr.free.nrw.commons.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import fr.free.nrw.commons.R
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.databinding.ActivityProfileBinding
import fr.free.nrw.commons.profile.achievements.AchievementsFragment
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import timber.log.Timber
import javax.inject.Inject

/**
 * This activity will set two tabs, achievements and
 * each tab will have their own fragments
 */
class ProfileActivity : BaseActivity() {
    lateinit var binding: ActivityProfileBinding

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var achievementsFragment: AchievementsFragment
    private lateinit var leaderboardFragment: LeaderboardFragment
    private lateinit var userName: String
    private var shouldShowContributions: Boolean = false
    private var contributionsFragment: ContributionsFragment? = null

    fun setScroll(canScroll: Boolean) {
        binding.viewPager.canScroll = canScroll
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.let {
            userName = it.getString(KEY_USERNAME, "")
            shouldShowContributions = it.getBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding.root)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarBinding.toolbar)

        binding.toolbarBinding.toolbar.setNavigationOnClickListener {
            onSupportNavigateUp()
        }

        userName = intent.getStringExtra(KEY_USERNAME) ?: ""
        title = userName
        shouldShowContributions = intent.getBooleanExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, false)

        viewPagerAdapter = ViewPagerAdapter(this, supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        setTabs()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (contributionsFragment?.mediaDetailPagerFragment?.isVisible == true) {
                        contributionsFragment?.backButtonClicked()
                        binding.tabLayout.visibility = View.VISIBLE
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setTabs() {
        achievementsFragment =
            AchievementsFragment().apply {
                arguments = bundleOf(KEY_USERNAME to userName)
            }

        leaderboardFragment =
            LeaderboardFragment().apply {
                arguments = bundleOf(KEY_USERNAME to userName)
            }

        contributionsFragment =
            ContributionsFragment().apply {
                arguments = bundleOf(KEY_USERNAME to userName)
            }

        viewPagerAdapter.setTabs(
            R.string.achievements_tab_title to achievementsFragment,
            R.string.leaderboard_tab_title to leaderboardFragment,
            R.string.contributions_fragment to contributionsFragment!!,
        )
        viewPagerAdapter.notifyDataSetChanged()
    }

    public override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_about, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_app_icon -> {
                val rootView = window.decorView.findViewById<View>(android.R.id.content)
                val screenshotSharer = ProfileScreenshotSharer(this)
                val screenShot = screenshotSharer.getScreenShot(rootView)
                if (screenShot == null) {
                    Timber.e("ScreenShot is null")
                    return false
                }
                screenshotSharer.showAlert(screenShot)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_USERNAME, userName)
        outState.putBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions)
    }

    fun setTabLayoutVisibility(isVisible: Boolean) {
        binding.tabLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    companion object {
        const val KEY_USERNAME = "username"
        const val KEY_SHOULD_SHOW_CONTRIBUTIONS = "shouldShowContributions"

        @JvmStatic
        fun startYourself(
            context: Context,
            userName: String,
            shouldShowContributions: Boolean,
        ) {
            val intent =
                Intent(context, ProfileActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(KEY_USERNAME, userName)
                    putExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions)
                }
            context.startActivity(intent)
        }
    }
}
