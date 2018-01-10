package fr.free.nrw.commons.featured;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;

/**
 * This is created to only display UI implementation. Needs to be changed in real implementation
 */

public class MockGridViewAdapter extends ArrayAdapter {
    private Context context;
    private int layoutResourceId;
    private ArrayList<FeaturedImage> data = new ArrayList();

    public MockGridViewAdapter(Context context, int layoutResourceId, ArrayList<FeaturedImage> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.layout_featured_images, null);
        }

        FeaturedImage item = data.get(position);
        MediaWikiImageView imageView = convertView.findViewById(R.id.featuredImageView);
        TextView fileName = convertView.findViewById(R.id.featuredImageTitle);
        TextView author = convertView.findViewById(R.id.featuredImageAuthor);
        fileName.setText("Test file name");
        author.setText("Uploaded by: Test user name");
        imageView.setMedia(item.getImage());
        return convertView;
    }

}