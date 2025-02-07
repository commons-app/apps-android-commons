package fr.free.nrw.commons.contributions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.MediaClient

/**
 * Represents The View Adapter for the List of Contributions
 */
class ContributionsListAdapter internal constructor(
    private val callback: Callback,
    private val mediaClient: MediaClient
) : PagedListAdapter<Contribution, ContributionViewHolder>(DIFF_CALLBACK) {
    /**
     * Initializes the view holder with contribution data
     */
    override fun onBindViewHolder(holder: ContributionViewHolder, position: Int) {
        holder.init(position, getItem(position))
    }

    fun getContributionForPosition(position: Int): Contribution? {
        return getItem(position)
    }

    /**
     * Creates the new View Holder which will be used to display items(contributions) using the
     * onBindViewHolder(viewHolder,position)
     */
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContributionViewHolder {
        val viewHolder = ContributionViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_contribution, parent, false),
            callback, mediaClient
        )
        return viewHolder
    }

    interface Callback {
        fun openMediaDetail(contribution: Int, isWikipediaPageExists: Boolean)

        fun addImageToWikipedia(contribution: Contribution?)
    }

    companion object {
        /**
         * Uses DiffUtil to calculate the changes in the list
         * It has methods that check ID and the content of the items to determine if its a new item
         */
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<Contribution> =
            object : DiffUtil.ItemCallback<Contribution>() {
                override fun areItemsTheSame(
                    oldContribution: Contribution,
                    newContribution: Contribution
                ): Boolean {
                    return oldContribution.pageId == newContribution.pageId
                }

                override fun areContentsTheSame(
                    oldContribution: Contribution,
                    newContribution: Contribution
                ): Boolean {
                    return oldContribution == newContribution
                }
            }
    }
}
