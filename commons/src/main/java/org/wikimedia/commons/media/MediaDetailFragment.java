package org.wikimedia.commons.media;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import org.wikimedia.commons.Media;
import org.wikimedia.commons.R;
import org.wikimedia.commons.Utils;

public class MediaDetailFragment extends SherlockFragment {

    private Media media;
    private DisplayImageOptions displayOptions;

    public static MediaDetailFragment forMedia(Media media) {
        MediaDetailFragment mf = new MediaDetailFragment();
        mf.media = media;
        return mf;
    }

    private ImageView image;
    private TextView title;
    private ProgressBar loadingProgress;
    private ImageView loadingFailed;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("media", media);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(media == null) {
            media = (Media)savedInstanceState.getParcelable("media");
        }
        View view = inflater.inflate(R.layout.fragment_media_detail, container, false);
        image = (ImageView) view.findViewById(R.id.mediaDetailImage);
        title = (TextView) view.findViewById(R.id.mediaDetailTitle);
        loadingProgress = (ProgressBar) view.findViewById(R.id.mediaDetailImageLoading);
        loadingFailed = (ImageView) view.findViewById(R.id.mediaDetailImageFailed);

        String actualUrl = TextUtils.isEmpty(media.getImageUrl()) ? media.getLocalUri().toString() : media.getThumbnailUrl(640);
        ImageLoader.getInstance().displayImage(actualUrl, image, displayOptions, new ImageLoadingListener() {
            public void onLoadingStarted() {
                loadingProgress.setVisibility(View.VISIBLE);
            }

            public void onLoadingFailed(FailReason failReason) {
                loadingProgress.setVisibility(View.GONE);
                loadingFailed.setVisibility(View.VISIBLE);

            }

            public void onLoadingComplete(Bitmap bitmap) {
                loadingProgress.setVisibility(View.GONE);
                loadingFailed.setVisibility(View.GONE);
                image.setVisibility(View.VISIBLE);
                if(bitmap.hasAlpha()) {
                    image.setBackgroundResource(android.R.color.white);
                }

            }

            public void onLoadingCancelled() {
                // wat?
                throw new RuntimeException("Image loading cancelled. But why?");

            }
        });
        title.setText(Utils.displayTitleFromTitle(media.getFilename()));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        displayOptions = new DisplayImageOptions.Builder().cacheInMemory()
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300))
                .cacheInMemory()
                .cacheOnDisc()
                .resetViewBeforeLoading().build();
    }
}
