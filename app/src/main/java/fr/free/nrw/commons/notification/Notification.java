package fr.free.nrw.commons.notification;

import org.wikipedia.util.DateUtil;

import fr.free.nrw.commons.utils.CommonsDateUtil;

/**
 * Created by root on 18.12.2017.
 */

public class Notification {
    public NotificationType notificationType;
    public String notificationText;
    public String date;
    public String link;
    public String iconUrl;
    public String notificationId;

    public Notification(NotificationType notificationType,
                        String notificationText,
                        String date,
                        String link,
                        String iconUrl,
                        String notificationId) {
        this.notificationType = notificationType;
        this.notificationText = notificationText;
        this.date = date;
        this.link = link;
        this.iconUrl = iconUrl;
        this.notificationId=notificationId;
    }

    public static Notification from(org.wikipedia.notifications.Notification wikiNotification) {
        org.wikipedia.notifications.Notification.Contents contents = wikiNotification.getContents();
        String notificationLink = contents == null || contents.getLinks() == null
                || contents.getLinks().getPrimary() == null ? "" : contents.getLinks().getPrimary().getUrl();
        return new Notification(NotificationType.UNKNOWN,
                contents == null ? "" : contents.getCompactHeader(),
                DateUtil.getMonthOnlyDateString(wikiNotification.getTimestamp()),
                notificationLink,
                "",
                String.valueOf(wikiNotification.id()));
    }

    @Override
    public String toString() {
        return "Notification" +
                "notificationType='" + notificationType + '\'' +
                ", notificationText='" + notificationText + '\'' +
                ", date='" + date + '\'' +
                ", link='" + link + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", notificationId='" + notificationId + '\'' +
                '}';
    }
}
