package fr.free.nrw.commons.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.ActivityLogoutListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import javax.inject.Inject

/**
 * SingleWebViewActivity is a reusable activity webView based on a given url(initial url) and
 * closes itself when a specified success URL is reached to success url.
 */
class SingleWebViewActivity : ComponentActivity() {
    @Inject
    lateinit var cookieJar: CommonsCookieJar

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(VANISH_ACCOUNT_URL)
        val successUrl = intent.getStringExtra(VANISH_ACCOUNT_SUCCESS_URL)
        if (url == null || successUrl == null) {
            finish()
            return
        }
        ApplicationlessInjection
            .getInstance(applicationContext)
            .commonsApplicationComponent
            .inject(this)
        setCookies(url)
        enableEdgeToEdge()
        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        modifier = Modifier,
                        title = { Text(getString(R.string.vanish_account)) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    // Close the WebView Activity if the user taps the back button
                                    finish()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    // TODO("Add contentDescription)
                                    contentDescription = ""
                                )
                            }
                        }
                    )
                },
                content = {
                    WebViewComponent(
                        url = url,
                        successUrl = successUrl,
                        onSuccess = {
                            //Redirect the user to login screen like we do when the user logout's
                            val app = applicationContext as CommonsApplication
                            app.clearApplicationData(
                                applicationContext,
                                ActivityLogoutListener(activity = this, ctx = applicationContext)
                            )
                            finish()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                    )
                }
            )
        }
    }


    /**
     * @param url The initial URL which we are loading in the WebView.
     * @param successUrl The URL that, when reached, triggers the `onSuccess` callback.
     * @param onSuccess A callback that is invoked when the current url of webView is successUrl.
     * This is used when we want to close when the webView once a success url is hit.
     * @param modifier An optional [Modifier] to customize the layout or appearance of the WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun WebViewComponent(
        url: String,
        successUrl: String,
        onSuccess: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val webView = remember { mutableStateOf<WebView?>(null) }
        AndroidView(
            modifier = modifier,
            factory = {
                WebView(it).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true

                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {

                            request?.url?.let { url ->
                                Timber.d("URL Loading: $url")
                                if (url.toString() == successUrl) {
                                    Timber.d("Success URL detected. Closing WebView.")
                                    onSuccess() // Close the activity
                                    return true
                                }
                                return false
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            setCookies(url.orEmpty())
                        }

                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Timber.d("Console: ${message.message()} -- From line ${message.lineNumber()} of ${message.sourceId()}")
                            return true
                        }
                    }

                    loadUrl(url)
                }
            },
            update = {
                webView.value = it
            }
        )

    }

    /**
     * Sets cookies for the given URL using the cookies stored in the `CommonsCookieJar`.
     *
     * @param url The URL for which cookies need to be set.
     */
    private fun setCookies(url: String) {
        CookieManager.getInstance().let {
            val cookies = cookieJar.loadForRequest(url.toHttpUrl())
            for (cookie in cookies) {
                it.setCookie(url, cookie.toString())
            }
        }
    }

    companion object {
        private const val VANISH_ACCOUNT_URL = "VanishAccountUrl"
        private const val VANISH_ACCOUNT_SUCCESS_URL = "vanishAccountSuccessUrl"

        /**
         * Launch the WebViewActivity with the specified URL and success URL.
         * @param context The context from which the activity is launched.
         * @param url The initial URL to load in the WebView.
         * @param successUrl The URL that triggers the WebView to close when matched.
         */
        fun showWebView(
            context: Context,
            url: String,
            successUrl: String
        ) {
            val intent = Intent(
                context,
                SingleWebViewActivity::class.java
            ).apply {
                putExtra(VANISH_ACCOUNT_URL, url)
                putExtra(VANISH_ACCOUNT_SUCCESS_URL, successUrl)
            }
            context.startActivity(intent)
        }
    }
}

