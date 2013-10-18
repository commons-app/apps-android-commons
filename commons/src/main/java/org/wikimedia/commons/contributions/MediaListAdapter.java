package org.wikimedia.commons.contributions;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.volley.toolbox.ImageLoader;
import org.wikimedia.commons.CommonsApplication;
import org.wikimedia.commons.Media;
import org.wikimedia.commons.R;

import java.util.ArrayList;

public class MediaListAdapter extends BaseAdapter {
    private ArrayList<Media> mediaList;
    private Activity activity;

    public MediaListAdapter(Activity activity, ArrayList<Media> mediaList) {
        this.mediaList = mediaList;
        this.activity = activity;
    }

    public int getCount() {
        return mediaList.size();
    }

    public Object getItem(int i) {
        return mediaList.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.layout_contribution, null, false);
            view.setTag(new ContributionViewHolder(view));
        }

        Media m = (Media) getItem(i);
        ContributionViewHolder holder = (ContributionViewHolder) view.getTag();
        holder.imageView.setMedia(m, ((CommonsApplication)activity.getApplicationContext()).getImageLoader());
        holder.titleView.setText(m.getDisplayTitle());
        return view;
    }
}
