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

    private final Callback callback;
    private final MediaClient mediaClient;

    ContributionsListAdapter(final Callback callback,
        final MediaClient mediaClient) {
        super(DIFF_CALLBACK);
        this.callback = callback;
        this.mediaClient = mediaClient;
    }

    /**
     * Uses DiffUtil to calculate the changes in the list
     * It has methods that check ID and the content of the items to determine if its a new item
     */
    private static final DiffUtil.ItemCallback<Contribution> DIFF_CALLBACK =
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

    /**
     * Initializes the view holder with contribution data
     */
    @Override
    public void onBindViewHolder(@NonNull ContributionViewHolder holder, int position) {
        holder.init(position, getItem(position));
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
                .inflate(R.layout.layout_contribution, parent, false),
            callback, mediaClient);
        return viewHolder;
    }

    public interface Callback {

        void openMediaDetail(int contribution, boolean isWikipediaPageExists);

        void addImageToWikipedia(Contribution contribution);
    }
}
