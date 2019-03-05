package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.CropImage;

import java.util.ArrayList;

import fr.free.nrw.commons.filepicker.UploadableFile;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<UploadableFile> files;

    // Constructor
    public ImageAdapter(Context c, ArrayList<UploadableFile> files) {
        this.files = files;
        mContext = c;
    }

    public int getCount() {
        return files.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(250, 250));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageURI(files.get(position).getMediaUri());

        return imageView;
    }

    public void handleClickListener(int position){
        CropImage.activity(files.get(position).getMediaUri())
                .start((Activity) mContext);
    }

    // Keep all Images in array
}
