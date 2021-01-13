package fr.free.nrw.commons.actions;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import io.reactivex.Observable;

/**
 * This class acts as a Client to facilitate wiki page editing
 * services to various dependency providing modules such as the Network module, the Review Controller ,etc
 * 
 * The methods provided by this class will post to the Media wiki api
 * documented at: https://commons.wikimedia.org/w/api.php?action=help&modules=edit
 */
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

    /**
     * This method is used when the content of the page is to be replaced by new content received
     * @param pageTitle   Title of the page to edit
     * @param text        Holds the page content
     * @param summary     Edit summary
     */
    public Observable<Boolean> edit(String pageTitle, String text, String summary) {
        try {
            return pageEditInterface.postEdit(pageTitle, summary, text, csrfTokenClient.getTokenBlocking())
                    .map(editResponse -> editResponse.edit().editSucceeded());
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

    /**
     * This method is used when we need to append something to the end of wiki page content
     * @param pageTitle   Title of the page to edit
     * @param appendText  The received page content is added to beginning of the page
     * @param summary     Edit summary
     */
    public Observable<Boolean> appendEdit(String pageTitle, String appendText, String summary) {
        return Single.create((SingleOnSubscribe<String>) emitter -> {
            try {
                emitter.onSuccess(csrfTokenClient.getTokenBlocking());
            } catch (Throwable throwable) {
                emitter.onError(throwable);
                throwable.printStackTrace();
            }
        }).flatMapObservable(token -> pageEditInterface.postAppendEdit(pageTitle, summary, appendText, token)
            .map(editResponse -> editResponse.edit().editSucceeded()));

    }

    /**
     * This method is used when we need to add something to the starting of the page
     * @param pageTitle   Title of the page to edit
     * @param prependText The received page content is added to beginning of the page
     * @param summary     Edit summary
     */
    public Observable<Boolean> prependEdit(String pageTitle, String prependText, String summary) {
        try {
            return pageEditInterface.postPrependEdit(pageTitle, summary, prependText, csrfTokenClient.getTokenBlocking())
                    .map(editResponse -> editResponse.edit().editSucceeded());
        } catch (Throwable throwable) {
            return Observable.just(false);
        }
    }

}
