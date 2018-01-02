package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.auth.AccountUtil.ACCOUNT_TYPE;
import static fr.free.nrw.commons.auth.AccountUtil.AUTH_TOKEN_TYPE;

/**
 * Manage the current logged in user session.
 */
public class SessionManager {
    private final Context context;
    private final MediaWikiApi mediaWikiApi;
    private Account currentAccount; // Unlike a savings account...  ;-)

    public SessionManager(Context context, MediaWikiApi mediaWikiApi) {
        this.context = context;
        this.mediaWikiApi = mediaWikiApi;
        this.currentAccount = null;
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
        getAndSetAuthCookie().subscribeOn(Schedulers.io())
                .subscribe(authCookie -> {
                    mediaWikiApi.setAuthCookie(authCookie);
                });
        return true;
    }

    public Observable<String> getAndSetAuthCookie() {
        AccountManager accountManager = AccountManager.get(context);
        Account curAccount = getCurrentAccount();
        return Observable.fromCallable(() -> {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, AUTH_TOKEN_TYPE, false);
            if (authCookie == null) {
                Timber.d("Media wiki auth cookie is %s", mediaWikiApi.getAuthCookie());
                authCookie = mediaWikiApi.getAuthCookie();
                //authCookie = currentAccount.name + "|" + currentAccount.type + "|" + mediaWikiApi.getUserAgent();
                //mediaWikiApi.setAuthCookie(authCookie);

            }
            Timber.d("Auth cookie is %s", authCookie);
            return authCookie;
        }).onErrorReturn(throwable-> {
            Timber.e(throwable, "Auth cookie is still null :(");
            return null;
        });
    }

    public Completable clearAllAccounts() {
        AccountManager accountManager = AccountManager.get(context);
        Account[] allAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
        return Completable.fromObservable(Observable.fromArray(allAccounts)
                .map(a -> accountManager.removeAccount(a, null, null).getResult()))
                .doOnComplete(() -> currentAccount = null);
    }
}
