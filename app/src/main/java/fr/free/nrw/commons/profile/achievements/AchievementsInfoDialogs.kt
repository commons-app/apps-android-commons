package fr.free.nrw.commons.profile.achievements

import android.net.Uri
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleWebUrl

/**
 * Shows the dialogs behind the info icons of the achievements screen.
 *
 * @param fragment the fragment that hosts the achievements screen
 */
class AchievementsInfoDialogs(
    private val fragment: Fragment,
) {
    /**
     * To invoke the AlertDialog on clicking info button
     */
    fun showInfoDialog() {
        launchAlert(
            fragment.resources.getString(R.string.Achievements),
            fragment.resources.getString(R.string.achievements_info_message),
        )
    }

    /**
     * Explains the uploaded images count, with a link to the Commons project scope.
     */
    fun showUploadInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.images_uploaded),
            fragment.resources.getString(R.string.images_uploaded_explanation),
            IMAGES_UPLOADED_URL,
        )
    }

    /**
     * Explains the reverted images percentage, with a link to the Commons deletion policy.
     */
    fun showRevertedInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.image_reverts),
            fragment.resources.getString(R.string.images_reverted_explanation),
            IMAGES_REVERT_URL,
        )
    }

    /**
     * Explains the images used by wikis count, with a link to the Wikipedia image style guide.
     */
    fun showUsedByWikiInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.images_used_by_wiki),
            fragment.resources.getString(R.string.images_used_explanation),
            IMAGES_USED_URL,
        )
    }

    /**
     * Explains the Wikidata edits count, with a link to the Wikidata image property.
     */
    fun showImagesViaNearbyInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.statistics_wikidata_edits),
            fragment.resources.getString(R.string.images_via_nearby_explanation),
            IMAGES_NEARBY_PLACES_URL,
        )
    }

    /**
     * Explains the featured images count, with a link to Commons featured pictures.
     */
    fun showFeaturedImagesInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.statistics_featured),
            fragment.resources.getString(R.string.images_featured_explanation),
            IMAGES_FEATURED_URL,
        )
    }

    /**
     * Explains the thanks received count, with a link to the Thanks extension.
     */
    fun showThanksReceivedInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.statistics_thanks),
            fragment.resources.getString(R.string.thanks_received_explanation),
            THANKS_URL,
        )
    }

    /**
     * Explains the quality images count, with a link to Commons quality images.
     */
    fun showQualityImagesInfo() {
        launchAlertWithHelpLink(
            fragment.resources.getString(R.string.statistics_quality),
            fragment.resources.getString(R.string.quality_images_info),
            QUALITY_IMAGE_URL,
        )
    }

    /**
     * takes title and message as input to display alerts
     * @param title
     * @param message
     */
    private fun launchAlert(
        title: String,
        message: String,
    ) {
        showAlertDialog(
            fragment.requireActivity(),
            title,
            message,
            fragment.getString(R.string.ok),
            {},
        )
    }

    /**
     * Launch Alert with a READ MORE button and clicking it open a custom webpage
     */
    private fun launchAlertWithHelpLink(
        title: String,
        message: String,
        helpLinkUrl: String,
    ) {
        showAlertDialog(
            fragment.requireActivity(),
            title,
            message,
            fragment.getString(R.string.ok),
            fragment.getString(R.string.read_help_link),
            {},
            { handleWebUrl(fragment.requireContext(), Uri.parse(helpLinkUrl)) },
            null,
        )
    }

    companion object {
        /**
         * Help link URLs
         */
        internal const val IMAGES_UPLOADED_URL = "https://commons.wikimedia.org/wiki/Commons:Project_scope"

        // Kept on one line so the URL stays searchable.
        @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
        internal const val IMAGES_REVERT_URL = "https://commons.wikimedia.org/wiki/Commons:Deletion_policy#Reasons_for_deletion"
        internal const val IMAGES_USED_URL = "https://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style/Images"
        internal const val IMAGES_NEARBY_PLACES_URL = "https://www.wikidata.org/wiki/Property:P18"
        internal const val IMAGES_FEATURED_URL = "https://commons.wikimedia.org/wiki/Commons:Featured_pictures"
        internal const val QUALITY_IMAGE_URL = "https://commons.wikimedia.org/wiki/Commons:Quality_images"
        internal const val THANKS_URL = "https://www.mediawiki.org/wiki/Extension:Thanks"
    }
}
