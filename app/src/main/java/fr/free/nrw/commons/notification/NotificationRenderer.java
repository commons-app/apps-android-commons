package fr.free.nrw.commons.notification;

import android.graphics.drawable.PictureDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.borjabravo.readmoretextview.ReadMoreTextView;
import com.bumptech.glide.RequestBuilder;
import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.glide.SvgSoftwareLayerSetter;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

/**
 * Created by root on 19.12.2017.
 */

public class NotificationRenderer extends Renderer<Notification> {
    private RequestBuilder<PictureDrawable> requestBuilder;

    @BindView(R.id.title) ReadMoreTextView title;
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
        requestBuilder = GlideApp.with(inflatedView.getContext())
                .as(PictureDrawable.class)
                .error(R.drawable.round_icon_unknown)
                .transition(withCrossFade())
                .listener(new SvgSoftwareLayerSetter());
        return inflatedView;
    }

    @Override
    public void render() {
        Notification notification = getContent();
        setTitle(notification.notificationText);
        time.setText(notification.date);
        requestBuilder.load(notification.iconUrl).into(icon);
    }

    /**
     * Cleans up the notification text and sets it as the title
     * Clean up is required to fix escaped HTML string and extra white spaces at the beginning of the notification
     * @param notificationText
     */
    private void setTitle(String notificationText) {
        notificationText = notificationText.trim().replaceAll("(^\\h*)|(\\h*$)", "");
        notificationText = Html.fromHtml(notificationText).toString();
        notificationText = notificationText.concat(" ");
        title.setText(notificationText);
    }

    public interface NotificationClicked{
        void notificationClicked(Notification notification);
    }
}
