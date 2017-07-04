package fr.free.nrw.commons;

import java.io.IOException;

import org.apache.http.impl.client.AbstractHttpClient;

import fr.free.nrw.commons.libs.mediawiki_api.ApiResult;


/**
 * @author Addshore
 */
public class MWApi extends fr.free.nrw.commons.libs.mediawiki_api.MWApi {

    /** We don't actually use this but need to pass it in requests */
    private static String LOGIN_RETURN_TO_URL = "https://commons.wikimedia.org";

    public MWApi(String apiURL, AbstractHttpClient client) {
        super(apiURL, client);
    }

    /**
     * @param username String
     * @param password String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password) throws IOException {
        String token = this.getLoginToken();
        ApiResult loginApiResult = this.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", token)
                .param("loginreturnurl", LOGIN_RETURN_TO_URL)
                .prepareHttpRequestBuilder("POST")
                .request();
        return this.getErrorCodeToReturn( loginApiResult );
    }

    /**
     * @param username String
     * @param password String
     * @param twoFactorCode String
     * @return String as returned by this.getErrorCodeToReturn()
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password, String twoFactorCode) throws IOException {
        String token = this.getLoginToken();//TODO cache this instead of calling again when 2FAing
        ApiResult loginApiResult = this.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", token)
                .param("logincontinue", "1")
                .param("OATHToken", twoFactorCode)
                .prepareHttpRequestBuilder("POST")
                .request();

        return this.getErrorCodeToReturn( loginApiResult );
    }

    private String getLoginToken() throws IOException {
        ApiResult tokenResult = this.action("query")
                .param("action", "query")
                .param("meta", "tokens")
                .param("type", "login")
                .prepareHttpRequestBuilder("POST")
                .request();
        return tokenResult.getString("/api/query/tokens/@logintoken");
    }

    /**
     * @param loginApiResult ApiResult Any clientlogin api result
     * @return String On success: "PASS"
     *                   continue: "2FA" (More information required for 2FA)
     *                   failure: A failure message code (defined by mediawiki)
     *                   misc:    genericerror-UI, genericerror-REDIRECT, genericerror-RESTART
     */
    private String getErrorCodeToReturn( ApiResult loginApiResult ) {
        String status = loginApiResult.getString("/api/clientlogin/@status");
        if (status.equals("PASS")) {
            this.isLoggedIn = true;
            return status;
        } else if (status.equals("FAIL")) {
            return loginApiResult.getString("/api/clientlogin/@messagecode");
        } else if (
                status.equals("UI")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@id").equals("TOTPAuthenticationRequest")
                        && loginApiResult.getString("/api/clientlogin/requests/_v/@provider").equals("Two-factor authentication (OATH).")
                ) {
            return "2FA";
        }

        // UI, REDIRECT, RESTART
        return "genericerror-" + status;
    }

}
