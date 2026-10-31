/**
 * Activity handling user authentication.
 *
 * Suppressed TooManyFunctions because as an Activity handling Android lifecycle events,
 * state observation, input validation, dialog management, and UI callbacks, splitting
 * cohesive UI helper methods across synthetic classes reduces readability.
 * Authentication business logic, network requests, session state, and credentials validation
 * are decoupled into [LoginViewModel].
 */
@file:Suppress("TooManyFunctions")

package fr.free.nrw.commons.auth

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.ActivityLoginBinding
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.AbstractTextWatcher
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.hideKeyboard
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.utils.handleKeyboardInsets
import fr.free.nrw.commons.utils.handleWebUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LoginActivity : BaseActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var loginClient: LoginClient

    @Inject
    lateinit var viewModelFactory: LoginViewModel.LoginViewModelFactory

    internal lateinit var viewModel: LoginViewModel
    internal var binding: ActivityLoginBinding? = null
    internal var progressDialog: ProgressDialog? = null
    internal var lastLoginResult: LoginResult? = null

    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    private val textWatcher =
        AbstractTextWatcher {
            binding?.loginButton?.isEnabled =
                viewModel.isLoginButtonEnabled(
                    binding?.loginUsername?.text?.toString(),
                    binding?.loginPassword?.text?.toString(),
                    binding?.loginTwoFactor?.text?.toString(),
                    binding?.twoFactorContainer?.visibility == View.VISIBLE,
                )
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        ApplicationlessInjection
            .getInstance(this.applicationContext)
            .commonsApplicationComponent
            .inject(this)

        super.onCreate(savedInstanceState)

        accountAuthenticatorResponse =
            intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        val viewBinding = ActivityLoginBinding.inflate(layoutInflater)
        binding = viewBinding
        applyEdgeToEdgeAllInsets(viewBinding.root)
        viewBinding.root.handleKeyboardInsets()
        setContentView(viewBinding.root)

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        initObservers()
        initViews(viewBinding)
    }

    private fun initObservers() {
        viewModel.loginState.observe(this) { state ->
            renderLoginState(state)
        }
    }

    private fun renderLoginState(state: LoginState) {
        when (state) {
            is LoginState.Loading -> showLoggingProgressBar()
            is LoginState.Success -> {
                progressDialog?.dismiss()
                showSuccessAndDismissDialog()
                startMainActivity()
            }
            is LoginState.TwoFactorPrompt -> {
                progressDialog?.dismiss()
                lastLoginResult = state.loginResult
                askUserForTwoFactorAuthWithKeyboard()
            }
            is LoginState.EmailAuthPrompt -> {
                progressDialog?.dismiss()
                lastLoginResult = state.loginResult
                askUserForTwoFactorAuthWithKeyboard()
            }
            is LoginState.PasswordResetPrompt -> {
                progressDialog?.dismiss()
                showPasswordResetPrompt()
            }
            is LoginState.Error -> {
                progressDialog?.dismiss()
                showMessageAndCancelDialog(state.message ?: "")
            }
            is LoginState.Skipped -> {
                MainActivity.startYourself(this)
                finish()
            }
            is LoginState.AlreadyLoggedIn -> startMainActivity()
            is LoginState.Idle -> { /* No-op */ }
        }
    }

    private fun initViews(viewBinding: ActivityLoginBinding) {
        with(viewBinding) {
            loginUsername.addTextChangedListener(textWatcher)
            loginPassword.addTextChangedListener(textWatcher)
            loginTwoFactor.addTextChangedListener(textWatcher)

            loginPassword.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) hideKeyboard(v)
            }

            loginButton.setOnClickListener { performLogin() }
            skipLogin.setOnClickListener { skipLogin() }
            forgotPassword.setOnClickListener { forgotPassword() }
            aboutPrivacyPolicy.setOnClickListener { onPrivacyPolicyClicked() }
            signUpButton.setOnClickListener { signUp() }

            loginUsername.setOnEditorActionListener { _, actionId, keyEvent ->
                handleEditorAction(actionId, keyEvent)
            }
            loginPassword.setOnEditorActionListener { _, actionId, keyEvent ->
                handleEditorAction(actionId, keyEvent)
            }

            loginCredentials.visibility = if (isBetaFlavour) View.VISIBLE else View.GONE
            if (isBetaFlavour) {
                loginCredentials.text = getString(R.string.login_credential)
            }
        }

        intent.getStringExtra(CommonsApplication.LOGIN_MESSAGE_INTENT_KEY)?.let {
            showMessage(it, R.color.secondaryDarkColor)
        }
        intent.getStringExtra(CommonsApplication.LOGIN_USERNAME_INTENT_KEY)?.let {
            viewBinding.loginUsername.setText(it)
        }
    }

    private fun handleEditorAction(
        actionId: Int,
        keyEvent: KeyEvent?,
    ): Boolean =
        if (isTriggerAction(actionId, keyEvent)) {
            if (actionId == EditorInfo.IME_ACTION_NEXT && lastLoginResult != null) {
                askUserForTwoFactorAuthWithKeyboard()
            } else {
                performLogin()
            }
            true
        } else {
            false
        }

    private fun isTriggerAction(
        actionId: Int,
        keyEvent: KeyEvent?,
    ) = actionId == EditorInfo.IME_ACTION_NEXT ||
        actionId == EditorInfo.IME_ACTION_DONE ||
        keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER

    @VisibleForTesting
    fun performLogin() {
        val username = binding?.loginUsername?.text?.toString() ?: ""
        val password = binding?.loginPassword?.text?.toString() ?: ""
        val twoFactorCode = binding?.loginTwoFactor?.text?.toString() ?: ""
        viewModel.performLogin(username, password, twoFactorCode)
    }

    @VisibleForTesting
    internal fun skipLogin() {
        AlertDialog
            .Builder(this)
            .setTitle(R.string.skip_login_title)
            .setMessage(R.string.skip_login_message)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { dialog: DialogInterface, _ ->
                dialog.cancel()
                performSkipLogin()
            }.setNegativeButton(R.string.no) { dialog: DialogInterface, _ ->
                dialog.cancel()
            }.show()
    }

    @VisibleForTesting
    internal fun performSkipLogin() {
        viewModel.skipLogin()
    }

    @VisibleForTesting
    internal fun forgotPassword() = handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL))

    @VisibleForTesting
    internal fun onPrivacyPolicyClicked() = handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))

    @VisibleForTesting
    internal fun signUp() = startActivity(Intent(this, SignupActivity::class.java))

    private fun showLoggingProgressBar() {
        progressDialog =
            ProgressDialog(this).apply {
                isIndeterminate = true
                setTitle(getString(R.string.logging_in_title))
                setMessage(getString(R.string.logging_in_message))
                setCancelable(false)
            }
        progressDialog?.show()
    }

    @VisibleForTesting
    fun onLoginSuccess(loginResult: LoginResult) {
        compositeDisposable.clear()
        sessionManager.setUserLoggedIn(true)
        sessionManager.updateAccount(loginResult)
        progressDialog?.dismiss()
        showSuccessAndDismissDialog()
        startMainActivity()
    }

    @VisibleForTesting
    fun showPasswordResetPrompt() {
        showMessageAndCancelDialog(getString(R.string.you_must_reset_your_passsword))
    }

    private fun show2FAUI() {
        val currentBinding = binding ?: return
        if (lastLoginResult != null) {
            with(currentBinding) {
                twoFactorContainer.visibility = View.VISIBLE
                twoFactorContainer.hint =
                    getString(
                        if (lastLoginResult is LoginResult.EmailAuthResult) {
                            R.string.email_auth_code
                        } else {
                            R.string._2fa_code
                        },
                    )
                loginTwoFactor.visibility = View.VISIBLE
                loginTwoFactor.setOnEditorActionListener { _, actionId, event ->
                    val isEnter =
                        event != null &&
                            event.keyCode == KeyEvent.KEYCODE_ENTER &&
                            event.action == KeyEvent.ACTION_DOWN
                    if (actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
                        performLogin()
                        true
                    } else {
                        false
                    }
                }
            }
        } else {
            with(currentBinding) {
                twoFactorContainer.visibility = View.GONE
                loginTwoFactor.visibility = View.GONE
                loginTwoFactor.setText("")
            }
        }
    }

    @VisibleForTesting
    fun askUserForTwoFactorAuth() {
        if (binding == null) {
            Timber.w("Binding is null, reinitializing in askUserForTwoFactorAuth")
            val viewBinding = ActivityLoginBinding.inflate(layoutInflater)
            binding = viewBinding
            setContentView(viewBinding.root)
        }
        progressDialog?.dismiss()
        if (binding != null) {
            show2FAUI()
            binding?.loginTwoFactor?.requestFocus()
        } else {
            Timber.e("Binding is null in askUserForTwoFactorAuth after reinitialization attempt")
        }
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        showMessageAndCancelDialog(
            getString(
                if (lastLoginResult is LoginResult.EmailAuthResult) {
                    R.string.login_failed_email_auth_needed
                } else {
                    R.string.login_failed_2fa_needed
                },
            ),
        )
    }

    @VisibleForTesting
    fun askUserForTwoFactorAuthWithKeyboard() {
        progressDialog?.dismiss()
        if (binding != null) {
            show2FAUI()
            binding?.loginTwoFactor?.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding?.loginTwoFactor, InputMethodManager.SHOW_IMPLICIT)
        } else {
            Timber.e("Binding is null in askUserForTwoFactorAuthWithKeyboard")
        }
        showMessageAndCancelDialog(
            getString(
                if (lastLoginResult is LoginResult.EmailAuthResult) {
                    R.string.login_failed_email_auth_needed
                } else {
                    R.string.login_failed_2fa_needed
                },
            ),
        )
    }

    @VisibleForTesting
    fun showMessageAndCancelDialog(
        @StringRes resId: Int,
    ) {
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
        progressDialog?.dismiss()
    }

    @VisibleForTesting
    fun startMainActivity() {
        startActivityWithFlags(this, MainActivity::class.java, Intent.FLAG_ACTIVITY_SINGLE_TOP)
        finish()
    }

    private fun showMessage(
        @StringRes resId: Int,
        @ColorRes colorResId: Int,
    ) = binding?.let {
        it.errorMessage.text = getString(resId)
        it.errorMessage.setTextColor(ContextCompat.getColor(this, colorResId))
        it.errorMessageContainer.visibility = View.VISIBLE
    }

    private fun showMessage(
        message: String?,
        @ColorRes colorResId: Int,
    ) = binding?.let {
        if (!message.isNullOrBlank()) {
            it.errorMessage.text = message
            it.errorMessage.setTextColor(ContextCompat.getColor(this, colorResId))
            it.errorMessageContainer.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkLoginStatus()
    }

    override fun onDestroy() {
        try {
            if (progressDialog?.isShowing == true) {
                progressDialog!!.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding?.let {
            it.loginUsername.removeTextChangedListener(textWatcher)
            it.loginPassword.removeTextChangedListener(textWatcher)
            it.loginTwoFactor.removeTextChangedListener(textWatcher)
        }
        viewModel.cancel()
        binding = null
        super.onDestroy()
    }

    fun setAccountAuthenticatorResult(result: Bundle) {
        resultBundle = result
    }

    override fun finish() {
        accountAuthenticatorResponse?.let {
            if (resultBundle != null) {
                it.onResult(resultBundle)
            } else {
                it.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
            }
            accountAuthenticatorResponse = null
        }
        super.finish()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun setContentView(
        view: View?,
        params: ViewGroup.LayoutParams?,
    ) {
        super.setContentView(view, params)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SAVE_PROGRESS_DIALOG, progressDialog?.isShowing == true)
        binding?.let {
            outState.putString(SAVE_ERROR_MESSAGE, it.errorMessage.text.toString())
            outState.putString(SAVE_USERNAME, it.loginUsername.text.toString())
            outState.putString(SAVE_PASSWORD, it.loginPassword.text.toString())
            outState.putBoolean(SAVE_TWO_FACTOR_VISIBILITY, it.twoFactorContainer.visibility == View.VISIBLE)
        }
        val loginResultType =
            when (lastLoginResult) {
                is LoginResult.EmailAuthResult -> RESULT_TYPE_EMAIL_AUTH
                is LoginResult.OAuthResult -> RESULT_TYPE_2FA
                else -> RESULT_TYPE_NONE
            }
        outState.putString(SAVE_LAST_LOGIN_RESULT_TYPE, loginResultType)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding?.let {
            it.loginUsername.setText(savedInstanceState.getString(SAVE_USERNAME))
            it.loginPassword.setText(savedInstanceState.getString(SAVE_PASSWORD))
            val isTwoFactorVisible = savedInstanceState.getBoolean(SAVE_TWO_FACTOR_VISIBILITY, false)
            if (isTwoFactorVisible) {
                val loginResultType =
                    savedInstanceState.getString(SAVE_LAST_LOGIN_RESULT_TYPE) ?: RESULT_TYPE_NONE
                lastLoginResult =
                    when (loginResultType) {
                        RESULT_TYPE_EMAIL_AUTH -> LoginResult.EmailAuthResult("", null, null, null)
                        RESULT_TYPE_2FA -> LoginResult.OAuthResult("", null, null, null)
                        else -> null
                    }
                show2FAUI()
            } else {
                it.twoFactorContainer.visibility = View.GONE
                it.loginTwoFactor.visibility = View.GONE
                it.loginTwoFactor.setText("")
                lastLoginResult = null
            }
        }
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

    companion object {
        fun startYourself(context: Context) = context.startActivity(Intent(context, LoginActivity::class.java))

        const val SAVE_PROGRESS_DIALOG: String = "ProgressDialog_state"
        const val SAVE_ERROR_MESSAGE: String = "errorMessage"
        const val SAVE_USERNAME: String = "username"
        const val SAVE_PASSWORD: String = "password"
        const val SAVE_TWO_FACTOR_VISIBILITY: String = "twoFactorVisibility"
        const val SAVE_LAST_LOGIN_RESULT_TYPE: String = "lastLoginResultType"
        const val RESULT_TYPE_EMAIL_AUTH = "EMAIL_AUTH"
        const val RESULT_TYPE_2FA = "2FA"
        const val RESULT_TYPE_NONE = "NONE"
    }
}
