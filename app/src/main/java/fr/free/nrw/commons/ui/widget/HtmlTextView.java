package fr.free.nrw.commons.ui.widget;

import android.content.Context;
import android.os.Build;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

/**
 * An {@link AppCompatTextView} which formats the text to HTML displayable text and makes any
 * links clickable.
 */
public class HtmlTextView extends AppCompatTextView {

    /**
     * Constructs a new instance of HtmlTextView
     * @param context the context of the view
     * @param attrs the set of attributes for the view
     */
    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setMovementMethod(LinkMovementMethod.getInstance());
        setText(fromHtml(getText().toString()));
    }

    /**
     * Sets the text to be displayed
     * @param newText the text to be displayed
     */
    public void setHtmlText(String newText) {
        setText(fromHtml(newText));
    }

    /**
     * Fix Html.fromHtml is deprecated problem
     *
     * @param source provided Html string
     * @return returned Spanned of appropriate method according to version check
     */
    private static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            //noinspection deprecation
            return Html.fromHtml(source);
        }
    }
}
