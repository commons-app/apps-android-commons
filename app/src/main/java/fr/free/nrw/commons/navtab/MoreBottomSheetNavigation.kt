package fr.free.nrw.commons.navtab

import android.content.Intent
import android.net.Uri
import fr.free.nrw.commons.AboutActivity
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.review.ReviewActivity
import fr.free.nrw.commons.settings.SettingsActivity
import fr.free.nrw.commons.startWelcome

/** Handles destinations opened from the more bottom sheet. */
internal class MoreBottomSheetNavigation(
    private val fragment: MoreBottomSheetFragment,
) {
    companion object {
        private const val GITHUB_ISSUES_URL =
            "https://github.com/commons-app/apps-android-commons/issues"
    }

    /** Opens GitHub issues. */
    fun onFeedbackGithubClicked() {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(GITHUB_ISSUES_URL)
            }
        fragment.startActivity(intent)
    }

    /** Opens the about screen. */
    fun onAboutClicked() {
        val intent =
            Intent(fragment.activity, AboutActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        fragment.requireActivity().startActivity(intent)
    }

    /** Opens the welcome tutorial. */
    fun onTutorialClicked() {
        fragment.requireContext().startWelcome()
    }

    /** Opens settings. */
    fun onSettingsClicked() {
        val intent =
            Intent(fragment.activity, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        fragment.requireActivity().startActivity(intent)
    }

    /** Opens the current user profile. */
    fun onProfileClicked(userName: String) {
        ProfileActivity.startYourself(fragment.requireActivity(), userName, false)
    }

    /** Opens peer review. */
    fun onPeerReviewClicked() {
        ReviewActivity.startYourself(fragment.requireActivity())
    }
}
