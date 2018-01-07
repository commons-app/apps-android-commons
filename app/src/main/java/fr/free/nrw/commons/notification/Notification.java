package fr.free.nrw.commons.notification;

/**
 * Created by root on 18.12.2017.
 */

public class Notification {
    public NotificationType notificationType;
    public String notificationText;


    Notification (NotificationType notificationType, String notificationText) {
        this.notificationType = notificationType;
        this.notificationText = notificationText;
    }


    public enum NotificationType {
        /* Added for test purposes, needs to be rescheduled after implementing
         fetching notifications from server */
        edit,
        mention,
        message,
        block;
    }
}
