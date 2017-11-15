package fr.free.nrw.commons.auth;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wuman.android.auth.OAuthManager;

import butterknife.ButterKnife;
import fr.free.nrw.commons.OAuthUtil;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;

public class OAuthLoginActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.isDarkTheme(this) ? R.style.DarkAppTheme : R.style.LightAppTheme);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth_login);
        ButterKnife.bind(this);

        authenticate();
    }

    private void authenticate() {
        OAuthManager oAuthManager = OAuthUtil.getOAuthManager(this, getFragmentManager());
        oAuthManager.authorize10a("wikimedia-commons", null, null);
    }
}
