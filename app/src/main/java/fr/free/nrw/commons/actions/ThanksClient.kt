package fr.free.nrw.commons.actions

import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF
import io.reactivex.Observable
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.auth.login.LoginFailedException
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
    private val service: ThanksInterface
) {
    /**
     * Thanks a user for a particular revision
     * @param revisionId The revision ID the user would like to thank someone for
     * @return if thanks was successfully sent to intended recipient
     */
    fun thank(revisionId: Long): Observable<Boolean> {
        return try {
            service.thank(
                revisionId.toString(),                      // Rev
                null,                                       // Log
                csrfTokenClient.getTokenBlocking(),              // Token
                CommonsApplication.getInstance().userAgent  // Source
            ).map {
                mwThankPostResponse -> mwThankPostResponse.result?.success == 1
            }
        }
        catch (throwable: Throwable) {
            if (throwable is InvalidLoginTokenException) {
                Observable.error(throwable)
            }
            else {
                Observable.just(false)
            }
        }
    }

}
