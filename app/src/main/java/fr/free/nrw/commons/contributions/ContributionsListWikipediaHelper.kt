package fr.free.nrw.commons.contributions

import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.model.WikiSite

/**
 * Helper class handling Wikipedia integration dialogs and actions for contributions.
 */
class ContributionsListWikipediaHelper(
    private val fragment: Fragment,
    private val languageWikipediaSiteProvider: () -> WikiSite?,
) : WikipediaInstructionsDialogFragment.Callback {
    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    fun addImageToWikipedia(contribution: Contribution?) {
        val currentActivity = fragment.activity ?: return
        showAlertDialog(
            currentActivity,
            currentActivity.getString(R.string.add_picture_to_wikipedia_article_title),
            currentActivity.getString(R.string.add_picture_to_wikipedia_article_desc),
            {
                if (contribution != null) {
                    showAddImageToWikipediaInstructions(contribution)
                }
            },
            {},
        )
    }

    /**
     * Display confirmation dialog with instructions when the user tries to add image to wikipedia
     *
     * @param contribution
     */
    fun showAddImageToWikipediaInstructions(contribution: Contribution) {
        val fragmentManager = fragment.parentFragmentManager
        val dialogFragment = WikipediaInstructionsDialogFragment.newInstance(contribution)
        dialogFragment.callback =
            WikipediaInstructionsDialogFragment.Callback { contr: Contribution?, copyWikicode: Boolean ->
                onConfirmClicked(contr, copyWikicode)
            }
        dialogFragment.show(fragmentManager, "WikimediaFragment")
    }

    /**
     * Open the editor for the language Wikipedia
     *
     * @param contribution
     */
    override fun onConfirmClicked(
        contribution: Contribution?,
        copyWikicode: Boolean,
    ) {
        val context = fragment.context ?: return
        if (copyWikicode && contribution?.media?.wikiCode != null) {
            context.copyToClipboard("wikicode", contribution.media.wikiCode)
        }

        val pageTitle = contribution?.wikidataPlace?.getWikipediaPageTitle()
        val site = languageWikipediaSiteProvider()
        if (site != null && pageTitle != null) {
            val url = site.mobileUrl() + "/wiki/" + pageTitle
            handleWebUrl(context, url.toUri())
        }
    }
}
