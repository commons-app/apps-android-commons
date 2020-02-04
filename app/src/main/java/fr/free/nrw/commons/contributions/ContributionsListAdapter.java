package fr.free.nrw.commons.contributions;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.model.DisplayableContribution;

/**
 * Represents The View Adapter for the List of Contributions  
 */
public class ContributionsListAdapter extends RecyclerView.Adapter<ContributionViewHolder> {

    private Callback callback;
    private List<Contribution> contributions;

    public ContributionsListAdapter(Callback callback) {
        this.callback = callback;
        contributions=new ArrayList<>();
    }

    /**
     * Creates the new View Holder which will be used to display items(contributions)
     * using the onBindViewHolder(viewHolder,position) 
     */
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
        final Contribution contribution = contributions.get(position);
        DisplayableContribution displayableContribution = new DisplayableContribution(contribution,
                position);
        holder.init(position, displayableContribution);
    }

    @Override
    public int getItemCount() {
        return contributions==null?0:contributions.size();
    }

    public void setContributions(List<Contribution> contributionList) {
        if(null!=contributionList) {
            this.contributions.clear();
            this.contributions.addAll(contributionList);
            notifyDataSetChanged();
        }
    }

    public interface Callback {

        void retryUpload(Contribution contribution);

        void deleteUpload(Contribution contribution);

        void openMediaDetail(int contribution);

        Contribution getContributionForPosition(int position);
    }
}
