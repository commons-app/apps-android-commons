package fr.free.nrw.commons.ui.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;

import fr.free.nrw.commons.Utils;

/**
 * An {@link AppCompatTextView} which formats the text to HTML displayable text and makes any
 * links clickable.
 */
public class HtmlTextView extends AppCompatTextView {

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setMovementMethod(LinkMovementMethod.getInstance());
        setText(Utils.fromHtml(getText().toString()));
    }
}
