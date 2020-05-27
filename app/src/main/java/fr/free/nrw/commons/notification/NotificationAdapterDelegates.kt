package fr.free.nrw.commons.notification

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import kotlinx.android.synthetic.main.activity_login.title
import kotlinx.android.synthetic.main.item_notification.*
import org.wikipedia.util.StringUtil


fun notificationDelegate(onNotificationClicked: (Notification) -> Unit) =
    adapterDelegateLayoutContainer<Notification, Notification>(R.layout.item_notification) {
        containerView.setOnClickListener { onNotificationClicked(item) }
        bind {
            title.text = item.processedNotificationText
            time.text = item.date
        }

    }

private val Notification.processedNotificationText: CharSequence
    get() = notificationText.trim()
        .replace("(^\\s*)|(\\s*$)".toRegex(), "")
        .let { StringUtil.fromHtml(it).toString() }
        .let { if (it.length > 280) "${it.substring(0, 279)}..." else it } + " "
