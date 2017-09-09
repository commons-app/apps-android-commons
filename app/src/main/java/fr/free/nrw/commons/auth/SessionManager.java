package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import java.io.IOException;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Manage the current logged in user session.
 */
public class SessionManager {
    private final Context context;
    private final AccountUtil accountUtil;
    private final MediaWikiApi mediaWikiApi;
    private Account currentAccount; // Unlike a savings account...  ;-)

    public SessionManager(Context context, AccountUtil accountUtil, MediaWikiApi mediaWikiApi) {
        this.context = context;
        this.accountUtil = accountUtil;
        this.mediaWikiApi = mediaWikiApi;
        this.currentAccount = null;
    }

    /**
     * @return Account|null
     */
    public Account getCurrentAccount() {
        if (currentAccount == null) {
            AccountManager accountManager = AccountManager.get(context);
            Account[] allAccounts = accountManager.getAccountsByType(accountUtil.accountType());
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

        accountManager.invalidateAuthToken(accountUtil.accountType(), mediaWikiApi.getAuthCookie());
        try {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, "", false);
            mediaWikiApi.setAuthCookie(authCookie);
            return true;
        } catch (OperationCanceledException | NullPointerException | IOException | AuthenticatorException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Completable clearAllAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        Account[] allAccounts = accountManager.getAccountsByType(accountUtil.accountType());
        return Completable.fromObservable(Observable.fromArray(allAccounts)
                .map(a -> accountManager.removeAccount(a, null, null).getResult()))
                .doOnComplete(() -> currentAccount = null);
    }
}
