package fr.free.nrw.commons.contributions;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.media.MediaClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents The View Adapter for the List of Contributions  
 */
public class ContributionsListAdapter extends RecyclerView.Adapter<ContributionViewHolder> {

    private Callback callback;
    private final MediaClient mediaClient;
    private List<Contribution> contributions;

    ContributionsListAdapter(final Callback callback,
        final MediaClient mediaClient) {
        this.callback = callback;
        this.mediaClient = mediaClient;
        contributions = new ArrayList<>();
        setHasStableIds(true);
    }

    /**
     * Creates the new View Holder which will be used to display items(contributions)
     * using the onBindViewHolder(viewHolder,position) 
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

    @Override
    public void onBindViewHolder(@NonNull final ContributionViewHolder holder, final int position) {
        final Contribution contribution = contributions.get(position);
        holder.init(position, contribution);
    }

    @Override
    public int getItemCount() {
        return contributions.size();
    }

    public void setContributions(@NonNull final List<Contribution> contributionList) {
        contributions = contributionList;
        notifyDataSetChanged();
    }

    Contribution getContributionForPosition(final int position) {
        return contributions.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return Integer.parseInt(contributions.get(position).getPageId());
    }

    public interface Callback {

        void retryUpload(Contribution contribution);

        void deleteUpload(Contribution contribution);

        void openMediaDetail(int contribution);
    }
}
