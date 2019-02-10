package fr.free.nrw.commons.notification;

import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.pedrogomez.renderers.Renderer;
import com.daimajia.swipe.SwipeLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;
import timber.log.Timber;

/**
 * Created by root on 19.12.2017.
 */

public class NotificationRenderer extends Renderer<Notification> {
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.time)
    TextView time;
    @BindView(R.id.icon)
    ImageView icon;
    @BindView(R.id.swipeLayout)
    SwipeLayout swipeLayout;
    @BindView(R.id.bottom)
    LinearLayout bottomLayout;

    private NotificationClicked listener;
    private boolean isarchivedvisible = false;


    NotificationRenderer(NotificationClicked listener, boolean isarchivedvisible) {
        this.listener = listener;
        this.isarchivedvisible = isarchivedvisible;
    }
    @OnClick(R.id.bottom)
    void onBottomLayoutClicked(){
        Notification notification = getContent();
        Timber.d("NotificationID: %s", notification.notificationId);
        listener.markNotificationAsRead(notification);
    }
    @Override
    protected void setUpView(View rootView) {

    }
    @Override
    protected void hookListeners(View rootView) {
        rootView.setOnClickListener(v -> listener.notificationClicked(getContent()));
    }

    @Override
    protected View inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        View inflatedView = layoutInflater.inflate(R.layout.item_notification, viewGroup, false);
        ButterKnife.bind(this, inflatedView);
        if (isarchivedvisible) {
            swipeLayout.setSwipeEnabled(false);
        }else {
            swipeLayout.setSwipeEnabled(true);
        }
        swipeLayout.addDrag(SwipeLayout.DragEdge.Top, bottomLayout);
        swipeLayout.addRevealListener(R.id.bottom_wrapper_child1, (child, edge, fraction, distance) -> {
            View star = child.findViewById(R.id.star);
            float d = child.getHeight() / 2 - star.getHeight() / 2;
            ViewHelper.setTranslationY(star, d * fraction);
            ViewHelper.setScaleX(star, fraction + 0.6f);
            ViewHelper.setScaleY(star, fraction + 0.6f);
            int c = (Integer) evaluate(fraction, Color.parseColor("#dddddd"), Color.parseColor("#90960a0a"));
            child.setBackgroundColor(c);
        });
        return inflatedView;
    }

    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int) ((startA + (int) (fraction * (endA - startA))) << 24) |
                (int) ((startR + (int) (fraction * (endR - startR))) << 16) |
                (int) ((startG + (int) (fraction * (endG - startG))) << 8) |
                (int) ((startB + (int) (fraction * (endB - startB))));
    }

    @Override
    public void render() {
        Notification notification = getContent();
        setTitle(notification.notificationText);
        time.setText(notification.date);
    }

    /**
     * Cleans up the notification text and sets it as the title
     * Clean up is required to fix escaped HTML string and extra white spaces at the beginning of the notification
     *
     * @param notificationText
     */
    private void setTitle(String notificationText) {
        notificationText = notificationText.trim().replaceAll("(^\\s*)|(\\s*$)", "");
        notificationText = Html.fromHtml(notificationText).toString();
        if (notificationText.length() > 280) {
            notificationText = notificationText.substring(0, 279);
            notificationText = notificationText.concat("...");
        }
        notificationText = notificationText.concat(" ");
        title.setText(notificationText);
    }

    public interface NotificationClicked {
        void notificationClicked(Notification notification);
        void markNotificationAsRead(Notification notification);
    }
}
