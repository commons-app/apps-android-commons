package fr.free.nrw.commons.notification;

import static fr.free.nrw.commons.di.NetworkingModule.NAMED_COMMONS_CSRF;

import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;

@Singleton
public class NotificationClient {

  private final Service service;
  private final CsrfTokenClient csrfTokenClient;

  @Inject
  public NotificationClient(@Named("commons-service") Service service,
      @Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient) {
    this.service = service;
    this.csrfTokenClient = csrfTokenClient;
  }

  public Single<List<Notification>> getNotifications(boolean archived) {
    return service
        .getAllNotifications("wikidatawiki|commonswiki|enwiki", archived ? "read" : "!read", null)
        .map(mwQueryResponse -> mwQueryResponse.query().notifications().list())
        .flatMap(Observable::fromIterable)
        .map(notification -> Notification.from(notification))
        .toList();
  }

  public Observable<Boolean> markNotificationAsRead(String notificationId) {
    try {
      return service.markRead(csrfTokenClient.getTokenBlocking(), notificationId, "")
          .map(mwQueryResponse -> mwQueryResponse.success());
    } catch (Throwable throwable) {
      return Observable.just(false);
    }
  }
}
