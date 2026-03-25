package fr.free.nrw.commons

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.theme.CommonsAppTheme
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.handleWebUrl

/**
 * Represents about screen of this app
 */
class AboutActivity : BaseActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val versionName = applicationContext.getVersionNameWithSha()

        setContent {
            CommonsAppTheme{
                 Surface(
                     modifier = Modifier.fillMaxSize(),
                     color = MaterialTheme.colorScheme.background
                 ) {
                     AboutScreen(
                         versionName = versionName,
                         onNavigateUp = { onBackPressedDispatcher.onBackPressed() },
                         onShareClicked = { shareApp() },
                         onFacebookClicked = { launchFacebook(this) },
                         onGithubClicked = { launchGithub(this) },
                         onWebsiteClicked = { launchWebsite(this) },
                         onRateUsClicked = { launchRatings(this) },
                         onUserGuideClicked = { launchUserGuide(this) },
                         onPrivacyPolicyClicked = { launchPrivacyPolicy(this) },
                         onTranslateProceed = { langCode -> launchTranslate(this, langCode) },
                         onCreditsClicked = { launchCredits(this) },
                         onFaqClicked = { launchFrequentlyAskedQuesions(this) }
                     )
                 }
             }
        }
    }
    private fun shareApp() {
        val shareText = getString(R.string.share_text).format(Urls.PLAY_STORE_URL_PREFIX + packageName)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    private fun launchFacebook(context: Context) {
        try{
            val intent = Intent(ACTION_VIEW, Urls.FACEBOOK_APP_URL.toUri())
            intent.setPackage(Urls.FACEBOOK_PACKAGE_NAME)
            context.startActivity(intent)
        } catch (e: Exception) {
            handleWebUrl(context, Urls.FACEBOOK_WEB_URL.toUri())
        }
    }

    private fun launchGithub(context: Context) {
        try{
            val intent = Intent(ACTION_VIEW, Urls.GITHUB_REPO_URL.toUri())
            intent.setPackage(Urls.GITHUB_PACKAGE_NAME)
            context.startActivity(intent)
        } catch (e: Exception) {
            handleWebUrl(context, Urls.GITHUB_REPO_URL.toUri())
        }
    }

    private fun launchWebsite(context: Context) {
        handleWebUrl(context, Urls.WEBSITE_URL.toUri())
    }

    private fun launchRatings(context: Context) {
        try {
            context.startActivity(
                Intent(
                    ACTION_VIEW,
                    (Urls.PLAY_STORE_PREFIX + packageName).toUri()
                )
            )
        } catch (_: ActivityNotFoundException) {
            handleWebUrl(context, (Urls.PLAY_STORE_URL_PREFIX + packageName).toUri())
        }
    }

    private fun launchCredits(context: Context) {
        handleWebUrl(context, Urls.CREDITS_URL.toUri())
    }

    private fun launchUserGuide(context: Context) {
        handleWebUrl(context, Urls.USER_GUIDE_URL.toUri())
    }

    private fun launchPrivacyPolicy(context: Context) {
        handleWebUrl(context, BuildConfig.PRIVACY_POLICY_URL.toUri())    }

    private fun launchFrequentlyAskedQuesions(context: Context) {
        handleWebUrl(context, Urls.FAQ_URL.toUri())
    }
    private fun launchTranslate(context: Context, langCode: String) {
        handleWebUrl(context, (Urls.TRANSLATE_WIKI_URL + langCode).toUri())
    }
}
