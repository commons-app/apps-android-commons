package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.EventLog;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

class LoginTask extends AsyncTask<String, String, String> {

    private LoginActivity loginActivity;
    private String username;
    private String password;
    private String twoFactorCode = "";
    private AccountUtil accountUtil;
    private CommonsApplication app;
    private MediaWikiApi mwApi;

    public LoginTask(LoginActivity loginActivity, String username, String password, String twoFactorCode, AccountUtil accountUtil, CommonsApplication application, MediaWikiApi mwApi) {
        this.loginActivity = loginActivity;
        this.username = username;
        this.password = password;
        this.twoFactorCode = twoFactorCode;
        this.accountUtil = accountUtil;
        this.app = application;
        this.mwApi = mwApi;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loginActivity.progressDialog = new ProgressDialog(loginActivity);
        loginActivity.progressDialog.setIndeterminate(true);
        loginActivity.progressDialog.setTitle(loginActivity.getString(R.string.logging_in_title));
        loginActivity.progressDialog.setMessage(loginActivity.getString(R.string.logging_in_message));
        loginActivity.progressDialog.setCanceledOnTouchOutside(false);
        loginActivity.progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            if (twoFactorCode.isEmpty()) {
                return mwApi.login(username, password);
            } else {
                return mwApi.login(username, password, twoFactorCode);
            }
        } catch (IOException e) {
            // Do something better!
            return "NetworkFailure";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Timber.d("Login done!");

        EventLog.schema(CommonsApplication.EVENT_LOGIN_ATTEMPT, app, mwApi)
                .param("username", username)
                .param("result", result)
                .log();

        if (result.equals("PASS")) {
            handlePassResult();
        } else {
            handleOtherResults(result);
        }
    }

    private void handlePassResult() {
        loginActivity.showSuccessToastAndDismissDialog();

        AccountAuthenticatorResponse response = null;

        Bundle extras = loginActivity.getIntent().getExtras();
        if (extras != null) {
            Timber.d("Bundle of extras: %s", extras);
            response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (response != null) {
                Bundle authResult = new Bundle();
                authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, accountUtil.accountType());
                response.onResult(authResult);
            }
        }

        accountUtil.createAccount(response, username, password);
        loginActivity.startMainActivity();
    }

    /**
     * Match known failure message codes and provide messages.
     * @param result String
     */
    private void handleOtherResults(String result) {
        if (result.equals("NetworkFailure")) {
            // Matches NetworkFailure which is created by the doInBackground method
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_network);
        } else if (result.toLowerCase().contains("nosuchuser".toLowerCase()) || result.toLowerCase().contains("noname".toLowerCase())) {
            // Matches nosuchuser, nosuchusershort, noname
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_username);
            loginActivity.emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("wrongpassword".toLowerCase())) {
            // Matches wrongpassword, wrongpasswordempty
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_password);
            loginActivity.emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("throttle".toLowerCase())) {
            // Matches unknown throttle error codes
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_throttled);
        } else if (result.toLowerCase().contains("userblocked".toLowerCase())) {
            // Matches login-userblocked
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_blocked);
        } else if (result.equals("2FA")) {
            loginActivity.askUserForTwoFactorAuth();
        } else {
            // Occurs with unhandled login failure codes
            Timber.d("Login failed with reason: %s", result);
            loginActivity.showUserToastAndCancelDialog(R.string.login_failed_generic);
        }
    }
}
