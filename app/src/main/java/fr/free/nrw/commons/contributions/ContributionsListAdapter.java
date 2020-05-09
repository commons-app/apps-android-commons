package fr.free.nrw.commons.contributions;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.MediaClient;

/**
 * Represents The View Adapter for the List of Contributions
 */
public class ContributionsListAdapter extends
    PagedListAdapter<Contribution, ContributionViewHolder> {

    private Callback callback;
    private final MediaClient mediaClient;

    ContributionsListAdapter(final Callback callback,
        final MediaClient mediaClient) {
        super(DIFF_CALLBACK);
        this.callback = callback;
        this.mediaClient = mediaClient;
        setHasStableIds(true);
    }

    private static DiffUtil.ItemCallback<Contribution> DIFF_CALLBACK =
        new DiffUtil.ItemCallback<Contribution>() {
            @Override
            public boolean areItemsTheSame(final Contribution oldContribution, final Contribution newContribution) {
                return oldContribution.getPageId().equals(newContribution.getPageId());
            }

            @Override
            public boolean areContentsTheSame(final Contribution oldContribution, final Contribution newContribution) {
                return oldContribution.equals(newContribution);
            }
        };

    @Override
    public void onBindViewHolder(@NonNull final ContributionViewHolder holder, final int position) {
        final Contribution contribution = getItem(position);
        if (contribution != null) {
            holder.init(position, contribution);
        }
    }

    Contribution getContributionForPosition(final int position) {
        return getItem(position);
    }

    /**
     * Creates the new View Holder which will be used to display items(contributions) using the
     * onBindViewHolder(viewHolder,position)
     */
    @NonNull
    @Override
    public ContributionViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
        final int viewType) {
        final ContributionViewHolder viewHolder = new ContributionViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_contribution, parent, false), callback, mediaClient);
        return viewHolder;
    }

    public interface Callback {

        void retryUpload(Contribution contribution);

        void deleteUpload(Contribution contribution);

        void openMediaDetail(int contribution);
    }
}
