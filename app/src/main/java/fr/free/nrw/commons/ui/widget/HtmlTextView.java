package fr.free.nrw.commons.ui.widget;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

import org.wikipedia.util.StringUtil;

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
        setText(StringUtil.fromHtml(getText().toString()));
    }

    /**
     * Sets the text to be displayed
     * @param newText the text to be displayed
     */
    public void setHtmlText(String newText) {
        setText(StringUtil.fromHtml(newText));
    }
}
