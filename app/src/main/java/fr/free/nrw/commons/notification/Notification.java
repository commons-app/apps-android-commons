package fr.free.nrw.commons.notification;

/**
 * Created by root on 18.12.2017.
 */

public class Notification {
    public NotificationType notificationType;
    public String notificationText;
    public String date;
    public String description;
    public String link;

    public Notification(NotificationType notificationType, String notificationText, String date, String description, String link) {
        this.notificationType = notificationType;
        this.notificationText = notificationText;
        this.date = date;
        this.description = description;
        this.link = link;
    }
}
