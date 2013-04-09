package org.wikimedia.commons;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {
    private TextView versionText;
    private TextView licenseText;
    private TextView improveText;
    private TextView privacyPolicyText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        versionText = (TextView) findViewById(R.id.about_version);
        licenseText = (TextView) findViewById(R.id.about_license);
        improveText = (TextView) findViewById(R.id.about_improve);
        privacyPolicyText = (TextView) findViewById(R.id.about_privacy_policy);


        versionText.setText(CommonsApplication.APPLICATION_VERSION);

        licenseText.setMovementMethod(LinkMovementMethod.getInstance());
        improveText.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicyText.setMovementMethod(LinkMovementMethod.getInstance());
    }
}