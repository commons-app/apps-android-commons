package fr.free.nrw.commons.actions

import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF
import io.reactivex.Observable
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwPostResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Client for the Wkikimedia Thanks API extension
 * Thanks are used by a user to show gratitude to another user for their contributions
 */
@Singleton
class ThanksClient @Inject constructor(
    @param:Named(NAMED_COMMONS_CSRF) private val csrfTokenClient: CsrfTokenClient,
    @param:Named("commons-service") private val service: Service
) {
    /**
     * Thanks a user for a particular revision
     * @param revisionId The revision ID the user would like to thank someone for
     * @return if thanks was successfully sent to intended recipient
     */
    fun thank(revisionId: Long): Observable<Boolean> {
        return try {
            service.thank(revisionId.toString(), null, csrfTokenClient.tokenBlocking, CommonsApplication.getInstance().userAgent)
                .map { mwThankPostResponse -> mwThankPostResponse.result.success== 1 }
        } catch (throwable: Throwable) {
            Observable.just(false)
        }
    }

}