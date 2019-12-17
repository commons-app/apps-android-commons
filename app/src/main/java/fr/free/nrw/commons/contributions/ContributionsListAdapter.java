package fr.free.nrw.commons.contributions;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.model.DisplayableContribution;

public class ContributionsListAdapter extends RecyclerView.Adapter<ContributionViewHolder> {

    private Callback callback;

    public ContributionsListAdapter(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ContributionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContributionViewHolder viewHolder = new ContributionViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_contribution, parent, false), callback);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ContributionViewHolder holder, int position) {
        final Contribution contribution = callback.getContributionForPosition(position);
        DisplayableContribution displayableContribution = new DisplayableContribution(contribution,
                position);
        holder.init(position, displayableContribution);
    }

    @Override
    public int getItemCount() {
        return callback.getNumberOfContributions();
    }

    public interface Callback {

        void retryUpload(Contribution contribution);

        void deleteUpload(Contribution contribution);

        void openMediaDetail(int contribution);

        int getNumberOfContributions();

        Contribution getContributionForPosition(int position);

        int findItemPositionWithId(String lastVisibleItemID);
    }
}
