package fr.free.nrw.commons.auth

import android.accounts.AccountAuthenticatorActivity
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import butterknife.ButterKnife
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.WelcomeActivity
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.ActivityUtils
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_login.*
import org.wikipedia.AppAdapter
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.login.LoginClient
import org.wikipedia.login.LoginClient.LoginCallback
import org.wikipedia.login.LoginResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class LoginActivity : AccountAuthenticatorActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    @field:Named(NetworkingModule.NAMED_COMMONS_WIKI_SITE)
    lateinit var commonsWikiSite: WikiSite

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var loginClient: LoginClient

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    var progressDialog: ProgressDialog? = null
    private var delegate: AppCompatDelegate? = null
    private val textWatcher = LoginTextWatcher()
    private val compositeDisposable = CompositeDisposable()
    private var loginToken: Call<MwQueryResponse>? = null
    val saveProgressDailog = "ProgressDailog_state"
    val saveErrorMessage = "errorMessage"
    val saveUsername = "username"
    val savePassword = "password"

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApplicationlessInjection.getInstance(this.applicationContext).commonsApplicationComponent.inject(this)
        val isDarkTheme = systemThemeUtils.isDeviceInNightMode
        setTheme(if (isDarkTheme) R.style.DarkAppTheme else R.style.LightAppTheme)
        getDelegate().installViewFactory()
        getDelegate().onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ButterKnife.bind(this)

        login_username.addTextChangedListener(textWatcher)
        login_password.addTextChangedListener(textWatcher)
        login_two_factor.addTextChangedListener(textWatcher)

        if (isBetaFlavour) {
            login_credentials.text = getString(R.string.login_credential)
        } else {
            login_credentials.visibility = View.GONE
        }

        login_password.setOnFocusChangeListener { view, hasFocus -> if (!hasFocus) ViewUtil.hideKeyboard(view) }

        login_password.setOnEditorActionListener(fun ( _, actionId, keyEvent ): Boolean {
            if (login_button.isEnabled) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    performLogin()
                    return true
                } else if (keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    performLogin()
                    return true
                }
            }
            return false
        })

        skip_login.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.skip_login_title)
                .setMessage(R.string.skip_login_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                    performSkipLogin()
                })
                .setNegativeButton(R.string.no, { dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                })
                .show()
        }

        forgot_password.setOnClickListener {
            Utils.handleWebUrl(this, Uri.parse(BuildConfig.FORGOT_PASSWORD_URL))
        }

        about_privacy_policy.setOnClickListener {
            Utils.handleWebUrl(this, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
        }

        sign_up_button.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        login_button.setOnClickListener {
            performLogin()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        getDelegate().onPostCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (applicationKvStore.getBoolean("firstrun", true)) {
            WelcomeActivity.startYourself(this)
        }

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
                progressDialog?.dismiss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        login_username.removeTextChangedListener(textWatcher)
        login_password.removeTextChangedListener(textWatcher)
        login_two_factor.removeTextChangedListener(textWatcher)
        delegate!!.onDestroy()
        loginClient.cancel()
        super.onDestroy()
    }

    private fun performLogin() {
        Timber.d("Login to start!")
        val username = login_username.text.toString()
        val password = login_password.text.toString()
        val twoFactorCode = login_two_factor.text.toString()
        showLoggingProgressBar()
        doLogin(username, password, twoFactorCode)
    }

    private fun doLogin(
        username: String,
        password: String,
        twoFactorCode: String
    ) {
        progressDialog!!.show()
        loginToken = ServiceFactory.get(commonsWikiSite).loginToken
        loginToken!!.enqueue(object : Callback<MwQueryResponse> {
            override fun onResponse(call: Call<MwQueryResponse>, response: Response<MwQueryResponse>) {
                loginClient.login(
                    commonsWikiSite, username, password, null, twoFactorCode,
                    response.body()!!.query()!!.loginToken(), object : LoginCallback {
                        override fun success(result: LoginResult) {
                            Timber.d("Login Success")
                            onLoginSuccess(result)
                        }

                        override fun twoFactorPrompt(
                            caught: Throwable,
                            token: String?
                        ) {
                            Timber.d("Requesting 2FA prompt")
                            hideProgress()
                            askUserForTwoFactorAuth()
                        }

                        override fun passwordResetPrompt(token: String?) {
                            Timber.d("Showing password reset prompt")
                            hideProgress()
                            showPasswordResetPrompt()
                        }

                        override fun error(caught: Throwable) {
                            Timber.e(caught)
                            hideProgress()
                            showMessageAndCancelDialog(caught.localizedMessage)
                        }
                    })
            }

            override fun onFailure(call: Call<MwQueryResponse>, t: Throwable) {
                Timber.e(t)
                showMessageAndCancelDialog(t.localizedMessage)
            }
        })
    }

    private fun hideProgress() {
        progressDialog!!.dismiss()
    }

    private fun showPasswordResetPrompt() {
        showMessageAndCancelDialog(getString(R.string.you_must_reset_your_passsword))
    }

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
        progressDialog = ProgressDialog(this)
        progressDialog!!.isIndeterminate = true
        progressDialog!!.setTitle(getString(R.string.logging_in_title))
        progressDialog!!.setMessage(getString(R.string.logging_in_message))
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.show()
    }

    private fun onLoginSuccess(loginResult: LoginResult) {
        if (!progressDialog!!.isShowing) {
            // no longer attached to activity!
            return
        }
        sessionManager.isUserLoggedIn = true
        AppAdapter.get().updateAccount(loginResult)
        progressDialog!!.dismiss()
        showSuccessAndDismissDialog()
        startMainActivity()
    }

    override fun onStart() {
        super.onStart()
        delegate!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        delegate!!.onStop()
    }

    override fun onPostResume() {
        super.onPostResume()
        getDelegate().onPostResume()
    }

    override fun setContentView(
        view: View,
        params: ViewGroup.LayoutParams
    ) {
        getDelegate().setContentView(view, params)
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

    override fun getMenuInflater(): MenuInflater {
        return getDelegate().menuInflater
    }

    private fun askUserForTwoFactorAuth() {
        progressDialog!!.dismiss()
        two_factor_container!!.visibility = View.VISIBLE
        login_two_factor.visibility = View.VISIBLE
        login_two_factor.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
        showMessageAndCancelDialog(R.string.login_failed_2fa_needed)
    }

    private fun showMessageAndCancelDialog(@StringRes resId: Int) {
        showMessage(resId, R.color.secondaryDarkColor)
        progressDialog?.cancel()
    }

    private fun showMessageAndCancelDialog(error: String) {
        showMessage(error, R.color.secondaryDarkColor)
        progressDialog?.cancel()
    }

    private fun showSuccessAndDismissDialog() {
        showMessage(R.string.login_success, R.color.primaryDarkColor)
        progressDialog?.dismiss()
    }

    private fun startMainActivity() {
        ActivityUtils.startActivityWithFlags(this, MainActivity::class.java, Intent.FLAG_ACTIVITY_SINGLE_TOP)
        finish()
    }

    private fun showMessage(@StringRes resId: Int, @ColorRes colorResId: Int) {
        error_message.text = getString(resId)
        error_message.setTextColor(ContextCompat.getColor(this, colorResId))
        error_message_container.visibility = View.VISIBLE
    }

    private fun showMessage(message: String, @ColorRes colorResId: Int) {
        error_message.text = message
        error_message.setTextColor(ContextCompat.getColor(this, colorResId))
        error_message_container.visibility = View.VISIBLE
    }

    private fun getDelegate(): AppCompatDelegate {
        if (delegate == null) {
            delegate = AppCompatDelegate.create(this, null)
        }
        return delegate!!
    }

    private inner class LoginTextWatcher : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(editable: Editable) {
            // Enable the login button if we have a username and password, and either have a 2FA code or one is not necessary
            val enabled = login_username.text?.isNotEmpty() == true
                    && login_password.text?.isNotEmpty() == true
                    && (BuildConfig.DEBUG || login_two_factor.text?.isNotEmpty() == true || login_two_factor.visibility != View.VISIBLE)
            login_button.isEnabled = enabled
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // if progressDialog is visible during the configuration change  then store state as  true else false so that
        // we maintain visiblity of progressDailog after configuration change
        if (progressDialog != null && progressDialog!!.isShowing) {
            outState.putBoolean(saveProgressDailog, true)
        } else {
            outState.putBoolean(saveProgressDailog, false)
        }
        outState.putString(saveErrorMessage, error_message.text.toString()) //Save the errorMessage
        outState.putString(saveUsername, login_username.text.toString()) // Save the username
        outState.putString(savePassword, login_password.text.toString()) // Save the password
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        login_username.setText(savedInstanceState.getString(saveUsername))
        login_password.setText(savedInstanceState.getString(savePassword))
        if (savedInstanceState.getBoolean(saveProgressDailog)) {
            performLogin()
        }
        val errorMessage = savedInstanceState.getString(saveErrorMessage)
        if (sessionManager.isUserLoggedIn) {
            showMessage(R.string.login_success, R.color.primaryDarkColor)
        } else {
            showMessage(errorMessage, R.color.secondaryDarkColor)
        }
    }

    companion object {
        fun startYourself(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            context.startActivity(intent)
        }
    }
}