package org.wikipedia.login;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;

public class LoginOAuthResult extends LoginResult {

    public LoginOAuthResult(@NonNull WikiSite site, @NonNull String status, @Nullable String userName,
                     @Nullable String password, @Nullable String message) {
        super(site, status, userName, password, message);
    }
}
