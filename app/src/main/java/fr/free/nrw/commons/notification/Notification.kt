package fr.free.nrw.commons.notification

import org.wikipedia.util.DateUtil

/**
 * Created by root on 18.12.2017.
 */
data class Notification(var notificationType: NotificationType,
                   var notificationText: String,
                   var date: String,
                   var link: String,
                   var iconUrl: String,
                   var notificationId: String) {
    override fun toString(): String {
        return "Notification" +
                "notificationType='" + notificationType + '\'' +
                ", notificationText='" + notificationText + '\'' +
                ", date='" + date + '\'' +
                ", link='" + link + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", notificationId='" + notificationId + '\'' +
                '}'
    }

    companion object {
        @JvmStatic
        fun from(wikiNotification: org.wikipedia.notifications.Notification): Notification {
            val contents = wikiNotification.contents
            val notificationLink = if (contents == null || contents.links == null || contents.links!!.primary == null) "" else contents.links!!.primary!!.url
            return Notification(NotificationType.UNKNOWN,
                    contents?.compactHeader ?: "",
                    DateUtil.getMonthOnlyDateString(wikiNotification.timestamp),
                    notificationLink,
                    "", wikiNotification.id().toString())
        }
    }

}