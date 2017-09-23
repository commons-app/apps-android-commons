package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;

public class MultipleUploadListFragment extends Fragment {

    public interface OnMultipleUploadInitiatedHandler {
        void OnMultipleUploadInitiated();
    }

    private GridView photosGrid;
    private PhotoDisplayAdapter photosAdapter;
    private EditText baseTitle;
    private TitleTextWatcher textWatcher = new TitleTextWatcher();

    private Point photoSize;
    private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
    private OnMultipleUploadInitiatedHandler multipleUploadInitiatedHandler;

    private boolean imageOnlyMode;

    private static class UploadHolderView {
        private Uri imageUri;
        private SimpleDraweeView image;
        private TextView title;
        private RelativeLayout overlay;
    }

    private class PhotoDisplayAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return detailProvider.getTotalMediaCount();
        }

        @Override
        public Object getItem(int i) {
            return detailProvider.getMediaAtPosition(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            UploadHolderView holder;

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.layout_upload_item, viewGroup, false);
                holder = new UploadHolderView();
                holder.image = (SimpleDraweeView) view.findViewById(R.id.uploadImage);
                holder.title = (TextView) view.findViewById(R.id.uploadTitle);
                holder.overlay = (RelativeLayout) view.findViewById(R.id.uploadOverlay);

                holder.image.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, photoSize.y));
                holder.image.setHierarchy(GenericDraweeHierarchyBuilder
                        .newInstance(getResources())
                        .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                                R.drawable.ic_image_black_24dp, getContext().getTheme()))
                        .setFailureImage(VectorDrawableCompat.create(getResources(),
                                R.drawable.ic_error_outline_black_24dp, getContext().getTheme()))
                        .build());
                view.setTag(holder);
            } else {
                holder = (UploadHolderView) view.getTag();
            }

            Contribution up = (Contribution) this.getItem(i);

            if (holder.imageUri == null || !holder.imageUri.equals(up.getLocalUri())) {
                holder.image.setImageURI(up.getLocalUri().toString());
                holder.imageUri = up.getLocalUri();
            }

            if (!imageOnlyMode) {
                holder.overlay.setVisibility(View.VISIBLE);
                holder.title.setText(up.getFilename());
            } else {
                holder.overlay.setVisibility(View.GONE);
            }

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
        picWidth = Math.min((int) (192 * screenMetrics.density), Math.max((int) (120 * screenMetrics.density), picWidth / 48 * 48));
        int picHeight = Math.min(picWidth, (int) (192 * screenMetrics.density)); // Max Height is same as Contributions list

        return new Point(picWidth, picHeight);
    }

    public void notifyDatasetChanged() {
        if (photosAdapter != null) {
            photosAdapter.notifyDataSetChanged();
        }
    }

    public void setImageOnlyMode(boolean mode) {
        imageOnlyMode = mode;
        if (imageOnlyMode) {
            baseTitle.setVisibility(View.GONE);
        } else {
            baseTitle.setVisibility(View.VISIBLE);
        }
        photosAdapter.notifyDataSetChanged();
        photosGrid.setEnabled(!mode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multiple_uploads_list, container, false);
        photosGrid = (GridView) view.findViewById(R.id.multipleShareBackground);
        baseTitle = (EditText) view.findViewById(R.id.multipleBaseTitle);

        photosAdapter = new PhotoDisplayAdapter();
        photosGrid.setAdapter(photosAdapter);
        photosGrid.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        photoSize = calculatePicDimension(detailProvider.getTotalMediaCount());
        photosGrid.setColumnWidth(photoSize.x);

        baseTitle.addTextChangedListener(textWatcher);
        return view;
    }

    @Override
    public void onDestroyView() {
        baseTitle.removeTextChangedListener(textWatcher);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.fragment_multiple_upload_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_upload_multiple:
                multipleUploadInitiatedHandler.OnMultipleUploadInitiated();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        detailProvider = (MediaDetailPagerFragment.MediaDetailProvider) getActivity();
        multipleUploadInitiatedHandler = (OnMultipleUploadInitiatedHandler) getActivity();

        setHasOptionsMenu(true);
    }

    private class TitleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i1, int i2, int i3) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i1, int i2, int i3) {
            for (int i = 0; i < detailProvider.getTotalMediaCount(); i++) {
                Contribution up = (Contribution) detailProvider.getMediaAtPosition(i);
                Boolean isDirty = (Boolean) up.getTag("isDirty");
                if (isDirty == null || !isDirty) {
                    if (!TextUtils.isEmpty(charSequence)) {
                        up.setFilename(charSequence.toString() + " - " + ((Integer) up.getTag("sequence") + 1));
                    } else {
                        up.setFilename("");
                    }
                }
            }
            detailProvider.notifyDatasetChanged();
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    }
}
