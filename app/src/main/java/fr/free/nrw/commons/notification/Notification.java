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
    public String iconUrl;
    public String dateWithYear;
    public String notificationId;

    public Notification(NotificationType notificationType, String notificationText, String date, String description, String link, String iconUrl, String dateWithYear, String notificationId) {
        this.notificationType = notificationType;
        this.notificationText = notificationText;
        this.date = date;
        this.description = description;
        this.link = link;
        this.iconUrl = iconUrl;
        this.dateWithYear = dateWithYear;
        this.notificationId=notificationId;
    }

    @Override
    public String toString() {
        return "Notification" +
                "notificationType='" + notificationType + '\'' +
                ", notificationText='" + notificationText + '\'' +
                ", date='" + date + '\'' +
                ", description='" + description + '\'' +
                ", link='" + link + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", dateWithYear=" + dateWithYear +
                ", notificationId='" + notificationId + '\'' +
                '}';
    }
}
