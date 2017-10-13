package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.mwapi.EventLog;
import fr.free.nrw.commons.utils.AbstractTextWatcher;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class LoginActivity extends AccountAuthenticatorActivity {

    @BindView(R.id.loginButton)
    Button loginButton;
    @BindView(R.id.signupButton)
    Button signupButton;
    @BindView(R.id.loginUsername)
    EditText usernameEdit;
    @BindView(R.id.loginPassword)
    EditText passwordEdit;
    @BindView(R.id.loginTwoFactor)
    EditText twoFactorEdit;

    private ProgressDialog progressDialog;
    private LoginTextWatcher textWatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        textWatcher = new LoginTextWatcher();
        usernameEdit.addTextChangedListener(textWatcher);
        passwordEdit.addTextChangedListener(textWatcher);
        twoFactorEdit.addTextChangedListener(textWatcher);
        passwordEdit.setOnEditorActionListener(newLoginInputActionListener());
        loginButton.setOnClickListener(view -> performLogin());
        signupButton.setOnClickListener(view -> signUp());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            WelcomeActivity.startYourself(this);
            prefs.edit().putBoolean("firstrun", false).apply();
        }
        if (alreadyLoggedIn()) {
            startMainActivity();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        usernameEdit.removeTextChangedListener(textWatcher);
        passwordEdit.removeTextChangedListener(textWatcher);
        twoFactorEdit.removeTextChangedListener(textWatcher);
        loginButton.setOnClickListener(null);
        signupButton.setOnClickListener(null);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when Login button is clicked.
     */
    private void performLogin() {
        showLoginProgressDialog();

        String username = canonicializeUsername(usernameEdit.getText().toString());
        String password = passwordEdit.getText().toString();
        String twoFactorCode = twoFactorEdit.getText().toString();

        Single.fromCallable(() -> getLoginResult(username, password, twoFactorCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> processLoginResult(result, username, password),
                        Timber::e
                );
    }

    /**
     * Called when Sign Up button is clicked.
     */
    private void signUp() {
        startActivity(new Intent(this, SignupActivity.class));
    }

    private boolean alreadyLoggedIn() {
        return ((CommonsApplication)getApplication()).getCurrentAccount() != null;
    }

    private void askUserForTwoFactorAuth() {
        if (BuildConfig.DEBUG) {
            twoFactorEdit.setVisibility(View.VISIBLE);
            showUserToastAndCancelDialog(R.string.login_failed_2fa_needed);
        } else {
            showUserToastAndCancelDialog(R.string.login_failed_2fa_not_supported);
        }
    }

    private void showUserToastAndCancelDialog(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
        progressDialog.cancel();
    }

    private void showSuccessToastAndDismissDialog() {
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        progressDialog.dismiss();
    }

    private void emptySensitiveEditFields() {
        passwordEdit.setText("");
        twoFactorEdit.setText("");
    }

    private void startMainActivity() {
        ContributionsActivity.startYourself(this);
        finish();
    }

    private String getLoginResult(String username, String password, String twoFactorCode) {
        try {
            CommonsApplication app = ((CommonsApplication) getApplication());
            if (twoFactorCode.isEmpty()) {
                return app.getMWApi().login(username, password);
            } else {
                return app.getMWApi().login(username, password, twoFactorCode);
            }
        } catch (IOException e) {
            // Do something better!
            return "NetworkFailure";
        }
    }

    private void processLoginResult(String result, String username, String password) {
        Timber.d("Login done!");

        EventLog.schema(CommonsApplication.EVENT_LOGIN_ATTEMPT)
                .param("username", username)
                .param("result", result)
                .log();

        if (result.equals("PASS")) {
            handlePassResult(username, password);
        } else {
            handleOtherResults(result);
        }
    }

    private void handlePassResult(String username, String password) {
        showSuccessToastAndDismissDialog();

        AccountAuthenticatorResponse response = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Timber.d("Bundle of extras: %s", extras);
            response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (response != null) {
                Bundle authResult = new Bundle();
                authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, AccountUtil.ACCOUNT_TYPE);
                response.onResult(authResult);
            }
        }

        AccountUtil.createAccount(response, username, password);
        startMainActivity();
    }

    /**
     * Match known failure message codes and provide messages.
     *
     * @param result String
     */
    private void handleOtherResults(String result) {
        if (result.equals("NetworkFailure")) {
            // Matches NetworkFailure which is created by the doInBackground method
            showUserToastAndCancelDialog(R.string.login_failed_network);
        } else if (result.toLowerCase().contains("nosuchuser".toLowerCase()) || result.toLowerCase().contains("noname".toLowerCase())) {
            // Matches nosuchuser, nosuchusershort, noname
            showUserToastAndCancelDialog(R.string.login_failed_username);
            emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("wrongpassword".toLowerCase())) {
            // Matches wrongpassword, wrongpasswordempty
            showUserToastAndCancelDialog(R.string.login_failed_password);
            emptySensitiveEditFields();
        } else if (result.toLowerCase().contains("throttle".toLowerCase())) {
            // Matches unknown throttle error codes
            showUserToastAndCancelDialog(R.string.login_failed_throttled);
        } else if (result.toLowerCase().contains("userblocked".toLowerCase())) {
            // Matches login-userblocked
            showUserToastAndCancelDialog(R.string.login_failed_blocked);
        } else if (result.equals("2FA")) {
            askUserForTwoFactorAuth();
        } else {
            // Occurs with unhandled login failure codes
            Timber.d("Login failed with reason: %s", result);
            showUserToastAndCancelDialog(R.string.login_failed_generic);
        }
    }

    /**
     * Because Mediawiki is upercase-first-char-then-case-sensitive :)
     *
     * @param username String
     * @return String canonicial username
     */
    private String canonicializeUsername(String username) {
        return new PageTitle(username).getText();
    }

    private void showLoginProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(getString(R.string.logging_in_title));
        progressDialog.setMessage(getString(R.string.logging_in_message));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private class LoginTextWatcher extends AbstractTextWatcher {
        @Override
        public void afterTextChanged(Editable editable) {
            if (usernameEdit.getText().length() != 0 && passwordEdit.getText().length() != 0 &&
                    (BuildConfig.DEBUG || twoFactorEdit.getText().length() != 0 || twoFactorEdit.getVisibility() != View.VISIBLE)) {
                loginButton.setEnabled(true);
            } else {
                loginButton.setEnabled(false);
            }
        }
    }

    private TextView.OnEditorActionListener newLoginInputActionListener() {
        return (textView, actionId, keyEvent) -> {
            if (loginButton.isEnabled()) {
                boolean isImeDone = actionId == IME_ACTION_DONE;
                boolean isKeyboardEnter = keyEvent != null
                        && keyEvent.getAction() == ACTION_DOWN
                        && keyEvent.getKeyCode() == KEYCODE_ENTER;
                if (isImeDone || isKeyboardEnter) {
                    performLogin();
                    return true;
                }
            }
            return false;
        };
    }

}
