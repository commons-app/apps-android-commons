package fr.free.nrw.commons.ui.LongTitlePreferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by seannemann on 6/27/2018.
 */

public class LongTitleListPreference extends ListPreference {
    public LongTitleListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongTitleListPreference(Context context) {
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
