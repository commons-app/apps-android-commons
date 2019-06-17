package fr.free.nrw.commons.leaderboard;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;

/**
 * This is created to only display UI implementation. Needs to be changed in real implementation
 */

public class leaderboardAdapter extends ArrayAdapter {
    private List<Media> data;

    public leaderboardAdapter(Context context, int layoutResourceId, List<Media> data) {
        super(context, layoutResourceId, data);
        this.data = data;
    }

    /**
     * Adds more item to the list
     * Its triggered on scrolling down in the list
     * @param images
     */
    public void addItems(List<Media> images) {
        if (data == null) {
            data = new ArrayList<>();
        }
        data.addAll(images);
        notifyDataSetChanged();
    }

    /**
     * Check the first item in the new list with old list and returns true if they are same
     * Its triggered on successful response of the fetch images API.
     * @param images
     */
    public boolean containsAll(List<Media> images){
        if (images == null || images.isEmpty()) {
            return false;
        }
        if (data == null) {
            data = new ArrayList<>();
            return false;
        }
        if (data.size() <= 0) {
            return false;
        }
        String fileName = data.get(0).getFilename();
        String imageName = images.get(0).getFilename();
        return imageName.equals(fileName);
    }

    @Override
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    /**
     * Sets up the UI for the category image item
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.layout_leaderboard_item, null);
        }
        Media item = data.get(position);
        SimpleDraweeView imageView = convertView.findViewById(R.id.userImageView);
        TextView fileName = convertView.findViewById(R.id.user_name);
        TextView author = convertView.findViewById(R.id.score);
        fileName.setText(item.getDisplayTitle());
        author.setText("score");
        imageView.setImageURI(item.getThumbUrl());
        return convertView;
    }
}
