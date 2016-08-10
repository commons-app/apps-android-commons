package fr.free.nrw.commons.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import fr.free.nrw.commons.R;

public class SignupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SignupActivity", "Signup Activity started");

        WebView webView = new WebView(this);
        setContentView(webView);

        webView.setWebViewClient(new MyWebViewClient());
        //myWebView.loadUrl("https://commons.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page");
        //Mobile page, looks better than the above
        webView.loadUrl("https://commons.m.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes");

        //After Create Account button is pressed within WebView, it brings user to https://commons.m.wikimedia.org/w/index.php?title=Main_Page&welcome=yes. So can we just override that URL?
        //Do we NEED to enable JS? Validation seems to work fine here
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.equals("https://commons.m.wikimedia.org/w/index.php?title=Main_Page&welcome=yes")) {
                // Signup success, so load LoginActivity again
                Log.d("SignupActivity", "Overriding URL" + url);
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                return true;
            } else {
                Log.d("SignupActivity", "Not overriding URL, URL is: " + url);
                return false;
            }

        }

    }
}
