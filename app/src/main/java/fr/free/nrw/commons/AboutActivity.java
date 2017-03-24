package fr.free.nrw.commons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import fr.free.nrw.commons.theme.BaseActivity;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AboutActivity extends BaseActivity {
    @BindView(R.id.about_version) TextView versionText;
    @BindView(R.id.about_license) TextView licenseText;
    @BindView(R.id.about_improve) TextView improveText;
    @BindView(R.id.about_privacy_policy) TextView privacyPolicyText;
    @BindView(R.id.about_uploads_to) TextView uploadsToText;
    @BindView(R.id.about_credits) TextView creditsText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().hide();

        ButterKnife.bind(this);

        uploadsToText.setText(CommonsApplication.EVENTLOG_WIKI);
        versionText.setText(BuildConfig.VERSION_NAME);

        // We can't use formatted strings directly because it breaks with
        // our localization tools. Grab an HTML string and turn it into
        // a formatted string.
        fixFormatting(licenseText, R.string.about_license);
        fixFormatting(improveText, R.string.about_improve);
        fixFormatting(privacyPolicyText, R.string.about_privacy_policy);
        fixFormatting(creditsText, R.string.about_credits);

        licenseText.setMovementMethod(LinkMovementMethod.getInstance());
        improveText.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicyText.setMovementMethod(LinkMovementMethod.getInstance());
        creditsText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void fixFormatting(TextView textView, int resource) {
        textView.setText(Html.fromHtml(getResources().getString(resource)));
    }
}