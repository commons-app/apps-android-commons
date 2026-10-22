package fr.free.nrw.commons.auth

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import timber.log.Timber

class SignupActivity : BaseActivity() {
    private var webView: WebView? = null
    private var isInErrorState = false
    private var lastErrorMessage: String? = null
    private var lastFailingUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Signup Activity started")

        webView = WebView(this)
        webView?.id = View.generateViewId()
        applyEdgeToEdgeAllInsets(webView!!)

        savedInstanceState?.let {
            isInErrorState = it.getBoolean(KEY_IS_ERROR_STATE, false)
            lastErrorMessage = it.getString(KEY_ERROR_MESSAGE)
            lastFailingUrl = it.getString(KEY_FAILING_URL)
        }

        with(webView!!) {
            setContentView(this)
            webViewClient = MyWebViewClient()

            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }

            settings.javaScriptEnabled = true

            if (isInErrorState && lastErrorMessage != null) {
                loadCustomErrorPage(lastErrorMessage, lastFailingUrl)
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

    /**
     * Known bug in androidx.appcompat library version 1.1.0 being tracked here
     * https://issuetracker.google.com/issues/141132133
     * App tries to put light/dark theme to webview and crashes in the process
     * This code tries to prevent applying the theme when sdk is between api 21 to 25
     */
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
        outState.putString(KEY_FAILING_URL, lastFailingUrl)
    }

    private inner class MyWebViewClient : WebViewClient() {
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String,
        ): Boolean =
            when (url) {
                BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL -> {
                    Timber.d("Overriding URL %s", url)

                    Toast
                        .makeText(
                            this@SignupActivity,
                            R.string.account_created,
                            Toast.LENGTH_LONG,
                        ).show()

                    finish()
                    true
                }

                RETRY_URL -> {
                    Timber.d("Retrying signup page load")
                    retryLastRequest()
                    true
                }

                else -> {
                    Timber.d("Not overriding URL, URL is: %s", url)
                    false
                }
            }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean = shouldOverrideUrlLoading(view, request.url.toString())

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            // Only replace the page when the main document fails, not for sub-resources
            if (!request.isForMainFrame) return

            val unknownError = getString(R.string.signup_webview_error_unknown)
            val errorMessage =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    error.description?.toString() ?: unknownError
                } else {
                    unknownError
                }
            val failingUrl = request.url?.toString()
            Timber.e("WebView error: $errorMessage for URL: $failingUrl")
            loadCustomErrorPage(errorMessage, failingUrl)
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String,
        ) {
            Timber.e("WebView error: $description ($errorCode) for URL: $failingUrl")
            loadCustomErrorPage(description, failingUrl)
        }
    }

    /**
     * Reloads the URL that failed, falling back to the signup landing page.
     */
    private fun retryLastRequest() {
        val targetUrl = lastFailingUrl ?: BuildConfig.SIGNUP_LANDING_URL
        isInErrorState = false
        lastErrorMessage = null
        lastFailingUrl = null
        webView?.loadUrl(targetUrl)
    }

    private fun loadCustomErrorPage(
        errorMessage: String?,
        failingUrl: String?,
    ) {
        val html = buildErrorPageHtml(errorMessage)
        webView?.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        isInErrorState = true
        lastErrorMessage = errorMessage
        lastFailingUrl = failingUrl
    }

    private fun buildErrorPageHtml(errorMessage: String?): String {
        val isDark = systemThemeUtils.isDeviceInNightMode()
        val message = TextUtils.htmlEncode(errorMessage ?: getString(R.string.signup_webview_error_unknown))
        return ERROR_PAGE_TEMPLATE
            .replace("%BG_COLOR%", if (isDark) "#303030" else "#fafafa")
            .replace("%TEXT_COLOR%", if (isDark) "#FFFFFF" else "#000000")
            .replace("%SECONDARY_TEXT_COLOR%", if (isDark) "#CCCCCC" else "#666666")
            .replace("%BUTTON_COLOR%", if (isDark) "#1e8cab" else "#0c609c")
            .replace("%TITLE%", getString(R.string.signup_webview_error_title))
            .replace("%MESSAGE%", message)
            .replace("%RETRY_URL%", RETRY_URL)
            .replace("%RETRY_LABEL%", getString(R.string.retry))
    }

    companion object {
        private const val KEY_IS_ERROR_STATE = "is_error_state"
        private const val KEY_ERROR_MESSAGE = "error_message"
        private const val KEY_FAILING_URL = "failing_url"

        /**
         * Custom scheme used by the retry button of the error page. It is intercepted in
         * [MyWebViewClient.shouldOverrideUrlLoading] to reload the page that failed.
         */
        private const val RETRY_URL = "commons-retry://reload"

        private val ERROR_PAGE_TEMPLATE =
            """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        background-color: %BG_COLOR%;
                        color: %TEXT_COLOR%;
                        text-align: center;
                        padding: 20px;
                        font-family: -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
                        margin: 0;
                    }
                    .error-icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                    }
                    .error-title {
                        font-size: 24px;
                        margin-bottom: 10px;
                        font-weight: bold;
                    }
                    .error-message {
                        margin-bottom: 20px;
                        color: %SECONDARY_TEXT_COLOR%;
                        line-height: 1.5;
                    }
                    .retry-button {
                        display: inline-block;
                        background-color: %BUTTON_COLOR%;
                        color: #FFFFFF;
                        padding: 12px 32px;
                        border-radius: 4px;
                        font-size: 16px;
                        text-decoration: none;
                        cursor: pointer;
                        margin-top: 20px;
                    }
                    .retry-button:active {
                        opacity: 0.8;
                    }
                </style>
            </head>
            <body>
                <div class="error-icon">&#9888;</div>
                <h2 class="error-title">%TITLE%</h2>
                <p class="error-message">%MESSAGE%</p>
                <a class="retry-button" href="%RETRY_URL%">%RETRY_LABEL%</a>
            </body>
            </html>
            """.trimIndent()
    }
}
