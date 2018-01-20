package fr.free.nrw.commons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;

/**
 * Represents about screen of this app
 */
public class AboutActivity extends NavigationBaseActivity {
    @BindView(R.id.about_version) TextView versionText;
    @BindView(R.id.about_license) HtmlTextView aboutLicenseText;

    /**
     * This method helps in the creation About screen
     *
     * @param savedInstanceState Data bundle
     */
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

    public void getOpenFacebookIntent(View view) {

        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + "1921335171459985"));
            intent.setPackage("com.facebook.katana");
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + "1921335171459985")));
        }

    }

    public void getOpenGithubIntent(View view) {

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/commons-app/apps-android-commons\\"));
        startActivity(browserIntent);
    }

    public void getOpenWebsiteIntent(View view) {

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://commons-app.github.io/\\"));
        startActivity(browserIntent);
    }
}