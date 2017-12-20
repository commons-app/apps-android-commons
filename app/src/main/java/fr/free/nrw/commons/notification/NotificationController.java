package fr.free.nrw.commons.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by root on 19.12.2017.
 */

public class NotificationController {

    public static List<Notification> loadNotifications() {
        List<Notification> notifications = new ArrayList<>();
        notifications.add(new Notification(Notification.NotificationType.message, "notification 1"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 2"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 3"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 4"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 5"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 6"));
        notifications.add(new Notification(Notification.NotificationType.message, "notification 7"));
        return notifications;
    }
}
