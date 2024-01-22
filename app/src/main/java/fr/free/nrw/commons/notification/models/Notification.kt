package fr.free.nrw.commons.notification.models

/**
 * Created by root on 18.12.2017.
 */
data class Notification(
    var notificationType: NotificationType,
    var notificationText: String,
    var date: String,
    var link: String,
    var iconUrl: String,
    var notificationId: String
)