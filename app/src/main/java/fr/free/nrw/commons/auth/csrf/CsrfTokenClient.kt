package fr.free.nrw.commons.auth.csrf

import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginFailedException
import fr.free.nrw.commons.auth.login.LoginResult
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.Callable
import java.util.concurrent.Executors.newSingleThreadExecutor

class CsrfTokenClient(
    private val sessionManager: SessionManager,
    private val csrfTokenInterface: CsrfTokenInterface,
    private val loginClient: LoginClient,
    private val logoutClient: LogoutClient
) {
    private var retries = 0
    private var csrfTokenCall: Call<MwQueryResponse?>? = null


    @Throws(Throwable::class)
    fun getTokenBlocking(): String {
        var token = ""
        val userName = sessionManager.userName ?: ""
        val password = sessionManager.password ?: ""

        for (retry in 0 until MAX_RETRIES_OF_LOGIN_BLOCKING) {
            try {
                if (retry > 0) {
                    // Log in explicitly
                    loginClient.loginBlocking(userName, password, "")
                }

                // Get CSRFToken response off the main thread.
                val response = newSingleThreadExecutor().submit(Callable {
                    csrfTokenInterface.getCsrfTokenCall().execute()
                }).get()

                if (response.body()?.query()?.csrfToken().isNullOrEmpty()) {
                    continue
                }

                token = response.body()!!.query()!!.csrfToken()!!
                if (sessionManager.isUserLoggedIn && token == ANON_TOKEN) {
                    throw InvalidLoginTokenException(ANONYMOUS_TOKEN_MESSAGE)
                }
                break
            } catch (e: LoginFailedException) {
               throw InvalidLoginTokenException(ANONYMOUS_TOKEN_MESSAGE)
            }
            catch (t: Throwable) {
                Timber.w(t)
            }
        }

        if (token.isEmpty() || token == ANON_TOKEN) {
            throw InvalidLoginTokenException(ANONYMOUS_TOKEN_MESSAGE)
        }
        return token
    }

    @VisibleForTesting
    fun request(service: CsrfTokenInterface, cb: Callback): Call<MwQueryResponse?> =
        requestToken(service, object : Callback {
            override fun success(token: String?) {
                if (sessionManager.isUserLoggedIn && token == ANON_TOKEN) {
                    retryWithLogin(cb) {
                        InvalidLoginTokenException(ANONYMOUS_TOKEN_MESSAGE)
                    }
                } else {
                    cb.success(token)
                }
            }

            override fun failure(caught: Throwable?) = retryWithLogin(cb) { caught }

            override fun twoFactorPrompt() = cb.twoFactorPrompt()
        })

    @VisibleForTesting
    fun requestToken(service: CsrfTokenInterface, cb: Callback): Call<MwQueryResponse?> {
        val call = service.getCsrfTokenCall()
        call.enqueue(object : retrofit2.Callback<MwQueryResponse?> {
            override fun onResponse(call: Call<MwQueryResponse?>, response: Response<MwQueryResponse?>) {
                if (call.isCanceled) {
                    return
                }
                cb.success(response.body()!!.query()!!.csrfToken())
            }

            override fun onFailure(call: Call<MwQueryResponse?>, t: Throwable) {
                if (call.isCanceled) {
                    return
                }
                cb.failure(t)
            }
        })
        return call
    }

    private fun retryWithLogin(callback: Callback, caught: () -> Throwable?) {
        val userName = sessionManager.userName
        val password = sessionManager.password
        if (retries < MAX_RETRIES && !userName.isNullOrEmpty() && !password.isNullOrEmpty()) {
            retries++
            logoutClient.logout()
            login(userName, password, callback) {
                Timber.i("retrying...")
                cancel()
                csrfTokenCall = request(csrfTokenInterface, callback)
            }
        } else {
            callback.failure(caught())
        }
    }

    private fun login(
        username: String,
        password: String,
        callback: Callback,
        retryCallback: () -> Unit
    ) = loginClient.request(username, password, object : LoginCallback {
        override fun success(loginResult: LoginResult) {
            if (loginResult.pass) {
                sessionManager.updateAccount(loginResult)
                retryCallback()
            } else {
                callback.failure(LoginFailedException(loginResult.message))
            }
        }

        override fun twoFactorPrompt(caught: Throwable, token: String?) =
            callback.twoFactorPrompt()

        // Should not happen here, but call the callback just in case.
        override fun passwordResetPrompt(token: String?) =
            callback.failure(LoginFailedException("Logged in with temporary password."))

        override fun error(caught: Throwable) = callback.failure(caught)
    })

    private fun cancel() {
        loginClient.cancel()
        if (csrfTokenCall != null) {
            csrfTokenCall!!.cancel()
            csrfTokenCall = null
        }
    }

    interface Callback {
        fun success(token: String?)
        fun failure(caught: Throwable?)
        fun twoFactorPrompt()
    }

    companion object {
        private const val ANON_TOKEN = "+\\"
        private const val MAX_RETRIES = 1
        private const val MAX_RETRIES_OF_LOGIN_BLOCKING = 2
        const val INVALID_TOKEN_ERROR_MESSAGE = "Invalid token, or login failure."
        const val ANONYMOUS_TOKEN_MESSAGE = "App believes we're logged in, but got anonymous token."
    }
}
class InvalidLoginTokenException(message: String) : Exception(message)

