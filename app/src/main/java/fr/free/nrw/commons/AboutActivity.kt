package fr.free.nrw.commons

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.core.net.toUri
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.databinding.ActivityAboutBinding
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopInsets
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.utils.setUnderlinedText
import java.util.Collections

/**
 * Represents about screen of this app
 */
class AboutActivity : BaseActivity() {
    /*
         This View Binding class is auto-generated for each xml file. The format is usually the name
         of the file with PascalCasing (The underscore characters will be ignored).
         More information is available at https://developer.android.com/topic/libraries/view-binding
     */
    private var binding: ActivityAboutBinding? = null

    /**
     * This method helps in the creation About screen
     *
     * @param savedInstanceState Data bundle
     */
    @SuppressLint("StringFormatInvalid")
    // Some translations (e.g. fo, and the qq documentation locale) lack the format
    // placeholder; translations are managed on translatewiki, so suppress rather than edit.
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
          Instead of just setting the view with the xml file. We need to use View Binding class.
         */

        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        applyEdgeToEdgeTopInsets(binding!!.toolbarLayout)
        setContentView(view)

        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val aboutText = getString(R.string.about_license)
        /*
          We can then access all the views by just using the id names like this.
          camelCasing is used with underscore characters being ignored.
         */
        binding!!.aboutLicense.setHtmlText(aboutText)

        val improveText =
            String.format(getString(R.string.about_improve), Urls.NEW_ISSUE_URL)
        binding!!.aboutImprove.setHtmlText(improveText)
        binding!!.aboutVersion.text = applicationContext.getVersionNameWithSha()

        binding!!.aboutFaq.setUnderlinedText(R.string.about_faq)
        binding!!.aboutRateUs.setUnderlinedText(R.string.about_rate_us)
        binding!!.aboutUserGuide.setUnderlinedText(R.string.user_guide)
        binding!!.aboutPrivacyPolicy.setUnderlinedText(R.string.about_privacy_policy)
        binding!!.aboutTranslate.setUnderlinedText(R.string.about_translate)
        binding!!.aboutCredits.setUnderlinedText(R.string.about_credits)

        /*
          To set listeners, we can create a separate method and use lambda syntax.
         */
        val linkHandler = AboutLinkHandler(this)
        binding!!.facebookLaunchIcon.setOnClickListener(linkHandler::launchFacebook)
        binding!!.githubLaunchIcon.setOnClickListener(linkHandler::launchGithub)
        binding!!.websiteLaunchIcon.setOnClickListener(linkHandler::launchWebsite)
        binding!!.aboutRateUs.setOnClickListener(linkHandler::launchRatings)
        binding!!.aboutCredits.setOnClickListener(linkHandler::launchCredits)
        binding!!.aboutPrivacyPolicy.setOnClickListener(linkHandler::launchPrivacyPolicy)
        binding!!.aboutUserGuide.setOnClickListener(linkHandler::launchUserGuide)
        binding!!.aboutFaq.setOnClickListener(linkHandler::launchFrequentlyAskedQuesions)
        binding!!.aboutTranslate.setOnClickListener(::launchTranslate)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_about, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.share_app_icon -> {
                val shareText =
                    String.format(
                        getString(R.string.share_text),
                        Urls.PLAY_STORE_URL_PREFIX + this.packageName,
                    )
                val sendIntent = Intent()
                sendIntent.setAction(Intent.ACTION_SEND)
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText)
                sendIntent.setType("text/plain")
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    // View.OnClickListener method reference, see onCreate
    @Suppress("UnusedParameter")
    fun launchTranslate(view: View?) {
        val sortedLocalizedNamesRef = instance.languageLookUpTable!!.getCanonicalNames()
        Collections.sort(sortedLocalizedNamesRef)
        val languageAdapter =
            ArrayAdapter(
                this@AboutActivity,
                android.R.layout.simple_spinner_dropdown_item,
                sortedLocalizedNamesRef,
            )
        val spinner = Spinner(this@AboutActivity)
        spinner.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        spinner.adapter = languageAdapter
        spinner.gravity = 17
        spinner.setPadding(50, 0, 0, 0)

        val positiveButtonRunnable =
            Runnable {
                val langCode = instance.languageLookUpTable!!.getCodes()[spinner.selectedItemPosition]
                handleWebUrl(this@AboutActivity, (Urls.TRANSLATE_WIKI_URL + langCode).toUri())
            }
        showAlertDialog(
            this,
            getString(R.string.about_translate_title),
            getString(R.string.about_translate_message),
            getString(R.string.about_translate_proceed),
            getString(R.string.about_translate_cancel),
            positiveButtonRunnable,
            {},
            spinner,
        )
    }
}
