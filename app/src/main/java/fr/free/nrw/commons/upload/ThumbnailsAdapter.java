package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;

/**
 * The adapter class for image thumbnails to be shown while uploading.
 */
class ThumbnailsAdapter extends RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder> {

    List<UploadableFile> uploadableFiles;
    private Callback callback;

    public ThumbnailsAdapter(Callback callback) {
        this.uploadableFiles = new ArrayList<>();
        this.callback = callback;
    }

    /**
     * Sets the data, the media files
     * @param uploadableFiles
     */
    public void setUploadableFiles(
            List<UploadableFile> uploadableFiles) {
        this.uploadableFiles=uploadableFiles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_upload_thumbnail, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.bind(position);
    }

    @Override
    public int getItemCount() {
        return uploadableFiles.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.rl_container)
        RelativeLayout rlContainer;
        @BindView(R.id.iv_thumbnail)
        SimpleDraweeView background;
        @BindView(R.id.iv_error)
        ImageView ivError;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        /**
         * Binds a row item to the ViewHolder
         * @param position
         */
        public void bind(int position) {
            UploadableFile uploadableFile = uploadableFiles.get(position);
            Uri uri = uploadableFile.getMediaUri();
            background.setImageURI(Uri.fromFile(new File(String.valueOf(uri))));

            if (position == callback.getCurrentSelectedFilePosition()) {
                rlContainer.setEnabled(true);
                rlContainer.setClickable(true);
                rlContainer.setAlpha(1.0f);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    rlContainer.setElevation(10);
                }
            } else {
                rlContainer.setEnabled(false);
                rlContainer.setClickable(false);
                rlContainer.setAlpha(0.5f);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    rlContainer.setElevation(0);
                }
            }
        }
    }

    /**
     * Callback used to get the current selected file position
     */
    interface Callback {

        int getCurrentSelectedFilePosition();
    }
}
