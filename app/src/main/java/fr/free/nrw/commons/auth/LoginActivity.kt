package fr.free.nrw.commons.auth

import android.accounts.AccountAuthenticatorActivity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.ActivityLoginBinding
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.utils.AbstractTextWatcher
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.hideKeyboard
import fr.free.nrw.commons.utils.handleKeyboardInsets
import fr.free.nrw.commons.utils.handleWebUrl
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class LoginActivity : AccountAuthenticatorActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var loginClient: LoginClient

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    private var binding: ActivityLoginBinding? = null
    private var progressDialog: ProgressDialog? = null
    private val textWatcher = AbstractTextWatcher(::onTextChanged)
    private val compositeDisposable = CompositeDisposable()
    private val delegate: AppCompatDelegate by lazy {
        AppCompatDelegate.create(this, null)
    }
    private var lastLoginResult: LoginResult? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApplicationlessInjection
            .getInstance(this.applicationContext)
            .commonsApplicationComponent
            .inject(this)

        val isDarkTheme = systemThemeUtils.isDeviceInNightMode()
        setTheme(if (isDarkTheme) R.style.DarkAppTheme else R.style.LightAppTheme)
        delegate.installViewFactory()
        delegate.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding!!.root)
        binding!!.root.handleKeyboardInsets()
        with(binding!!) {
            setContentView(root)

            loginUsername.addTextChangedListener(textWatcher)
            loginPassword.addTextChangedListener(textWatcher)
            loginTwoFactor.addTextChangedListener(textWatcher)

            skipLogin.setOnClickListener { skipLogin() }
            forgotPassword.setOnClickListener { forgotPassword() }
            aboutPrivacyPolicy.setOnClickListener { onPrivacyPolicyClicked() }
            signUpButton.setOnClickListener { signUp() }
            loginButton.setOnClickListener { performLogin() }
            loginPassword.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (binding!!.loginButton.isEnabled && isTriggerAction(actionId, keyEvent)) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT && lastLoginResult != null) {
                        askUserForTwoFactorAuthWithKeyboard()
                        true
                    } else {
                        performLogin()
                        true
                    }
                } else {
                    false
                }
            }

            loginPassword.onFocusChangeListener =
                View.OnFocusChangeListener(::onPasswordFocusChanged)

            if (isBetaFlavour) {
                loginCredentials.text = getString(R.string.login_credential)
            } else {
                loginCredentials.visibility = View.GONE
            }

            intent.getStringExtra(CommonsApplication.LOGIN_MESSAGE_INTENT_KEY)?.let {
                showMessage(it, R.color.secondaryDarkColor)
            }

            intent.getStringExtra(CommonsApplication.LOGIN_USERNAME_INTENT_KEY)?.let {
                loginUsername.setText(it)
            }
        }
    }

    @VisibleForTesting
    fun askUserForTwoFactorAuthWithKeyboard() {
        if (binding == null) {
            Timber.w("Binding is null, reinitializing in askUserForTwoFactorAuthWithKeyboard")
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
        }
        progressDialog!!.dismiss()
        if (binding != null) {
            with(binding!!) {
                twoFactorContainer.visibility = View.VISIBLE
                twoFactorContainer.hint = getString(if (lastLoginResult is LoginResult.EmailAuthResult) R.string.email_auth_code else R.string._2fa_code)
                loginTwoFactor.visibility = View.VISIBLE
                loginTwoFactor.requestFocus()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(loginTwoFactor, InputMethodManager.SHOW_IMPLICIT)

                loginTwoFactor.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        performLogin()
                        true
                    } else {
                        false
                    }
                }
            }
        } else {
            Timber.e("Binding is null in askUserForTwoFactorAuthWithKeyboard after reinitialization attempt")
        }
        showMessageAndCancelDialog(getString(if (lastLoginResult is LoginResult.EmailAuthResult) R.string.login_failed_email_auth_needed else R.string.login_failed_2fa_needed))
    }
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        delegate.onPostCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        if (sessionManager.currentAccount != null && sessionManager.isUserLoggedIn) {
            applicationKvStore.putBoolean("login_skipped", false)
            startMainActivity()
        }

        if (applicationKvStore.getBoolean("login_skipped", false)) {
            performSkipLogin()
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        try {
            // To prevent leaked window when finish() is called, see http://stackoverflow.com/questions/32065854/activity-has-leaked-window-at-alertdialog-show-method
            if (progressDialog?.isShowing == true) {
                progressDialog!!.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        with(binding!!) {
            loginUsername.removeTextChangedListener(textWatcher)
            loginPassword.removeTextChangedListener(textWatcher)
            loginTwoFactor.removeTextChangedListener(textWatcher)
        }
        delegate.onDestroy()
        loginClient.cancel()
        binding = null
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        delegate.onStart()
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }

    override fun onPostResume() {
        super.onPostResume()
        delegate.onPostResume()
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams) {
        delegate.setContentView(view, params)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // if progressDialog is visible during the configuration change  then store state as  true else false so that
        // we maintain visibility of progressDialog after configuration change
        if (progressDialog != null && progressDialog!!.isShowing) {
            outState.putBoolean(SAVE_PROGRESS_DIALOG, true)
        } else {
            outState.putBoolean(SAVE_PROGRESS_DIALOG, false)
        }
        outState.putString(
            SAVE_ERROR_MESSAGE,
            binding!!.errorMessage.text.toString()
        ) //Save the errorMessage
        outState.putString(
            SAVE_USERNAME,
            binding!!.loginUsername.text.toString()
        ) // Save the username
        outState.putString(
            SAVE_PASSWORD,
            binding!!.loginPassword.text.toString()
        ) // Save the password
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding!!.loginUsername.setText(savedInstanceState.getString(SAVE_USERNAME))
        binding!!.loginPassword.setText(savedInstanceState.getString(SAVE_PASSWORD))
        if (savedInstanceState.getBoolean(SAVE_PROGRESS_DIALOG)) {
            performLogin()
        }
        val errorMessage = savedInstanceState.getString(SAVE_ERROR_MESSAGE)
        if (sessionManager.isUserLoggedIn) {
            showMessage(R.string.login_success, R.color.primaryDarkColor)
        } else {
            showMessage(errorMessage, R.color.secondaryDarkColor)
        }
    }

    /**
     * Hides the keyboard if the user's focus is not on the password (hasFocus is false).
     * @param view The keyboard
     * @param hasFocus Set to true if the keyboard has focus
     */
    private fun onPasswordFocusChanged(view: View, hasFocus: Boolean) {
        if (!hasFocus) {
            hideKeyboard(view)
        }
    }

    private fun onEditorAction(textView: TextView, actionId: Int, keyEvent: KeyEvent?) =
        if (binding!!.loginButton.isEnabled && isTriggerAction(actionId, keyEvent)) {
            performLogin()
            true
        } else false

    private fun isTriggerAction(actionId: Int, keyEvent: KeyEvent?) =
        actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER

    private fun skipLogin() {
        AlertDialog.Builder(this)
            .setTitle(R.string.skip_login_title)
            .setMessage(R.string.skip_login_message)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { dialog: DialogInterface, which: Int ->
                dialog.cancel()
                performSkipLogin()
            }
            .setNegativeButton(R.string.no) { dialog: DialogInterface, which: Int ->
                dialog.cancel()
            }
            .show()
    }

    private fun forgotPassword() =
        handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL))

    private fun onPrivacyPolicyClicked() =
        handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))

    private fun signUp() =
        startActivity(Intent(this, SignupActivity::class.java))

    @VisibleForTesting
    fun performLogin() {
        Timber.d("Login to start!")
        val username = binding!!.loginUsername.text.toString()
        val password = binding!!.loginPassword.text.toString()
        val twoFactorCode = binding!!.loginTwoFactor.text.toString()

        showLoggingProgressBar()
        loginClient.doLogin(username,
            password,
            lastLoginResult,
            twoFactorCode,
            Locale.getDefault().language,
            object : LoginCallback {
                override fun success(loginResult: LoginResult) = runOnUiThread {
                    Timber.d("Login Success")
                    progressDialog!!.dismiss()
                    onLoginSuccess(loginResult)
                }

                override fun twoFactorPrompt(loginResult: LoginResult, caught: Throwable, token: String?) = runOnUiThread {
                    Timber.d("Requesting 2FA prompt")
                    progressDialog!!.dismiss()
                    lastLoginResult = loginResult
                    askUserForTwoFactorAuthWithKeyboard()
                }

                override fun emailAuthPrompt(loginResult: LoginResult, caught: Throwable, token: String?) = runOnUiThread {
                    Timber.d("Requesting email auth prompt")
                    progressDialog!!.dismiss()
                    lastLoginResult = loginResult
                    askUserForTwoFactorAuthWithKeyboard()
                }

                override fun passwordResetPrompt(token: String?) = runOnUiThread {
                    Timber.d("Showing password reset prompt")
                    progressDialog!!.dismiss()
                    showPasswordResetPrompt()
                }

                override fun error(caught: Throwable) = runOnUiThread {
                    Timber.e(caught)
                    progressDialog!!.dismiss()
                    showMessageAndCancelDialog(caught.localizedMessage ?: "")
                }
            }
        )
    }

    private fun showPasswordResetPrompt() =
        showMessageAndCancelDialog(getString(R.string.you_must_reset_your_passsword))

    /**
     * This function is called when user skips the login.
     * It redirects the user to Explore Activity.
     */
    private fun performSkipLogin() {
        applicationKvStore.putBoolean("login_skipped", true)
        MainActivity.startYourself(this)
        finish()
    }

    private fun showLoggingProgressBar() {
        progressDialog = ProgressDialog(this).apply {
            isIndeterminate = true
            setTitle(getString(R.string.logging_in_title))
            setMessage(getString(R.string.logging_in_message))
            setCancelable(false)
        }
        progressDialog!!.show()
    }

    private fun onLoginSuccess(loginResult: LoginResult) {
        compositeDisposable.clear()
        sessionManager.setUserLoggedIn(true)
        sessionManager.updateAccount(loginResult)
        progressDialog!!.dismiss()
        showSuccessAndDismissDialog()
        startMainActivity()
    }

    override fun getMenuInflater(): MenuInflater =
        delegate.menuInflater

    @VisibleForTesting
    fun askUserForTwoFactorAuth() {
        if (binding == null) {
            Timber.w("Binding is null, reinitializing in askUserForTwoFactorAuth")
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding!!.root)
        }
        progressDialog!!.dismiss()
        if (binding != null) {
            with(binding!!) {
                twoFactorContainer.visibility = View.VISIBLE
                twoFactorContainer.hint = getString(if (lastLoginResult is LoginResult.EmailAuthResult) R.string.email_auth_code else R.string._2fa_code)
                loginTwoFactor.visibility = View.VISIBLE
                loginTwoFactor.requestFocus()

                loginTwoFactor.setOnEditorActionListener { _, actionId, event ->
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                        performLogin()
                        true
                    } else {
                        false
                    }
                }
            }
        } else {
            Timber.e("Binding is null in askUserForTwoFactorAuth after reinitialization attempt")
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        showMessageAndCancelDialog(getString(if (lastLoginResult is LoginResult.EmailAuthResult) R.string.login_failed_email_auth_needed else R.string.login_failed_2fa_needed))
    }

    @VisibleForTesting
    fun showMessageAndCancelDialog(@StringRes resId: Int) {
        showMessage(resId, R.color.secondaryDarkColor)
        progressDialog?.cancel()
    }

    @VisibleForTesting
    fun showMessageAndCancelDialog(error: String) {
        showMessage(error, R.color.secondaryDarkColor)
        progressDialog?.cancel()
    }

    @VisibleForTesting
    fun showSuccessAndDismissDialog() {
        showMessage(R.string.login_success, R.color.primaryDarkColor)
        progressDialog!!.dismiss()
    }

    @VisibleForTesting
    fun startMainActivity() {
        startActivityWithFlags(this, MainActivity::class.java, Intent.FLAG_ACTIVITY_SINGLE_TOP)
        finish()
    }

    private fun showMessage(@StringRes resId: Int, @ColorRes colorResId: Int) = with(binding!!) {
        errorMessage.text = getString(resId)
        errorMessage.setTextColor(ContextCompat.getColor(this@LoginActivity, colorResId))
        errorMessageContainer.visibility = View.VISIBLE
    }

    private fun showMessage(message: String?, @ColorRes colorResId: Int) = with(binding!!) {
        errorMessage.text = message
        errorMessage.setTextColor(ContextCompat.getColor(this@LoginActivity, colorResId))
        errorMessageContainer.visibility = View.VISIBLE
    }

    private fun onTextChanged(text: String) {
        val enabled =
            binding!!.loginUsername.text!!.length != 0 && binding!!.loginPassword.text!!.length != 0 &&
                    (BuildConfig.DEBUG || binding!!.loginTwoFactor.text!!.length != 0 || binding!!.loginTwoFactor.visibility != View.VISIBLE)
        binding!!.loginButton.isEnabled = enabled
    }

    companion object {
        fun startYourself(context: Context) =
            context.startActivity(Intent(context, LoginActivity::class.java))

        const val SAVE_PROGRESS_DIALOG: String = "ProgressDialog_state"
        const val SAVE_ERROR_MESSAGE: String = "errorMessage"
        const val SAVE_USERNAME: String = "username"
        const val SAVE_PASSWORD: String = "password"
    }
}
