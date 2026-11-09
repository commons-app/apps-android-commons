package fr.free.nrw.commons.notification

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityNotificationBinding

/** Applies the read or unread section labels to the notification screen. */
internal class NotificationLabels(
    private val activity: AppCompatActivity,
    private val binding: ActivityNotificationBinding,
) {
    /** Updates the toolbar title for the selected notification section. */
    fun setPageTitle(isRead: Boolean) {
        activity.supportActionBar?.title =
            if (isRead) {
                activity.getString(R.string.read_notifications)
            } else {
                activity.getString(R.string.notifications)
            }
    }

    /** Updates the empty notification message for the selected notification section. */
    fun setEmptyView(isRead: Boolean) {
        binding.noNotificationText.text =
            if (isRead) {
                activity.getString(R.string.no_read_notification)
            } else {
                activity.getString(R.string.no_notification)
            }
    }

    /** Updates the archive menu label for the selected notification section. */
    fun setMenuItemTitle(
        isRead: Boolean,
        notificationMenuItem: MenuItem?,
    ) {
        notificationMenuItem?.title =
            if (isRead) {
                activity.getString(R.string.menu_option_unread)
            } else {
                activity.getString(R.string.menu_option_read)
            }
    }
}
