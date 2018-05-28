package fr.free.nrw.commons.auth;

import android.os.Bundle;

import javax.inject.Inject;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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

        showBlockStatus();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(AUTH_COOKIE, authCookie);
    }

    protected abstract void onAuthCookieAcquired(String authCookie);

    protected abstract void onAuthFailure();

    protected void showBlockStatus()
    {
        Observable.fromCallable(() -> mediaWikiApi.isUserBlocked())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter(result -> result)
                .subscribe(result -> {
                            ViewUtil.showSnackbar(findViewById(android.R.id.content), R.string.block_notification);
                        }
                );
    }
}
