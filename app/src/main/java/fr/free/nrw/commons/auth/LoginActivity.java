package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.Toast;
import fr.free.nrw.commons.*;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.utils.CommonsAppSharedPref;
import timber.log.Timber;


public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "fr.free.nrw.commons.login.username";

    private CommonsAppSharedPref prefs;

    private Button loginButton;
    private EditText usernameEdit;
    EditText passwordEdit;
    EditText twoFactorEdit;
    ProgressDialog progressDialog;

    private CommonsApplication app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app = (CommonsApplication) getApplicationContext();

        setContentView(R.layout.activity_login);
        final LoginActivity that = this;

        loginButton = (Button) findViewById(R.id.loginButton);
        Button signupButton = (Button) findViewById(R.id.signupButton);
        usernameEdit = (EditText) findViewById(R.id.loginUsername);
        passwordEdit = (EditText) findViewById(R.id.loginPassword);
        twoFactorEdit = (EditText) findViewById(R.id.loginTwoFactor);

        prefs = CommonsAppSharedPref.getInstance(this);

        TextWatcher loginEnabler = newLoginTextWatcher();
        usernameEdit.addTextChangedListener(loginEnabler);
        passwordEdit.addTextChangedListener(loginEnabler);
        twoFactorEdit.addTextChangedListener(loginEnabler);
        passwordEdit.setOnEditorActionListener( newLoginInputActionListener() );

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                that.performLogin();
            }
        });
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { that.signUp(v); }
        });
    }

    private TextWatcher newLoginTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if(
                        usernameEdit.getText().length() != 0 &&
                                passwordEdit.getText().length() != 0 &&
                                ( BuildConfig.DEBUG || twoFactorEdit.getText().length() != 0 || twoFactorEdit.getVisibility() != View.VISIBLE )
                        ) {
                    loginButton.setEnabled(true);
                } else {
                    loginButton.setEnabled(false);
                }
            }
        };
    }

    private TextView.OnEditorActionListener newLoginInputActionListener() {
        return new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (loginButton.isEnabled()) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        performLogin();
                        return true;
                    } else if ((keyEvent != null) && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        performLogin();
                        return true;
                    }
                }
                return false;
            }
        };
    }

    protected void onResume() {
        super.onResume();
        if (prefs.getPreferenceBoolean("firstrun", true)) {
            this.startWelcomeIntent();
            prefs.putPreferenceBoolean("firstrun", false);
        }
        if (app.getCurrentAccount() != null) {
            startMainActivity();
        }
    }

    private void startWelcomeIntent() {
        Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
        startActivity(welcomeIntent);
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
        super.onDestroy();
    }

    private void performLogin() {
        Timber.d("Login to start!");
        LoginTask task = getLoginTask();
        task.execute();
    }

    private LoginTask getLoginTask() {
        return new LoginTask(
                this,
                canonicializeUsername( usernameEdit.getText().toString() ),
                passwordEdit.getText().toString(),
                twoFactorEdit.getText().toString()
        );
    }

    /**
     * Because Mediawiki is upercase-first-char-then-case-sensitive :)
     * @param username String
     * @return String canonicial username
     */
    private String canonicializeUsername( String username ) {
        return Utils.capitalize(username.substring(0,1)) + username.substring(1);
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
        if(BuildConfig.DEBUG) {
            twoFactorEdit.setVisibility(View.VISIBLE);
            showUserToastAndCancelDialog( R.string.login_failed_2fa_needed );
        }else{
            showUserToastAndCancelDialog( R.string.login_failed_2fa_not_supported );
        }
    }

    public void showUserToastAndCancelDialog( int resId ) {
        showUserToast( resId );
        progressDialog.cancel();
    }

    private void showUserToast( int resId ) {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_LONG).show();
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
        Intent intent = new Intent(this, ContributionsActivity.class);
        startActivity(intent);
        finish();
    }

}
