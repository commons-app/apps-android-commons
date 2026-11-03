package fr.free.nrw.commons.profile.achievements

import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.profile.achievements.LevelController.LevelInfo.Companion.from
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog

/**
 * Fills the achievements screen with the fetched statistics, level and badges.
 *
 * @param fragment the fragment that hosts the achievements screen
 * @param binding the views of the achievements screen
 * @param sessionManager used to tell whether the achievements belong to the logged-in user
 * @param userName the user whose achievements are shown
 */
class AchievementsStatsBinder(
    private val fragment: Fragment,
    private val binding: FragmentAchievementsBinding,
    private val sessionManager: SessionManager,
    private val userName: String?,
) {
    private lateinit var levelInfo: LevelController.LevelInfo

    /**
     * Shows the given achievements, works out the level from them and hides the progress bar.
     *
     * @param achievements the achievements to show, including the upload count
     */
    fun bind(achievements: Achievements) {
        if (binding.progressBar != null) {
            levelInfo =
                from(
                    achievements.imagesUploaded,
                    achievements.uniqueUsedImages,
                    achievements.notRevertPercentage,
                )
            inflateAchievements(achievements)
            setUploadProgress(achievements.imagesUploaded)
            setImageRevertPercentage(achievements.notRevertPercentage)
            binding.progressBar.visibility = View.GONE
        }
    }

    /**
     * This function is used to show badge on any view (button, imageView, etc)
     * @param view The View on which the badge will be displayed eg (button, imageView, etc)
     * @param count The number to be displayed inside the badge.
     * @param backgroundColor The badge background color. Default is R.attr.colorPrimary
     * @param badgeTextColor The badge text color. Default is R.attr.colorPrimary
     * @param badgeGravity The position of the badge [TOP_END,TOP_START,BOTTOM_END,BOTTOM_START]. Default is TOP_END
     * @return if the number is 0, then it will not create badge for it and hide the view
     * @see <a
     * href="https://developer.android.com/reference/com/google/android/material/badge/BadgeDrawable">BadgeDrawable
     * (Android Developer)</a>
     */
    fun showBadgesWithCount(
        view: View,
        count: Int,
        backgroundColor: Int = R.attr.colorPrimary,
        badgeTextColor: Int = R.attr.textEnabled,
        badgeGravity: Int = BadgeDrawable.TOP_END,
    ) {
        // https://stackoverflow.com/a/67742035
        if (count == 0) {
            view.visibility = View.GONE
            return
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(
            object :
                ViewTreeObserver.OnGlobalLayoutListener {
                /**
                 * Callback method to be invoked when the global layout state or the visibility of views
                 * within the view tree changes
                 */
                @ExperimentalBadgeUtils
                override fun onGlobalLayout() {
                    view.visibility = View.VISIBLE
                    val badgeDrawable = BadgeDrawable.create(fragment.requireActivity())
                    badgeDrawable.number = count
                    badgeDrawable.badgeGravity = badgeGravity
                    badgeDrawable.badgeTextColor = badgeTextColor
                    badgeDrawable.backgroundColor = backgroundColor
                    BadgeUtils.attachBadgeDrawable(badgeDrawable, view)
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                }
            },
        )
    }

    /**
     * used to the uploaded images progressbar
     * @param uploadCount
     */
    private fun setUploadProgress(uploadCount: Int) {
        if (uploadCount == 0) {
            setZeroAchievements()
        } else {
            binding.imagesUploadedProgressbar.visibility = View.VISIBLE
            binding.imagesUploadedProgressbar.progress =
                PERCENT * uploadCount / levelInfo.maxUploadCount
            binding.imageUploadedTVCount.text = uploadCount.toString() + "/" + levelInfo.maxUploadCount
        }
    }

    /**
     * Tells the user that there are no achievements yet and shows empty statistics.
     */
    private fun setZeroAchievements() {
        val message =
            if (sessionManager.userName != userName) {
                fragment.getString(R.string.no_achievements_yet, userName)
            } else {
                fragment.getString(R.string.you_have_no_achievements_yet)
            }
        showAlertDialog(
            fragment.requireActivity(),
            null,
            message,
            fragment.getString(R.string.ok),
            {},
        )

//        binding.imagesUploadedProgressbar.setVisibility(View.INVISIBLE);
//        binding.imageRevertsProgressbar.setVisibility(View.INVISIBLE);
//        binding.imagesUsedByWikiProgressBar.setVisibility(View.INVISIBLE);
        // binding.achievementBadgeImage.visibility = View.INVISIBLE // TODO
        binding.imagesUsedCount.setText(R.string.no_image)
        binding.imagesRevertedText.setText(R.string.no_image_reverted)
        binding.imagesUploadTextParam.setText(R.string.no_image_uploaded)
    }

    /**
     * used to set the non revert image percentage
     * @param notRevertPercentage
     */
    private fun setImageRevertPercentage(notRevertPercentage: Int) {
        binding.imageRevertsProgressbar.visibility = View.VISIBLE
        binding.imageRevertsProgressbar.progress = notRevertPercentage
        val revertPercentage = notRevertPercentage.toString()
        binding.imageRevertTVCount.text = "$revertPercentage%"
        binding.imagesRevertLimitText.text =
            fragment.resources.getString(R.string.achievements_revert_limit_message) +
            levelInfo.minNonRevertPercentage + "%"
    }

    /**
     * Used the inflate the fetched statistics of the images uploaded by user
     * and assign badge and level. Also stores the achievements level of the user in BasicKvStore to display in menu
     * @param achievements
     */
    private fun inflateAchievements(achievements: Achievements) {
        // Thanks Received Badge
        showBadgesWithCount(view = binding.thanksImageIcon, count = achievements.thanksReceived)

        // Featured Images Badge
        showBadgesWithCount(view = binding.featuredImageIcon, count = achievements.featuredImages)

        // Quality Images Badge
        showBadgesWithCount(view = binding.qualityImageIcon, count = achievements.qualityImages)

        binding.imagesUsedByWikiProgressBar.progress =
            PERCENT * achievements.uniqueUsedImages / levelInfo.maxUniqueImages
        binding.imagesUsedCount.text = (
            achievements.uniqueUsedImages.toString() + "/" +
                levelInfo.maxUniqueImages
        )

        binding.achievementLevel.text = fragment.getString(R.string.level, levelInfo.levelNumber)
        binding.achievementBadgeImage.setImageDrawable(
            VectorDrawableCompat.create(
                fragment.resources,
                R.drawable.badge,
                ContextThemeWrapper(fragment.activity, levelInfo.levelStyle).theme,
            ),
        )
        binding.achievementBadgeText.text = levelInfo.levelNumber.toString()
        val store = BasicKvStore(fragment.requireContext(), userName)
        store.putString("userAchievementsLevel", levelInfo.levelNumber.toString())
    }

    private companion object {
        // Progress bars on this screen go from 0 to 100.
        const val PERCENT = 100
    }
}
