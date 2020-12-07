package fr.free.nrw.commons.auth

import io.reactivex.Observable
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler for logout
 */
@Singleton
class LogoutClient @Inject constructor(@param:Named("commons-service") private val service: Service) {

    /**
     * Fetches the CSRF token and uses that to post the logout api call
     * @return API result of logout call
     */
    fun postLogout(): Observable<MwPostResponse> {
        return service.csrfToken.flatMap { tokenResponse: MwQueryResponse ->
            service.postLogout(tokenResponse.query()!!.csrfToken()!!)
        }
    }

}