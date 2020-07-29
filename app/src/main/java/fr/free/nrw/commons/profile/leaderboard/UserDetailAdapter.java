package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.AVATAR_SOURCE_URL;

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

public class UserDetailAdapter extends RecyclerView.Adapter<UserDetailAdapter.DataViewHolder> {

    LeaderboardResponse leaderboardResponse;

    public UserDetailAdapter(LeaderboardResponse leaderboardResponse) {
        this.leaderboardResponse = leaderboardResponse;
    }

    public class DataViewHolder extends RecyclerView.ViewHolder {

        TextView rank;
        SimpleDraweeView avatar;
        TextView username;
        TextView count;

        public DataViewHolder(@NonNull View itemView) {
            super(itemView);
            this.rank = itemView.findViewById(R.id.rank);
            this.avatar = itemView.findViewById(R.id.avatar);
            this.username = itemView.findViewById(R.id.username);
            this.count = itemView.findViewById(R.id.count);
        }

        public Context getContext() {
            return itemView.getContext();
        }
    }

    @NonNull
    @Override
    public UserDetailAdapter.DataViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
        int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.leaderboard_user_element, parent, false);
        return new DataViewHolder(view);
    }

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
            Uri.parse(String.format(AVATAR_SOURCE_URL, leaderboardResponse.getAvatar(),
                leaderboardResponse.getAvatar())));
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
