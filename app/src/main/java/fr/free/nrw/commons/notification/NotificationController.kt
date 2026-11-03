package fr.free.nrw.commons.notification

import fr.free.nrw.commons.notification.models.Notification
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by root on 19.12.2017.
 */
@Singleton
class NotificationController
    @Inject
    constructor(
        private val notificationClient: NotificationClient,
    ) {
        fun getNotifications(archived: Boolean): Single<List<Notification>> =
            notificationClient.getNotifications(archived)

        fun markAsRead(notification: Notification): Observable<Boolean> =
            notificationClient.markNotificationAsRead(notification.notificationId)
    }
