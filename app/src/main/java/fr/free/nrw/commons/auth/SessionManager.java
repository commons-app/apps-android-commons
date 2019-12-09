package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.login.LoginResult;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Manage the current logged in user session.
 */
@Singleton
public class SessionManager {
    private final Context context;
    private Account currentAccount; // Unlike a savings account...  ;-)
    private JsonKvStore defaultKvStore;
    private static final String KEY_RAWUSERNAME = "rawusername";

    @Inject
    public SessionManager(Context context,
                          @Named("default_preferences") JsonKvStore defaultKvStore) {
        this.context = context;
        this.currentAccount = null;
        this.defaultKvStore = defaultKvStore;
    }

    private boolean createAccount(@NonNull String userName, @NonNull String password) {
        Account account = getCurrentAccount();
        if (account == null || TextUtils.isEmpty(account.name) || !account.name.equals(userName)) {
            removeAccount();
            account = new Account(userName, BuildConfig.ACCOUNT_TYPE);
            return accountManager().addAccountExplicitly(account, password, null);
        }
        return true;
    }

    private void removeAccount() {
        Account account = getCurrentAccount();
        if (account != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager().removeAccountExplicitly(account);
            } else {
                //noinspection deprecation
                accountManager().removeAccount(account, null, null);
            }
        }
    }

    public void updateAccount(LoginResult result) {
        boolean accountCreated = createAccount(result.getUserName(), result.getPassword());
        if (accountCreated) {
            setPassword(result.getPassword());
        }
    }

    private void setPassword(@NonNull String password) {
        Account account = getCurrentAccount();
        if (account != null) {
            accountManager().setPassword(account, password);
        }
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

    public boolean doesAccountExist() {
        return getCurrentAccount() != null;
    }

    @Nullable
    public String getUserName() {
        Account account = getCurrentAccount();
        return account == null ? null : account.name;
    }

    @Nullable
    private String getRawUserName() {
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

    public boolean isUserLoggedIn() {
        return defaultKvStore.getBoolean("isUserLoggedIn", false);
    }

    void setUserLoggedIn(boolean isLoggedIn) {
        defaultKvStore.putBoolean("isUserLoggedIn", isLoggedIn);
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
                    currentAccount = null;
                });
    }
}
