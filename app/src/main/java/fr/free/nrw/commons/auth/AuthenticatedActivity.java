package fr.free.nrw.commons.auth;

import android.os.Bundle;

import javax.inject.Inject;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

import static fr.free.nrw.commons.auth.AccountUtil.AUTH_COOKIE;

public abstract class AuthenticatedActivity extends NavigationBaseActivity {

    @Inject SessionManager sessionManager;
    @Inject
    MediaWikiApi mediaWikiApi;
    private String authCookie;

    protected void requestAuthToken() {
        if (authCookie != null) {
            onAuthCookieAcquired(authCookie);
            return;
        }
        authCookie = sessionManager.getAuthCookie();
        if (authCookie != null) {
            onAuthCookieAcquired(authCookie);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            authCookie = savedInstanceState.getString(AUTH_COOKIE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AUTH_COOKIE, authCookie);
    }

    protected abstract void onAuthCookieAcquired(String authCookie);

    protected abstract void onAuthFailure();
}
