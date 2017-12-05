package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorResponse;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.mwapi.EventLog;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_ACCOUNT_NAME;
import static android.accounts.AccountManager.KEY_ACCOUNT_TYPE;
import static fr.free.nrw.commons.auth.AccountUtil.ACCOUNT_TYPE;

class LoginTask extends AsyncTask<String, String, String> {

    private LoginActivity loginActivity;
    private String username;
    private String password;
    private String twoFactorCode = "";
    private AccountUtil accountUtil;
    private MediaWikiApi mwApi;
    private SharedPreferences prefs;

    public LoginTask(LoginActivity loginActivity, String username, String password,
                     String twoFactorCode, AccountUtil accountUtil,
                     MediaWikiApi mwApi, SharedPreferences prefs) {
        this.loginActivity = loginActivity;
        this.username = username;
        this.password = password;
        this.twoFactorCode = twoFactorCode;
        this.accountUtil = accountUtil;
        this.mwApi = mwApi;
        this.prefs = prefs;
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

        EventLog.schema(CommonsApplication.EVENT_LOGIN_ATTEMPT, mwApi, prefs)
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
        loginActivity.showSuccessAndDismissDialog();

        AccountAuthenticatorResponse response = null;

        Bundle extras = loginActivity.getIntent().getExtras();
        if (extras != null) {
            Timber.d("Bundle of extras: %s", extras);
            response = extras.getParcelable(KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (response != null) {
                Bundle authResult = new Bundle();
                authResult.putString(KEY_ACCOUNT_NAME, username);
                authResult.putString(KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
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
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_network);
        } else if (result.toLowerCase().contains("nosuchuser".toLowerCase()) || result.toLowerCase().contains("noname".toLowerCase())) {
            // Matches nosuchuser, nosuchusershort, noname
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_username);
            loginActivity.emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("wrongpassword".toLowerCase())) {
            // Matches wrongpassword, wrongpasswordempty
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_password);
            loginActivity.emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("throttle".toLowerCase())) {
            // Matches unknown throttle error codes
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_throttled);
        } else if (result.toLowerCase().contains("userblocked".toLowerCase())) {
            // Matches login-userblocked
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_blocked);
        } else if (result.equals("2FA")) {
            loginActivity.askUserForTwoFactorAuth();
        } else {
            // Occurs with unhandled login failure codes
            Timber.d("Login failed with reason: %s", result);
            loginActivity.showMessageAndCancelDialog(R.string.login_failed_generic);
        }
    }
}
