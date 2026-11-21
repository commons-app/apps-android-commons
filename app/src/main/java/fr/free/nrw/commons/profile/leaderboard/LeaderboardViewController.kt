package fr.free.nrw.commons.profile.leaderboard

import android.view.View
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentLeaderboardBinding

/** Controls visibility while leaderboard data is loading or available. */
class LeaderboardViewController(
    private val binding: FragmentLeaderboardBinding,
) {
    /**
     * to hide progressbar
     */
    fun hideProgressBar() =
        with(binding) {
            progressBar.visibility = View.GONE
            categorySpinner.visibility = View.VISIBLE
            durationSpinner.visibility = View.VISIBLE
            scroll.visibility = View.VISIBLE
            leaderboardList.visibility = View.VISIBLE
        }

    /**
     * to show progressbar
     */
    fun showProgressBar() =
        with(binding) {
            progressBar.visibility = View.VISIBLE
            scroll.visibility = View.INVISIBLE
        }

    /**
     * used to hide the layouts while fetching results from api
     */
    fun hideLayouts() =
        with(binding) {
            categorySpinner.visibility = View.INVISIBLE
            durationSpinner.visibility = View.INVISIBLE
            leaderboardList.visibility = View.INVISIBLE
        }

    /**
     * Set the spinners for the leaderboard filters
     */
    fun setSpinners() {
        val categoryAdapter =
            ArrayAdapter.createFromResource(
                binding.root.context,
                R.array.leaderboard_categories,
                android.R.layout.simple_spinner_item,
            )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        val durationAdapter =
            ArrayAdapter.createFromResource(
                binding.root.context,
                R.array.leaderboard_durations,
                android.R.layout.simple_spinner_item,
            )
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.durationSpinner.adapter = durationAdapter
    }

    /**
     * update the state of the scroll button based on the list's scroll position and user's rank
     */
    fun updateScrollButtonState(
        userRank: Int,
        threshold: Int,
    ) {
        val layoutManager = binding.leaderboardList.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()

        val context = binding.root.context

        // show "Go to Top" if the user has scrolled significantly down and they are past their own rank
        // if the user's rank is high (like rank 500), we only want to show "Go to Top" when they scroll past rank 500
        // if they are at rank 5, we show it once they scroll past the threshold.
        val shouldShowGoToTop = firstVisible > threshold && (userRank == 0 || firstVisible > userRank)

        if (shouldShowGoToTop) {
            binding.scroll.text = context.getString(R.string.leaderboard_go_to_top_button_text)
            binding.scroll.setIconResource(android.R.drawable.arrow_up_float)
            binding.scroll.visibility = View.VISIBLE
        } else {
            // near to the top of the leaderboard or haven't reached their own rank yet
            binding.scroll.text = context.getString(R.string.leaderboard_my_rank_button_text)
            binding.scroll.setIconResource(0)
            binding.scroll.visibility = View.VISIBLE
        }
    }
}
