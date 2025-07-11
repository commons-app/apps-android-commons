package fr.free.nrw.commons

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.databinding.ActivityAboutBinding
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import java.util.Collections
import androidx.core.net.toUri
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.utils.setUnderlinedText

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
    @SuppressLint("StringFormatInvalid")  //TODO:
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
          Instead of just setting the view with the xml file. We need to use View Binding class.
         */
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view: View = binding!!.root
        setContentView(view)

        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val aboutText = getString(R.string.about_license)
        /*
          We can then access all the views by just using the id names like this.
          camelCasing is used with underscore characters being ignored.
         */
        binding!!.aboutLicense.setHtmlText(aboutText)

        @SuppressLint("StringFormatMatches") // TODO:
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
        binding!!.facebookLaunchIcon.setOnClickListener(::launchFacebook)
        binding!!.githubLaunchIcon.setOnClickListener(::launchGithub)
        binding!!.websiteLaunchIcon.setOnClickListener(::launchWebsite)
        binding!!.aboutRateUs.setOnClickListener(::launchRatings)
        binding!!.aboutCredits.setOnClickListener(::launchCredits)
        binding!!.aboutPrivacyPolicy.setOnClickListener(::launchPrivacyPolicy)
        binding!!.aboutUserGuide.setOnClickListener(::launchUserGuide)
        binding!!.aboutFaq.setOnClickListener(::launchFrequentlyAskedQuesions)
        binding!!.aboutTranslate.setOnClickListener(::launchTranslate)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun launchFacebook(view: View?) {
        val intent: Intent
        try {
            intent = Intent(ACTION_VIEW, Urls.FACEBOOK_APP_URL.toUri())
            intent.setPackage(Urls.FACEBOOK_PACKAGE_NAME)
            startActivity(intent)
        } catch (e: Exception) {
            handleWebUrl(this, Urls.FACEBOOK_WEB_URL.toUri())
        }
    }

    fun launchGithub(view: View?) {
        val intent: Intent
        try {
            intent = Intent(ACTION_VIEW, Urls.GITHUB_REPO_URL.toUri())
            intent.setPackage(Urls.GITHUB_PACKAGE_NAME)
            startActivity(intent)
        } catch (e: Exception) {
            handleWebUrl(this, Urls.GITHUB_REPO_URL.toUri())
        }
    }

    fun launchWebsite(view: View?) {
        handleWebUrl(this, Urls.WEBSITE_URL.toUri())
    }

    fun launchRatings(view: View?) {
        try {
            startActivity(
                Intent(
                    ACTION_VIEW,
                    (Urls.PLAY_STORE_PREFIX + packageName).toUri()
                )
            )
        } catch (_: ActivityNotFoundException) {
            handleWebUrl(this, (Urls.PLAY_STORE_URL_PREFIX + packageName).toUri())
        }
    }

    fun launchCredits(view: View?) {
        handleWebUrl(this, Urls.CREDITS_URL.toUri())
    }

    fun launchUserGuide(view: View?) {
        handleWebUrl(this, Urls.USER_GUIDE_URL.toUri())
    }

    fun launchPrivacyPolicy(view: View?) {
        handleWebUrl(this, BuildConfig.PRIVACY_POLICY_URL.toUri())
    }

    fun launchFrequentlyAskedQuesions(view: View?) {
        handleWebUrl(this, Urls.FAQ_URL.toUri())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_about, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share_app_icon -> {
                val shareText = String.format(
                    getString(R.string.share_text),
                    Urls.PLAY_STORE_URL_PREFIX + this.packageName
                )
                val sendIntent = Intent()
                sendIntent.setAction(Intent.ACTION_SEND)
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText)
                sendIntent.setType("text/plain")
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun launchTranslate(view: View?) {
        val sortedLocalizedNamesRef = instance.languageLookUpTable!!.getCanonicalNames()
        Collections.sort(sortedLocalizedNamesRef)
        val languageAdapter = ArrayAdapter(
            this@AboutActivity,
            android.R.layout.simple_spinner_dropdown_item, sortedLocalizedNamesRef
        )
        val spinner = Spinner(this@AboutActivity)
        spinner.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        spinner.adapter = languageAdapter
        spinner.gravity = 17
        spinner.setPadding(50, 0, 0, 0)

        val positiveButtonRunnable = Runnable {
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
            spinner
        )
    }
}
