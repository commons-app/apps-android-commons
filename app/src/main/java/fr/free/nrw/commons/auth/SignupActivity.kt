package fr.free.nrw.commons.auth

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.theme.BaseActivity
import timber.log.Timber

class SignupActivity : BaseActivity() {
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Signup Activity started")

        webView = WebView(this)
        with(webView!!) {
            setContentView(this)
            webViewClient = MyWebViewClient()
            // Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can
            // trust Wikimedia's site... right?
            settings.javaScriptEnabled = true
            loadUrl(BuildConfig.SIGNUP_LANDING_URL)
        }
    }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
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

    private inner class MyWebViewClient : WebViewClient() {
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            if (url == BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL) {
                //Signup success, so clear cookies, notify user, and load LoginActivity again
                Timber.d("Overriding URL %s", url)

                Toast.makeText(
                    this@SignupActivity, R.string.account_created, Toast.LENGTH_LONG
                ).show()

                // terminate on task completion.
                finish()
                true
            } else {
                //If user clicks any other links in the webview
                Timber.d("Not overriding URL, URL is: %s", url)
                false
            }
    }
}
