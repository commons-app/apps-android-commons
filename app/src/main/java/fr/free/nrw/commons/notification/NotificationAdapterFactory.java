package fr.free.nrw.commons.notification;

import android.support.annotation.NonNull;

import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Created by root on 19.12.2017.
 */

class NotificationAdapterFactory {
    private NotificationRenderer.NotificationClicked listener;
    private boolean isarchivedvisible = false;

    NotificationAdapterFactory(@NonNull NotificationRenderer.NotificationClicked listener, boolean isarchivedvisible) {
        this.listener = listener;
        this.isarchivedvisible = isarchivedvisible;
    }

    public RVRendererAdapter<Notification> create(List<Notification> notifications) {
        RendererBuilder<Notification> builder = new RendererBuilder<Notification>()
                .bind(Notification.class, new NotificationRenderer(listener, isarchivedvisible));
        ListAdapteeCollection<Notification> collection = new ListAdapteeCollection<>(
                notifications != null ? notifications : Collections.<Notification>emptyList());
        return new RVRendererAdapter<>(builder, collection);
    }
}
