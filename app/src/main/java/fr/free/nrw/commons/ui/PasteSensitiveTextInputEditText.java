package fr.free.nrw.commons.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import com.google.android.material.textfield.TextInputEditText;
import fr.free.nrw.commons.R;

public class PasteSensitiveTextInputEditText extends TextInputEditText {

    private boolean formattingAllowed = true;

    public PasteSensitiveTextInputEditText(final Context context) {
        super(context);
    }

    public PasteSensitiveTextInputEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        formattingAllowed = extractFormattingAttribute(context, attrs);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {

        // if not paste command, or formatting is allowed, return default
        if(id != android.R.id.paste || formattingAllowed){
            return super.onTextContextMenuItem(id);
        }

        // if its paste and formatting not allowed
        boolean proceeded;
        if(VERSION.SDK_INT >= 23) {
            proceeded = super.onTextContextMenuItem(android.R.id.pasteAsPlainText);
        }else {
            proceeded = super.onTextContextMenuItem(id);
            if (proceeded && getText() != null) {
                // rewrite with plain text so formatting is lost
                setText(getText().toString());
                setSelection(getText().length());
            }
        }
        return proceeded;
    }

    private boolean extractFormattingAttribute(Context context, AttributeSet attrs){

        boolean formatAllowed = true;

        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs, R.styleable.PasteSensitiveTextInputEditText, 0, 0);

        try {
            formatAllowed = a.getBoolean(
                R.styleable.PasteSensitiveTextInputEditText_allowFormatting, true);
        } finally {
            a.recycle();
        }
        return formatAllowed;
    }

    public void setFormattingAllowed(boolean formattingAllowed){
        this.formattingAllowed = formattingAllowed;
    }
}
