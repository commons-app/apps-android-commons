package org.wikimedia.commons;

import android.content.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.*;

import org.wikimedia.commons.contributions.*;
import org.wikimedia.commons.media.*;


public class MultipleUploadListFragment extends SherlockFragment {

    private GridView photosGrid;
    private PhotoDisplayAdapter photosAdapter;
    private EditText baseTitle;

    private Point photoSize;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;

    private DisplayImageOptions uploadDisplayOptions;

    private static class UploadHolderView {
        Uri imageUri;

        ImageView image;
        TextView title;
    }

    private class PhotoDisplayAdapter extends BaseAdapter {

        public int getCount() {
            return detailProvider.getTotalMediaCount();
        }

        public Object getItem(int i) {
            return detailProvider.getMediaAtPosition(i);
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

    @Override
    public void onStop() {
        super.onStop();

        // FIXME: Stops the keyboard from being shown 'stale' while moving out of this fragment into the next
        View target = getView().findFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
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

    public void notifyDatasetChanged() {
        if(photosAdapter != null) {
            photosAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multiple_uploads_list, null);
        photosGrid = (GridView)view.findViewById(R.id.multipleShareBackground);
        baseTitle = (EditText)view.findViewById(R.id.multipleBaseTitle);


        photosAdapter = new PhotoDisplayAdapter();
        photosGrid.setAdapter(photosAdapter);
        photosGrid.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        photoSize = calculatePicDimension(detailProvider.getTotalMediaCount());
        photosGrid.setColumnWidth(photoSize.x);

        baseTitle.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i1, int i2, int i3) {

            }

            public void onTextChanged(CharSequence charSequence, int i1, int i2, int i3) {
                for(int i = 0; i < detailProvider.getTotalMediaCount(); i++) {
                    Contribution up = (Contribution) detailProvider.getMediaAtPosition(i);
                    Boolean isDirty = (Boolean)up.getTag("isDirty");
                    if(isDirty == null || !isDirty) {
                        if(!TextUtils.isEmpty(charSequence)) {
                            up.setFilename(charSequence.toString() + " - " + ((Integer)up.getTag("sequence") + 1));
                        } else {
                            up.setFilename("");
                        }
                    }
                }
                detailProvider.notifyDatasetChanged();

            }

            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uploadDisplayOptions = Utils.getGenericDisplayOptions().build();
        detailProvider = (MediaDetailPagerFragment.MediaDetailProvider)getActivity();
    }


}
