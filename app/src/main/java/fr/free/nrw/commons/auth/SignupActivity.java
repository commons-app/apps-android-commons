package fr.free.nrw.commons.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import fr.free.nrw.commons.R;

public class SignupActivity extends Activity {

    private boolean otherPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SignupActivity", "Signup Activity started");
        otherPage = false;


        WebView webView = new WebView(this);
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
                otherPage = true;
                return false;
            }
        }
    }

    /*
    @Override
    public void onBackPressed() {
        if (otherPage == true) {
            //If we are in any page except the main signup page, back button should take us back to signup page
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        }
        else {
            //If we are in signup page, back button should take us back to LoginActivity
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }
    }
    */
}
