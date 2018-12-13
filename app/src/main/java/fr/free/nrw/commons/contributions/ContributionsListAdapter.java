package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.upload.UploadService;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;

class ContributionsListAdapter extends CursorAdapter {

    private final ContributionDao contributionDao;
    private Cursor allContributions;
    private UploadService uploadService;

    public ContributionsListAdapter(Context context, Cursor c, int flags, ContributionDao contributionDao) {
        super(context, c, flags);
        this.contributionDao = contributionDao;
    }

    public void setUploadService(Cursor allContributions, UploadService uploadService) {
        this.allContributions = allContributions;
        this.uploadService = uploadService;
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

        views.seqNumView.setText(String.valueOf(cursor.getPosition() + 1));
        views.seqNumView.setVisibility(View.VISIBLE);
        views.position = cursor.getPosition();


        switch (contribution.getState()) {
            case Contribution.STATE_COMPLETED:
                views.stateView.setVisibility(View.GONE);
                views.progressView.setVisibility(View.GONE);
                views.retryButton.setVisibility(View.GONE);
                views.cancelButton.setVisibility(View.GONE);
                views.stateView.setText("");
                break;
            case Contribution.STATE_QUEUED:
                views.stateView.setVisibility(View.VISIBLE);
                views.progressView.setVisibility(View.GONE);
                views.stateView.setText(R.string.contribution_state_queued);
                views.retryButton.setVisibility(View.GONE);
                views.cancelButton.setVisibility(View.GONE);
                break;
            case Contribution.STATE_IN_PROGRESS:
                views.stateView.setVisibility(View.GONE);
                views.progressView.setVisibility(View.VISIBLE);
                views.retryButton.setVisibility(View.GONE);
                views.cancelButton.setVisibility(View.GONE);
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
                views.retryButton.setVisibility(View.VISIBLE);
                views.cancelButton.setVisibility(View.VISIBLE);

                views.retryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        retryUpload(cursor);
                    }
                });

                views.cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteUpload(cursor);
                    }
                });


                break;
        }
    }

    /**
     * Retry upload when it is failed
     * @param cursor cursor will be retried
     */
    public void retryUpload(Cursor cursor) {
        // TODO: first check for internet connection, if not display a message and do nothing.
        Contribution c = contributionDao.fromCursor(cursor);
        if (c.getState() == STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Timber.d("Restarting for %s", c.toString());
        } else {
            Timber.d("Skipping re-upload for non-failed %s", c.toString());
        }
    }

    /**
     * Delete a failed upload attempt
     * @param cursor cursor which will be deleted
     */
    public void deleteUpload(Cursor cursor) {
        // TODO: check internet connection, warn user and do nothing is a problem occurred

        Contribution c = contributionDao.fromCursor(cursor);
        if (c.getState() == STATE_FAILED) {
            Timber.d("Deleting failed contrib %s", c.toString());
            contributionDao.delete(c);
        } else {
            Timber.d("Skipping deletion for non-failed contrib %s", c.toString());
        }
    }
}
