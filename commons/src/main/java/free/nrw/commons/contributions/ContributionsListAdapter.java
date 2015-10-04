package free.nrw.commons.contributions;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import free.nrw.commons.CommonsApplication;
import free.nrw.commons.MediaWikiImageView;
import free.nrw.commons.Utils;
import free.nrw.commons.R;

class ContributionsListAdapter extends CursorAdapter {

    private DisplayImageOptions contributionDisplayOptions = Utils.getGenericDisplayOptions().build();;
    private Activity activity;

    public ContributionsListAdapter(Activity activity, Cursor c, int flags) {
        super(activity, c, flags);
        this.activity = activity;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View parent = activity.getLayoutInflater().inflate(R.layout.layout_contribution, viewGroup, false);
        parent.setTag(new ContributionViewHolder(parent));
        return parent;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ContributionViewHolder views = (ContributionViewHolder)view.getTag();
        final Contribution contribution = Contribution.fromCursor(cursor);

        String actualUrl = (contribution.getLocalUri() != null && !TextUtils.isEmpty(contribution.getLocalUri().toString())) ? contribution.getLocalUri().toString() : contribution.getThumbnailUrl(640);

        if(views.url == null || !views.url.equals(actualUrl)) {
            if(actualUrl.startsWith("http")) {
                MediaWikiImageView mwImageView = (MediaWikiImageView)views.imageView;
                mwImageView.setMedia(contribution, ((CommonsApplication) activity.getApplicationContext()).getImageLoader());
                // FIXME: For transparent images
            } else {
                com.nostra13.universalimageloader.core.ImageLoader.getInstance().displayImage(actualUrl, views.imageView, contributionDisplayOptions, new SimpleImageLoadingListener() {

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if(loadedImage.hasAlpha()) {
                            views.imageView.setBackgroundResource(android.R.color.white);
                        }
                        views.seqNumView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        super.onLoadingFailed(imageUri, view, failReason);
                        MediaWikiImageView mwImageView = (MediaWikiImageView)views.imageView;
                        mwImageView.setMedia(contribution, ((CommonsApplication) activity.getApplicationContext()).getImageLoader());
                    }
                });
            }
            views.url = actualUrl;
        }

        BitmapDrawable actualImageDrawable = (BitmapDrawable)views.imageView.getDrawable();
        if(actualImageDrawable != null && actualImageDrawable.getBitmap() != null && actualImageDrawable.getBitmap().hasAlpha()) {
            views.imageView.setBackgroundResource(android.R.color.white);
        } else {
            views.imageView.setBackgroundDrawable(null);
        }

        views.titleView.setText(contribution.getDisplayTitle());

        views.seqNumView.setText(String.valueOf(cursor.getPosition() + 1));
        views.seqNumView.setVisibility(View.VISIBLE);

        switch(contribution.getState()) {
            case Contribution.STATE_COMPLETED:
                views.stateView.setVisibility(View.GONE);
                views.progressView.setVisibility(View.GONE);
                views.stateView.setText("");
                break;
            case Contribution.STATE_QUEUED:
                views.stateView.setVisibility(View.VISIBLE);
                views.progressView.setVisibility(View.GONE);
                views.stateView.setText(R.string.contribution_state_queued);
                break;
            case Contribution.STATE_IN_PROGRESS:
                views.stateView.setVisibility(View.GONE);
                views.progressView.setVisibility(View.VISIBLE);
                long total = contribution.getDataLength();
                long transferred = contribution.getTransferred();
                if(transferred == 0 || transferred >= total) {
                    views.progressView.setIndeterminate(true);
                } else {
                    views.progressView.setProgress((int)(((double)transferred / (double)total) * 100));
                }
                break;
            case Contribution.STATE_FAILED:
                views.stateView.setVisibility(View.VISIBLE);
                views.stateView.setText(R.string.contribution_state_failed);
                views.progressView.setVisibility(View.GONE);
                break;
        }

    }
}
