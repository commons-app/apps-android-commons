package fr.free.nrw.commons.actions;

import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.CommonsApplication;
import io.reactivex.Observable;

/**
 * Facilitates the Wkikimedia Thanks api extension, as described in the 
 * api documentation: "The Thanks extension includes an API for sending thanks"
 * 
 * In simple terms this class is used by a user to thank someone for adding 
 * contribution to the commons platform
 */
@Singleton
public class ThanksClient {

    private final CsrfTokenClient csrfTokenClient;
    private final Service service;

    @Inject
    public ThanksClient(@Named("commons-csrf") CsrfTokenClient csrfTokenClient,
                        @Named("commons-service") Service service) {
        this.csrfTokenClient = csrfTokenClient;
        this.service = service;
    }

    /**
     * Handles the Thanking logic
     * @param revisionID The revision ID you would like to thank someone for
     * @return if thanks was successfully sent to intended recipient, returned as a boolean observable
     */
    public Observable<Boolean> thank(long revisionId) {
        try {
            return service.thank(String.valueOf(revisionId), null,
                    csrfTokenClient.getTokenBlocking(),
                    CommonsApplication.getInstance().getUserAgent())
                    .map(mwQueryResponse -> mwQueryResponse.getSuccessVal() == 1);
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }
}
