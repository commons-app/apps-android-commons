package fr.free.nrw.commons.leaderboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.leaderboard.model.GetLeaderboardUser;

public class leaderboardAdapter extends RecyclerView.Adapter<leaderboardAdapter.leaderboardViewHolder> {

    private static final int ITEM = 0;
    private static final int LOADING = 1;
    private List<GetLeaderboardUser> getLeaderboardUsers;
    private Context context;
    private boolean isLoadingAdded = false;

    public leaderboardAdapter(Context context){

        this.context = context;
        getLeaderboardUsers = new ArrayList<>();
    }

    public List<GetLeaderboardUser> getGetLeaderboardUsers() {
        return getLeaderboardUsers;
    }

    public void setGetLeaderboardUsers(List<GetLeaderboardUser> getLeaderboardUsers) {
        this.getLeaderboardUsers = getLeaderboardUsers;
    }

    @NonNull
    @Override
    public leaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        leaderboardAdapter.leaderboardViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case ITEM:
                View view = inflater.inflate(R.layout.layout_leaderboard_item,parent,false);
                viewHolder = new leaderboardViewHolder(view);
                break;
            case LOADING:
                View view2 = inflater.inflate(R.layout.item_progess,parent,false);
                viewHolder = new leaderboardViewHolder(view2);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull leaderboardViewHolder holder, int position) {

        GetLeaderboardUser getLeaderboardUser = getLeaderboardUsers.get(position);

        switch (getItemViewType(position)) {
            case ITEM:
                leaderboardViewHolder leaderboardVH = (leaderboardViewHolder) holder;
                leaderboardVH.rank.setText(getLeaderboardUser.getRank());
                leaderboardVH.user_name.setText(getLeaderboardUser.getUsername());
                leaderboardVH.score.setText(getLeaderboardUser.getScore());
                break;
            case LOADING:
//                Do nothing
                break;
        }

    }

    @Override
    public int getItemCount() {
        return getLeaderboardUsers == null ? 0 : getLeaderboardUsers.size();
    }

    public void add(GetLeaderboardUser mc) {
        getLeaderboardUsers.add(mc);
        notifyItemInserted(getLeaderboardUsers.size() - 1);
    }

    public void addAll(List<GetLeaderboardUser> mcList) {
        for (GetLeaderboardUser mc : mcList) {
            add(mc);
        }
    }

    public void remove(GetLeaderboardUser city) {
        int position = getLeaderboardUsers.indexOf(city);
        if (position > -1) {
            getLeaderboardUsers.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clear() {
        isLoadingAdded = false;
        while (getItemCount() > 0) {
            remove(getItem(0));
        }
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }


    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new GetLeaderboardUser("","","",""));
    }

    public void removeLoadingFooter() {
        isLoadingAdded = false;

        int position = getLeaderboardUsers.size() - 1;
        GetLeaderboardUser item = getItem(position);

        if (item != null) {
            getLeaderboardUsers.remove(position);
            notifyItemRemoved(position);
        }
    }

    public GetLeaderboardUser getItem(int position) {
        return getLeaderboardUsers.get(position);
    }


    @Override
    public int getItemViewType(int position) {
        return (position == getLeaderboardUsers.size() - 1 && isLoadingAdded) ? LOADING : ITEM;
    }

    public class leaderboardViewHolder extends RecyclerView.ViewHolder{
        TextView rank;
        ImageView userImageView;
        TextView user_name;
        TextView score;
        public leaderboardViewHolder(View itemView){
            super(itemView);
            rank = itemView.findViewById(R.id.rank);
            userImageView = itemView.findViewById(R.id.userImageView);
            user_name = itemView.findViewById(R.id.user_name);
            score = itemView.findViewById(R.id.score);
        }
    }
}
