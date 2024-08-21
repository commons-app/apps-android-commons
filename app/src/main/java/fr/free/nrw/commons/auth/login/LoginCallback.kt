package fr.free.nrw.commons.auth.login

interface LoginCallback {
    fun success(loginResult: LoginResult)
    fun twoFactorPrompt(caught: Throwable, token: String?)
    fun passwordResetPrompt(token: String?)
    fun error(caught: Throwable)
}
