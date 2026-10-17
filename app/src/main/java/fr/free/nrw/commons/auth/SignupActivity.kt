package fr.free.nrw.commons.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import timber.log.Timber
import java.net.URLDecoder

class SignupActivity : BaseActivity() {
    private var webView: WebView? = null

    private var signupCompleted = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Signup Activity started")

        webView = WebView(this)
        applyEdgeToEdgeAllInsets(webView!!)
        with(webView!!) {
            setContentView(this)
            webViewClient = MyWebViewClient()
            // Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can
            // trust Wikimedia's site... right?
            settings.javaScriptEnabled = true
            loadUrl(BuildConfig.SIGNUP_LANDING_URL)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView!!.canGoBack()) {
                    webView!!.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    /**
     * Known bug in androidx.appcompat library version 1.1.0 being tracked here
     * https://issuetracker.google.com/issues/141132133
     * App tries to put light/dark theme to webview and crashes in the process
     * This code tries to prevent applying the theme when sdk is between api 21 to 25
     */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT <= 25 &&
            (resources.configuration.uiMode == applicationContext.resources.configuration.uiMode)
        ) return
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private fun sessionUsername(url: String): String? =
        try {
            extractUsername(CookieManager.getInstance().getCookie(url))
        } catch (e: Exception) {
            // CookieManager can throw if the WebView provider is unavailable
            Timber.e(e, "Unable to read WebView cookies")
            null
        }

    /**
     * Checks whether the account has been created and, if so, closes the signup screen and
     * hands the new username back to [LoginActivity].
     *
     * @return true if signup was detected as completed
     */
    @VisibleForTesting
    fun handleSignupSuccessIfNeeded(url: String?): Boolean {
        if (signupCompleted) return true
        if (url == null || !isSignupSuccessUrl(url)) return false

        Timber.d("Signup completed, URL is: %s", url)
        signupCompleted = true
        webView?.stopLoading()

        Toast.makeText(this, R.string.account_created, Toast.LENGTH_LONG).show()
        setResult(RESULT_OK, Intent().apply {
            sessionUsername(url)?.let {
                putExtra(CommonsApplication.LOGIN_USERNAME_INTENT_KEY, it)
            }
        })
        finish()
        return true
    }

    private inner class MyWebViewClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            if (handleSignupSuccessIfNeeded(url)) {
                Timber.d("Overriding URL %s", url)
                true
            } else {
                //If user clicks any other links in the webview
                Timber.d("Not overriding URL, URL is: %s", url)
                false
            }

        // Redirects that happen after submitting the signup form do not always go through
        // shouldOverrideUrlLoading, so also check every page that starts loading.
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            handleSignupSuccessIfNeeded(url)
        }
    }

    companion object {
        private val WIKIMEDIA_HOST_REGEX = Regex("""(^|\.)(wikimedia\.org|wmflabs\.org)$""")
        private val USERNAME_COOKIE_REGEX = Regex("""(?:^|;\s*)\w+UserName=([^;]+)""")

        /**
         * Whether [url] is the page MediaWiki redirects to once an account has been created.
         *
         * The exact redirect varies (mobile or desktop domain, extra parameters such as
         * `accountJustCreated=1`), so rather than comparing against a single URL this checks for
         * the `accountJustCreated` or `welcome=yes` parameters on a Wikimedia page.
         */
        @VisibleForTesting
        fun isSignupSuccessUrl(url: String): Boolean {
            if (url == BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL) return true
            val uri = Uri.parse(url)
            val host = uri.host ?: return false
            if (!WIKIMEDIA_HOST_REGEX.containsMatchIn(host)) return false
            return uri.getQueryParameter("accountJustCreated") == "1" ||
                uri.getQueryParameter("welcome") == "yes"
        }

        /**
         * Extracts the logged in username from a WebView cookie string, using the
         * `<wiki>UserName` cookie MediaWiki sets after login or account creation.
         */
        @VisibleForTesting
        fun extractUsername(cookies: String?): String? {
            val rawValue = cookies?.let { USERNAME_COOKIE_REGEX.find(it) }
                ?.groupValues?.get(1)?.trim() ?: return null
            val username = try {
                URLDecoder.decode(rawValue, "UTF-8")
            } catch (e: IllegalArgumentException) {
                rawValue
            }.replace('_', ' ').trim()
            return username.ifEmpty { null }
        }
    }
}
