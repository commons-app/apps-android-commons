package fr.free.nrw.commons.auth;

import android.content.Context;

public class AccountUtil {

    public static final String ACCOUNT_TYPE = "fr.free.nrw.commons";
    public static final String AUTH_COOKIE = "authCookie";
    public static final String AUTH_TOKEN_TYPE = "CommonsAndroid";
    private final Context context;

    public AccountUtil(Context context) {
        this.context = context;
    }

}
