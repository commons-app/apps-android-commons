package fr.free.nrw.commons.upload;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.databinding.ItemUploadThumbnailBinding;
import fr.free.nrw.commons.filepicker.UploadableFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The adapter class for image thumbnails to be shown while uploading.
 */
class ThumbnailsAdapter extends RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder> {
    public  static  Context context;
    List<UploadableFile> uploadableFiles;
    private Callback callback;

    private OnThumbnailDeletedListener listener;

    private ItemUploadThumbnailBinding binding;

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

    public void setOnThumbnailDeletedListener(OnThumbnailDeletedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        binding = ItemUploadThumbnailBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false);
        return new ViewHolder(binding.getRoot());
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


        RelativeLayout rlContainer;
        SimpleDraweeView background;
        ImageView ivError;

        ImageView ivCross;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rlContainer = binding.rlContainer;
            background = binding.ivThumbnail;
            ivError = binding.ivError;
            ivCross = binding.icCross;
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
                GradientDrawable border = new GradientDrawable();
                border.setShape(GradientDrawable.RECTANGLE);
                border.setStroke(8, context.getResources().getColor(R.color.primaryColor));
                rlContainer.setEnabled(true);
                rlContainer.setClickable(true);
                rlContainer.setAlpha(1.0f);
                rlContainer.setBackground(border);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    rlContainer.setElevation(10);
                }
            } else {
                rlContainer.setEnabled(false);
                rlContainer.setClickable(false);
                rlContainer.setAlpha(0.7f);
                rlContainer.setBackground(null);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    rlContainer.setElevation(0);
                }
            }

            ivCross.setOnClickListener(v -> {
                if(listener != null) {
                    listener.onThumbnailDeleted(position);
                }
            });
        }
    }

    /**
     * Callback used to get the current selected file position
     */
    interface Callback {

        int getCurrentSelectedFilePosition();
    }

    /**
     * Interface to listen to thumbnail delete events
     */

    public interface OnThumbnailDeletedListener {
        void onThumbnailDeleted(int position);
    }

}
