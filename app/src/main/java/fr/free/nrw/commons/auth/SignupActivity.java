package fr.free.nrw.commons.auth;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.theme.BaseActivity;
import timber.log.Timber;

public class SignupActivity extends BaseActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Signup Activity started");

        getSupportActionBar().hide();

        webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = webView.getSettings();
        //Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can trust Wikimedia's site... right?
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(CommonsApplication.ROOT_MOBILE_URL + "/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes");
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals(CommonsApplication.ROOT_MOBILE_URL + "/w/index.php?title=Main_Page&welcome=yes")) {
                //Signup success, so clear cookies, notify user, and load LoginActivity again
                Timber.d("Overriding URL %s", url);

                Toast toast = Toast.makeText(getApplicationContext(), "Account created!", Toast.LENGTH_LONG);
                toast.show();

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
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
}
