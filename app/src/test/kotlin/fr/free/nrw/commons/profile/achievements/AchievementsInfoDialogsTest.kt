package fr.free.nrw.commons.profile.achievements

import android.app.AlertDialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Looper
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AchievementsInfoDialogsTest {
    private lateinit var activity: ProfileActivity
    private lateinit var infoDialogs: AchievementsInfoDialogs

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val fragment = Fragment()
        activity.supportFragmentManager
            .beginTransaction()
            .add(fragment, null)
            .commitNowAllowingStateLoss()
        infoDialogs = AchievementsInfoDialogs(fragment)
    }

    @Test
    fun showInfoDialogShowsAchievementsExplanationWithoutHelpLink() {
        infoDialogs.showInfoDialog()

        val dialog = latestDialog()
        assertTitleAndMessage(R.string.Achievements, R.string.achievements_info_message)
        assertEquals(View.GONE, dialog.getButton(DialogInterface.BUTTON_NEGATIVE).visibility)
    }

    @Test
    fun showUploadInfoLinksToProjectScope() {
        infoDialogs.showUploadInfo()

        assertTitleAndMessage(R.string.images_uploaded, R.string.images_uploaded_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.IMAGES_UPLOADED_URL)
    }

    @Test
    fun showRevertedInfoLinksToDeletionPolicy() {
        infoDialogs.showRevertedInfo()

        assertTitleAndMessage(R.string.image_reverts, R.string.images_reverted_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.IMAGES_REVERT_URL)
    }

    @Test
    fun showUsedByWikiInfoLinksToImageStyleGuide() {
        infoDialogs.showUsedByWikiInfo()

        assertTitleAndMessage(R.string.images_used_by_wiki, R.string.images_used_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.IMAGES_USED_URL)
    }

    @Test
    fun showImagesViaNearbyInfoLinksToWikidataImageProperty() {
        infoDialogs.showImagesViaNearbyInfo()

        assertTitleAndMessage(R.string.statistics_wikidata_edits, R.string.images_via_nearby_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.IMAGES_NEARBY_PLACES_URL)
    }

    @Test
    fun showFeaturedImagesInfoLinksToFeaturedPictures() {
        infoDialogs.showFeaturedImagesInfo()

        assertTitleAndMessage(R.string.statistics_featured, R.string.images_featured_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.IMAGES_FEATURED_URL)
    }

    @Test
    fun showThanksReceivedInfoLinksToThanksExtension() {
        infoDialogs.showThanksReceivedInfo()

        assertTitleAndMessage(R.string.statistics_thanks, R.string.thanks_received_explanation)
        assertHelpLinkOpens(AchievementsInfoDialogs.THANKS_URL)
    }

    @Test
    fun showQualityImagesInfoLinksToQualityImages() {
        infoDialogs.showQualityImagesInfo()

        assertTitleAndMessage(R.string.statistics_quality, R.string.quality_images_info)
        assertHelpLinkOpens(AchievementsInfoDialogs.QUALITY_IMAGE_URL)
    }

    private fun latestDialog(): AlertDialog {
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowAlertDialog.getLatestAlertDialog()
    }

    private fun assertTitleAndMessage(
        @StringRes title: Int,
        @StringRes message: Int,
    ) {
        val shadowDialog = shadowOf(latestDialog())
        assertEquals(activity.getString(title), shadowDialog.title.toString())
        assertEquals(activity.getString(message), shadowDialog.message.toString())
    }

    private fun assertHelpLinkOpens(url: String) {
        val dialog = latestDialog()
        val helpButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        assertEquals(activity.getString(R.string.read_help_link), helpButton.text.toString())

        helpButton.performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(Uri.parse(url), shadowOf(activity).nextStartedActivity.data)
    }
}
