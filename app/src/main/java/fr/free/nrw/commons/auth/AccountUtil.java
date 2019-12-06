package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import androidx.annotation.Nullable;

import fr.free.nrw.commons.BuildConfig;
import timber.log.Timber;

public class AccountUtil {

    public static final String AUTH_TOKEN_TYPE = "CommonsAndroid";

    public AccountUtil() {
    }

    /**
     * @return Account|null
     */
    @Nullable
    public static Account account(Context context) {
        try {
            Account[] accounts = accountManager(context).getAccountsByType(BuildConfig.ACCOUNT_TYPE);
            if (accounts.length > 0) {
                return accounts[0];
            }
        } catch (SecurityException e) {
            Timber.e(e);
        }
        return null;
    }

    @Nullable
    public static String getUserName(Context context) {
        Account account = account(context);
        return account == null ? null : account.name;
    }

    private static AccountManager accountManager(Context context) {
        return AccountManager.get(context);
    }
}
