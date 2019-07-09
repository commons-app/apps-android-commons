package fr.free.nrw.commons.actions;

import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.CommonsApplication;
import io.reactivex.Observable;

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
