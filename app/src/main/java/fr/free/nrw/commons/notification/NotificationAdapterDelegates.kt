package fr.free.nrw.commons.notification

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.data.models.notification.Notification
import fr.free.nrw.commons.databinding.ItemNotificationBinding
import org.wikipedia.util.StringUtil


fun notificationDelegate(onNotificationClicked: (Notification) -> Unit) =
    adapterDelegateViewBinding<Notification, Notification, ItemNotificationBinding>({ layoutInflater, parent ->
        ItemNotificationBinding.inflate(layoutInflater, parent, false)
    }) {
        binding.root.setOnClickListener { onNotificationClicked(item) }
        bind {
            binding.title.text = item.processedNotificationText
            binding.time.text = item.date
        }

    }

private val Notification.processedNotificationText: CharSequence
    get() = notificationText.trim()
        .replace("(^\\s*)|(\\s*$)".toRegex(), "")
        .let { StringUtil.fromHtml(it).toString() }
        .let { if (it.length > 280) "${it.substring(0, 279)}..." else it } + " "
