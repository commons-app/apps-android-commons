package org.wikimedia.commons.auth;

import java.io.IOException;

import android.accounts.*;
import android.app.Activity;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class AuthenticatedActivity extends Activity {
    
    
    String accountType;
    public AuthenticatedActivity(String accountType) {
       this.accountType = accountType;
    }

   
    private class GetAuthCookieTask extends AsyncTask<String, String, String> {

        private Account account;
        private AccountManager accountManager;
        public GetAuthCookieTask(Account account, AccountManager accountManager) {
            this.account = account;
            this.accountManager = accountManager;
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result != null) {
                onAuthCookieAcquired(result);
            } else {
                onAuthFailure();
            }
        }

        @Override
        protected String doInBackground(String... params) {
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
//    private class onTokenAcquired implements AccountManagerCallback<Bundle> {
//
//        @Override
//        public void run(AccountManagerFuture<Bundle> result) {
//            Bundle bundle;
//            try {
//                bundle = result.getResult();
//            } catch (OperationCanceledException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            } catch (AuthenticatorException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//            Log.d("Commons", "Token Found!");
//            if(bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
//                String authCookie = bundle.getString(AccountManager.KEY_AUTHTOKEN);
//                onAuthCookieAcquired(authCookie);
//            } else {
//                if(bundle.containsKey(AccountManager.KEY_INTENT)) {
//                    Intent launchIntent = (Intent) bundle.get(AccountManager.KEY_INTENT);
//                    startActivityForResult(launchIntent, 0);
//                } else {
//                    
//                }
//            }
//            
//        }
//    }
//   
//    
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d("Commons", "Result of the loginactivity!");
//        if(resultCode == Activity.RESULT_OK) {
//            requestAuthToken();
//        }
//    }
//    
//    private void requestAuthToken() {
//        AccountManager accountManager = AccountManager.get(this);
//        Account[] allAccounts =accountManager.getAccountsByType(accountType);
//        if(allAccounts.length == 0) {
//            Log.d("Commons", "No accounts yet!");
//            // No Commons Accounts yet!
//            accountManager.addAccount(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE, "", null, null, this, new onTokenAcquired(), null);
//            return;
//        }
//        
//        Log.d("Commons", "Accounts found!");
//        // For now - always pick the first account
//        Account curAccount = allAccounts[0];
//        Bundle cookieOptions = new Bundle();
//        accountManager.getAuthToken(curAccount, "", cookieOptions, this, new onTokenAcquired(), null);
//    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccountManager accountManager = AccountManager.get(this);
        Account[] allAccounts =accountManager.getAccountsByType(accountType);
        if(allAccounts.length == 0) {
            // No Commons Accounts yet!
            accountManager.addAccount(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE, "", null, null, this, null, null);
            return;
        }
        
        // For now - always pick the first account
        Account curAccount = allAccounts[0];
        GetAuthCookieTask task = new GetAuthCookieTask(curAccount, accountManager);
        task.execute("");
    }
    protected void onAuthCookieAcquired(String authCookie) {
        
    }
    protected void onAuthFailure() {
        
    }
}
