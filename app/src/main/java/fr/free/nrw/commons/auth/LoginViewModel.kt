package fr.free.nrw.commons.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.kvstore.JsonKvStore
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * State representation for login operations in LoginActivity.
 */
sealed class LoginState {
    object Idle : LoginState()

    object Loading : LoginState()

    data class Success(
        val loginResult: LoginResult,
    ) : LoginState()

    data class TwoFactorPrompt(
        val loginResult: LoginResult,
    ) : LoginState()

    data class EmailAuthPrompt(
        val loginResult: LoginResult,
    ) : LoginState()

    object PasswordResetPrompt : LoginState()

    data class Error(
        val message: String?,
    ) : LoginState()

    object Skipped : LoginState()

    object AlreadyLoggedIn : LoginState()
}

/**
 * ViewModel managing authentication logic, credentials, and state for LoginActivity.
 */
class LoginViewModel(
    private val loginClient: LoginClient,
    private val sessionManager: SessionManager,
    private val applicationKvStore: JsonKvStore,
) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    var lastLoginResult: LoginResult? = null
        internal set

    /**
     * Checks if user is already logged in or previously skipped login.
     */
    fun checkLoginStatus() {
        if (sessionManager.currentAccount != null && sessionManager.isUserLoggedIn) {
            applicationKvStore.putBoolean("login_skipped", false)
            _loginState.value = LoginState.AlreadyLoggedIn
        } else if (applicationKvStore.getBoolean("login_skipped", false)) {
            _loginState.value = LoginState.Skipped
        }
    }

    /**
     * Executes login with the provided credentials.
     *
     * @param username The account username.
     * @param password The account password.
     * @param twoFactorCode The two factor or email auth code, if required.
     */
    fun performLogin(
        username: String,
        password: String,
        twoFactorCode: String,
    ) {
        _loginState.value = LoginState.Loading
        Timber.d("Login to start!")
        loginClient.doLogin(
            username,
            password,
            lastLoginResult,
            twoFactorCode,
            Locale.getDefault().language,
            object : LoginCallback {
                override fun success(loginResult: LoginResult) {
                    Timber.d("Login Success")
                    compositeDisposable.clear()
                    sessionManager.setUserLoggedIn(true)
                    sessionManager.updateAccount(loginResult)
                    _loginState.postValue(LoginState.Success(loginResult))
                }

                override fun twoFactorPrompt(
                    loginResult: LoginResult,
                    caught: Throwable,
                    token: String?,
                ) {
                    Timber.d("Requesting 2FA prompt")
                    lastLoginResult = loginResult
                    _loginState.postValue(LoginState.TwoFactorPrompt(loginResult))
                }

                override fun emailAuthPrompt(
                    loginResult: LoginResult,
                    caught: Throwable,
                    token: String?,
                ) {
                    Timber.d("Requesting email auth prompt")
                    lastLoginResult = loginResult
                    _loginState.postValue(LoginState.EmailAuthPrompt(loginResult))
                }

                override fun passwordResetPrompt(token: String?) {
                    Timber.d("Showing password reset prompt")
                    _loginState.postValue(LoginState.PasswordResetPrompt)
                }

                override fun error(caught: Throwable) {
                    Timber.e(caught)
                    _loginState.postValue(LoginState.Error(caught.localizedMessage ?: ""))
                }
            },
        )
    }

    /**
     * Records login as skipped and transitions state to Skipped.
     */
    fun skipLogin() {
        applicationKvStore.putBoolean("login_skipped", true)
        _loginState.value = LoginState.Skipped
    }

    /**
     * Validates if the login button should be enabled given the current input fields.
     */
    fun isLoginButtonEnabled(
        username: String?,
        password: String?,
        twoFactorCode: String?,
        isTwoFactorVisible: Boolean,
    ): Boolean {
        val usernameNotEmpty = !username.isNullOrEmpty()
        val passwordNotEmpty = !password.isNullOrEmpty()
        val twoFactorValid =
            BuildConfig.DEBUG ||
                !twoFactorCode.isNullOrEmpty() ||
                !isTwoFactorVisible
        return usernameNotEmpty && passwordNotEmpty && twoFactorValid
    }

    /**
     * Cancels any pending network requests and clears disposables.
     */
    fun cancel() {
        compositeDisposable.clear()
        loginClient.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancel()
    }

    class LoginViewModelFactory
        @Inject
        constructor(
            private val loginClient: LoginClient,
            private val sessionManager: SessionManager,
            @Named("default_preferences") private val applicationKvStore: JsonKvStore,
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(loginClient, sessionManager, applicationKvStore) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
}
