package fr.free.nrw.commons.auth.csrf

import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage
import javax.inject.Inject

class LogoutClient @Inject constructor(private val store: CommonsCookieStorage) {
    fun logout() = store.clear()
}