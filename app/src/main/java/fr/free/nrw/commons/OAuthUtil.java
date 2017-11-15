package fr.free.nrw.commons;

import android.app.FragmentManager;
import android.content.Context;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.wuman.android.auth.AuthorizationFlow;
import com.wuman.android.auth.AuthorizationUIController;
import com.wuman.android.auth.DialogFragmentController;
import com.wuman.android.auth.OAuthManager;
import com.wuman.android.auth.oauth2.store.SharedPreferencesCredentialStore;

import java.io.IOException;

import static fr.free.nrw.commons.auth.OAuthConstants.AUTHORIZATION_VERIFIER_SERVER_URL;
import static fr.free.nrw.commons.auth.OAuthConstants.CONSUMER_KEY;
import static fr.free.nrw.commons.auth.OAuthConstants.CONSUMER_SECRET;
import static fr.free.nrw.commons.auth.OAuthConstants.REDIRECT_URL;
import static fr.free.nrw.commons.auth.OAuthConstants.TEMPORARY_TOKEN_REQUEST_URL;
import static fr.free.nrw.commons.auth.OAuthConstants.TOKEN_SERVER_URL;

public class OAuthUtil {
    private static final String CREDENTIAL_STORE_PREF = "credentialStore";

    public static final JsonFactory JSON_FACTORY = new JacksonFactory();
    public static final HttpTransport HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport();

    public static OAuthManager getOAuthManager(Context context, FragmentManager fragmentManager) {
        return new OAuthManager(getAuthorizationFlow(context), getAuthorizationUIController(fragmentManager));
    }

    private static AuthorizationFlow getAuthorizationFlow(Context context) {
        AuthorizationFlow.Builder builder = new AuthorizationFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                HTTP_TRANSPORT,
                JSON_FACTORY,
                new GenericUrl(TOKEN_SERVER_URL),
                new ClientParametersAuthentication(CONSUMER_KEY, CONSUMER_SECRET),
                CONSUMER_KEY,
                AUTHORIZATION_VERIFIER_SERVER_URL);

        builder.setTemporaryTokenRequestUrl(TEMPORARY_TOKEN_REQUEST_URL);
        builder.setCredentialStore(getCredentialStore(context));
        return builder.build();
    }

    private static SharedPreferencesCredentialStore getCredentialStore(Context context) {
        return new SharedPreferencesCredentialStore(context,
                CREDENTIAL_STORE_PREF, new JacksonFactory());
    }

    private static AuthorizationUIController getAuthorizationUIController(FragmentManager fragmentManager) {
        return new DialogFragmentController(fragmentManager, true) {

            @Override
            public String getRedirectUri() throws IOException {
                return REDIRECT_URL;
            }

            @Override
            public boolean isJavascriptEnabledForWebView() {
                return true;
            }

            @Override
            public boolean disableWebViewCache() {
                return false;
            }

            @Override
            public boolean removePreviousCookie() {
                return false;
            }
        };
    }
}
