package org.wikimedia.commons;

import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.wikimedia.commons.contributions.Contribution;

import java.util.ArrayList;
import java.util.List;

public class MultipleUploadListFragment extends SherlockFragment {

    private GridView photosGrid;
    private PhotoDisplayAdapter photosAdapter;
    private EditText baseTitle;

    private Point photoSize;

    private ArrayList<Contribution> photosList;

    private DisplayImageOptions uploadDisplayOptions;

    private static class UploadHolderView {
        Uri imageUri;

        ImageView image;
        TextView title;
    }

    private class PhotoDisplayAdapter extends BaseAdapter {

        private ArrayList<Contribution> urisList;

        private PhotoDisplayAdapter(ArrayList<Contribution> urisList) {
            this.urisList = urisList;
        }

        public int getCount() {
            return urisList.size();
        }

        public Object getItem(int i) {
            return urisList.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            UploadHolderView holder;

            if(view == null) {
                view = getLayoutInflater(null).inflate(R.layout.layout_upload_item, null);
                holder = new UploadHolderView();
                holder.image = (ImageView) view.findViewById(R.id.uploadImage);
                holder.title = (TextView) view.findViewById(R.id.uploadTitle);

                holder.image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, photoSize.y));

                view.setTag(holder);
            } else {
                holder = (UploadHolderView)view.getTag();
            }


            Contribution up = (Contribution)this.getItem(i);

            if(holder.imageUri == null || !holder.imageUri.equals(up.getLocalUri())) {
                ImageLoader.getInstance().displayImage(up.getLocalUri().toString(), holder.image, uploadDisplayOptions);
                holder.imageUri = up.getLocalUri();
            }

            holder.title.setText(up.getFilename());

            return view;

        }
    }

    // FIXME: Wrong result type
    private Point calculatePicDimension(int count) {
        DisplayMetrics screenMetrics = getResources().getDisplayMetrics();
        int screenWidth = screenMetrics.widthPixels;
        int screenHeight = screenMetrics.heightPixels;

        int picWidth = Math.min((int) Math.sqrt(screenWidth * screenHeight / count), screenWidth);
        picWidth = Math.min((int)(192 * screenMetrics.density), Math.max((int) (120  * screenMetrics.density), picWidth / 48 * 48));
        int picHeight = Math.min(picWidth, (int)(192 * screenMetrics.density)); // Max Height is same as Contributions list
        return new Point(picWidth, picHeight);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multiple_uploads_list, container);
        photosGrid = (GridView)view.findViewById(R.id.multipleShareBackground);
        baseTitle = (EditText)view.findViewById(R.id.multipleBaseTitle);

        if(savedInstanceState != null) {
            setData(savedInstanceState.<Contribution>getParcelableArrayList("photosData"));
        }

        baseTitle.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                for(Contribution up: photosList) {
                    Boolean isDirty = (Boolean)up.getTag("isDirty");
                    if(isDirty == null || !isDirty) {
                        if(!TextUtils.isEmpty(charSequence)) {
                            up.setFilename(charSequence.toString() + " - " + i);
                        } else {
                            up.setFilename("");
                        }
                    }
                }
                photosAdapter.notifyDataSetChanged();

            }

            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadDisplayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .cacheInMemory()
                .resetViewBeforeLoading().build();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("photosData", photosList);
    }

    public void setData(ArrayList<Contribution> photosList) {
        if(this.photosList == null) {
            photosAdapter = new PhotoDisplayAdapter(photosList);
            photosGrid.setAdapter(photosAdapter);
        }
        this.photosList = photosList;
        photoSize = calculatePicDimension(photosList.size());
        photosAdapter.notifyDataSetChanged();
        photosGrid.setColumnWidth(photoSize.x);
    }
}
