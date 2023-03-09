package fr.free.nrw.commons;

import android.annotation.SuppressLint;
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
import androidx.annotation.NonNull;
import fr.free.nrw.commons.databinding.ActivityAboutBinding;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.DialogUtil;
import java.util.Collections;
import java.util.List;

/**
 * Represents about screen of this app
 */
public class AboutActivity extends BaseActivity {

    /*
      This View Binding class is auto-generated for each xml file. The format is usually the name
      of the file with PascalCasing (The underscore characters will be ignored).
      More information is available at https://developer.android.com/topic/libraries/view-binding
     */
    private ActivityAboutBinding binding;

    /**
     * This method helps in the creation About screen
     *
     * @param savedInstanceState Data bundle
     */
    @Override
    @SuppressLint("StringFormatInvalid")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
          Instead of just setting the view with the xml file. We need to use View Binding class.
         */
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        setSupportActionBar(binding.toolbarBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final String aboutText = getString(R.string.about_license);
        /*
          We can then access all the views by just using the id names like this.
          camelCasing is used with underscore characters being ignored.
         */
        binding.aboutLicense.setHtmlText(aboutText);

        @SuppressLint("StringFormatMatches")
        String improveText = String.format(getString(R.string.about_improve), Urls.NEW_ISSUE_URL);
        binding.aboutImprove.setHtmlText(improveText);
        binding.aboutVersion.setText(ConfigUtils.getVersionNameWithSha(getApplicationContext()));

        Utils.setUnderlinedText(binding.aboutFaq, R.string.about_faq, getApplicationContext());
        Utils.setUnderlinedText(binding.aboutRateUs, R.string.about_rate_us, getApplicationContext());
        Utils.setUnderlinedText(binding.aboutUserGuide, R.string.user_guide, getApplicationContext());
        Utils.setUnderlinedText(binding.aboutPrivacyPolicy, R.string.about_privacy_policy, getApplicationContext());
        Utils.setUnderlinedText(binding.aboutTranslate, R.string.about_translate, getApplicationContext());
        Utils.setUnderlinedText(binding.aboutCredits, R.string.about_credits, getApplicationContext());

        /*
          To set listeners, we can create a separate method and use lambda syntax.
        */
        binding.facebookLaunchIcon.setOnClickListener(this::launchFacebook);
        binding.githubLaunchIcon.setOnClickListener(this::launchGithub);
        binding.websiteLaunchIcon.setOnClickListener(this::launchWebsite);
        binding.aboutRateUs.setOnClickListener(this::launchRatings);
        binding.aboutCredits.setOnClickListener(this::launchCredits);
        binding.aboutPrivacyPolicy.setOnClickListener(this::launchPrivacyPolicy);
        binding.aboutUserGuide.setOnClickListener(this::launchUserGuide);
        binding.aboutFaq.setOnClickListener(this::launchFrequentlyAskedQuesions);
        binding.aboutTranslate.setOnClickListener(this::launchTranslate);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

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

    public void launchGithub(View view) {
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Urls.GITHUB_REPO_URL));
            intent.setPackage(Urls.GITHUB_PACKAGE_NAME);
            startActivity(intent);
        } catch (Exception e) {
            Utils.handleWebUrl(this, Uri.parse(Urls.GITHUB_REPO_URL));
        }
    }

    public void launchWebsite(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.WEBSITE_URL));
    }

    public void launchRatings(View view){
        Utils.rateApp(this);
    }

    public void launchCredits(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.CREDITS_URL));
    }

    public void launchUserGuide(View view) {
        Utils.handleWebUrl(this, Uri.parse(Urls.USER_GUIDE_URL));
    }

    public void launchPrivacyPolicy(View view) {
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL));
    }

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

        Runnable positiveButtonRunnable = () -> {
            String langCode = CommonsApplication.getInstance().getLanguageLookUpTable().getCodes().get(spinner.getSelectedItemPosition());
            Utils.handleWebUrl(AboutActivity.this, Uri.parse(Urls.TRANSLATE_WIKI_URL + langCode));
        };
        DialogUtil.showAlertDialog(this,
            getString(R.string.about_translate_title),
            getString(R.string.about_translate_message),
            getString(R.string.about_translate_proceed),
            getString(R.string.about_translate_cancel),
            positiveButtonRunnable,
            () -> {},
            spinner,
            true);
    }

}
