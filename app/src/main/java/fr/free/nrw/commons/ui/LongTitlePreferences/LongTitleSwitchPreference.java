package fr.free.nrw.commons.ui.LongTitlePreferences;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by seannemann on 6/27/2018.
 */

public class LongTitleSwitchPreference extends SwitchPreference {
    public LongTitleSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LongTitleSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongTitleSwitchPreference(Context context) {
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
