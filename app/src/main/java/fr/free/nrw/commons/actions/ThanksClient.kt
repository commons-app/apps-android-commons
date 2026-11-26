package fr.free.nrw.commons.actions

import com.google.gson.JsonParseException
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.di.NetworkingModule.Companion.NAMED_COMMONS_CSRF
import io.reactivex.Observable
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Client for the Wkikimedia Thanks API extension
 * Thanks are used by a user to show gratitude to another user for their contributions
 */
@Singleton
class ThanksClient
    @Inject
    constructor(
        @param:Named(NAMED_COMMONS_CSRF) private val csrfTokenClient: CsrfTokenClient,
        private val service: ThanksInterface,
    ) {
        /**
         * Thanks a user for a particular revision
         * @param revisionId The revision ID the user would like to thank someone for
         * @return if thanks was successfully sent to intended recipient
         */
        fun thank(revisionId: Long): Observable<Boolean> =
            executeWithToken(false, "Failed to send thanks") { token ->
                service
                    .thank(
                        revisionId.toString(), // Rev
                        null, // Log
                        token, // Token
                        CommonsApplication.instance.userAgent, // Source
                    ).map { mwThankPostResponse ->
                        mwThankPostResponse.result?.success == 1
                    }
            }

        private fun <T> executeWithToken(
            fallback: T,
            errorMessage: String,
            block: (String) -> Observable<T>,
        ): Observable<T> =
            try {
                block(csrfTokenClient.getTokenBlocking())
            } catch (e: InvalidLoginTokenException) {
                Observable.error(e)
            } catch (e: IOException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: JsonParseException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: ExecutionException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: IllegalStateException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            } catch (e: UninitializedPropertyAccessException) {
                Timber.e(e, errorMessage)
                Observable.just(fallback)
            }
    }
