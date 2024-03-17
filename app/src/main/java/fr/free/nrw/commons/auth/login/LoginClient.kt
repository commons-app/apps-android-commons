package fr.free.nrw.commons.auth.login

import android.text.TextUtils
import fr.free.nrw.commons.auth.login.LoginResult.OAuthResult
import fr.free.nrw.commons.auth.login.LoginResult.ResetPasswordResult
import fr.free.nrw.commons.wikidata.WikidataConstants.WIKIPEDIA_URL
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException

/**
 * Responsible for making login related requests to the server.
 */
class LoginClient(private val loginInterface: LoginInterface) {
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

    fun request(userName: String, password: String, cb: LoginCallback) {
        cancel()

        tokenCall = getLoginToken()
        tokenCall!!.enqueue(object : Callback<MwQueryResponse?> {
            override fun onResponse(call: Call<MwQueryResponse?>, response: Response<MwQueryResponse?>) {
                login(
                    userName, password, null, null, response.body()!!.query()!!.loginToken(),
                    userLanguage, cb
                )
            }

            override fun onFailure(call: Call<MwQueryResponse?>, caught: Throwable) {
                if (call.isCanceled) {
                    return
                }
                cb.error(caught)
            }
        })
    }

    fun login(
        userName: String, password: String, retypedPassword: String?, twoFactorCode: String?,
        loginToken: String?, userLanguage: String, cb: LoginCallback
    ) {
        this.userLanguage = userLanguage

        loginCall = if (twoFactorCode.isNullOrEmpty() && retypedPassword.isNullOrEmpty()) {
            loginInterface.postLogIn(userName, password, loginToken, userLanguage, WIKIPEDIA_URL)
        } else {
            loginInterface.postLogIn(
                userName, password, retypedPassword, twoFactorCode, loginToken, userLanguage, true
            )
        }

        loginCall!!.enqueue(object : Callback<LoginResponse?> {
            override fun onResponse(
                call: Call<LoginResponse?>,
                response: Response<LoginResponse?>
            ) {
                val loginResult = response.body()?.toLoginResult(password)
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
            }

            override fun onFailure(call: Call<LoginResponse?>, t: Throwable) {
                if (call.isCanceled) {
                    return
                }
                cb.error(t)
            }
        })
    }

    fun doLogin(
        username: String,
        password: String,
        twoFactorCode: String,
        userLanguage: String,
        loginCallback: LoginCallback
    ) {
        getLoginToken().enqueue(object :Callback<MwQueryResponse?>{
            override fun onResponse(
                call: Call<MwQueryResponse?>,
                response: Response<MwQueryResponse?>
            ) = if (response.isSuccessful){
                val loginToken = response.body()?.query()?.loginToken()
                loginToken?.let {
                    login(username, password, null, twoFactorCode, it, userLanguage, loginCallback)
                } ?: run {
                    loginCallback.error(IOException("Failed to retrieve login token"))
                }
            } else {
                loginCallback.error(IOException("Failed to retrieve login token"))
            }

            override fun onFailure(call: Call<MwQueryResponse?>, t: Throwable) {
                loginCallback.error(t)
            }
        })
    }
    @Throws(Throwable::class)
    fun loginBlocking(userName: String, password: String, twoFactorCode: String?) {
        val tokenResponse = getLoginToken().execute()
        if (tokenResponse.body()?.query()?.loginToken().isNullOrEmpty()) {
            throw IOException("Unexpected response when getting login token.")
        }

        val loginToken = tokenResponse.body()?.query()?.loginToken()
        val tempLoginCall = if (twoFactorCode.isNullOrEmpty()) {
            loginInterface.postLogIn(userName, password, loginToken, userLanguage, WIKIPEDIA_URL)
        } else {
            loginInterface.postLogIn(
                userName, password, null, twoFactorCode, loginToken, userLanguage, true
            )
        }

        val response = tempLoginCall.execute()
        val loginResponse = response.body() ?: throw IOException("Unexpected response when logging in.")
        val loginResult = loginResponse.toLoginResult(password) ?: throw IOException("Unexpected response when logging in.")

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
    }

    private fun getExtendedInfo(userName: String, loginResult: LoginResult, cb: LoginCallback) =
        loginInterface.getUserInfo(userName)
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response: MwQueryResponse? ->
                loginResult.userId = response?.query()?.userInfo()?.id() ?: 0
                loginResult.groups =
                    response?.query()?.getUserResponse(userName)?.groups ?: emptySet()
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
