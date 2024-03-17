package fr.free.nrw.commons.auth.login

sealed class LoginResult(
    val status: String,
    val userName: String?,
    val password: String?,
    val message: String?
) {
    var userId = 0
    var groups = emptySet<String>()
    val pass: Boolean get() = "PASS" == status

    class Result(
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ): LoginResult(status, userName, password, message)

    class OAuthResult(
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ) : LoginResult(status, userName, password, message)

    class ResetPasswordResult(
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ) : LoginResult(status, userName, password, message)
}
