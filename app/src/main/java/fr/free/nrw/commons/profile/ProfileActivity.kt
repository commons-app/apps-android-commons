package fr.free.nrw.commons.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.databinding.ActivityProfileBinding
import fr.free.nrw.commons.profile.achievements.AchievementsFragment
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.DialogUtil
import java.io.File
import java.io.FileOutputStream
import java.util.*
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
        binding.viewPager.setCanScroll(canScroll)
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
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarBinding.toolbar)

        binding.toolbarBinding.toolbar.setNavigationOnClickListener {
            onSupportNavigateUp()
        }

        userName = intent.getStringExtra(KEY_USERNAME) ?: ""
        title = userName
        shouldShowContributions = intent.getBooleanExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, false)

        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        setTabs()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setTabs() {
        val fragmentList = mutableListOf<Fragment>()
        val titleList = mutableListOf<String>()

        // Add Achievements tab
        achievementsFragment = AchievementsFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_USERNAME, userName)
            }
        }
        fragmentList.add(achievementsFragment)
        titleList.add(resources.getString(R.string.achievements_tab_title).uppercase())

        // Add Leaderboard tab
        leaderboardFragment = LeaderboardFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_USERNAME, userName)
            }
        }
        fragmentList.add(leaderboardFragment)
        titleList.add(resources.getString(R.string.leaderboard_tab_title).uppercase(Locale.ROOT))

        // Add Contributions tab
        contributionsFragment = ContributionsFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_USERNAME, userName)
            }
        }
        contributionsFragment?.let {
            fragmentList.add(it)
            titleList.add(getString(R.string.contributions_fragment).uppercase(Locale.ROOT))
        }

        viewPagerAdapter.setTabData(fragmentList, titleList)
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
                val screenShot = Utils.getScreenShot(rootView)
                if (screenShot == null) {
                    Log.e("ERROR", "ScreenShot is null")
                    return false
                }
                showAlert(screenShot)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showAlert(screenshot: Bitmap) {
        val view = layoutInflater.inflate(R.layout.image_alert_layout, null)
        val screenShotImage = view.findViewById<ImageView>(R.id.alert_image)
        val shareMessage = view.findViewById<TextView>(R.id.alert_text)

        screenShotImage.setImageBitmap(screenshot)
        shareMessage.setText(R.string.achievements_share_message)

        DialogUtil.showAlertDialog(
            this,
            null,
            null,
            getString(R.string.about_translate_proceed),
            getString(R.string.cancel),
            { shareScreen(screenshot) },
            {},
            view
        )
    }

    private fun shareScreen(bitmap: Bitmap) {
        try {
            val file = File(externalCacheDir, "screen.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            file.setReadable(true, false)

            val fileUri = FileProvider.getUriForFile(
                applicationContext,
                "$packageName.provider",
                file
            )

            grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val intent = Intent(Intent.ACTION_SEND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "image/png"
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_image_via)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_USERNAME, userName)
        outState.putBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions)
    }

    override fun onBackPressed() {
        if (contributionsFragment?.mediaDetailPagerFragment?.isVisible == true) {
            contributionsFragment?.backButtonClicked()
            binding.tabLayout.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

    fun setTabLayoutVisibility(isVisible: Boolean) {
        binding.tabLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    companion object {
        const val KEY_USERNAME = "username"
        const val KEY_SHOULD_SHOW_CONTRIBUTIONS = "shouldShowContributions"

        @JvmStatic
        fun startYourself(context: Context, userName: String, shouldShowContributions: Boolean) {
            val intent = Intent(context, ProfileActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(KEY_USERNAME, userName)
                putExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions)
            }
            context.startActivity(intent)
        }
    }
}