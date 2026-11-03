package fr.free.nrw.commons

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.view.View
import androidx.core.net.toUri
import fr.free.nrw.commons.utils.handleWebUrl
import timber.log.Timber

/** Opens external destinations linked from the About screen. */
class AboutLinkHandler(
    private val activity: Activity,
) {
    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens Facebook, falling back to its web page if the app cannot open. */
    @Suppress("UnusedParameter")
    fun launchFacebook(view: View?) {
        runCatching {
            val intent = Intent(ACTION_VIEW, Urls.FACEBOOK_APP_URL.toUri())
            intent.setPackage(Urls.FACEBOOK_PACKAGE_NAME)
            activity.startActivity(intent)
        }.onFailure { e ->
            if (e !is Exception) {
                throw e
            }
            Timber.e(e, "Could not open the Facebook app, falling back to the web page")
            handleWebUrl(activity, Urls.FACEBOOK_WEB_URL.toUri())
        }
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the GitHub repository, falling back to the browser if the app cannot open. */
    @Suppress("UnusedParameter")
    fun launchGithub(view: View?) {
        runCatching {
            val intent = Intent(ACTION_VIEW, Urls.GITHUB_REPO_URL.toUri())
            intent.setPackage(Urls.GITHUB_PACKAGE_NAME)
            activity.startActivity(intent)
        }.onFailure { e ->
            if (e !is Exception) {
                throw e
            }
            Timber.e(e, "Could not open the GitHub app, falling back to the web page")
            handleWebUrl(activity, Urls.GITHUB_REPO_URL.toUri())
        }
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the project website. */
    @Suppress("UnusedParameter")
    fun launchWebsite(view: View?) {
        handleWebUrl(activity, Urls.WEBSITE_URL.toUri())
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the app rating page, falling back to the Play Store website. */
    @Suppress("UnusedParameter")
    fun launchRatings(view: View?) {
        try {
            activity.startActivity(
                Intent(
                    ACTION_VIEW,
                    (Urls.PLAY_STORE_PREFIX + activity.packageName).toUri(),
                ),
            )
        } catch (_: ActivityNotFoundException) {
            handleWebUrl(activity, (Urls.PLAY_STORE_URL_PREFIX + activity.packageName).toUri())
        }
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the contributor credits. */
    @Suppress("UnusedParameter")
    fun launchCredits(view: View?) {
        handleWebUrl(activity, Urls.CREDITS_URL.toUri())
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the user guide. */
    @Suppress("UnusedParameter")
    fun launchUserGuide(view: View?) {
        handleWebUrl(activity, Urls.USER_GUIDE_URL.toUri())
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the privacy policy for this build. */
    @Suppress("UnusedParameter")
    fun launchPrivacyPolicy(view: View?) {
        handleWebUrl(activity, BuildConfig.PRIVACY_POLICY_URL.toUri())
    }

    // View.OnClickListener method reference, see AboutActivity.onCreate
    /** Opens the frequently asked questions. */
    @Suppress("UnusedParameter")
    fun launchFrequentlyAskedQuesions(view: View?) {
        handleWebUrl(activity, Urls.FAQ_URL.toUri())
    }
}
