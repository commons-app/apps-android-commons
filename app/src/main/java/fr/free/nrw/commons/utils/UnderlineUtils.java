package fr.free.nrw.commons.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

public final class UnderlineUtils {

    /**
     * This method sets underlined string text to a TextView
     *
     * @param textView TextView associated with string resource
     * @param stringResourceName string resource name
     * @param context
     */
    public static void setUnderlinedText(final TextView textView, final int stringResourceName, final Context context) {
        final SpannableString content = new SpannableString(context.getString(stringResourceName));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        textView.setText(content);
    }

}
