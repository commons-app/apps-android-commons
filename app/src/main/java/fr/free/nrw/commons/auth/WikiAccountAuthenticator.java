package fr.free.nrw.commons.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MWApi;

public class WikiAccountAuthenticator extends AbstractAccountAuthenticator {

    public static final String COMMONS_ACCOUNT_TYPE = "fr.free.nrw.commons";
    private Context context;
    public WikiAccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    private String getAuthCookie(String username, String password) throws IOException {
        MWApi api = CommonsApplication.createMWApi();
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
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, COMMONS_ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authCookie);
                return result;
            }
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

}
