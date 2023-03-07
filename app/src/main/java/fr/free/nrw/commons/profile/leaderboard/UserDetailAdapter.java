package fr.free.nrw.commons.profile.leaderboard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.internal.DebouncingOnClickListener;
import com.facebook.drawee.view.SimpleDraweeView;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;


/**
 * This class extends RecyclerView.Adapter and creates the UserDetail section of the leaderboard
 */
public class UserDetailAdapter extends RecyclerView.Adapter<UserDetailAdapter.DataViewHolder> {

    private LeaderboardResponse leaderboardResponse;

    /**
     * Stores the username of currently logged in user.
     */
    private String currentlyLoggedInUserName = null;

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

        // When user tap on avatar shows the toast on how to change avatar
        // fixing: https://github.com/commons-app/apps-android-commons/issues/47747
        if (currentlyLoggedInUserName == null) {
            // If the current login username has not been fetched yet, then fetch it.
            final AccountManager accountManager = AccountManager.get(username.getContext());
            final Account[] allAccounts = accountManager.getAccountsByType(
                BuildConfig.ACCOUNT_TYPE);
            if (allAccounts.length != 0) {
                currentlyLoggedInUserName = allAccounts[0].name;
            }
        }
        if (currentlyLoggedInUserName != null && currentlyLoggedInUserName.equals(
            leaderboardResponse.getUsername())) {
            avatar.setOnClickListener(new DebouncingOnClickListener() {
                @Override
                public void doClick(View v) {
                    Toast.makeText(v.getContext(),
                        R.string.set_up_avatar_toast_string,
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }
}
