package fr.free.nrw.commons.auth.login

import org.wikipedia.dataclient.WikiSite

open class LoginResult(
    val site: WikiSite,
    val status: String,
    val userName: String?,
    val password: String?,
    val message: String?
) {
    var userId = 0
    var groups = emptySet<String>()

    fun pass(): Boolean = "PASS" == status

    fun fail(): Boolean = "FAIL" == status
}
