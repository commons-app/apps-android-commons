package fr.free.nrw.commons.auth;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.webkit.WebView;

import fr.free.nrw.commons.R;

public class SignupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SignupActivity", "Signup Activity started");

        WebView webView = new WebView(this);
        setContentView(webView);
        //myWebView.loadUrl("https://commons.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page");

        //Mobile page, looks better than the above
        webView.loadUrl("https://commons.m.wikimedia.org/w/index.php?title=Special:CreateAccount&returnto=Main+Page&returntoquery=welcome%3Dyes");

        //After Create Account button is pressed within WebView, it brings user to https://commons.wikimedia.org/wiki/Main_Page. So can we just override that URL?
        //Do we NEED to enable JS? Validation seems to work fine here
    }

}
