package fr.free.nrw.commons.auth;

import android.accounts.AccountAuthenticatorActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDelegate;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import timber.log.Timber;

import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class LoginActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USERNAME = "fr.free.nrw.commons.login.username";

    @BindView(R.id.loginButton) Button loginButton;
    @BindView(R.id.signupButton) Button signupButton;
    @BindView(R.id.loginUsername) EditText usernameEdit;
    @BindView(R.id.loginPassword) EditText passwordEdit;
    @BindView(R.id.loginTwoFactor) EditText twoFactorEdit;
    @BindView(R.id.error_message_container) ViewGroup errorMessageContainer;
    @BindView(R.id.error_message) TextView errorMessage;

    private CommonsApplication app;
    ProgressDialog progressDialog;
    private AppCompatDelegate delegate;
    private SharedPreferences prefs = null;
    private LoginTextWatcher textWatcher = new LoginTextWatcher();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.isDarkTheme(this) ? R.style.DarkAppTheme : R.style.LightAppTheme);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        app = CommonsApplication.getInstance();

        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        prefs = getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);

        usernameEdit.addTextChangedListener(textWatcher);
        passwordEdit.addTextChangedListener(textWatcher);
        twoFactorEdit.addTextChangedListener(textWatcher);
        passwordEdit.setOnEditorActionListener(newLoginInputActionListener());

        loginButton.setOnClickListener(view -> performLogin());
        signupButton.setOnClickListener(view -> signUp());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.getBoolean("firstrun", true)) {
            WelcomeActivity.startYourself(this);
            prefs.edit().putBoolean("firstrun", false).apply();
        }
        if (app.getCurrentAccount() != null) {
            startMainActivity();
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
        if (BuildConfig.DEBUG) {
            twoFactorEdit.setVisibility(View.VISIBLE);
            showMessageAndCancelDialog(R.string.login_failed_2fa_needed);
        } else {
            showMessageAndCancelDialog(R.string.login_failed_2fa_not_supported);
        }
    }

    public void showMessageAndCancelDialog(@StringRes int resId) {
        showMessage(resId, R.color.secondaryDarkColor);
        progressDialog.cancel();
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
        NavigationBaseActivity.startActivityWithFlags(this, ContributionsActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
    }

    private void performLogin() {
        Timber.d("Login to start!");
        LoginTask task = getLoginTask();
        task.execute();
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

    private LoginTask getLoginTask() {
        return new LoginTask(
                this,
                canonicializeUsername(usernameEdit.getText().toString()),
                passwordEdit.getText().toString(),
                twoFactorEdit.getText().toString()
        );
    }

    /**
     * Because Mediawiki is upercase-first-char-then-case-sensitive :)
     * @param username String
     * @return String canonicial username
     */
    private String canonicializeUsername(String username) {
        return new PageTitle(username).getText();
    }

    private void showMessage(@StringRes int resId, @ColorRes int colorResId) {
        errorMessage.setText(getString(resId));
        errorMessage.setTextColor(ContextCompat.getColor(this, colorResId));
        errorMessageContainer.setVisibility(View.VISIBLE);
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
                    && (BuildConfig.DEBUG || twoFactorEdit.getText().length() != 0 || twoFactorEdit.getVisibility() != View.VISIBLE);
            loginButton.setEnabled(enabled);
        }
    }
}
