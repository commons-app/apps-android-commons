package fr.free.nrw.commons.auth

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import timber.log.Timber

class SignupActivity : BaseActivity() {
    private var webView: WebView? = null
    private var isInErrorState = false
    private var lastErrorMessage: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Signup Activity started")

        webView = WebView(this)
        applyEdgeToEdgeAllInsets(webView!!)

        savedInstanceState?.let {
            isInErrorState = it.getBoolean(KEY_IS_ERROR_STATE, false)
            lastErrorMessage = it.getString(KEY_ERROR_MESSAGE)
        }

        with(webView!!) {
            setContentView(this)
            webViewClient = MyWebViewClient()
            settings.javaScriptEnabled = true

            if (isInErrorState && lastErrorMessage != null) {
                this@SignupActivity.loadCustomErrorPage(lastErrorMessage)
            } else {
                loadUrl(BuildConfig.SIGNUP_LANDING_URL)
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView!!.canGoBack()) {
                        webView!!.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration) {
        if (Build.VERSION.SDK_INT <= 25 &&
            (resources.configuration.uiMode == applicationContext.resources.configuration.uiMode)
        ) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_ERROR_STATE, isInErrorState)
        outState.putString(KEY_ERROR_MESSAGE, lastErrorMessage)
    }

    private inner class MyWebViewClient : WebViewClient() {
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            if (url == BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL) {
                Timber.d("Overriding URL %s", url)
                Toast.makeText(
                    this@SignupActivity,
                    R.string.account_created,
                    Toast.LENGTH_LONG
                ).show()
                finish()
                true
            } else {
                Timber.d("Not overriding URL, URL is: %s", url)
                false
            }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean = shouldOverrideUrlLoading(view, request.url.toString())

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (!request.isForMainFrame) return

            val errorMessage = error.description?.toString() ?: "Unknown error"
            Timber.e("WebView error: $errorMessage")
            loadCustomErrorPage(errorMessage)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            Timber.e("WebView error: $description ($errorCode)")
            this@SignupActivity.loadCustomErrorPage(description)
        }
    }

    private fun loadCustomErrorPage(errorMessage: String?) {
        val isDark = systemThemeUtils.isDeviceInNightMode()
        val bgColor = if (isDark) "#303030" else "#fafafa"
        val textColor = if (isDark) "#FFFFFF" else "#000000"
        val secondaryTextColor = if (isDark) "#CCCCCC" else "#666666"
        val buttonColor = if (isDark) "#1e8cab" else "#0c609c"

        val html = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        background-color: $bgColor;
                        color: $textColor;
                        text-align: center;
                        padding: 20px;
                        font-family: sans-serif;
                        margin: 0;
                    }
                    .error-icon { font-size: 64px; margin-bottom: 20px; }
                    .error-title { font-size: 24px; margin-bottom: 10px; font-weight: bold; }
                    .error-message { margin-bottom: 20px; color: $secondaryTextColor; line-height: 1.5; }
                    .retry-button {
                        background-color: $buttonColor;
                        color: #FFFFFF;
                        padding: 12px 32px;
                        border: none;
                        border-radius: 4px;
                        font-size: 16px;
                        cursor: pointer;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="error-icon">&#9888;</div>
                <h2 class="error-title">Webpage not available</h2>
                <p class="error-message">$errorMessage</p>
                <button class="retry-button" onclick="window.location.reload()">Retry</button>
            </body>
            </html>
        """.trimIndent()

        webView?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        isInErrorState = true
        lastErrorMessage = errorMessage
    }

    companion object {
        private const val KEY_IS_ERROR_STATE = "is_error_state"
        private const val KEY_ERROR_MESSAGE = "error_message"
    }
}
