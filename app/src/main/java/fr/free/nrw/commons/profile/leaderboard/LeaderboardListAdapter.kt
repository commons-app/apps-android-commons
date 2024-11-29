package fr.free.nrw.commons.profile.leaderboard

import android.app.Activity
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.profile.leaderboard.LeaderboardList.Companion.DIFF_CALLBACK
import fr.free.nrw.commons.profile.leaderboard.LeaderboardListAdapter.ListViewHolder


/**
 * This class extends RecyclerView.Adapter and creates the List section of the leaderboard
 */
class LeaderboardListAdapter : PagedListAdapter<LeaderboardList, ListViewHolder>(DIFF_CALLBACK) {
    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var rank: TextView? = itemView.findViewById(R.id.user_rank)
        var avatar: SimpleDraweeView? = itemView.findViewById(R.id.user_avatar)
        var username: TextView? = itemView.findViewById(R.id.user_name)
        var count: TextView? = itemView.findViewById(R.id.user_count)
    }

    /**
     * Overrides the onCreateViewHolder and inflates the recyclerview list item layout
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder =
        ListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.leaderboard_list_element, parent, false)
        )

    /**
     * Overrides the onBindViewHolder Set the view at the specific position with the specific value
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) = with (holder) {
        val item = getItem(position)!!

        rank?.text = item.rank.toString()
        avatar?.setImageURI(Uri.parse(item.avatar))
        username?.text = item.username
        count?.text = item.categoryCount.toString()

        /*
          Now that we have our in app profile-section, lets take the user there
         */
        itemView.setOnClickListener { view: View ->
            if (view.context is ProfileActivity) {
                ((view.context) as Activity).finish()
            }
            ProfileActivity.startYourself(view.context, item.username, true)
        }
    }
}
