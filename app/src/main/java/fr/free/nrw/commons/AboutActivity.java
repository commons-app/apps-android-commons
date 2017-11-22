package fr.free.nrw.commons;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;

public class AboutActivity extends NavigationBaseActivity {
    @BindView(R.id.about_version) TextView versionText;
    @BindView(R.id.about_license) HtmlTextView aboutLicenseText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);

        String aboutText = getString(R.string.about_license, getString(R.string.trademarked_name));
        aboutLicenseText.setHtmlText(aboutText);

        versionText.setText(BuildConfig.VERSION_NAME);
        initDrawer();
    }
}