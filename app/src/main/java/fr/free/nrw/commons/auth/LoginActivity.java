package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import fr.free.nrw.commons.databinding.ActivityLoginBinding;
import fr.free.nrw.commons.utils.ActivityUtils;
import java.util.Locale;
import org.wikipedia.AppAdapter;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.login.LoginClient;
import org.wikipedia.login.LoginClient.LoginCallback;
import org.wikipedia.login.LoginResult;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.disposables.CompositeDisposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_WIKI_SITE;

public class LoginActivity extends AccountAuthenticatorActivity {

    @Inject
    SessionManager sessionManager;

    @Inject
    @Named(NAMED_COMMONS_WIKI_SITE)
    WikiSite commonsWikiSite;

    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @Inject
    LoginClient loginClient;

    @Inject
    SystemThemeUtils systemThemeUtils;

    private ActivityLoginBinding binding;
    ProgressDialog progressDialog;
    private AppCompatDelegate delegate;
    private LoginTextWatcher textWatcher = new LoginTextWatcher();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Call<MwQueryResponse> loginToken;
    final  String saveProgressDailog="ProgressDailog_state";
    final String saveErrorMessage ="errorMessage";
    final String saveUsername="username";
    final  String savePassword="password";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ApplicationlessInjection
                .getInstance(this.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        boolean isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        setTheme(isDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginUsername.addTextChangedListener(textWatcher);
        binding.loginPassword.addTextChangedListener(textWatcher);
        binding.loginTwoFactor.addTextChangedListener(textWatcher);

        binding.skipLogin.setOnClickListener(view -> skipLogin());
        binding.forgotPassword.setOnClickListener(view -> forgotPassword());
        binding.aboutPrivacyPolicy.setOnClickListener(view -> onPrivacyPolicyClicked());
        binding.signUpButton.setOnClickListener(view -> signUp());
        binding.loginButton.setOnClickListener(view -> performLogin());

        binding.loginPassword.setOnEditorActionListener(this::onEditorAction);
        binding.loginPassword.setOnFocusChangeListener(this::onPasswordFocusChanged);

        if (ConfigUtils.isBetaFlavour()) {
            binding.loginCredentials.setText(getString(R.string.login_credential));
        } else {
            binding.loginCredentials.setVisibility(View.GONE);
        }
    }
    /** 
     * Hides the keyboard if the user's focus is not on the password (hasFocus is false).
     * @param view The keyboard
     * @param hasFocus Set to true if the keyboard has focus
     */
    void onPasswordFocusChanged(View view, boolean hasFocus) {
        if (!hasFocus) {
            ViewUtil.hideKeyboard(view);
        }
    }

    boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (binding.loginButton.isEnabled()) {
            if (actionId == IME_ACTION_DONE) {
                performLogin();
                return true;
            } else if ((keyEvent != null) && keyEvent.getKeyCode() == KEYCODE_ENTER) {
                performLogin();
                return true;
            }
        }
        return false;
    }


    protected void skipLogin() {
        new AlertDialog.Builder(this).setTitle(R.string.skip_login_title)
                .setMessage(R.string.skip_login_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.cancel();
                    performSkipLogin();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                .show();
    }

    protected void forgotPassword() {
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL));
    }

    protected void onPrivacyPolicyClicked() {
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL));
    }

    protected void signUp() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sessionManager.getCurrentAccount() != null
                && sessionManager.isUserLoggedIn()) {
            applicationKvStore.putBoolean("login_skipped", false);
            startMainActivity();
        }

        if (applicationKvStore.getBoolean("login_skipped", false)) {
            performSkipLogin();
        }

    }

    @Override
    protected void onDestroy() {
        compositeDisposable.clear();
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        binding.loginUsername.removeTextChangedListener(textWatcher);
        binding.loginPassword.removeTextChangedListener(textWatcher);
        binding.loginTwoFactor.removeTextChangedListener(textWatcher);
        delegate.onDestroy();
        if(null!=loginClient) {
            loginClient.cancel();
        }
        binding = null;
        super.onDestroy();
    }

    public void performLogin() {
        Timber.d("Login to start!");
        final String username = binding.loginUsername.getText().toString();
        final String rawUsername = binding.loginUsername.getText().toString().trim();
        final String password = binding.loginPassword.getText().toString();
        String twoFactorCode = binding.loginTwoFactor.getText().toString();

        showLoggingProgressBar();
        doLogin(username, password, twoFactorCode);
    }

    private void doLogin(String username, String password, String twoFactorCode) {
        progressDialog.show();
        loginToken = ServiceFactory.get(commonsWikiSite).getLoginToken();
        loginToken.enqueue(
                new Callback<MwQueryResponse>() {
                    @Override
                    public void onResponse(Call<MwQueryResponse> call,
                                           Response<MwQueryResponse> response) {
                        loginClient.login(commonsWikiSite, username, password, null, twoFactorCode,
                                response.body().query().loginToken(), Locale.getDefault().getLanguage(), new LoginCallback() {
                                    @Override
                                    public void success(@NonNull LoginResult result) {
                                        Timber.d("Login Success");
                                        onLoginSuccess(result);
                                    }

                                    @Override
                                    public void twoFactorPrompt(@NonNull Throwable caught,
                                                                @Nullable String token) {
                                        Timber.d("Requesting 2FA prompt");
                                        hideProgress();
                                        askUserForTwoFactorAuth();
                                    }

                                    @Override
                                    public void passwordResetPrompt(@Nullable String token) {
                                        Timber.d("Showing password reset prompt");
                                        hideProgress();
                                        showPasswordResetPrompt();
                                    }

                                    @Override
                                    public void error(@NonNull Throwable caught) {
                                        Timber.e(caught);
                                        hideProgress();
                                        showMessageAndCancelDialog(caught.getLocalizedMessage());
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<MwQueryResponse> call, Throwable t) {
                        Timber.e(t);
                        showMessageAndCancelDialog(t.getLocalizedMessage());
                    }
                });

    }

    private void hideProgress() {
        progressDialog.dismiss();
    }

    private void showPasswordResetPrompt() {
        showMessageAndCancelDialog(getString(R.string.you_must_reset_your_passsword));
    }


    /**
     * This function is called when user skips the login.
     * It redirects the user to Explore Activity.
     */
    private void performSkipLogin() {
        applicationKvStore.putBoolean("login_skipped", true);
        MainActivity.startYourself(this);
        finish();
    }

    private void showLoggingProgressBar() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle(getString(R.string.logging_in_title));
        progressDialog.setMessage(getString(R.string.logging_in_message));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void onLoginSuccess(LoginResult loginResult) {
        if (!progressDialog.isShowing()) {
            // no longer attached to activity!
            return;
        }
        compositeDisposable.clear();
        sessionManager.setUserLoggedIn(true);
        AppAdapter.get().updateAccount(loginResult);
        progressDialog.dismiss();
        showSuccessAndDismissDialog();
        startMainActivity();
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

    public void askUserForTwoFactorAuth() {
        progressDialog.dismiss();
        binding.twoFactorContainer.setVisibility(VISIBLE);
        binding.loginTwoFactor.setVisibility(VISIBLE);
        binding.loginTwoFactor.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        showMessageAndCancelDialog(R.string.login_failed_2fa_needed);
    }

    public void showMessageAndCancelDialog(@StringRes int resId) {
        showMessage(resId, R.color.secondaryDarkColor);
        if (progressDialog != null) {
            progressDialog.cancel();
        }
    }

    public void showMessageAndCancelDialog(String error) {
        showMessage(error, R.color.secondaryDarkColor);
        if (progressDialog != null) {
            progressDialog.cancel();
        }
    }

    public void showSuccessAndDismissDialog() {
        showMessage(R.string.login_success, R.color.primaryDarkColor);
        progressDialog.dismiss();
    }

    public void startMainActivity() {
        ActivityUtils.startActivityWithFlags(this, MainActivity.class, Intent.FLAG_ACTIVITY_SINGLE_TOP);
        finish();
    }

    private void showMessage(@StringRes int resId, @ColorRes int colorResId) {
        binding.errorMessage.setText(getString(resId));
        binding.errorMessage.setTextColor(ContextCompat.getColor(this, colorResId));
        binding.errorMessageContainer.setVisibility(VISIBLE);
    }

    private void showMessage(String message, @ColorRes int colorResId) {
        binding.errorMessage.setText(message);
        binding.errorMessage.setTextColor(ContextCompat.getColor(this, colorResId));
        binding.errorMessageContainer.setVisibility(VISIBLE);
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
            boolean enabled = binding.loginUsername.getText().length() != 0 &&
                binding.loginPassword.getText().length() != 0 &&
                (BuildConfig.DEBUG || binding.loginTwoFactor.getText().length() != 0 ||
                    binding.loginTwoFactor.getVisibility() != VISIBLE);
            binding.loginButton.setEnabled(enabled);
        }
    }

    public static void startYourself(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // if progressDialog is visible during the configuration change  then store state as  true else false so that
        // we maintain visibility of progressDailog after configuration change
        if(progressDialog!=null&&progressDialog.isShowing()) {
            outState.putBoolean(saveProgressDailog,true);
        } else {
            outState.putBoolean(saveProgressDailog,false);
        }
        outState.putString(saveErrorMessage,binding.errorMessage.getText().toString()); //Save the errorMessage
        outState.putString(saveUsername,getUsername()); // Save the username
        outState.putString(savePassword,getPassword()); // Save the password
    }
    private String getUsername() {
        return binding.loginUsername.getText().toString();
    }
    private String getPassword(){
        return  binding.loginPassword.getText().toString();
  }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        binding.loginUsername.setText(savedInstanceState.getString(saveUsername));
        binding.loginPassword.setText(savedInstanceState.getString(savePassword));
        if(savedInstanceState.getBoolean(saveProgressDailog)) {
            performLogin();
        }
        String errorMessage=savedInstanceState.getString(saveErrorMessage);
        if(sessionManager.isUserLoggedIn()) {
            showMessage(R.string.login_success, R.color.primaryDarkColor);
        } else {
            showMessage(errorMessage, R.color.secondaryDarkColor);
        }
    }
}
