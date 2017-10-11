package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;


public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "fr.free.nrw.commons.login.username";

    private SharedPreferences prefs = null;

    private Button loginButton;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText twoFactorEdit;
    ProgressDialog progressDialog;
    private LoginTextWatcher textWatcher = new LoginTextWatcher();

    private CommonsApplication app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = CommonsApplication.getInstance();

        setContentView(R.layout.activity_login);

        loginButton = (Button) findViewById(R.id.loginButton);
        Button signupButton = (Button) findViewById(R.id.signupButton);
        usernameEdit = (EditText) findViewById(R.id.loginUsername);
        passwordEdit = (EditText) findViewById(R.id.loginPassword);
        twoFactorEdit = (EditText) findViewById(R.id.loginTwoFactor);

        prefs = getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);

        usernameEdit.addTextChangedListener(textWatcher);
        passwordEdit.addTextChangedListener(textWatcher);
        twoFactorEdit.addTextChangedListener(textWatcher);
        passwordEdit.setOnEditorActionListener(newLoginInputActionListener());

        loginButton.setOnClickListener(this::performLogin);
        signupButton.setOnClickListener(this::signUp);
    }

    private class LoginTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

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
                if (actionId == IME_ACTION_DONE) {
                    performLogin(textView);
                    return true;
                } else if ((keyEvent != null) && keyEvent.getKeyCode() == KEYCODE_ENTER) {
                    performLogin(textView);
                    return true;
                }
            }
            return false;
        };
    }

    protected void onResume() {
        super.onResume();
        if (prefs.getBoolean("firstrun", true)) {
            WelcomeActivity.startYourself(this);
            prefs.edit().putBoolean("firstrun", false).apply();
        }
        if (app.getCurrentAccount() != null) {
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
        super.onDestroy();
    }

    private void performLogin(View view) {
        Timber.d("Login to start!");
        LoginTask task = getLoginTask();
        task.execute();
    }

    private LoginTask getLoginTask() {
        return new LoginTask(
                this,
                canonicializeUsername(usernameEdit.getText().toString()),
                passwordEdit.getText().toString(),
                twoFactorEdit.getText().toString()
        );
    }

    /**
     * Because Mediawiki is upercase-first-char-then-case-sensitive :)
     * @param username String
     * @return String canonicial username
     */
    private String canonicializeUsername(String username) {
        return new PageTitle(username).getText();
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
     * Called when Sign Up button is clicked.
     * @param view View
     */
    public void signUp(View view) {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    public void askUserForTwoFactorAuth() {
        if (BuildConfig.DEBUG) {
            twoFactorEdit.setVisibility(View.VISIBLE);
            showUserToastAndCancelDialog(R.string.login_failed_2fa_needed);
        } else {
            showUserToastAndCancelDialog(R.string.login_failed_2fa_not_supported);
        }
    }

    public void showUserToastAndCancelDialog(int resId) {
        showUserToast(resId);
        progressDialog.cancel();
    }

    private void showUserToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
    }

    public void showSuccessToastAndDismissDialog() {
        Toast successToast = Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT);
        successToast.show();
        progressDialog.dismiss();
    }

    public void emptySensitiveEditFields() {
        passwordEdit.setText("");
        twoFactorEdit.setText("");
    }

    public void startMainActivity() {
        ContributionsActivity.startYourself(this);
        finish();
    }

}
