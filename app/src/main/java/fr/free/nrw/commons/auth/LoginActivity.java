package fr.free.nrw.commons.auth;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.View.VISIBLE;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static fr.free.nrw.commons.CommonsApplication.loginMessageIntentKey;
import static fr.free.nrw.commons.CommonsApplication.loginUsernameIntentKey;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.login.LoginCallback;
import fr.free.nrw.commons.auth.login.LoginResult;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.databinding.ActivityLoginBinding;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.di.CommonsApplicationComponent;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.ActivityUtils;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.Locale;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class LoginActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @Inject
    SystemThemeUtils systemThemeUtils;

    private LoginViewModel model;
    private ActivityLoginBinding binding;
    private ProgressDialog progressDialog;
    private LoginTextWatcher textWatcher = new LoginTextWatcher();
    private LoginCallback loginCallback = new LoginCallbackImpl();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonsApplicationComponent commonsApplicationComponent = ApplicationlessInjection
            .getInstance(this.getApplicationContext())
            .getCommonsApplicationComponent();
        commonsApplicationComponent.inject(this);

        model = new ViewModelProvider(this,
            new LoginViewModelFactory(commonsApplicationComponent)).get(LoginViewModel.class);

        boolean isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        setTheme(isDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginUsername.addTextChangedListener(textWatcher);
        binding.loginPassword.addTextChangedListener(textWatcher);
        binding.loginTwoFactor.addTextChangedListener(textWatcher);

        binding.skipLogin.setOnClickListener(view -> skipLogin());
        binding.forgotPassword.setOnClickListener(view -> Utils.handleWebUrl(
            this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL)));
        binding.aboutPrivacyPolicy.setOnClickListener(view -> Utils.handleWebUrl(
            this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)));
        binding.signUpButton.setOnClickListener(view -> startActivity(new Intent(this, SignupActivity.class)));
        binding.loginButton.setOnClickListener(view -> performLogin());

        binding.loginPassword.setOnEditorActionListener(this::onEditorAction);
        binding.loginPassword.setOnFocusChangeListener(this::onPasswordFocusChanged);

        if (ConfigUtils.isBetaFlavour()) {
            binding.loginCredentials.setText(getString(R.string.login_credential));
        } else {
            binding.loginCredentials.setVisibility(View.GONE);
        }

        String message = getIntent().getStringExtra(loginMessageIntentKey);
        if (message != null) {
            showMessage(message, R.color.secondaryDarkColor);
        }

        String username = getIntent().getStringExtra(loginUsernameIntentKey);
        if (username != null) {
            binding.loginUsername.setText(username);
        }
    }

    /**
     * Hides the keyboard if the user's focus is not on the password (hasFocus is false).
     *
     * @param view     The keyboard
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
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (progressDialog != null && progressDialog.isShowing()) {
                hideProgress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.loginUsername.removeTextChangedListener(textWatcher);
        binding.loginPassword.removeTextChangedListener(textWatcher);
        binding.loginTwoFactor.removeTextChangedListener(textWatcher);

        binding = null;
        super.onDestroy();
    }

    public void performLogin() {
        final String username = Objects.requireNonNull(binding.loginUsername.getText()).toString();
        final String password = Objects.requireNonNull(binding.loginPassword.getText()).toString();
        final String twoFactorCode = Objects.requireNonNull(binding.loginTwoFactor.getText())
            .toString();

        showLoggingProgressBar();

        model.doLogin(
            username, password, twoFactorCode, Locale.getDefault().getLanguage(), loginCallback
        );
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        });
    }

    private void showPasswordResetPrompt() {
        showMessageAndCancelDialog(getString(R.string.you_must_reset_your_passsword));
    }

    /**
     * This function is called when user skips the login. It redirects the user to Explore
     * Activity.
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
        sessionManager.setUserLoggedIn(true);
        sessionManager.updateAccount(loginResult);
        showMessage(R.string.login_success, R.color.primaryDarkColor);
        hideProgress();
        startMainActivity();
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

    public void askUserForTwoFactorAuth() {
        hideProgress();
        binding.twoFactorContainer.setVisibility(VISIBLE);
        binding.loginTwoFactor.setVisibility(VISIBLE);
        binding.loginTwoFactor.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(
            Context.INPUT_METHOD_SERVICE);
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

    public void startMainActivity() {
        ActivityUtils.startActivityWithFlags(this, MainActivity.class,
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
        outState.putBoolean("ProgressDailog_state",
            progressDialog != null && progressDialog.isShowing());
        outState.putString("errorMessage",
            binding.errorMessage.getText().toString()); //Save the errorMessage
        outState.putString("username",
            binding.loginUsername.getText().toString()); // Save the username
        outState.putString("password",
            binding.loginPassword.getText().toString()); // Save the password
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        binding.loginUsername.setText(savedInstanceState.getString("username"));
        binding.loginPassword.setText(savedInstanceState.getString("password"));
        if (savedInstanceState.getBoolean("ProgressDailog_state")) {
            performLogin();
        }
        String errorMessage = savedInstanceState.getString("errorMessage");
        if (sessionManager.isUserLoggedIn()) {
            showMessage(R.string.login_success, R.color.primaryDarkColor);
        } else {
            showMessage(errorMessage, R.color.secondaryDarkColor);
        }
    }

    private class LoginCallbackImpl implements LoginCallback {

        @Override
        public void success(@NonNull LoginResult loginResult) {
            Log.e("###", "success = Thread = "+Thread.currentThread().getName());
            runOnUiThread(() -> {
                Timber.d("Login Success");
                hideProgress();
                onLoginSuccess(loginResult);
            });
        }

        @Override
        public void twoFactorPrompt(@NonNull Throwable caught, @Nullable String token) {
            Log.e("###", "twoFactorPrompt = Thread = "+Thread.currentThread().getName());
            runOnUiThread(() -> {
                Timber.d("Requesting 2FA prompt");
                hideProgress();
                askUserForTwoFactorAuth();
            });
        }

        @Override
        public void passwordResetPrompt(@Nullable String token) {
            Log.e("###", "passwordResetPrompt = Thread = "+Thread.currentThread().getName());
            runOnUiThread(() -> {
                Timber.d("Showing password reset prompt");
                hideProgress();
                showPasswordResetPrompt();
            });
        }

        @Override
        public void error(@NonNull Throwable caught) {
            Log.e("###", "error = Thread = "+Thread.currentThread().getName());
            runOnUiThread(() -> {
                Timber.e(caught);
                hideProgress();
                showMessageAndCancelDialog(caught.getLocalizedMessage());
            });
        }
    }
}
