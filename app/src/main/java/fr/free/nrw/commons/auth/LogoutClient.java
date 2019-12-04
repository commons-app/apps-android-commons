package fr.free.nrw.commons.auth;


import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.mwapi.MwPostResponse;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Observable;

/**
 * Handler for logout
 */
@Singleton
public class LogoutClient {

    private final Service service;

    @Inject
    public LogoutClient(@Named("commons-service") Service service) {
        this.service = service;
    }

    /**
     * Fetches the  CSRF token and uses that to post the logout api call
     * @return
     */
    public Observable<MwPostResponse> postLogout() {
        return service.getCsrfToken().concatMap(tokenResponse -> service.postLogout(
                Objects.requireNonNull(Objects.requireNonNull(tokenResponse.query()).csrfToken())));
    }
}
