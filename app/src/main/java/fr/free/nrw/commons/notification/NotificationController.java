package fr.free.nrw.commons.notification;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Created by root on 19.12.2017.
 */
@Singleton
public class NotificationController {

    private NotificationClient notificationClient;


    @Inject
    public NotificationController(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public Single<List<Notification>> getNotifications(boolean archived) {
        return notificationClient.getNotifications(archived);
    }

    Observable<Boolean> markAsRead(Notification notification) {
        return notificationClient.markNotificationAsRead(notification.getNotificationId());
    }
}
