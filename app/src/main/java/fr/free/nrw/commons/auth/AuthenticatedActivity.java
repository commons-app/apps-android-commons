package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;

import javax.inject.Inject;

import fr.free.nrw.commons.theme.NavigationBaseActivity;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static fr.free.nrw.commons.auth.AccountUtil.ACCOUNT_TYPE;

public abstract class AuthenticatedActivity extends NavigationBaseActivity {

    @Inject SessionManager sessionManager;

    private String authCookie;
    
    private void getAuthCookie(Account account, AccountManager accountManager) {
        Single.fromCallable(() -> accountManager.blockingGetAuthToken(account, "", false))
                .subscribeOn(Schedulers.io())
                .doOnError(Timber::e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::onAuthCookieAcquired,
                        throwable -> onAuthFailure());
    }

    private void addAccount(AccountManager accountManager) {
        Single.just(accountManager.addAccount(ACCOUNT_TYPE, null, null,
                null, AuthenticatedActivity.this, null, null))
                .subscribeOn(Schedulers.io())
                .map(AccountManagerFuture::getResult)
                .doOnEvent((bundle, throwable) -> {
                    if (!bundle.containsKey(KEY_ACCOUNT_NAME)) {
                        throw new RuntimeException("Bundle doesn't contain account-name key: "
                                + KEY_ACCOUNT_NAME);
                    }
                })
                .map(bundle -> bundle.getString(KEY_ACCOUNT_NAME))
                .doOnError(Timber::e)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                            Account[] allAccounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
                            Account curAccount = allAccounts[0];
                            getAuthCookie(curAccount, accountManager);
                        },
                        throwable -> onAuthFailure());
    }

    protected void requestAuthToken() {
        if(authCookie != null) {
            onAuthCookieAcquired(authCookie);
            return;
        }
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = sessionManager.getCurrentAccount();
        if(curAccount == null) {
            addAccount(accountManager);
        } else {
            getAuthCookie(curAccount, accountManager);
        }
    }
    
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

    protected abstract void onAuthCookieAcquired(String authCookie);
    protected abstract void onAuthFailure();
}
