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
import fr.free.nrw.commons.profile.models.LeaderboardResponse;

/**
 * This class extends RecyclerView.Adapter and creates the UserDetail section of the leaderboard
 */
public class UserDetailAdapter extends RecyclerView.Adapter<UserDetailAdapter.DataViewHolder> {

    private LeaderboardResponse leaderboardResponse;

    public UserDetailAdapter(LeaderboardResponse leaderboardResponse) {
        this.leaderboardResponse = leaderboardResponse;
    }

    public class DataViewHolder extends RecyclerView.ViewHolder {

        private TextView rank;
        private SimpleDraweeView avatar;
        private TextView username;
        private TextView count;

        public DataViewHolder(@NonNull View itemView) {
            super(itemView);
            this.rank = itemView.findViewById(R.id.rank);
            this.avatar = itemView.findViewById(R.id.avatar);
            this.username = itemView.findViewById(R.id.username);
            this.count = itemView.findViewById(R.id.count);
        }

        /**
         * This method will return the Context
         * @return Context
         */
        public Context getContext() {
            return itemView.getContext();
        }
    }

    /**
     * Overrides the onCreateViewHolder and sets the view with leaderboard user element layout
     * @param parent
     * @param viewType
     * @return
     */
    @NonNull
    @Override
    public UserDetailAdapter.DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
        int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.leaderboard_user_element, parent, false);
        return new DataViewHolder(view);
    }

    /**
     * Overrides the onBindViewHolder Set the view at the specific position with the specific value
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull UserDetailAdapter.DataViewHolder holder, int position) {
        TextView rank = holder.rank;
        SimpleDraweeView avatar = holder.avatar;
        TextView username = holder.username;
        TextView count = holder.count;

        rank.setText(String.format("%s %d",
            holder.getContext().getResources().getString(R.string.rank_prefix),
            leaderboardResponse.getRank()));

        avatar.setImageURI(
            Uri.parse(leaderboardResponse.getAvatar()));
        username.setText(leaderboardResponse.getUsername());
        count.setText(String.format("%s %d",
            holder.getContext().getResources().getString(R.string.count_prefix),
            leaderboardResponse.getCategoryCount()));

    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
