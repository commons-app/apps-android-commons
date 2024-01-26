package fr.free.nrw.commons.auth.login

import org.wikipedia.dataclient.WikiSite

class LoginResetPasswordResult(
    site: WikiSite,
    status: String,
    userName: String?,
    password: String?,
    message: String?
) : LoginResult(site, status, userName, password, message)
