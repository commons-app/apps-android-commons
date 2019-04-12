package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.utils.ConfigUtils;

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
        versionText.setText(ConfigUtils.getVersionNameWithSha(getApplicationContext()));
        TextView rate_us = findViewById(R.id.about_rate_us);
        TextView privacy_policy = findViewById(R.id.about_privacy_policy);
        TextView translate = findViewById(R.id.about_translate);
        TextView credits = findViewById(R.id.about_credits);
        TextView faq = findViewById(R.id.about_faq);

        rate_us.setText(StringUtil.fromHtml(getString(R.string.about_rate_us)));
        privacy_policy.setText(StringUtil.fromHtml(getString(R.string.about_privacy_policy)));
        translate.setText(StringUtil.fromHtml(getString(R.string.about_translate)));
        credits.setText(StringUtil.fromHtml(getString(R.string.about_credits)));
        faq.setText(StringUtil.fromHtml(getString(R.string.about_faq)));

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
                String shareText = "Upload photos to Wikimedia Commons on your phone\nDownload the Commons app: http://play.google.com/store/apps/details?id=fr.free.nrw.commons";
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share app via..."));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.about_translate)
    public void launchTranslate(View view) {
        final ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(AboutActivity.this,
                android.R.layout.simple_spinner_dropdown_item, CommonsApplication.getInstance().getLanguageLookUpTable().getLocalizedNames());
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
                    Utils.handleWebUrl(AboutActivity.this,Uri.parse("https://translatewiki.net/w/i.php?title=Special:Translate&language="
                            + langCode + "&group=commons-android-strings&filter=%21translated&action=translate ?"));
                });
        builder.setNegativeButton(R.string.about_translate_cancel, (dialog, which) -> finish());
        builder.create().show();

    }

}
