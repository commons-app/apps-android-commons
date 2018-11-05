package fr.free.nrw.commons.notification;

import android.graphics.Color;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.borjabravo.readmoretextview.ReadMoreTextView;
import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;

/**
 * Created by root on 19.12.2017.
 */

public class NotificationRenderer extends Renderer<Notification> {
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
        return inflatedView;
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
     * @param notificationText
     */
    private void setTitle(String notificationText) {
        notificationText = notificationText.trim().replaceAll("(^\\s*)|(\\s*$)", "");
        notificationText = Html.fromHtml(notificationText).toString();
        notificationText = notificationText.concat(" ");

        SpannableString ss = new SpannableString(notificationText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                listener.notificationClicked(getContent());
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLACK);
            }
        };

        // Attach a ClickableSpan to the range (start:0, end:notificationText.length()) of the String
        ss.setSpan(clickableSpan, 0, notificationText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(ss, TextView.BufferType.SPANNABLE);
    }

    public interface NotificationClicked{
        void notificationClicked(Notification notification);
    }
}
