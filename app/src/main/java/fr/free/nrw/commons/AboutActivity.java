package fr.free.nrw.commons;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {
    private TextView versionText;
    private TextView licenseText;
    private TextView improveText;
    private TextView privacyPolicyText;
    private TextView uploadsToText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        versionText = (TextView) findViewById(R.id.about_version);
        licenseText = (TextView) findViewById(R.id.about_license);
        improveText = (TextView) findViewById(R.id.about_improve);
        privacyPolicyText = (TextView) findViewById(R.id.about_privacy_policy);
        uploadsToText = (TextView) findViewById(R.id.about_uploads_to);

        uploadsToText.setText(fr.free.nrw.commons.CommonsApplication.EVENTLOG_WIKI);
        versionText.setText(fr.free.nrw.commons.CommonsApplication.APPLICATION_VERSION);

        // We can't use formatted strings directly because it breaks with
        // our localization tools. Grab an HTML string and turn it into
        // a formatted string.
        fixFormatting(licenseText, R.string.about_license);
        fixFormatting(improveText, R.string.about_improve);
        fixFormatting(privacyPolicyText, R.string.about_privacy_policy);

        licenseText.setMovementMethod(LinkMovementMethod.getInstance());
        improveText.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicyText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void fixFormatting(TextView textView, int resource) {
        textView.setText(Html.fromHtml(getResources().getString(resource)));
    }
}