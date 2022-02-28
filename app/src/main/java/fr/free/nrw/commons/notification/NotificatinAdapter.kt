package fr.free.nrw.commons.notification

import fr.free.nrw.commons.data.models.notification.Notification
import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter

internal class NotificatinAdapter(onNotificationClicked: (Notification) -> Unit) :
    BaseDelegateAdapter<Notification>(
        notificationDelegate(onNotificationClicked),
        areItemsTheSame = { oldItem, newItem -> oldItem.notificationId == newItem.notificationId }
    )
