package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;
import timber.log.Timber;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.rl_container)
        RelativeLayout rlContainer;
        @BindView(R.id.iv_thumbnail)
        SimpleDraweeView background;
        @BindView(R.id.iv_error)
        ImageView ivError;
        @BindView(R.id.iv_close)
        ImageButton ivClose;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            ivClose.setOnClickListener(this);
        }

        /**
         * Binds a row item to the ViewHolder
         * @param position
         */
        public void bind(int position) {
            UploadableFile uploadableFile = uploadableFiles.get(position);
            Uri uri = uploadableFile.getMediaUri();
            background.setImageURI(Uri.fromFile(new File(String.valueOf(uri))));

//            Timber.e("Current position : "+position+" Current selected file position : "+callback.getCurrentSelectedFilePosition());

            if (position == callback.getCurrentSelectedFilePosition()) {
                rlContainer.setEnabled(true);
                rlContainer.setClickable(true);
                background.setAlpha(1.0f);
                ivError.setAlpha(1.0f);
                ivClose.setAlpha(1.0f);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    background.setElevation(10);
                }
            } else {
                rlContainer.setEnabled(false);
                rlContainer.setClickable(false);
                background.setAlpha(0.5f); //once an image is deleted the newly bound image will not have faded effect if rlContainer is used
                ivError.setAlpha(0.5f);
                ivClose.setAlpha(0.5f);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    background.setElevation(0);
                }
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.iv_close:
                    removeImageAt(getAdapterPosition());
                    break;
            }
        }
    }

    private void removeImageAt(int position) {
        notifyItemRemoved(position);
    }

    /**
     * Callback used to get the current selected file position
     */
    interface Callback {
        int getCurrentSelectedFilePosition();
    }
}
