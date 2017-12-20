package fr.free.nrw.commons.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

/**
 * Created by root on 19.12.2017.
 */

public class NotificationRenderer extends Renderer<Notification> {
    @BindView(R.id.title) TextView title;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.time) TextView time;
    @BindView(R.id.icon) ImageView icon;
    private NotificationClicked listener;


    NotificationRenderer(NotificationClicked listener) {
        this.listener = listener;
    }

    @Override
    protected void setUpView(View view) {    }

    @Override
    protected void hookListeners(View rootView) {
        rootView.setOnClickListener(v -> listener.notificationClicked(getContent()));
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        View inflatedView = layoutInflater.inflate(R.layout.item_notification, viewGroup, false);
        ButterKnife.bind(this, inflatedView);
        return inflatedView;
    }

    @Override
    public void render() {
            Notification notification = getContent();
            title.setText(notification.notificationText);
            time.setText("3d");
            description.setText("Example notification description");
            switch (notification.notificationType) {
                case edit:
                    icon.setImageResource(R.drawable.ic_edit_black_24dp);
                    break;
                case message:
                    icon.setImageResource(R.drawable.ic_message_black_24dp);
                    break;
                case mention:
                    icon.setImageResource(R.drawable.ic_chat_bubble_black_24px);
                    break;
                default:
                    icon.setImageResource(R.drawable.round_icon_unknown);
            }
    }

    public interface NotificationClicked{
        void notificationClicked(Notification notification);
    }
}
