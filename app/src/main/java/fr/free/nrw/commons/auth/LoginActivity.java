package fr.free.nrw.commons.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.explore.categories.ExploreActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.ui.widget.HtmlTextView;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static fr.free.nrw.commons.auth.AccountUtil.AUTH_TOKEN_TYPE;

public class LoginActivity extends AccountAuthenticatorActivity {

    @Inject MediaWikiApi mwApi;
    @Inject SessionManager sessionManager;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @BindView(R.id.loginButton) Button loginButton;
    @BindView(R.id.signupButton) Button signupButton;
    @BindView(R.id.loginUsername) EditText usernameEdit;
    @BindView(R.id.loginPassword) EditText passwordEdit;
    @BindView(R.id.loginTwoFactor) EditText twoFactorEdit;
    @BindView(R.id.error_message_container) ViewGroup errorMessageContainer;
    @BindView(R.id.error_message) TextView errorMessage;
    @BindView(R.id.login_credentials) TextView loginCredentials;
    @BindView(R.id.two_factor_container) TextInputLayout twoFactorContainer;
    @BindView(R.id.forgotPassword) HtmlTextView forgotPasswordText;
    @BindView(R.id.skipLogin) HtmlTextView skipLoginText;

    ProgressDialog progressDialog;
    private AppCompatDelegate delegate;
    private LoginTextWatcher textWatcher = new LoginTextWatcher();

    private Boolean loginCurrentlyInProgress = false;
    private Boolean errorMessageShown = false;
    private String  resultantError;
    private static final String RESULTANT_ERROR = "resultantError";
    private static final String ERROR_MESSAGE_SHOWN = "errorMessageShown";
    private static final String LOGGING_IN = "loggingIn";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(this.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        boolean isDarkTheme = applicationKvStore.getBoolean("theme", false);
        setTheme(isDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        usernameEdit.addTextChangedListener(textWatcher);
        usernameEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ViewUtil.hideKeyboard(v);
            }
        });

        passwordEdit.addTextChangedListener(textWatcher);
        passwordEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ViewUtil.hideKeyboard(v);
            }
        });

        twoFactorEdit.addTextChangedListener(textWatcher);
        passwordEdit.setOnEditorActionListener(newLoginInputActionListener());

        loginButton.setOnClickListener(view -> performLogin());
        signupButton.setOnClickListener(view -> signUp());

        forgotPasswordText.setOnClickListener(view -> forgotPassword());
        skipLoginText.setOnClickListener(view -> new AlertDialog.Builder(this).setTitle(R.string.skip_login_title)
                .setMessage(R.string.skip_login_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.cancel();
                    skipLogin();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                .show());

        if (ConfigUtils.isBetaFlavour()) {
            loginCredentials.setText(getString(R.string.login_credential));
        } else {
            loginCredentials.setVisibility(View.GONE);
        }
    }

    /**
     * This function is called when user skips the login.
     * It redirects the user to Explore Activity.
     */
    private void skipLogin() {
        applicationKvStore.putBoolean("login_skipped", true);
        ExploreActivity.startYourself(this);
        finish();

    }

    private void forgotPassword() {
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL));
    }

    @OnClick(R.id.about_privacy_policy)
    void onPrivacyPolicyClicked() {
        Utils.handleWebUrl(this,Uri.parse("https://github.com/commons-app/apps-android-commons/wiki/Privacy-policy\\"));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (applicationKvStore.getBoolean("firstrun", true)) {
            WelcomeActivity.startYourself(this);
        }

        if (sessionManager.getCurrentAccount() != null
                && sessionManager.isUserLoggedIn()
                && sessionManager.getCachedAuthCookie() != null) {
            applicationKvStore.putBoolean("login_skipped", false);
            sessionManager.revalidateAuthToken();
            startMainActivity();
        }

        if (applicationKvStore.getBoolean("login_skipped", false)) {
            skipLogin();
        }

    }

    @Override
    protected void onDestroy() {
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        usernameEdit.removeTextChangedListener(textWatcher);
        passwordEdit.removeTextChangedListener(textWatcher);
        twoFactorEdit.removeTextChangedListener(textWatcher);
        delegate.onDestroy();
        super.onDestroy();
    }

    private void performLogin() {
        loginCurrentlyInProgress = true;
        Timber.d("Login to start!");
        final String username = canonicializeUsername(usernameEdit.getText().toString());
        final String rawUsername = StringUtils.capitalize(usernameEdit.getText().toString().trim());
        final String password = passwordEdit.getText().toString();
        String twoFactorCode = twoFactorEdit.getText().toString();

        showLoggingProgressBar();
        Observable.fromCallable(() -> login(username, password, twoFactorCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> handleLogin(username, rawUsername, password, result));
    }

    private String login(String username, String password, String twoFactorCode) {
        try {
            if (twoFactorCode.isEmpty()) {
                return mwApi.login(username, password);
            } else {
                return mwApi.login(username, password, twoFactorCode);
            }
        } catch (IOException e) {
            // Do something better!
            return "NetworkFailure";
        }
    }

    private void handleLogin(String username, String rawUsername, String password, String result) {
        Timber.d("Login done!");
        if (result.equals("PASS")) {
            handlePassResult(username, rawUsername , password);
        } else {
            loginCurrentlyInProgress = false;
            errorMessageShown = true;
            resultantError = result;
            handleOtherResults(result);
        }
    }

    private void showLoggingProgressBar() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(getString(R.string.logging_in_title));
        progressDialog.setMessage(getString(R.string.logging_in_message));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void handlePassResult(String username, String rawUsername, String password) {
        showSuccessAndDismissDialog();
        requestAuthToken();
        AccountAuthenticatorResponse response = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Timber.d("Bundle of extras: %s", extras);
            response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            if (response != null) {
                Bundle authResult = new Bundle();
                authResult.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                authResult.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.ACCOUNT_TYPE);
                response.onResult(authResult);
            }
        }

        sessionManager.createAccount(response, username, rawUsername, password);
        startMainActivity();
    }

    protected void requestAuthToken() {
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = sessionManager.getCurrentAccount();
        if (curAccount != null) {
            accountManager.setAuthToken(curAccount, AUTH_TOKEN_TYPE, mwApi.getAuthCookie());
        }
    }

    /**
     * Match known failure message codes and provide messages.
     *
     * @param result String
     */
    private void handleOtherResults(String result) {
        if (result.equals("NetworkFailure")) {
            // Matches NetworkFailure which is created by the doInBackground method
            showMessageAndCancelDialog(R.string.login_failed_network);
        } else if (result.toLowerCase(Locale.getDefault()).contains("nosuchuser".toLowerCase()) || result.toLowerCase().contains("noname".toLowerCase())) {
            // Matches nosuchuser, nosuchusershort, noname
            showMessageAndCancelDialog(R.string.login_failed_wrong_credentials);
            emptySensitiveEditFields();
        } else if (result.toLowerCase(Locale.getDefault()).contains("wrongpassword".toLowerCase())) {
            // Matches wrongpassword, wrongpasswordempty
            showMessageAndCancelDialog(R.string.login_failed_wrong_credentials);
            emptySensitiveEditFields();
        } else if (result.toLowerCase(Locale.getDefault()).contains("throttle".toLowerCase())) {
            // Matches unknown throttle error codes
            showMessageAndCancelDialog(R.string.login_failed_throttled);
        } else if (result.toLowerCase(Locale.getDefault()).contains("userblocked".toLowerCase())) {
            // Matches login-userblocked
            showMessageAndCancelDialog(R.string.login_failed_blocked);
        } else if (result.equals("2FA")) {
            askUserForTwoFactorAuth();
        } else {
            // Occurs with unhandled login failure codes
            Timber.d("Login failed with reason: %s", result);
            showMessageAndCancelDialog(R.string.login_failed_generic);
        }
    }

    /**
     * Because Mediawiki is upercase-first-char-then-case-sensitive :)
     * @param username String
     * @return String canonicial username
     */
    private String canonicializeUsername(String username) {
        return new PageTitle(username).getText();
    }

    @Override
    protected void onStart() {
        super.onStart();
        delegate.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        delegate.onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @NonNull
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOGGING_IN, loginCurrentlyInProgress);
        outState.putBoolean(ERROR_MESSAGE_SHOWN, errorMessageShown);
        outState.putString(RESULTANT_ERROR, resultantError);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loginCurrentlyInProgress = savedInstanceState.getBoolean(LOGGING_IN, false);
        errorMessageShown = savedInstanceState.getBoolean(ERROR_MESSAGE_SHOWN, false);
        if (loginCurrentlyInProgress) {
            performLogin();
        }
        if (errorMessageShown) {
            resultantError = savedInstanceState.getString(RESULTANT_ERROR);
            handleOtherResults(resultantError);
        }
    }

    public void askUserForTwoFactorAuth() {
        progressDialog.dismiss();
        twoFactorContainer.setVisibility(VISIBLE);
        twoFactorEdit.setVisibility(VISIBLE);
        showMessageAndCancelDialog(R.string.login_failed_2fa_needed);
    }

    public void showMessageAndCancelDialog(@StringRes int resId) {
        showMessage(resId, R.color.secondaryDarkColor);
        if (progressDialog != null){
            progressDialog.cancel();
        }
    }

    public void showSuccessAndDismissDialog() {
        showMessage(R.string.login_success, R.color.primaryDarkColor);
        progressDialog.dismiss();
    }

    public void emptySensitiveEditFields() {
        passwordEdit.setText("");
        twoFactorEdit.setText("");
    }

    public void startMainActivity() {
        NavigationBaseActivity.startActivityWithFlags(this, MainActivity.class, Intent.FLAG_ACTIVITY_SINGLE_TOP);
        finish();
    }

    private void signUp() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private TextView.OnEditorActionListener newLoginInputActionListener() {
        return (textView, actionId, keyEvent) -> {
            if (loginButton.isEnabled()) {
                if (actionId == IME_ACTION_DONE) {
                    performLogin();
                    return true;
                } else if ((keyEvent != null) && keyEvent.getKeyCode() == KEYCODE_ENTER) {
                    performLogin();
                    return true;
                }
            }
            return false;
        };
    }

    private void showMessage(@StringRes int resId, @ColorRes int colorResId) {
        errorMessage.setText(getString(resId));
        errorMessage.setTextColor(ContextCompat.getColor(this, colorResId));
        errorMessageContainer.setVisibility(VISIBLE);
    }

    private AppCompatDelegate getDelegate() {
        if (delegate == null) {
            delegate = AppCompatDelegate.create(this, null);
        }
        return delegate;
    }

    private class LoginTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            boolean enabled = usernameEdit.getText().length() != 0 && passwordEdit.getText().length() != 0
                    && (BuildConfig.DEBUG || twoFactorEdit.getText().length() != 0 || twoFactorEdit.getVisibility() != VISIBLE);
            loginButton.setEnabled(enabled);
        }
    }

    public static void startYourself(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }
}
