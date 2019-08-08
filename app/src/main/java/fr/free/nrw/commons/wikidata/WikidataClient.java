package fr.free.nrw.commons.wikidata;

import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Observable;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_WIKI_DATA_CSRF;

@Singleton
public class WikidataClient {

    private final Service service;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public WikidataClient(@Named("wikidata-service") Service service,
                          @Named(NAMED_WIKI_DATA_CSRF) CsrfTokenClient csrfTokenClient) {
        this.service = service;
        this.csrfTokenClient = csrfTokenClient;
    }

    public Observable<Long> createClaim(String entityId, String property, String snaktype, String value) {
        try {
            return service.postCreateClaim(entityId, snaktype, property, value, "en", csrfTokenClient.getTokenBlocking())
                    .map(mwPostResponse -> {
                        if (mwPostResponse.getSuccessVal() == 1) {
                            return 1L;
                        }
                        return -1L;
                    });
        } catch (Throwable throwable) {
            return Observable.just(-1L);
        }
    }
}
