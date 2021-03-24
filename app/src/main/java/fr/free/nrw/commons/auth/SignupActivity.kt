package fr.free.nrw.commons.auth

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Signup Activity started")

        webView = WebView(this)
        setContentView(webView)

        webView!!.webViewClient = MyWebViewClient()
        val webSettings = webView!!.settings
        /*Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can
         trust Wikimedia's site... right?*/
        webSettings.javaScriptEnabled = true

        webView!!.loadUrl(BuildConfig.SIGNUP_LANDING_URL)
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url == BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL) {
                //Signup success, so clear cookies, notify user, and load LoginActivity again
                Timber.d("Overriding URL %s", url)

                val toast = Toast.makeText(this@SignupActivity, R.string.account_created, Toast.LENGTH_LONG)
                toast.show()
                // terminate on task completion.
                finish()
                return true
            } else {
                //If user clicks any other links in the webview
                Timber.d("Not overriding URL, URL is: %s", url)
                return false
            }
        }
    }

    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
        }
    }
}