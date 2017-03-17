package fr.free.nrw.commons.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import fr.free.nrw.commons.theme.BaseActivity;

public class SignupActivity extends BaseActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SignupActivity", "Signup Activity started");

        webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = webView.getSettings();
        //Needed to refresh Captcha. Might introduce XSS vulnerabilities, but we can trust Wikimedia's site... right?
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl("https://commons.m.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes");
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals("https://commons.m.wikimedia.org/w/index.php?title=Main_Page&welcome=yes")) {
                //Signup success, so clear cookies, notify user, and load LoginActivity again
                Log.d("SignupActivity", "Overriding URL" + url);

                Toast toast = Toast.makeText(getApplicationContext(), "Account created!", Toast.LENGTH_LONG);
                toast.show();

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                return true;
            } else {
                //If user clicks any other links in the webview
                Log.d("SignupActivity", "Not overriding URL, URL is: " + url);
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
