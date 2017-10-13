package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public abstract class AuthenticatedActivity extends NavigationBaseActivity {

    private String authCookie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            authCookie = savedInstanceState.getString("authCookie");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("authCookie", authCookie);
    }

    private void getAuthCookie(Account account, AccountManager accountManager) {
        Single.fromCallable(() -> accountManager.blockingGetAuthToken(account, "", false))
                .subscribeOn(Schedulers.io())
                .doOnError(Timber::e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this:: onAuthCookieAcquired,
                        throwable -> onAuthFailure());
    }

    private void addAccount(AccountManager accountManager) {
        Single.just(accountManager.addAccount(AccountUtil.ACCOUNT_TYPE, null, null, null, AuthenticatedActivity.this, null, null))
                .subscribeOn(Schedulers.io())
                .map(AccountManagerFuture::getResult)
                .doOnEvent((bundle, throwable) -> {
                    if (!bundle.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                        throw new RuntimeException("Bundle doesn't contain account-name key: "
                                + AccountManager.KEY_ACCOUNT_NAME);
                    }
                })
                .map(bundle -> bundle.getString(AccountManager.KEY_ACCOUNT_NAME))
                .doOnError(Timber::e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                            Account[] allAccounts = accountManager.getAccountsByType(AccountUtil.ACCOUNT_TYPE);
                            Account curAccount = allAccounts[0];
                            getAuthCookie(curAccount, accountManager);
                        },
                        throwable -> onAuthFailure());
    }

    protected void requestAuthToken() {
        if (authCookie != null) {
            onAuthCookieAcquired(authCookie);
            return;
        }
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = ((CommonsApplication)getApplication()).getCurrentAccount();
        if (curAccount == null) {
            addAccount(accountManager);
        } else {
            getAuthCookie(curAccount, accountManager);
        }
    }

    protected abstract void onAuthCookieAcquired(String authCookie);

    protected abstract void onAuthFailure();
}
