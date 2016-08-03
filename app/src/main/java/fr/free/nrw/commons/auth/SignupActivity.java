package fr.free.nrw.commons.auth;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

import fr.free.nrw.commons.R;

public class SignupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Log.d("SignupActivity", "Signup Activity started");
    }

}
