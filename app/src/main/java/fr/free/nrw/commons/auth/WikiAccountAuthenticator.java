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

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

class WikiAccountAuthenticator extends AbstractAccountAuthenticator {

    private static final String PARAM_USERNAME = "fr.free.nrw.commons.login.username";
    private Context context;

    public WikiAccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    private Bundle unsupportedOperation() {
        Bundle bundle = new Bundle();
        bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);

        // HACK: the docs indicate that this is a required key bit it's not displayed to the user.
        bundle.putString(AccountManager.KEY_ERROR_MESSAGE, "");

        return bundle;
    }

    private boolean supportedAccountType(@Nullable String type) {
        return AccountUtil.ACCOUNT_TYPE.equals(type);
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
        Intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, Intent);

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
        MediaWikiApi api = CommonsApplication.getInstance().getMWApi();
        //TODO add 2fa support here
        String result = api.login(username, password);
        if(result.equals("PASS")) {
            return api.getAuthCookie();
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
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountUtil.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authCookie);
                return result;
            }
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(PARAM_USERNAME, account.name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
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
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
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
