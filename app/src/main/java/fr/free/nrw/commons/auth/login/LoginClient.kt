package fr.free.nrw.commons.auth.login

import android.text.TextUtils
import fr.free.nrw.commons.auth.login.LoginResult.EmailAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.OAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.ResetPasswordResult
import fr.free.nrw.commons.wikidata.WikidataConstants.WIKIPEDIA_URL
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

data class LoginRequest(
    val username: String,
    val password: String,
    val retypedPassword: String? = null,
    val twoFactorCode: String? = null,
    val emailAuthCode: String? = null,
    val loginToken: String? = null,
    val userLanguage: String = "",
)

/**
 * Responsible for making login related requests to the server.
 */
class LoginClient(
    private val loginInterface: LoginInterface,
) {
    private var tokenCall: Call<MwQueryResponse?>? = null
    private var loginCall: Call<LoginResponse?>? = null

    /**
     * userLanguage
     * It holds the value of the user's device language code.
     * For example, if user's device language is English it will hold En
     * The value will be fetched when the user clicks Login Button in the LoginActivity
     */
    private var userLanguage = ""

    private fun getLoginToken() = loginInterface.getLoginToken()

    fun request(
        userName: String,
        password: String,
        cb: LoginCallback,
    ) {
        cancel()

        tokenCall = getLoginToken()
        tokenCall!!.enqueue(
            object : Callback<MwQueryResponse?> {
                override fun onResponse(
                    call: Call<MwQueryResponse?>,
                    response: Response<MwQueryResponse?>,
                ) {
                    val loginToken = response.body()?.query()?.loginToken()
                    login(
                        LoginRequest(
                            username = userName,
                            password = password,
                            loginToken = loginToken,
                            userLanguage = userLanguage,
                        ),
                        cb,
                    )
                }

                override fun onFailure(
                    call: Call<MwQueryResponse?>,
                    caught: Throwable,
                ) {
                    if (call.isCanceled) {
                        return
                    }
                    cb.error(caught)
                }
            },
        )
    }

    fun login(
        request: LoginRequest,
        cb: LoginCallback,
    ) {
        this.userLanguage = request.userLanguage

        loginCall =
            if (request.twoFactorCode.isNullOrEmpty() &&
                request.emailAuthCode.isNullOrEmpty() &&
                request.retypedPassword.isNullOrEmpty()
            ) {
                loginInterface.postLogIn(
                    request.username,
                    request.password,
                    request.loginToken,
                    request.userLanguage,
                    WIKIPEDIA_URL,
                )
            } else {
                loginInterface.postLogIn(
                    request.username,
                    request.password,
                    request.retypedPassword,
                    request.twoFactorCode,
                    request.emailAuthCode,
                    request.loginToken,
                    request.userLanguage,
                    true,
                )
            }

        loginCall!!.enqueue(
            object : Callback<LoginResponse?> {
                override fun onResponse(
                    call: Call<LoginResponse?>,
                    response: Response<LoginResponse?>,
                ) {
                    val loginResult = response.body()?.toLoginResult(request.password)
                    if (loginResult != null) {
                        if (loginResult.pass && !loginResult.userName.isNullOrEmpty()) {
                            // The server could do some transformations on user names, e.g. on some
                            // wikis is uppercases the first letter.
                            getExtendedInfo(loginResult.userName, loginResult, cb)
                        } else if ("UI" == loginResult.status) {
                            when (loginResult) {
                                is OAuthResult ->
                                    cb.twoFactorPrompt(
                                        loginResult,
                                        LoginFailedException(loginResult.message),
                                        request.loginToken,
                                    )

                                is EmailAuthResult ->
                                    cb.emailAuthPrompt(
                                        loginResult,
                                        LoginFailedException(loginResult.message),
                                        request.loginToken,
                                    )

                                is ResetPasswordResult -> cb.passwordResetPrompt(request.loginToken)

                                is LoginResult.Result ->
                                    cb.error(
                                        LoginFailedException(loginResult.message),
                                    )
                            }
                        } else {
                            cb.error(LoginFailedException(loginResult.message))
                        }
                    } else {
                        cb.error(IOException("Login failed. Unexpected response."))
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse?>,
                    t: Throwable,
                ) {
                    if (call.isCanceled) {
                        return
                    }
                    cb.error(t)
                }
            },
        )
    }

    @Suppress("LongParameterList")
    fun login(
        userName: String,
        password: String,
        retypedPassword: String?,
        twoFactorCode: String?,
        emailAuthCode: String?,
        loginToken: String?,
        userLanguage: String,
        cb: LoginCallback,
    ) = login(
        LoginRequest(
            username = userName,
            password = password,
            retypedPassword = retypedPassword,
            twoFactorCode = twoFactorCode,
            emailAuthCode = emailAuthCode,
            loginToken = loginToken,
            userLanguage = userLanguage,
        ),
        cb,
    )

    fun doLogin(
        request: LoginRequest,
        lastLoginResult: LoginResult?,
        loginCallback: LoginCallback,
    ) {
        cancel()

        tokenCall = getLoginToken()
        tokenCall!!.enqueue(
            object : Callback<MwQueryResponse?> {
                override fun onResponse(
                    call: Call<MwQueryResponse?>,
                    response: Response<MwQueryResponse?>,
                ) {
                    if (call.isCanceled) {
                        return
                    }
                    if (response.isSuccessful) {
                        val loginToken = response.body()?.query()?.loginToken()
                        loginToken?.let {
                            login(
                                LoginRequest(
                                    username = request.username,
                                    password = request.password,
                                    twoFactorCode = if (lastLoginResult is OAuthResult) request.twoFactorCode else null,
                                    emailAuthCode = if (lastLoginResult is EmailAuthResult) request.twoFactorCode else null,
                                    loginToken = it,
                                    userLanguage = request.userLanguage,
                                ),
                                loginCallback,
                            )
                        } ?: run {
                            loginCallback.error(IOException("Failed to retrieve login token"))
                        }
                    } else {
                        loginCallback.error(IOException("Failed to retrieve login token"))
                    }
                }

                override fun onFailure(
                    call: Call<MwQueryResponse?>,
                    t: Throwable,
                ) {
                    if (call.isCanceled) {
                        return
                    }
                    loginCallback.error(t)
                }
            },
        )
    }

    @Suppress("LongParameterList")
    fun doLogin(
        username: String,
        password: String,
        lastLoginResult: LoginResult?,
        twoFactorCode: String,
        userLanguage: String,
        loginCallback: LoginCallback,
    ) = doLogin(
        LoginRequest(
            username = username,
            password = password,
            twoFactorCode = twoFactorCode,
            userLanguage = userLanguage,
        ),
        lastLoginResult,
        loginCallback,
    )

    @Throws(Throwable::class)
    fun loginBlocking(
        userName: String,
        password: String,
        twoFactorCode: String? = null,
        emailAuthCode: String? = null,
    ) {
        val tokenResponse = getLoginToken().execute()
        if (tokenResponse
                .body()
                ?.query()
                ?.loginToken()
                .isNullOrEmpty()
        ) {
            throw IOException("Unexpected response when getting login token.")
        }

        val loginToken = tokenResponse.body()?.query()?.loginToken()
        val tempLoginCall =
            if (twoFactorCode.isNullOrEmpty() && emailAuthCode.isNullOrEmpty()) {
                loginInterface.postLogIn(userName, password, loginToken, userLanguage, WIKIPEDIA_URL)
            } else {
                loginInterface.postLogIn(
                    userName,
                    password,
                    null,
                    twoFactorCode,
                    emailAuthCode,
                    loginToken,
                    userLanguage,
                    true,
                )
            }

        val response = tempLoginCall.execute()
        val loginResponse = response.body() ?: throw IOException("Unexpected response when logging in.")
        val loginResult =
            loginResponse.toLoginResult(password) ?: throw IOException("Unexpected response when logging in.")

        if ("UI" == loginResult.status) {
            if (loginResult is OAuthResult || loginResult is EmailAuthResult) {
                // TODO: Find a better way to boil up the warning about 2FA
                throw LoginFailedException(loginResult.message)
            }
            throw LoginFailedException(loginResult.message)
        }

        if (!loginResult.pass || TextUtils.isEmpty(loginResult.userName)) {
            throw LoginFailedException(loginResult.message)
        }
    }

    private fun getExtendedInfo(
        userName: String,
        loginResult: LoginResult,
        cb: LoginCallback,
    ) = loginInterface
        .getUserInfo(userName)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ response: MwQueryResponse? ->
            loginResult.userId = response?.query()?.userInfo()?.id() ?: 0
            loginResult.groups =
                response?.query()?.getUserResponse(userName)?.getGroups() ?: emptySet()
            cb.success(loginResult)
        }, { caught: Throwable ->
            Timber.e(caught, "Login succeeded but getting group information failed. ")
            cb.error(caught)
        })

    fun cancel() {
        tokenCall?.let {
            it.cancel()
            tokenCall = null
        }

        loginCall?.let {
            it.cancel()
            loginCall = null
        }
    }
}
