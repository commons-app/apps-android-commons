package org.wikimedia.commons.auth;

import java.io.IOException;

import android.content.*;
import android.text.*;
import de.keyboardsurfer.android.widget.crouton.*;
import android.os.*;
import android.accounts.*;
import android.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.support.v4.app.NavUtils;

import org.wikimedia.commons.*;
import org.wikimedia.commons.EventLog;
import org.wikimedia.commons.contributions.*;


public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "org.wikimedia.commons.login.username";

    private CommonsApplication app;

    Button loginButton;
    EditText usernameEdit;
    EditText passwordEdit;

    private class LoginTask extends AsyncTask<String, String, String> {

        Activity context;
        ProgressDialog dialog;
        String username;
        String password;

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("Commons", "Login done!");

            EventLog.schema(CommonsApplication.EVENT_LOGIN_ATTEMPT)
                    .param("username", username)
                    .param("result", result)
                    .log();

            if (result.equals("Success")) {
                dialog.dismiss();
                Toast successToast = Toast.makeText(context, R.string.login_success, Toast.LENGTH_SHORT);
                successToast.show();
                Account account = new Account(username, WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
                boolean accountCreated = AccountManager.get(context).addAccountExplicitly(account, password, null);

                Bundle extras = context.getIntent().getExtras();
                if (extras != null) {
                    if (accountCreated) { // Pass the new account back to the account manager
                        AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
                        Bundle authResult = new Bundle();
                        authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                        authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
                        response.onResult(authResult);
                    }
                }
                // FIXME: If the user turns it off, it shouldn't be auto turned back on
                ContentResolver.setSyncAutomatically(account, ContributionsContentProvider.AUTHORITY, true); // Enable sync by default!
                context.finish();
            } else {
                int response;
                if(result.equals("NetworkFailure")) {
                    response = R.string.login_failed_network;
                } else if(result.equals("NotExists") || result.equals("Illegal") || result.equals("NotExists")) {
                    response = R.string.login_failed_username;
                    passwordEdit.setText("");
                } else if(result.equals("EmptyPass") || result.equals("WrongPass")) {
                    response = R.string.login_failed_password;
                    passwordEdit.setText("");
                } else if(result.equals("Throttled")) {
                    response = R.string.login_failed_throttled;
                } else if(result.equals("Blocked")) {
                    response = R.string.login_failed_blocked;
                } else {
                    // Should never really happen
                    response = R.string.login_failed_generic;
                }
                Crouton.makeText(context, response, Style.ALERT, R.id.loginErrors).show();
                dialog.dismiss();
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setTitle(getString(R.string.logging_in_title));
            dialog.setMessage(getString(R.string.logging_in_message));
            dialog.show();
        }

        LoginTask(Activity context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            username = params[0];
            password = params[1];
            try {
                return app.getApi().login(username, password);
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
        usernameEdit = (EditText) findViewById(R.id.loginUsername);
        passwordEdit = (EditText) findViewById(R.id.loginPassword);
        final Activity that = this;

        TextWatcher loginEnabler = new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            public void onTextChanged(CharSequence charSequence, int start, int count, int after) { }

            public void afterTextChanged(Editable editable) {
                if(usernameEdit.getText().length() != 0 && passwordEdit.getText().length() != 0) {
                    loginButton.setEnabled(true);
                } else {
                    loginButton.setEnabled(false);
                }
            }
        };

        usernameEdit.addTextChangedListener(loginEnabler);
        passwordEdit.addTextChangedListener(loginEnabler);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString();
                // Because Mediawiki is upercase-first-char-then-case-sensitive :)
                String canonicalUsername = username.substring(0,1).toUpperCase() + username.substring(1);

                String password = passwordEdit.getText().toString();
                
                Log.d("Commons", "Login to start!");
                LoginTask task = new LoginTask(that);
                task.execute(canonicalUsername, password);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
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

}
