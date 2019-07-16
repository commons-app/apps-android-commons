package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Completable;
import io.reactivex.Observable;
import timber.log.Timber;

import static android.accounts.AccountManager.ERROR_CODE_REMOTE_EXCEPTION;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;

/**
 * Manage the current logged in user session.
 */
@Singleton
public class SessionManager {
    private final Context context;
    private final MediaWikiApi mediaWikiApi;
    private Account currentAccount; // Unlike a savings account...  ;-)
    private JsonKvStore defaultKvStore;
    private static final String KEY_RAWUSERNAME = "rawusername";
    private Bundle userdata = new Bundle();

    @Inject
    public SessionManager(Context context,
                          MediaWikiApi mediaWikiApi,
                          @Named("default_preferences") JsonKvStore defaultKvStore) {
        this.context = context;
        this.mediaWikiApi = mediaWikiApi;
        this.currentAccount = null;
        this.defaultKvStore = defaultKvStore;
    }

    /**
     * Creata a new account
     *
     * @param response
     * @param username
     * @param rawusername
     * @param password
     */
    public void createAccount(@Nullable AccountAuthenticatorResponse response,
                              String username, String rawusername, String password) {

        Account account = new Account(username, BuildConfig.ACCOUNT_TYPE);
        userdata.putString(KEY_RAWUSERNAME, rawusername);
        boolean created = accountManager().addAccountExplicitly(account, password, userdata);

        Timber.d("account creation " + (created ? "successful" : "failure"));

        if (created) {
            if (response != null) {
                Bundle bundle = new Bundle();
                bundle.putString(KEY_ACCOUNT_NAME, username);
                bundle.putString(KEY_ACCOUNT_TYPE, BuildConfig.ACCOUNT_TYPE);


                response.onResult(bundle);
            }

        } else {
            if (response != null) {
                response.onError(ERROR_CODE_REMOTE_EXCEPTION, "");
            }
            Timber.d("account creation failure");
        }

        // FIXME: If the user turns it off, it shouldn't be auto turned back on
        ContentResolver.setSyncAutomatically(account, BuildConfig.CONTRIBUTION_AUTHORITY, true); // Enable sync by default!
        ContentResolver.setSyncAutomatically(account, BuildConfig.MODIFICATION_AUTHORITY, true); // Enable sync by default!
    }

    /**
     * @return Account|null
     */
    @Nullable
    public Account getCurrentAccount() {
        if (currentAccount == null) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE);
            if (allAccounts.length != 0) {
                currentAccount = allAccounts[0];
            }
        }
        return currentAccount;
    }

    @Nullable
    public String getUserName() {
        Account account = getCurrentAccount();
        return account == null ? null : account.name;
    }

    @Nullable
    public String getRawUserName() {
        Account account = getCurrentAccount();
        return account == null ? null : accountManager().getUserData(account, KEY_RAWUSERNAME);
    }

    public String getAuthorName(){
        return getRawUserName() == null ? getUserName() : getRawUserName();
    }


    @Nullable
    public String getPassword() {
        Account account = getCurrentAccount();
        return account == null ? null : accountManager().getPassword(account);
    }

    private AccountManager accountManager() {
        return AccountManager.get(context);
    }

    public Boolean revalidateAuthToken() {
        AccountManager accountManager = AccountManager.get(context);
        Account curAccount = getCurrentAccount();

        if (curAccount == null) {
            return false; // This should never happen
        }

        accountManager.invalidateAuthToken(BuildConfig.ACCOUNT_TYPE, null);
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
        return defaultKvStore.getString("getAuthCookie", null);
    }

    public boolean isUserLoggedIn() {
        return defaultKvStore.getBoolean("isUserLoggedIn", false);
    }

    public void forceLogin(Context context) {
        if (context != null) {
            LoginActivity.startYourself(context);
        }
    }

    /**
     * 1. Clears existing accounts from account manager
     * 2. Calls MediaWikiApi's logout function to clear cookies
     * @return
     */
    public Completable logout() {
        AccountManager accountManager = AccountManager.get(context);
        Account[] allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE);
        return Completable.fromObservable(Observable.fromArray(allAccounts)
                .map(a -> accountManager.removeAccount(a, null, null).getResult()))
                .doOnComplete(() -> {
                    mediaWikiApi.logout();
                    currentAccount = null;
                });
    }
}
