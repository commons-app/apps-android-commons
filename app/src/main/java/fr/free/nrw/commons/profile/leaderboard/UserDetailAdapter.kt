package fr.free.nrw.commons.profile.leaderboard

import android.accounts.AccountManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.profile.leaderboard.UserDetailAdapter.DataViewHolder
import java.util.Locale

/**
 * This class extends RecyclerView.Adapter and creates the UserDetail section of the leaderboard
 */
class UserDetailAdapter(private val leaderboardResponse: LeaderboardResponse) :
    RecyclerView.Adapter<DataViewHolder>() {
    /**
     * Stores the username of currently logged in user.
     */
    private var currentlyLoggedInUserName: String? = null

    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rank: TextView = itemView.findViewById(R.id.rank)
        val avatar: SimpleDraweeView = itemView.findViewById(R.id.avatar)
        val username: TextView = itemView.findViewById(R.id.username)
        val count: TextView = itemView.findViewById(R.id.count)
    }

    /**
     * Overrides the onCreateViewHolder and sets the view with leaderboard user element layout
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DataViewHolder = DataViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_user_element, parent, false)
    )

    /**
     * Overrides the onBindViewHolder Set the view at the specific position with the specific value
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: DataViewHolder, position: Int) = with(holder) {
        val resources = itemView.context.resources

        avatar.setImageURI(Uri.parse(leaderboardResponse.avatar))
        username.text = leaderboardResponse.username
        rank.text = String.format(
            Locale.getDefault(),
            "%s %d",
            resources.getString(R.string.rank_prefix),
            leaderboardResponse.rank
        )
        count.text = String.format(
            Locale.getDefault(),
            "%s %d",
            resources.getString(R.string.count_prefix),
            leaderboardResponse.categoryCount
        )

        // When user tap on avatar shows the toast on how to change avatar
        // fixing: https://github.com/commons-app/apps-android-commons/issues/47747
        if (currentlyLoggedInUserName == null) {
            // If the current login username has not been fetched yet, then fetch it.
            val accountManager = AccountManager.get(itemView.context)
            val allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE)
            if (allAccounts.isNotEmpty()) {
                currentlyLoggedInUserName = allAccounts[0].name
            }
        }
        if (currentlyLoggedInUserName != null && currentlyLoggedInUserName == leaderboardResponse.username) {
            avatar.setOnClickListener { v: View ->
                Toast.makeText(
                    v.context, R.string.set_up_avatar_toast_string, Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = 1
}
