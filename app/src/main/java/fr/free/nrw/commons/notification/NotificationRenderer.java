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
    @BindView(R.id.tvName)
    TextView tvName;
    @BindView(R.id.tvDesc) TextView tvDesc;
    @BindView(R.id.distance) TextView distance;
    @BindView(R.id.icon)
    ImageView icon;
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
            tvName.setText(notification.notificationText);
            switch (notification.notificationType) {
                case edit:
                    icon.setImageResource(R.drawable.round_icon_unknown);
                case message:
                    icon.setImageResource(R.drawable.round_icon_unknown);
                case mention:
                    icon.setImageResource(R.drawable.round_icon_unknown);
                case block:
                    icon.setImageResource(R.drawable.round_icon_unknown);
                default:
                    icon.setImageResource(R.drawable.round_icon_unknown);
            }
    }

    public interface NotificationClicked{
        void notificationClicked(Notification notification);
    }
}
