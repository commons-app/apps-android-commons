package fr.free.nrw.commons;

import java.io.IOException;

import org.apache.http.impl.client.AbstractHttpClient;
import org.mediawiki.api.ApiResult;

/**
 * @author Addshore
 */
public class MWApi extends org.mediawiki.api.MWApi {

    public MWApi(String apiURL, AbstractHttpClient client) {
        super(apiURL, client);
    }

    /**
     * @param username String
     * @param password String
     * @return String On success: "PASS"
     *                   failure: A failure message code (deifned by mediawiki)
     *                   misc:    genericerror-UI, genericerror-REDIRECT, genericerror-RESTART
     * @throws IOException On api request IO issue
     */
    public String login(String username, String password) throws IOException {

        /** Request a login token to be used later to log in. */
        ApiResult tokenData = this.action("query")
                .param("action", "query")
                .param("meta", "tokens")
                .param("type", "login")
                .post();
        String token = tokenData.getString("/api/query/tokens/@logintoken");

        /** Actually log in. */
        ApiResult loginData = this.action("clientlogin")
                .param("rememberMe", "1")
                .param("username", username)
                .param("password", password)
                .param("logintoken", token)
                .param("loginreturnurl", "http://example.com/")//TODO return to url?
                .post();
        String status = loginData.getString("/api/clientlogin/@status");

        if (status.equals("PASS")) {
            this.isLoggedIn = true;
            return status;

        } else if (status.equals("FAIL")) {
            return loginData.getString("/api/clientlogin/@messagecode");
        }

        // UI, REDIRECT, RESTART
        return "genericerror-" + status;
    }


}
