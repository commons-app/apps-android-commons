package fr.free.nrw.commons.profile.leaderboard;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.drawee.view.SimpleDraweeView;
import fr.free.nrw.commons.R;
import java.util.List;

public class LeaderboardListAdapter extends RecyclerView.Adapter<LeaderboardListAdapter.ListViewHolder> {

    private List<LeaderboardList> leaderboardList;

    private String avatarSourceURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0a/%s/1024px-%s.png";

    public class ListViewHolder extends RecyclerView.ViewHolder {
        TextView rank;
        SimpleDraweeView avatar;
        TextView username;
        TextView count;

        public ListViewHolder(View itemView) {
            super(itemView);
            this.rank = itemView.findViewById(R.id.user_rank);
            this.avatar = itemView.findViewById(R.id.user_avatar);
            this.username = itemView.findViewById(R.id.user_name);
            this.count = itemView.findViewById(R.id.user_count);
        }

        /**
         * This method will return the Context
         * @return Context
         */
        public Context getContext() {
            return itemView.getContext();
        }
    }

    public LeaderboardListAdapter(List<LeaderboardList> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }

    /**
     * Overrides the onCreateViewHolder and inflates the recyclerview list item layout
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    @Override
    public LeaderboardListAdapter.ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.leaderboard_list_element, parent, false);

        return new ListViewHolder(view);
    }

    /**
     * Overrides the onBindViewHolder Set the view at the specific position with the specific value
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull LeaderboardListAdapter.ListViewHolder holder, int position) {
        TextView rank = holder.rank;
        SimpleDraweeView avatar = holder.avatar;
        TextView username = holder.username;
        TextView count = holder.count;

        rank.setText(leaderboardList.get(position).getRank().toString());

        avatar.setImageURI(
            Uri.parse(String.format(avatarSourceURL, leaderboardList.get(position).getAvatar(),
                leaderboardList.get(position).getAvatar())));
        username.setText(leaderboardList.get(position).getUsername());
        count.setText(leaderboardList.get(position).getCategoryCount().toString());
    }

    /**
     * Override the getItemCount method
     * @return the size of the recycler view list
     */
    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }
}
