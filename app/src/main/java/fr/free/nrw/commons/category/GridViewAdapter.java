package fr.free.nrw.commons.category;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.R;

/**
 * This is created to only display UI implementation. Needs to be changed in real implementation
 */

public class GridViewAdapter extends ArrayAdapter {
    private Context context;
    private List<Media> data;

    public GridViewAdapter(Context context, int layoutResourceId, List<Media> data) {
        super(context, layoutResourceId, data);
        this.context = context;
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
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.layout_category_images, null);
        }

        Media item = data.get(position);
        MediaWikiImageView imageView = convertView.findViewById(R.id.categoryImageView);
        TextView fileName = convertView.findViewById(R.id.categoryImageTitle);
        TextView author = convertView.findViewById(R.id.categoryImageAuthor);
        fileName.setText(item.getDisplayTitle());
        setAuthorView(item, author);
        imageView.setMedia(item);
        return convertView;
    }

    /**
     * Shows author information if its present
     * @param item
     * @param author
     */
    private void setAuthorView(Media item, TextView author) {
        if (item.getCreator() != null && !item.getCreator().equals("")) {
            String uploadedByTemplate = context.getString(R.string.image_uploaded_by);

            String uploadedBy = String.format(Locale.getDefault(), uploadedByTemplate, item.getCreator());
            author.setText(uploadedBy);
        } else {
            author.setVisibility(View.GONE);
        }
    }
}
