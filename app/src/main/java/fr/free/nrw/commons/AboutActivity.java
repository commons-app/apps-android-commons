package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import fr.free.nrw.commons.theme.BaseActivity;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.utils.ConfigUtils;

/**
 * Represents about screen of this app
 */
public class AboutActivity extends BaseActivity {
    @BindView(R.id.about_version) TextView versionText;
    @BindView(R.id.about_license) HtmlTextView aboutLicenseText;
    @BindView(R.id.about_faq) TextView faqText;
    @BindView(R.id.about_improve) HtmlTextView improve;
    @BindView(R.id.about_rate_us) TextView rateUsText;
    @BindView(R.id.about_privacy_policy) TextView privacyPolicyText;
    @BindView(R.id.about_translate) TextView translateText;
    @BindView(R.id.about_credits) TextView creditsText;
    @BindView(R.id.toolbar) Toolbar toolbar;
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
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String aboutText = getString(R.string.about_license);
        aboutLicenseText.setHtmlText(aboutText);

        @SuppressLint("StringFormatMatches")
        String improveText = String.format(getString(R.string.about_improve), Urls.NEW_ISSUE_URL);
        improve.setHtmlText(improveText);

        versionText.setText(ConfigUtils.getVersionNameWithSha(getApplicationContext()));

        Utils.setUnderlinedText(faqText, R.string.about_faq, getApplicationContext());
        Utils.setUnderlinedText(rateUsText, R.string.about_rate_us, getApplicationContext());
        Utils.setUnderlinedText(privacyPolicyText, R.string.about_privacy_policy, getApplicationContext());
        Utils.setUnderlinedText(translateText, R.string.about_translate, getApplicationContext());
        Utils.setUnderlinedText(creditsText, R.string.about_credits, getApplicationContext());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @OnClick(R.id.facebook_launch_icon)
    public void launchFacebook(View view) {
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.FACEBOOK_APP_URL));
            intent.setPackage(Urls.FACEBOOK_PACKAGE_NAME);
            startActivity(intent);
        } catch (Exception e) {
            Utils.handleWebUrl(this, Uri.parse(Urls.FACEBOOK_WEB_URL));
        }
    }

    @OnClick(R.id.github_launch_icon)
    public void launchGithub(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.GITHUB_REPO_URL));
    }

    @OnClick(R.id.website_launch_icon)
    public void launchWebsite(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.WEBSITE_URL));
    }

    @OnClick(R.id.about_rate_us)
    public void launchRatings(View view){
        Utils.rateApp(this);
    }

    @OnClick(R.id.about_credits)
    public void launchCredits(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.CREDITS_URL));
    }

    @OnClick(R.id.about_privacy_policy)
    public void launchPrivacyPolicy(View view) {
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL));
    }


    @OnClick(R.id.about_faq)
    public void launchFrequentlyAskedQuesions(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.FAQ_URL));
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
                String shareText = String.format(getString(R.string.share_text), Urls.PLAY_STORE_URL_PREFIX + this.getPackageName());
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.about_translate)
    public void launchTranslate(View view) {
        @NonNull List<String> sortedLocalizedNamesRef = CommonsApplication.getInstance().getLanguageLookUpTable().getCanonicalNames();
        Collections.sort(sortedLocalizedNamesRef);
        final ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(AboutActivity.this,
                android.R.layout.simple_spinner_dropdown_item, sortedLocalizedNamesRef);
        final Spinner spinner = new Spinner(AboutActivity.this);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setAdapter(languageAdapter);
        spinner.setGravity(17);
        spinner.setPadding(50,0,0,0);
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
        builder.setView(spinner);
        builder.setTitle(R.string.about_translate_title)
                .setMessage(R.string.about_translate_message)
                .setPositiveButton(R.string.about_translate_proceed, (dialog, which) -> {
                    String langCode = CommonsApplication.getInstance().getLanguageLookUpTable().getCodes().get(spinner.getSelectedItemPosition());
                    Utils.handleWebUrl(AboutActivity.this, Uri.parse(Urls.TRANSLATE_WIKI_URL + langCode));
                });
        builder.setNegativeButton(R.string.about_translate_cancel, (dialog, which) -> dialog.cancel());
        builder.create().show();

    }

}
