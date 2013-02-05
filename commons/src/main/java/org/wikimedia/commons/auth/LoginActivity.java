package org.wikimedia.commons.auth;

import java.io.IOException;

import android.content.ContentResolver;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.R;
import org.wikimedia.commons.R.id;
import org.wikimedia.commons.R.layout;
import org.wikimedia.commons.R.menu;
import org.wikimedia.commons.R.string;

import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v4.app.NavUtils;
import org.wikimedia.commons.contributions.ContributionsContentProvider;

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
                Toast failureToast = Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG);
                dialog.dismiss();
                failureToast.show();
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
                return "Failure";
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
