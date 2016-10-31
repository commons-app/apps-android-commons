package fr.free.nrw.commons.auth;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Utils;

public abstract class AuthenticatedActivity extends AppCompatActivity {

    String accountType;
    CommonsApplication app;

    private String authCookie;

    public AuthenticatedActivity(String accountType) {
        this.accountType = accountType;
    }

    private class GetAuthCookieTask extends AsyncTask<Void, String, String> {
        private Account account;
        private AccountManager accountManager;

        public GetAuthCookieTask(Account account, AccountManager accountManager) {
            this.account = account;
            this.accountManager = accountManager;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                authCookie = result;
                onAuthCookieAcquired(result);
            } else {
                onAuthFailure();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return accountManager.blockingGetAuthToken(account, "", false);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return null;
            } catch (AuthenticatorException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private class AddAccountTask extends AsyncTask<Void, String, String> {
        private AccountManager accountManager;

        public AddAccountTask(AccountManager accountManager) {
            this.accountManager = accountManager;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                if (ActivityCompat.checkSelfPermission(app, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Account[] allAccounts = accountManager.getAccountsByType(accountType);
                Account curAccount = allAccounts[0];
                GetAuthCookieTask getCookieTask = new GetAuthCookieTask(curAccount, accountManager);
                getCookieTask.execute();
            } else {
                onAuthFailure();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            AccountManagerFuture<Bundle> resultFuture = accountManager.addAccount(accountType, null, null, null, AuthenticatedActivity.this, null, null);
            Bundle result;
            try {
                result = resultFuture.getResult();
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                return null;
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            if(result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                return result.getString(AccountManager.KEY_ACCOUNT_NAME);
            } else {
                return null;
            }
            
        }
    }

    protected void requestAuthToken() {
        if(authCookie != null) {
            onAuthCookieAcquired(authCookie);
            return;
        }
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = app.getCurrentAccount();
        if(curAccount == null) {
            AddAccountTask addAccountTask = new AddAccountTask(accountManager);
            // This AsyncTask blocks until the Login Activity returns
            // And since in Android 4.x+ only one background thread runs all AsyncTasks
            // And since LoginActivity can't return until it's own AsyncTask (that does the login)
            // returns, we have a deadlock!
            // Fixed by explicitly asking this to be executed in parallel
            // See: https://groups.google.com/forum/?fromgroups=#!topic/android-developers/8M0RTFfO7-M
            Utils.executeAsyncTask(addAccountTask);
        } else {
            GetAuthCookieTask task = new GetAuthCookieTask(curAccount, accountManager);
            task.execute();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (CommonsApplication)this.getApplicationContext();
        if(savedInstanceState != null) {
            authCookie = savedInstanceState.getString("authCookie");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("authCookie", authCookie);
    }

    protected abstract void onAuthCookieAcquired(String authCookie);
    protected abstract void onAuthFailure();
}
