package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;

class ContributionsListAdapter extends CursorAdapter {

    private final ContributionDao contributionDao;

    public ContributionsListAdapter(Context context, Cursor c, int flags, ContributionDao contributionDao) {
        super(context, c, flags);
        this.contributionDao = contributionDao;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View parent = LayoutInflater.from(context)
                .inflate(R.layout.layout_contribution, viewGroup, false);
        parent.setTag(new ContributionViewHolder(parent));
        return parent;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ContributionViewHolder views = (ContributionViewHolder)view.getTag();
        final Contribution contribution = contributionDao.fromCursor(cursor);

        views.imageView.setMedia(contribution);
        views.titleView.setText(contribution.getDisplayTitle());
        views.deleteView.setOnClickListener(v -> delete());
        views.seqNumView.setText(String.valueOf(cursor.getPosition() + 1));
        views.seqNumView.setVisibility(View.VISIBLE);

        switch (contribution.getState()) {
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
                if (transferred == 0 || transferred >= total) {
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

    private void delete() {
        Uri uri = Uri.parse(BuildConfig.DELETE_CONTRIBUTION_URL);
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(mContext, R.color.primaryColor));
        builder.setSecondaryToolbarColor(ContextCompat.getColor(mContext, R.color.primaryDarkColor));
        builder.setExitAnimations(mContext, android.R.anim.slide_in_left,
                android.R.anim.slide_out_right);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        customTabsIntent.launchUrl(mContext, uri);
    }
}
