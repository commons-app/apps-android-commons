package fr.free.nrw.commons.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import fr.free.nrw.commons.BuildConfig;

import static fr.free.nrw.commons.auth.AccountUtil.AUTH_TOKEN_TYPE;

public class WikiAccountAuthenticator extends AbstractAccountAuthenticator {
    private static final String[] SYNC_AUTHORITIES = {BuildConfig.CONTRIBUTION_AUTHORITY, BuildConfig.MODIFICATION_AUTHORITY};

    @NonNull
    private final Context context;

    public WikiAccountAuthenticator(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Bundle bundle = new Bundle();
        bundle.putString("test", "editProperties");
        return bundle;
    }

    @Override
    public Bundle addAccount(@NonNull AccountAuthenticatorResponse response,
                             @NonNull String accountType, @Nullable String authTokenType,
                             @Nullable String[] requiredFeatures, @Nullable Bundle options)
            throws NetworkErrorException {

        if (!supportedAccountType(accountType)) {
            Bundle bundle = new Bundle();
            bundle.putString("test", "addAccount");
            return bundle;
        }

        return addAccount(response);
    }

    @Override
    public Bundle confirmCredentials(@NonNull AccountAuthenticatorResponse response,
                                     @NonNull Account account, @Nullable Bundle options)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putString("test", "confirmCredentials");
        return bundle;
    }

    @Override
    public Bundle getAuthToken(@NonNull AccountAuthenticatorResponse response,
                               @NonNull Account account, @NonNull String authTokenType,
                               @Nullable Bundle options)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putString("test", "getAuthToken");
        return bundle;
    }

    @Nullable
    @Override
    public String getAuthTokenLabel(@NonNull String authTokenType) {
        return supportedAccountType(authTokenType) ? AUTH_TOKEN_TYPE : null;
    }

    @Nullable
    @Override
    public Bundle updateCredentials(@NonNull AccountAuthenticatorResponse response,
                                    @NonNull Account account, @Nullable String authTokenType,
                                    @Nullable Bundle options)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putString("test", "updateCredentials");
        return bundle;
    }

    @Nullable
    @Override
    public Bundle hasFeatures(@NonNull AccountAuthenticatorResponse response,
                              @NonNull Account account, @NonNull String[] features)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    private boolean supportedAccountType(@Nullable String type) {
        return BuildConfig.ACCOUNT_TYPE.equals(type);
    }

    private Bundle addAccount(AccountAuthenticatorResponse response) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
                                           Account account) throws NetworkErrorException {
        Bundle result = super.getAccountRemovalAllowed(response, account);

        if (result.containsKey(AccountManager.KEY_BOOLEAN_RESULT)
                && !result.containsKey(AccountManager.KEY_INTENT)) {
            boolean allowed = result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT);

            if (allowed) {
                for (String auth : SYNC_AUTHORITIES) {
                    ContentResolver.cancelSync(account, auth);
                }
            }
        }

        return result;
    }
}
