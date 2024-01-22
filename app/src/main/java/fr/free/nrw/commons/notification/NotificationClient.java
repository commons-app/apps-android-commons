package fr.free.nrw.commons.notification;

import fr.free.nrw.commons.notification.models.Notification;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

@Singleton
public class NotificationClient {

    private final NotificationInterface service;
    private final CsrfTokenClient csrfTokenClient;

    @Inject
    public NotificationClient(
        final NotificationInterface service,
        @Named(NAMED_COMMONS_CSRF) final CsrfTokenClient csrfTokenClient) {
        this.service = service;
        this.csrfTokenClient = csrfTokenClient;
    }

    public Single<List<Notification>> getNotifications(final boolean archived) {
        return service.getAllNotifications("wikidatawiki|commonswiki|enwiki", archived ? "read" : "!read", null)
                .map(mwQueryResponse -> mwQueryResponse.query().notifications().list())
                .flatMap(Observable::fromIterable)
                .map(Notification::from)
                .toList();
    }

    public Observable<Boolean> markNotificationAsRead(final String notificationId) {
        try {
            return service.markRead(csrfTokenClient.getTokenBlocking(), notificationId, "")
                    .map(MwQueryResponse::success);
        } catch (final Throwable throwable) {
            return Observable.just(false);
        }
    }
}
