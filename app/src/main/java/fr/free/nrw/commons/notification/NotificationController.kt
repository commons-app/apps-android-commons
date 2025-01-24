package fr.free.nrw.commons.notification

import fr.free.nrw.commons.notification.models.Notification
import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by root on 19.12.2017.
 */
@Singleton
class NotificationController @Inject constructor(
    private val notificationClient: NotificationClient
) {

    fun getNotifications(archived: Boolean): Single<List<Notification>> {
        return notificationClient.getNotifications(archived)
    }

    fun markAsRead(notification: Notification): Observable<Boolean> {
        return notificationClient.markNotificationAsRead(notification.notificationId)
    }
}
