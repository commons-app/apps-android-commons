package fr.free.nrw.commons.auth.login

import android.text.TextUtils
import fr.free.nrw.commons.auth.login.LoginResult.OAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.ResetPasswordResult
import fr.free.nrw.commons.wikidata.WikidataConstants.WIKIPEDIA_URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

/**
 * Responsible for making login related requests to the server.
 */
class LoginClient(
    private val loginInterface: LoginInterface,
    private val dispatcher: CoroutineDispatcher
) {
    /**
     * userLanguage
     * It holds the value of the user's device language code.
     * For example, if user's device language is English it will hold En
     * The value will be fetched when the user clicks Login Button in the LoginActivity
     */
    private var userLanguage = ""

    suspend fun request(userName: String, password: String, cb: LoginCallback) {
        val tokenResponse = loginInterface.getLoginToken()

        if (tokenResponse.isSuccessful) {
            login(
                userName, password,
                null, null,
                tokenResponse.body()?.query()?.loginToken(),
                userLanguage, cb
            )
        } else {
            cb.error(HttpException(tokenResponse))
        }
    }

    suspend fun login(
        userName: String, password: String, retypedPassword: String?, twoFactorCode: String?,
        loginToken: String?, userLanguage: String, cb: LoginCallback
    ) {
        this@LoginClient.userLanguage = userLanguage
        val loginResponse = if (twoFactorCode.isNullOrEmpty() && retypedPassword.isNullOrEmpty()) {
            loginInterface.postLogIn(userName, password, loginToken, userLanguage, WIKIPEDIA_URL)
        } else {
            loginInterface.postLogIn(
                userName,
                password,
                retypedPassword,
                twoFactorCode,
                loginToken,
                userLanguage,
                true
            )
        }

        if (loginResponse.isSuccessful) {
            val loginResult = loginResponse.body()?.toLoginResult(password)
            if (loginResult != null) {
                if (loginResult.pass && !loginResult.userName.isNullOrEmpty()) {
                    // The server could do some transformations on user names, e.g. on some
                    // wikis is uppercases the first letter.
                    getExtendedInfo(loginResult.userName, loginResult, cb)
                } else if ("UI" == loginResult.status) {
                    when (loginResult) {
                        is OAuthResult -> cb.twoFactorPrompt(
                            LoginFailedException(loginResult.message),
                            loginToken
                        )

                        is ResetPasswordResult -> cb.passwordResetPrompt(loginToken)

                        is LoginResult.Result -> cb.error(
                            LoginFailedException(loginResult.message)
                        )
                    }
                } else {
                    cb.error(LoginFailedException(loginResult.message))
                }
            } else {
                cb.error(IOException("Login failed. Unexpected response."))
            }
        } else {
            cb.error(HttpException(loginResponse))
        }
    }

    fun doLogin(
        username: String,
        password: String,
        twoFactorCode: String,
        userLanguage: String,
        loginCallback: LoginCallback
    ) = runBlocking(dispatcher) {
        val tokenResponse = loginInterface.getLoginToken()

        if (tokenResponse.isSuccessful) {
            val loginToken = tokenResponse.body()?.query()?.loginToken()
            loginToken?.let {
                login(username, password, null, twoFactorCode, it, userLanguage, loginCallback)
            } ?: run {
                loginCallback.error(IOException("Failed to retrieve login token"))
            }
        } else {
            loginCallback.error(HttpException(tokenResponse))
        }
    }

    @Throws(Throwable::class)
    suspend fun loginBlocking(userName: String, password: String, twoFactorCode: String?) {
        val tokenResponse = loginInterface.getLoginToken()

        if (tokenResponse.isSuccessful) {
            if (tokenResponse.body()?.query()?.loginToken().isNullOrEmpty()) {
                throw IOException("Unexpected response when getting login token.")
            }

            val loginToken = tokenResponse.body()?.query()?.loginToken()
            val tempLoginCall = if (twoFactorCode.isNullOrEmpty()) {
                loginInterface.postLogIn(
                    userName,
                    password,
                    loginToken,
                    userLanguage,
                    WIKIPEDIA_URL
                )
            } else {
                loginInterface.postLogIn(
                    userName, password, null, twoFactorCode, loginToken, userLanguage, true
                )
            }

            if (tempLoginCall.isSuccessful) {
                val loginResponse = tempLoginCall.body()
                    ?: throw IOException("Unexpected response when logging in.")
                val loginResult = loginResponse.toLoginResult(password)
                    ?: throw IOException("Unexpected response when logging in.")
                if ("UI" == loginResult.status) {
                    if (loginResult is OAuthResult) {
                        // TODO: Find a better way to boil up the warning about 2FA
                        throw LoginFailedException(loginResult.message)
                    }
                    throw LoginFailedException(loginResult.message)
                }

                if (!loginResult.pass || TextUtils.isEmpty(loginResult.userName)) {
                    throw LoginFailedException(loginResult.message)
                }
            } else {
                throw HttpException(tempLoginCall)
            }
        } else {
            throw HttpException(tokenResponse)
        }
    }

    private suspend fun getExtendedInfo(
        userName: String,
        loginResult: LoginResult,
        cb: LoginCallback
    ) {
        val loginResponse = loginInterface.getUserInfo(userName)

        if (loginResponse.isSuccessful) {
            with(loginResult) {
                userId = loginResponse.body()?.query()?.userInfo()?.id() ?: 0
                groups =
                    loginResponse.body()?.query()?.getUserResponse(userName)?.groups ?: emptySet()
            }
            cb.success(loginResult)
        } else {
            val caught = HttpException(loginResponse)
            Timber.e(caught, "Login succeeded but getting group information failed. ")
            cb.error(caught)
        }
    }
}
