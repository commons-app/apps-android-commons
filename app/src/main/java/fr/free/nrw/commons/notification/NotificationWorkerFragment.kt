package fr.free.nrw.commons.notification

import android.app.Fragment
import android.os.Bundle

import fr.free.nrw.commons.notification.models.Notification


/**
 * Created by knightshade on 25/2/18.
 */
class NotificationWorkerFragment : Fragment() {

    var notificationList: List<Notification>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }
}
