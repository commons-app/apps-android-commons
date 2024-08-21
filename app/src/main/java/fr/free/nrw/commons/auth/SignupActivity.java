package fr.free.nrw.commons.auth;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.BaseActivity;
import timber.log.Timber;

public class SignupActivity extends BaseActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Signup Activity started");

        webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = webView.getSettings();
        /*Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can
         trust Wikimedia's site... right?*/
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(BuildConfig.SIGNUP_LANDING_URL);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals(BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL)) {
                //Signup success, so clear cookies, notify user, and load LoginActivity again
                Timber.d("Overriding URL %s", url);

                Toast toast = Toast.makeText(SignupActivity.this,
                        R.string.account_created, Toast.LENGTH_LONG);
                toast.show();
                // terminate on task completion.
                finish();
                return true;
            } else {
                //If user clicks any other links in the webview
                Timber.d("Not overriding URL, URL is: %s", url);
                return false;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Known bug in androidx.appcompat library version 1.1.0 being tracked here
     * https://issuetracker.google.com/issues/141132133
     * App tries to put light/dark theme to webview and crashes in the process
     * This code tries to prevent applying the theme when sdk is between api 21 to 25
     * @param overrideConfiguration
     */
    @Override
    public void applyOverrideConfiguration(final Configuration overrideConfiguration) {
        if (Build.VERSION.SDK_INT <= 25 &&
            (getResources().getConfiguration().uiMode == getApplicationContext().getResources().getConfiguration().uiMode)) {
            return;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }
}
