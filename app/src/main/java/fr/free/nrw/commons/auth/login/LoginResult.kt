package fr.free.nrw.commons.auth.login

import org.wikipedia.dataclient.WikiSite

sealed class LoginResult(
    val site: WikiSite,
    val status: String,
    val userName: String?,
    val password: String?,
    val message: String?
) {
    var userId = 0
    var groups = emptySet<String>()
    val pass: Boolean get() = "PASS" == status

    class Result(
        site: WikiSite,
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ): LoginResult(site, status, userName, password, message)

    class OAuthResult(
        site: WikiSite,
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ) : LoginResult(site, status, userName, password, message)

    class ResetPasswordResult(
        site: WikiSite,
        status: String,
        userName: String?,
        password: String?,
        message: String?
    ) : LoginResult(site, status, userName, password, message)
}
