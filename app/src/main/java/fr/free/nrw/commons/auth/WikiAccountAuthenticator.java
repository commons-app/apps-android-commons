package fr.free.nrw.commons.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import fr.free.nrw.commons.mwapi.MediaWikiApi;

import static android.accounts.AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION;
import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static android.accounts.AccountManager.KEY_AUTHTOKEN;
import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static android.accounts.AccountManager.KEY_ERROR_CODE;
import static android.accounts.AccountManager.KEY_ERROR_MESSAGE;
import static android.accounts.AccountManager.KEY_INTENT;
import static fr.free.nrw.commons.auth.AccountUtil.ACCOUNT_TYPE;
import static fr.free.nrw.commons.auth.LoginActivity.PARAM_USERNAME;

public class WikiAccountAuthenticator extends AbstractAccountAuthenticator {

    private final Context context;
    private MediaWikiApi mediaWikiApi;

    public WikiAccountAuthenticator(Context context, MediaWikiApi mwApi) {
        super(context);
        this.context = context;
        this.mediaWikiApi = mwApi;
    }

    private Bundle unsupportedOperation() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_ERROR_CODE, ERROR_CODE_UNSUPPORTED_OPERATION);

        // HACK: the docs indicate that this is a required key bit it's not displayed to the user.
        bundle.putString(KEY_ERROR_MESSAGE, "");

        return bundle;
    }

    private boolean supportedAccountType(@Nullable String type) {
        return ACCOUNT_TYPE.equals(type);
    }

    @Override
    public Bundle addAccount(@NonNull AccountAuthenticatorResponse response,
                             @NonNull String accountType, @Nullable String authTokenType,
                             @Nullable String[] requiredFeatures, @Nullable Bundle options)
            throws NetworkErrorException {

        if (!supportedAccountType(accountType)) {
            return unsupportedOperation();
        }

        return addAccount(response);
    }

    private Bundle addAccount(AccountAuthenticatorResponse response) {
        Intent Intent = new Intent(context, LoginActivity.class);
        Intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INTENT, Intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(@NonNull AccountAuthenticatorResponse response,
                                     @NonNull Account account, @Nullable Bundle options)
            throws NetworkErrorException {
        return unsupportedOperation();
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return unsupportedOperation();
    }

    private String getAuthCookie(String username, String password) throws IOException {
        //TODO add 2fa support here
        String result = mediaWikiApi.login(username, password);
        if (result.equals("PASS")) {
            return mediaWikiApi.getAuthCookie();
        } else {
            return null;
        }
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(context);
        final String password = am.getPassword(account);
        if (password != null) {
            String authCookie;
            try {
                authCookie = getAuthCookie(account.name, password);
            } catch (IOException e) {
                // Network error!
                e.printStackTrace();
                throw new NetworkErrorException(e);
            }
            if (authCookie != null) {
                final Bundle result = new Bundle();
                result.putString(KEY_ACCOUNT_NAME, account.name);
                result.putString(KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
                result.putString(KEY_AUTHTOKEN, authCookie);
                return result;
            }
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(PARAM_USERNAME, account.name);
        intent.putExtra(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_INTENT, intent);
        return bundle;
    }

    @Nullable
    @Override
    public String getAuthTokenLabel(@NonNull String authTokenType) {
        //Note: the wikipedia app actually returns a string here....
        //return supportedAccountType(authTokenType) ? context.getString(R.string.wikimedia) : null;
        return null;
    }

    @Nullable
    @Override
    public Bundle hasFeatures(@NonNull AccountAuthenticatorResponse response,
                              @NonNull Account account, @NonNull String[] features)
            throws NetworkErrorException {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_BOOLEAN_RESULT, false);
        return bundle;
    }

    @Nullable
    @Override
    public Bundle updateCredentials(@NonNull AccountAuthenticatorResponse response,
                                    @NonNull Account account, @Nullable String authTokenType,
                                    @Nullable Bundle options)
            throws NetworkErrorException {
        return unsupportedOperation();
    }

}
