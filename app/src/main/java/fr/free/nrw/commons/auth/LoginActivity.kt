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
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.ActivityLoginBinding
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.AbstractTextWatcher
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.hideKeyboard
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

        binding = ActivityLoginBinding.inflate(layoutInflater)
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
            loginPassword.setOnEditorActionListener(::onEditorAction)

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
        loginClient?.cancel()
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
            outState.putBoolean(saveProgressDialog, true)
        } else {
            outState.putBoolean(saveProgressDialog, false)
        }
        outState.putString(
            saveErrorMessage,
            binding!!.errorMessage.text.toString()
        ) //Save the errorMessage
        outState.putString(
            saveUsername,
            binding!!.loginUsername.text.toString()
        ) // Save the username
        outState.putString(
            savePassword,
            binding!!.loginPassword.text.toString()
        ) // Save the password
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding!!.loginUsername.setText(savedInstanceState.getString(saveUsername))
        binding!!.loginPassword.setText(savedInstanceState.getString(savePassword))
        if (savedInstanceState.getBoolean(saveProgressDialog)) {
            performLogin()
        }
        val errorMessage = savedInstanceState.getString(saveErrorMessage)
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
        actionId == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER

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
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL))

    private fun onPrivacyPolicyClicked() =
        Utils.handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))

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
            twoFactorCode,
            Locale.getDefault().language,
            object : LoginCallback {
                override fun success(loginResult: LoginResult) = runOnUiThread {
                    Timber.d("Login Success")
                    progressDialog!!.dismiss()
                    onLoginSuccess(loginResult)
                }

                override fun twoFactorPrompt(caught: Throwable, token: String?) = runOnUiThread {
                    Timber.d("Requesting 2FA prompt")
                    progressDialog!!.dismiss()
                    askUserForTwoFactorAuth()
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
        progressDialog!!.dismiss()
        with(binding!!) {
            twoFactorContainer.visibility = View.VISIBLE
            loginTwoFactor.visibility = View.VISIBLE
            loginTwoFactor.requestFocus()
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        showMessageAndCancelDialog(R.string.login_failed_2fa_needed)
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

        const val saveProgressDialog: String = "ProgressDialog_state"
        const val saveErrorMessage: String = "errorMessage"
        const val saveUsername: String = "username"
        const val savePassword: String = "password"
    }
}
