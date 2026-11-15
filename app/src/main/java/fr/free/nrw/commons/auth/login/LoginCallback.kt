package fr.free.nrw.commons.auth.login

interface LoginCallback {
    fun success(loginResult: LoginResult)

    fun twoFactorPrompt(
        loginResult: LoginResult,
        caught: Throwable,
        token: String?,
    )

    fun emailAuthPrompt(
        loginResult: LoginResult,
        caught: Throwable,
        token: String?,
    )

    fun passwordResetPrompt(token: String?)

    fun error(caught: Throwable)
}
