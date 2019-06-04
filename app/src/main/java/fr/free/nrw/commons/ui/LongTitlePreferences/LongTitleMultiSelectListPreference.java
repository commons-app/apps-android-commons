package fr.free.nrw.commons.ui.LongTitlePreferences;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class LongTitleMultiSelectListPreference extends MultiSelectListPreference {
    /*
    public LongTitleMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LongTitleMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    */
    public LongTitleMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongTitleMultiSelectListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view)
    {
        super.onBindView(view);

        TextView title= view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
        }
    }

}
