package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;

import static android.widget.Toast.LENGTH_SHORT;

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
    @SuppressLint("StringFormatInvalid")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);
        String aboutText = getString(R.string.about_license);
        aboutLicenseText.setHtmlText(aboutText);

        versionText.setText(BuildConfig.VERSION_NAME);
        initDrawer();
    }

    @OnClick(R.id.facebook_launch_icon)
    public void launchFacebook(View view) {
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + "1921335171459985"));
            intent.setPackage("com.facebook.katana");
            startActivity(intent);
        } catch (Exception e) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + "1921335171459985\\"));
            if(intent.resolveActivity(this.getPackageManager()) != null){
                Utils.handleWebUrl(this,Uri.parse("https://www.facebook.com/" + "1921335171459985"));
            } else {
                Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @OnClick(R.id.github_launch_icon)
    public void launchGithub(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/commons-app/apps-android-commons\\"));
        //check if web browser available
        if (browserIntent.resolveActivity(this.getPackageManager()) != null) {
            Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons\\"));
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }
    }

    @OnClick(R.id.website_launch_icon)
    public void launchWebsite(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://commons-app.github.io/\\"));
        if (browserIntent.resolveActivity(this.getPackageManager()) != null) {
            Utils.handleWebUrl(this,Uri.parse("https://commons-app.github.io/\\"));
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }
    }

    @OnClick(R.id.about_rate_us)
    public void launchRatings(View view){
        Utils.rateApp(this);
    }

    @OnClick(R.id.about_credits)
    public void launchCredits(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/commons-app/apps-android-commons/blob/master/CREDITS/\\"));
        if (browserIntent.resolveActivity(this.getPackageManager()) != null) {
            Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/blob/master/CREDITS/\\"));
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }
    }

    @OnClick(R.id.about_privacy_policy)
    public void launchPrivacyPolicy(View view) {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/commons-app/apps-android-commons/wiki/Privacy-policy\\"));
        if (browserIntent.resolveActivity(this.getPackageManager()) != null) {
            Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/wiki/Privacy-policy\\"));
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.no_web_browser), LENGTH_SHORT);
            toast.show();
        }
    }

}
