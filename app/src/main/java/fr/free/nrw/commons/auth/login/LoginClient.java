package fr.free.nrw.commons.auth.login;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.ListUserResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Responsible for making login related requests to the server.
 */
public class LoginClient {
    @Nullable private Call<MwQueryResponse> tokenCall;
    @Nullable private Call<LoginResponse> loginCall;
    /**
     * userLanguage
     * It holds the value of the user's device language code.
     * For example, if user's device language is English it will hold En
     * The value will be fetched when the user clicks Login Button in the LoginActivity
     */
    @NonNull private String userLanguage = "En";

    public void request(@NonNull final WikiSite wiki, @NonNull final String userName,
                        @NonNull final String password, @NonNull final LoginCallback cb) {
        cancel();

        tokenCall = ServiceFactory.get(wiki, LoginInterface.class).getLoginToken();
        tokenCall.enqueue(new Callback<MwQueryResponse>() {
            @Override public void onResponse(@NonNull final Call<MwQueryResponse> call,
                                             @NonNull final Response<MwQueryResponse> response) {
                login(wiki, userName, password, null, null, response.body().query().loginToken(),
                    userLanguage, cb);
            }

            @Override
            public void onFailure(@NonNull final Call<MwQueryResponse> call, @NonNull final Throwable caught) {
                if (call.isCanceled()) {
                    return;
                }
                cb.error(caught);
            }
        });
    }

    public void login(@NonNull final WikiSite wiki, @NonNull final String userName, @NonNull final String password,
               @Nullable final String retypedPassword, @Nullable final String twoFactorCode,
               @Nullable final String loginToken, @NonNull final String userLanguage, @NonNull final LoginCallback cb) {
        this.userLanguage = userLanguage;
        loginCall = TextUtils.isEmpty(twoFactorCode) && TextUtils.isEmpty(retypedPassword)
                ? ServiceFactory.get(wiki, LoginInterface.class).postLogIn(userName, password, loginToken, userLanguage, Service.WIKIPEDIA_URL)
                : ServiceFactory.get(wiki, LoginInterface.class).postLogIn(userName, password, retypedPassword, twoFactorCode, loginToken,
                    userLanguage, true);
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull final Call<LoginResponse> call, @NonNull final Response<LoginResponse> response) {
                final LoginResponse loginResponse = response.body();
                final LoginResult loginResult = loginResponse.toLoginResult(wiki, password);
                if (loginResult != null) {
                    if (loginResult.pass() && !TextUtils.isEmpty(loginResult.getUserName())) {
                        // The server could do some transformations on user names, e.g. on some
                        // wikis is uppercases the first letter.
                        final String actualUserName = loginResult.getUserName();
                        getExtendedInfo(wiki, actualUserName, loginResult, cb);
                    } else if ("UI".equals(loginResult.getStatus())) {
                        if (loginResult instanceof LoginOAuthResult) {
                            cb.twoFactorPrompt(new LoginFailedException(loginResult.getMessage()), loginToken);
                        } else if (loginResult instanceof LoginResetPasswordResult) {
                            cb.passwordResetPrompt(loginToken);
                        } else {
                            cb.error(new LoginFailedException(loginResult.getMessage()));
                        }
                    } else {
                        cb.error(new LoginFailedException(loginResult.getMessage()));
                    }
                } else {
                    cb.error(new IOException("Login failed. Unexpected response."));
                }
            }

            @Override
            public void onFailure(@NonNull final Call<LoginResponse> call, @NonNull final Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                cb.error(t);
            }
        });
    }

    public void loginBlocking(@NonNull final WikiSite wiki, @NonNull final String userName,
                              @NonNull final String password, @Nullable final String twoFactorCode) throws Throwable {
        final Response<MwQueryResponse> tokenResponse = ServiceFactory.get(wiki, LoginInterface.class).getLoginToken().execute();
        if (tokenResponse.body() == null || TextUtils.isEmpty(tokenResponse.body().query().loginToken())) {
            throw new IOException("Unexpected response when getting login token.");
        }
        final String loginToken = tokenResponse.body().query().loginToken();

        final Call<LoginResponse> tempLoginCall = StringUtils.defaultIfEmpty(twoFactorCode, "").isEmpty()
                ? ServiceFactory.get(wiki, LoginInterface.class).postLogIn(userName, password, loginToken, userLanguage, Service.WIKIPEDIA_URL)
                : ServiceFactory.get(wiki, LoginInterface.class).postLogIn(userName, password, null, twoFactorCode, loginToken,
                    userLanguage, true);
        final Response<LoginResponse> response = tempLoginCall.execute();
        final LoginResponse loginResponse = response.body();
        if (loginResponse == null) {
            throw new IOException("Unexpected response when logging in.");
        }
        final LoginResult loginResult = loginResponse.toLoginResult(wiki, password);
        if (loginResult == null) {
            throw new IOException("Unexpected response when logging in.");
        }
        if ("UI".equals(loginResult.getStatus())) {
            if (loginResult instanceof LoginOAuthResult) {

                // TODO: Find a better way to boil up the warning about 2FA
                throw new LoginFailedException(loginResult.getMessage());

            }
            throw new LoginFailedException(loginResult.getMessage());
        }
        if (!loginResult.pass() || TextUtils.isEmpty(loginResult.getUserName())) {
            throw new LoginFailedException(loginResult.getMessage());
        }
    }

    @SuppressLint("CheckResult")
    private void getExtendedInfo(@NonNull final WikiSite wiki, @NonNull final String userName,
                                 @NonNull final LoginResult loginResult, @NonNull final LoginCallback cb) {
        ServiceFactory.get(wiki, LoginInterface.class).getUserInfo(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    final ListUserResponse user = response.query().getUserResponse(userName);
                    final int id = response.query().userInfo().id();
                    loginResult.setUserId(id);
                    loginResult.setGroups(user.getGroups());
                    cb.success(loginResult);
                    Timber.v("Found user ID " + id + " for " + wiki.subdomain());
                }, caught -> {
                    Timber.e(caught, "Login succeeded but getting group information failed. ");
                    cb.error(caught);
                });
    }

    public void cancel() {
        cancelTokenRequest();
        cancelLogin();
    }

    private void cancelTokenRequest() {
        if (tokenCall == null) {
            return;
        }
        tokenCall.cancel();
        tokenCall = null;
    }

    private void cancelLogin() {
        if (loginCall == null) {
            return;
        }
        loginCall.cancel();
        loginCall = null;
    }
}
