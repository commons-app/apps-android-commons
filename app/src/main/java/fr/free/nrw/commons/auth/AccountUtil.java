package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import timber.log.Timber;

public class AccountUtil {

    public static final String ACCOUNT_TYPE = "fr.free.nrw.commons";

    public static void createAccount(@Nullable AccountAuthenticatorResponse response,
                                     String username, String password) {

        Account account = new Account(username, ACCOUNT_TYPE);
        boolean created = accountManager().addAccountExplicitly(account, password, null);

        Timber.d("account creation " + (created ? "successful" : "failure"));

        if (created) {
            if (response != null) {
                Bundle bundle = new Bundle();
                bundle.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);


                response.onResult(bundle);
            }

        } else {
            if (response != null) {
                response.onError(AccountManager.ERROR_CODE_REMOTE_EXCEPTION, "");
            }
            Timber.d("account creation failure");
        }

        // FIXME: If the user turns it off, it shouldn't be auto turned back on
        ContentResolver.setSyncAutomatically(account, ContributionsContentProvider.AUTHORITY, true); // Enable sync by default!
        ContentResolver.setSyncAutomatically(account, ModificationsContentProvider.AUTHORITY, true); // Enable sync by default!
    }

    private static AccountManager accountManager() {
        return AccountManager.get(app());
    }

    @NonNull
    private static CommonsApplication app() {
        return CommonsApplication.getInstance();
    }

}
