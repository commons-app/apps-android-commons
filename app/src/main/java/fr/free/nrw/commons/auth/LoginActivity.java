package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.EventLog;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import timber.log.Timber;


public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "fr.free.nrw.commons.login.username";

    private CommonsApplication app;

    private SharedPreferences prefs = null;

    Button loginButton;
    Button signupButton;
    EditText usernameEdit;
    EditText passwordEdit;
    EditText twoFactorEdit;
    ProgressDialog dialog;

    private class LoginTask extends AsyncTask<String, String, String> {

        Activity context;
        String username;
        String password;
        String twoFactorCode = "";

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Timber.d("Login done!");

            EventLog.schema(CommonsApplication.EVENT_LOGIN_ATTEMPT)
                    .param("username", username)
                    .param("result", result)
                    .log();

            if (result.equals("PASS")) {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                Toast successToast = Toast.makeText(context, R.string.login_success, Toast.LENGTH_SHORT);
                successToast.show();
                Account account = new Account(username, WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
                boolean accountCreated = AccountManager.get(context).addAccountExplicitly(account, password, null);

                Bundle extras = context.getIntent().getExtras();

                if (extras != null) {
                    Timber.d("Bundle of extras: %s", extras);
                    if (accountCreated) { // Pass the new account back to the account manager
                        AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                        Bundle authResult = new Bundle();
                        authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                        authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);

                        if (response != null) {
                            response.onResult(authResult);
                        }
                    }
                }
                // FIXME: If the user turns it off, it shouldn't be auto turned back on
                ContentResolver.setSyncAutomatically(account, ContributionsContentProvider.AUTHORITY, true); // Enable sync by default!
                ContentResolver.setSyncAutomatically(account, ModificationsContentProvider.AUTHORITY, true); // Enable sync by default!

                Intent intent = new Intent(context, ContributionsActivity.class);
                startActivity(intent);
                finish();

            } else {
                int response;
                // Match known failure message codes and provide messages
                if(result.equals("NetworkFailure")) {
                    // Matches NetworkFailure which is created by the doInBackground method
                    response = R.string.login_failed_network;
                } else if (result.toLowerCase().contains("nosuchuser".toLowerCase()) || result.toLowerCase().contains("noname".toLowerCase())) {
                    // Matches nosuchuser, nosuchusershort, noname
                    response = R.string.login_failed_username;
                    emptySensitiveEditFields();
                } else if (result.toLowerCase().contains("wrongpassword".toLowerCase())) {
                    // Matches wrongpassword, wrongpasswordempty
                    response = R.string.login_failed_password;
                    emptySensitiveEditFields();
                } else if (result.toLowerCase().contains("throttle".toLowerCase())) {
                    // Matches unknown throttle error codes
                    response = R.string.login_failed_throttled;
                } else if (result.toLowerCase().contains("userblocked".toLowerCase())) {
                    // Matches login-userblocked
                    response = R.string.login_failed_blocked;
                } else if (result.equals("2FA")){
                    twoFactorEdit.setVisibility(View.VISIBLE);
                    response = R.string.login_failed_2fa_needed;
                } else {
                    // Occurs with unhandled login failure codes
                    Timber.d("Login failed with reason: %s", result);
                    response = R.string.login_failed_generic;
                }
                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                dialog.cancel();
            }
        }

        private void emptySensitiveEditFields() {
            passwordEdit.setText("");
            twoFactorEdit.setText("");
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setTitle(getString(R.string.logging_in_title));
            dialog.setMessage(getString(R.string.logging_in_message));
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        LoginTask(Activity context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            username = params[0];
            password = params[1];
            if(params.length > 2) {
                twoFactorCode = params[2];
            }

            try {
                if(twoFactorCode.isEmpty()) {
                    return app.getApi().login(username, password);
                } else {
                    return app.getApi().login(username, password, twoFactorCode);
                }
            } catch (IOException e) {
                // Do something better!
                return "NetworkFailure";
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (CommonsApplication) this.getApplicationContext();
        setContentView(R.layout.activity_login);
        loginButton = (Button) findViewById(R.id.loginButton);
        signupButton = (Button) findViewById(R.id.signupButton);
        usernameEdit = (EditText) findViewById(R.id.loginUsername);
        passwordEdit = (EditText) findViewById(R.id.loginPassword);
        twoFactorEdit = (EditText) findViewById(R.id.loginTwoFactor);
        final LoginActivity that = this;

        prefs = getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);

        TextWatcher loginEnabler = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable editable) {
                if(
                        usernameEdit.getText().length() != 0 &&
                        passwordEdit.getText().length() != 0 &&
                        ( twoFactorEdit.getText().length() != 0 || twoFactorEdit.getVisibility() != View.VISIBLE )
                        ) {
                    loginButton.setEnabled(true);
                } else {
                    loginButton.setEnabled(false);
                }
            }
        };

        usernameEdit.addTextChangedListener(loginEnabler);
        passwordEdit.addTextChangedListener(loginEnabler);
        twoFactorEdit.addTextChangedListener(loginEnabler);
        passwordEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                that.performLogin();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (prefs.getBoolean("firstrun", true)) {
            // Do first run stuff here then set 'firstrun' as false
            Intent welcomeIntent = new Intent(this, WelcomeActivity.class);
            startActivity(welcomeIntent);
            prefs.edit().putBoolean("firstrun", false).apply();
        }
    }


    @Override
    protected void onDestroy() {
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void performLogin() {
        String username = usernameEdit.getText().toString();
        // Because Mediawiki is upercase-first-char-then-case-sensitive :)
        String canonicalUsername = Utils.capitalize(username.substring(0,1)) + username.substring(1);

        String password = passwordEdit.getText().toString();

        String twoFactorCode = twoFactorEdit.getText().toString();

        Timber.d("Login to start!");
        LoginTask task = new LoginTask(this);
        if(twoFactorCode.isEmpty()) {
            task.execute(canonicalUsername, password);
        } else {
            task.execute(canonicalUsername, password, twoFactorCode);
        }
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

    //Called when Sign Up button is clicked
    public void signUp(View view) {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }
}
