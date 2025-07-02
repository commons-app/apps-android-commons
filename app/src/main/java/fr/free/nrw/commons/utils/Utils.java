package fr.free.nrw.commons.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.widget.TextView;

public class Utils {

    /*
    *Copies the content to the clipboard
    *
    */
    public static void copy(String label,String text, Context context){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

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
