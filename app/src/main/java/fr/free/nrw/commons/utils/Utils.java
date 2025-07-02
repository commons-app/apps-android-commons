package fr.free.nrw.commons.utils;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

public class Utils {

    /**
     * This method sets underlined string text to a TextView
     *
     * @param textView TextView associated with string resource
     * @param stringResourceName string resource name
     * @param context
     */
    public static void setUnderlinedText(TextView textView, int stringResourceName, Context context) {
        SpannableString content = new SpannableString(context.getString(stringResourceName));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        textView.setText(content);
    }

}
