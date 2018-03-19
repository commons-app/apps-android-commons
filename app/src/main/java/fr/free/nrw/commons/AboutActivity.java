package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    @BindView(R.id.about_faq) TextView faqText;

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
        SpannableString content = new SpannableString(getString(R.string.about_faq));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        faqText.setText(content);
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
            Utils.handleWebUrl(this,Uri.parse("https://www.facebook.com/" + "1921335171459985"));
        }
    }

    @OnClick(R.id.github_launch_icon)
    public void launchGithub(View view) {
        Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons\\"));
    }

    @OnClick(R.id.website_launch_icon)
    public void launchWebsite(View view) {
        Utils.handleWebUrl(this,Uri.parse("https://commons-app.github.io/\\"));
    }

    @OnClick(R.id.about_rate_us)
    public void launchRatings(View view){
        Utils.rateApp(this);
    }

    @OnClick(R.id.about_credits)
    public void launchCredits(View view) {
        Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/blob/master/CREDITS/\\"));
    }

    @OnClick(R.id.about_privacy_policy)
    public void launchPrivacyPolicy(View view) {
        Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/wiki/Privacy-policy\\"));
    }

    @OnClick(R.id.about_faq)
    public void launchFrequentlyAskedQuesions(View view) {
        Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/wiki/Frequently-Asked-Questions\\"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_app_icon:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "http://play.google.com/store/apps/details?id=fr.free.nrw.commons");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share app via..."));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
