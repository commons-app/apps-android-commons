package free.nrw.commons.campaigns;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import free.nrw.commons.Utils;

class CampaignsListAdapter extends CursorAdapter {

    private DisplayImageOptions contributionDisplayOptions = Utils.getGenericDisplayOptions().build();;
    private Activity activity;

    public CampaignsListAdapter(Activity activity, Cursor c, int flags) {
        super(activity, c, flags);
        this.activity = activity;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View parent = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        return parent;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView campaignName = (TextView)view.findViewById(android.R.id.text1);

        Campaign campaign = Campaign.fromCursor(cursor);

        campaignName.setText(campaign.getTitle());
    }

}
