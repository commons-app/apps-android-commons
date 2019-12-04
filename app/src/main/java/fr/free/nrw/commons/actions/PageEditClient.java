package fr.free.nrw.commons.actions;

import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import io.reactivex.Observable;

public class PageEditClient {

    private final CsrfTokenClient csrfTokenClient;
    private final PageEditInterface pageEditInterface;
    private final Service service;

    public PageEditClient(CsrfTokenClient csrfTokenClient,
                          PageEditInterface pageEditInterface,
                          Service service) {
        this.csrfTokenClient = csrfTokenClient;
        this.pageEditInterface = pageEditInterface;
        this.service = service;
    }

    public Observable<Boolean> edit(String pageTitle, String text, String summary) {
        try {
            return pageEditInterface.postEdit(pageTitle, summary, text, csrfTokenClient.getTokenBlocking())
                    .map(editResponse -> editResponse.edit().editSucceeded());
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    public Observable<Boolean> appendEdit(String pageTitle, String appendText, String summary) {
        try {
            return pageEditInterface.postAppendEdit(pageTitle, summary, appendText, csrfTokenClient.getTokenBlocking())
                    .map(editResponse -> editResponse.edit().editSucceeded());
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    public Observable<Boolean> prependEdit(String pageTitle, String prependText, String summary) {
        try {
            return pageEditInterface.postPrependEdit(pageTitle, summary, prependText, csrfTokenClient.getTokenBlocking())
                    .map(editResponse -> editResponse.edit().editSucceeded());
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

}
