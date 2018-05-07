package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

    String language[] = { "Kazakh", "Afrikaans", "Arabic", "Bengali", "Asturianu", "azərbaycanca", "Bikol Central",
    "Bulgarain", "বাংলা", "Bosanski", "Brezhoneg","català","کوردی", " čeština", " kaszëbsczi", "Cymraeg", "dansk", "Deutsch"
    ,"Zazaki", "डोटेली","Ελληνικά","euskara","español","فارسی","suomi", "français" ,"Nordfriisk", "galego", "Hawaiʻi"
    ,"हिन्दी","Hunsrik","עברית","hornjoserbsce","magyar","interlingua","Bahasa Indonesia", "íslenska","Italian","japanese",
    "Basa Jawa", "ქართული", " ភាសាខ្មែរ","ಕನ್ನಡ", "한국어","къарачай-малкъар","Кыргызча", "latina", "Lëtzebuergesch", "lietuvių",
    "latviešu", "Malagasy", "македонски"," മലയാളം","монгол","मराठी","Bahasa Melayu","Malti", "नेपाली",  "norsk bokmål",
    " Nederlands","occitan","ଓଡ଼ିଆ","ਪੰਜਾਬੀ","polsk","Piemontèis","پښتو","português","română","русский"," سنڌي", " සිංහල",
    "slovenčina"," سرائیکی", "svenska", "தமிழ்", "ತುಳು"," తెలుగు"," ไทย", "Türkçe","українська", "اردو", "Tiếng Việt",
    " მარგალური","ייִדיש",};

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
        TextView rate_us = findViewById(R.id.about_rate_us);
        TextView privacy_policy = findViewById(R.id.about_privacy_policy);
        TextView translate = findViewById(R.id.about_translate);
        TextView credits = findViewById(R.id.about_credits);
        TextView faq = findViewById(R.id.about_faq);

        rate_us.setText(Html.fromHtml(getString(R.string.about_rate_us)));
        privacy_policy.setText(Html.fromHtml(getString(R.string.about_privacy_policy)));
        translate.setText(Html.fromHtml(getString(R.string.about_translate)));
        credits.setText(Html.fromHtml(getString(R.string.about_credits)));
        faq.setText(Html.fromHtml(getString(R.string.about_faq)));

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
        final ArrayAdapter<String> languageAdapter = new ArrayAdapter<String>(AboutActivity.this,
                android.R.layout.simple_spinner_item, language);
        final Spinner spinner = new Spinner(AboutActivity.this);
        spinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        spinner.setAdapter(languageAdapter);
        spinner.setGravity(17);

        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
        builder.setView(spinner);
        builder.setTitle(R.string.about_translate_title)
                .setMessage(R.string.about_translate_message)
                .setPositiveButton(R.string.about_translate_proceed, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String languageSelected = spinner.getSelectedItem().toString();
                        TokensTranslations tokensTranslations = new TokensTranslations();
                        tokensTranslations.initailize();
                        String token = tokensTranslations.getTranslationToken(languageSelected);
                        Utils.handleWebUrl(AboutActivity.this,Uri.parse("https://translatewiki.net/w/i.php?title=Special:Translate&language="+token+"&group=commons-android-strings&filter=%21translated&action=translate ?"));
                    }
                });
        builder.setNegativeButton(R.string.about_translate_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();

    }

}
