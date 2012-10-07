package org.wikimedia.commons;

import java.io.IOException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class LoginActivity extends Activity {
    
    private CommonsApplication app;
    
    Button loginButton;
    EditText usernameEdit;
    EditText passwordEdit;
    
    private class LoginTask extends AsyncTask<String, String, String> {

        Activity context;
        ProgressDialog dialog;
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(result.equals("Success")) {
                dialog.cancel();
                Toast successToast = Toast.makeText(context, R.string.login_success, Toast.LENGTH_SHORT);
                successToast.show();
                context.finish();
            } else {
                Toast failureToast = Toast.makeText(context, R.string.login_failed, Toast.LENGTH_LONG);
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
            String username = params[0];
            String password = params[1];
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
        app = (CommonsApplication)this.getApplicationContext();
        setContentView(R.layout.activity_login);
        loginButton = (Button)findViewById(R.id.loginButton);
        usernameEdit = (EditText)findViewById(R.id.loginUsername);
        passwordEdit = (EditText)findViewById(R.id.loginPassword);
        final Activity that = this;
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString();
                String password = passwordEdit.getText().toString();
                
                LoginTask task = new LoginTask(that);
                task.execute(username, password);
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
