package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Completable;
import io.reactivex.Observable;
import timber.log.Timber;

import static fr.free.nrw.commons.auth.AccountUtil.ACCOUNT_TYPE;

/**
 * Manage the current logged in user session.
 */
public class SessionManager {
    private final Context context;
    private final MediaWikiApi mediaWikiApi;
    private Account currentAccount; // Unlike a savings account...  ;-)
    private SharedPreferences sharedPreferences;

    public SessionManager(Context context, MediaWikiApi mediaWikiApi, SharedPreferences sharedPreferences) {
        this.context = context;
        this.mediaWikiApi = mediaWikiApi;
        this.currentAccount = null;
        this.sharedPreferences = sharedPreferences;
    }

    /**
     * @return Account|null
     */
    public Account getCurrentAccount() {
        if (currentAccount == null) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] allAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
            if (allAccounts.length != 0) {
                currentAccount = allAccounts[0];
            }
        }
        return currentAccount;
    }

    public Boolean revalidateAuthToken() {
        AccountManager accountManager = AccountManager.get(context);
        Account curAccount = getCurrentAccount();

        if (curAccount == null) {
            return false; // This should never happen
        }

        accountManager.invalidateAuthToken(ACCOUNT_TYPE, mediaWikiApi.getAuthCookie());
        String authCookie = getAuthCookie();

        if (authCookie == null) {
            return false;
        }
        mediaWikiApi.setAuthCookie(authCookie);
        return true;
    }

    public String getAuthCookie() {
        if (!isUserLoggedIn()) {
            Timber.e("User is not logged in");
            return null;
        } else {
            String authCookie = getCachedAuthCookie();
            if (authCookie == null) {
                Timber.e("Auth cookie is null even after login");
            }
            return authCookie;
        }
    }

    public String getCachedAuthCookie() {
        return sharedPreferences.getString("getAuthCookie", null);
    }

    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isUserLoggedIn", false);
    }

    public void forceLogin(Context context) {
        if (context != null) {
            LoginActivity.startYourself(context);
        }
    }

    public Completable clearAllAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        Account[] allAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        return Completable.fromObservable(Observable.fromArray(allAccounts)
                .map(a -> accountManager.removeAccount(a, null, null).getResult()))
                .doOnComplete(() -> currentAccount = null);
    }
}
