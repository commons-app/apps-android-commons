package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.model.DisplayableContribution;
import fr.free.nrw.commons.upload.UploadService;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;

class ContributionsListAdapter extends CursorAdapter {

    private final ContributionDao contributionDao;
    private UploadService uploadService;
    private Context context;

    public ContributionsListAdapter(Context context,
                                    Cursor c,
                                    int flags,
                                    ContributionDao contributionDao) {
        super(context, c, flags);
        this.context = context;
        this.contributionDao = contributionDao;
    }

    public void setUploadService(UploadService uploadService) {
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

        DisplayableContribution displayableContribution = new DisplayableContribution(contribution,
                cursor.getPosition(),
                new DisplayableContribution.ContributionActions() {
                    @Override
                    public void retryUpload() {
                        ContributionsListAdapter.this.retryUpload(contribution);
                    }

                    @Override
                    public void deleteUpload() {
                        ContributionsListAdapter.this.deleteUpload(contribution);
                    }
                });
        views.bindModel(context, displayableContribution);
    }

    /**
     * Retry upload when it is failed
     * @param contribution contribution to be retried
     */
    private void retryUpload(Contribution contribution) {
        if (NetworkUtils.isInternetConnectionEstablished(context)) {
            if (contribution.getState() == STATE_FAILED
                    && uploadService!= null) {
                uploadService.queue(UploadService.ACTION_UPLOAD_FILE, contribution);
                Timber.d("Restarting for %s", contribution.toString());
            } else {
                Timber.d("Skipping re-upload for non-failed %s", contribution.toString());
            }
        } else {
            ViewUtil.showLongToast(context, R.string.this_function_needs_network_connection);
        }

    }

    /**
     * Delete a failed upload attempt
     * @param contribution contribution to be deleted
     */
    private void deleteUpload(Contribution contribution) {
        if (NetworkUtils.isInternetConnectionEstablished(context)) {
            if (contribution.getState() == STATE_FAILED) {
                Timber.d("Deleting failed contrib %s", contribution.toString());
                contributionDao.delete(contribution);
            } else {
                Timber.d("Skipping deletion for non-failed contrib %s", contribution.toString());
            }
        } else {
            ViewUtil.showLongToast(context, R.string.this_function_needs_network_connection);
        }

    }
}
