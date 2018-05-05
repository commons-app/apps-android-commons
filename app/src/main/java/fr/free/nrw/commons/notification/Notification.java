package fr.free.nrw.commons.notification;

import android.content.Context;

public class Notification {
    public final NotificationType notificationType;
    public final String date;
    public final String description;
    public final String link;
    public final String iconUrl;
    public final int notificationTextFormatId;
    public final String[] notificationTextParameters;
    public String notificationText;

    public Notification(NotificationType notificationType, String notificationText, String date, String description, String link, String iconUrl) {
        this.notificationType = notificationType;
        this.notificationText = notificationText;
        this.date = date;
        this.description = description;
        this.link = link;
        this.iconUrl = iconUrl;
        this.notificationTextFormatId = 0;
        this.notificationTextParameters = new String[0];
    }

    public Notification(NotificationType notificationType, String date, String description, String link, String icon, int notificationTextFormatId, String[] notificationTextParameters) {
        this.notificationType = notificationType;
        this.date = date;
        this.description = description;
        this.link = link;
        this.iconUrl = icon;
        this.notificationTextFormatId = notificationTextFormatId;
        this.notificationTextParameters = notificationTextParameters;
        this.notificationText = null;
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    public Notification format(Context context) {
        notificationText = context.getString(notificationTextFormatId, notificationTextParameters);
        return this;
    }
}
