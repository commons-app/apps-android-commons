package fr.free.nrw.commons.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.AndroidRuntimeException
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.ActivityLogoutListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * SingleWebViewActivity is a reusable activity webView based on a given url (initial url) and
 * closes itself when a specified success URL is reached.
 */
class SingleWebViewActivity : ComponentActivity() {
    @Inject
    lateinit var cookieJar: CommonsCookieJar

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(VANISH_ACCOUNT_URL)
        val successUrl = intent.getStringExtra(VANISH_ACCOUNT_SUCCESS_URL)
        if (!areValidLaunchUrls(url, successUrl)) {
            Timber.w("Rejected invalid or untrusted URLs in SingleWebViewActivity")
            finish()
            return
        }
        val initialUrl = url!!
        val targetSuccessUrl = successUrl!!
        ApplicationlessInjection
            .getInstance(applicationContext)
            .commonsApplicationComponent
            .inject(this)
        setCookies(initialUrl)
        enableEdgeToEdge()
        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        modifier = Modifier,
                        title = { Text(getString(R.string.vanish_account)) },
                        navigationIcon = {
                            IconButton(
                                onClick = { finish() },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = getString(R.string.back),
                                )
                            }
                        },
                    )
                },
                content = { paddingValues ->
                    WebViewComponent(
                        url = initialUrl,
                        successUrl = targetSuccessUrl,
                        onSuccess = {
                            // Redirect the user to login screen like we do when the user logs out
                            val app = applicationContext as CommonsApplication
                            app.clearApplicationData(
                                applicationContext,
                                ActivityLogoutListener(activity = this, ctx = applicationContext),
                            )
                            finish()
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                    )
                },
            )
        }
    }

    /**
     * @param url The initial URL which we are loading in the WebView.
     * @param successUrl The URL that, when reached, triggers the `onSuccess` callback.
     * @param onSuccess A callback that is invoked when the current url of webView is successUrl.
     * @param modifier An optional [Modifier] to customize the layout or appearance of the WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun WebViewComponent(
        url: String,
        successUrl: String,
        onSuccess: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val webView = remember { mutableStateOf<WebView?>(null) }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                WebView(context).apply {
                    configureSecureWebView(this)
                    webViewClient = createWebViewClient(successUrl, onSuccess)
                    webChromeClient = createWebChromeClient()
                    loadUrl(url)
                }
            },
            update = {
                webView.value = it
            },
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSecureWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        try {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        } catch (e: AndroidRuntimeException) {
            Timber.w(e, "Unable to configure third-party cookie policy")
        }
    }

    private fun createWebViewClient(
        successUrl: String,
        onSuccess: () -> Unit,
    ): WebViewClient =
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val targetUri = request?.url
                val targetUrl = targetUri?.toString()
                return when (decideNavigation(targetUrl, request?.isForMainFrame == true, successUrl)) {
                    NavigationDecision.CLOSE_ON_SUCCESS -> {
                        Timber.d("Success URL detected. Closing WebView.")
                        onSuccess()
                        true
                    }
                    NavigationDecision.ALLOW_IN_WEBVIEW -> {
                        setCookies(targetUrl.orEmpty())
                        false
                    }
                    NavigationDecision.OPEN_EXTERNAL_BROWSER -> {
                        if (targetUri != null) {
                            handleWebUrl(this@SingleWebViewActivity, targetUri)
                        }
                        true
                    }
                    NavigationDecision.BLOCK -> {
                        Timber.w("Blocked untrusted navigation in SingleWebViewActivity")
                        true
                    }
                }
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?,
            ) {
                super.onPageFinished(view, url)
                if (isAllowedWebViewUrl(url)) {
                    setCookies(url.orEmpty())
                }
            }
        }

    private fun createWebChromeClient(): WebChromeClient =
        object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if (BuildConfig.DEBUG) {
                    Timber.d(
                        "WebView console (line %d of %s)",
                        message.lineNumber(),
                        message.sourceId(),
                    )
                }
                return true
            }
        }

    /**
     * Sets cookies for the given trusted Wikimedia HTTPS URL using the cookies stored in
     * [CommonsCookieJar]. Untrusted or non-HTTPS URLs are ignored.
     *
     * @param url The URL for which cookies need to be set.
     */
    private fun setCookies(url: String) {
        val httpUrl = url.toHttpUrlOrNull()
        if (httpUrl == null || !CommonsCookieJar.isTrustedWikimediaSsoUrl(httpUrl)) {
            return
        }
        try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieJar.loadForRequest(httpUrl)
            for (cookie in cookies) {
                cookieManager.setCookie(url, formatCookieForWebView(cookie, httpUrl))
            }
            cookieManager.flush()
        } catch (e: AndroidRuntimeException) {
            Timber.e(e, "Unable to set WebView cookies")
        }
    }

    internal enum class NavigationDecision {
        CLOSE_ON_SUCCESS,
        ALLOW_IN_WEBVIEW,
        OPEN_EXTERNAL_BROWSER,
        BLOCK,
    }

    companion object {
        private const val VANISH_ACCOUNT_URL = "VanishAccountUrl"
        private const val VANISH_ACCOUNT_SUCCESS_URL = "vanishAccountSuccessUrl"
        private const val CENTRALAUTH_COOKIE_PREFIX = "centralauth_"
        private const val SESSION_COOKIE_SUFFIX = "session"
        private const val TOKEN_COOKIE_SUFFIX = "token"

        internal fun areValidLaunchUrls(
            url: String?,
            successUrl: String?,
        ): Boolean = isAllowedWebViewUrl(url) && isAllowedWebViewUrl(successUrl)

        internal fun isAllowedWebViewUrl(rawUrl: String?): Boolean {
            val httpUrl = rawUrl?.toHttpUrlOrNull() ?: return false
            return CommonsCookieJar.isTrustedWikimediaSsoUrl(httpUrl)
        }

        internal fun decideNavigation(
            requestUrl: String?,
            isForMainFrame: Boolean,
            successUrl: String,
        ): NavigationDecision =
            when {
                requestUrl == null -> NavigationDecision.BLOCK
                requestUrl == successUrl -> NavigationDecision.CLOSE_ON_SUCCESS
                isAllowedWebViewUrl(requestUrl) -> NavigationDecision.ALLOW_IN_WEBVIEW
                isForMainFrame && isExternalHttpsUrl(requestUrl) -> NavigationDecision.OPEN_EXTERNAL_BROWSER
                else -> NavigationDecision.BLOCK
            }

        private fun isExternalHttpsUrl(rawUrl: String): Boolean =
            rawUrl.toHttpUrlOrNull()?.isHttps == true && Uri.parse(rawUrl).host != null

        internal fun formatCookieForWebView(
            cookie: Cookie,
            targetUrl: HttpUrl,
        ): String {
            val builder =
                Cookie
                    .Builder()
                    .name(cookie.name)
                    .value(cookie.value)
                    .expiresAt(cookie.expiresAt)
                    .path(cookie.path)
                    .secure()
            if (CommonsCookieJar.domainMatches(targetUrl.host, cookie.domain)) {
                if (cookie.hostOnly) {
                    builder.hostOnlyDomain(cookie.domain)
                } else {
                    builder.domain(cookie.domain)
                }
            } else {
                builder.hostOnlyDomain(targetUrl.host)
            }
            if (cookie.httpOnly || isSensitiveCookieName(cookie.name)) {
                builder.httpOnly()
            }
            return builder.build().toString()
        }

        private fun isSensitiveCookieName(name: String): Boolean =
            name.startsWith(CENTRALAUTH_COOKIE_PREFIX, ignoreCase = true) ||
                name.endsWith(SESSION_COOKIE_SUFFIX, ignoreCase = true) ||
                name.endsWith(TOKEN_COOKIE_SUFFIX, ignoreCase = true)

        /**
         * Launch the WebViewActivity with the specified URL and success URL.
         * @param context The context from which the activity is launched.
         * @param url The initial URL to load in the WebView.
         * @param successUrl The URL that triggers the WebView to close when matched.
         */
        fun showWebView(
            context: Context,
            url: String,
            successUrl: String,
        ) {
            val intent =
                Intent(
                    context,
                    SingleWebViewActivity::class.java,
                ).apply {
                    putExtra(VANISH_ACCOUNT_URL, url)
                    putExtra(VANISH_ACCOUNT_SUCCESS_URL, successUrl)
                }
            context.startActivity(intent)
        }
    }
}
