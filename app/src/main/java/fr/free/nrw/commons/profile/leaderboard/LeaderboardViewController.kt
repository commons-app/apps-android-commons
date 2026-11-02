package fr.free.nrw.commons.profile.leaderboard

import android.view.View
import android.widget.ArrayAdapter
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
        binding.let {
            it.progressBar.visibility = View.GONE
            it.categorySpinner.visibility = View.VISIBLE
            it.durationSpinner.visibility = View.VISIBLE
            it.scroll.visibility = View.VISIBLE
            it.leaderboardList.visibility = View.VISIBLE
        }

    /**
     * to show progressbar
     */
    fun showProgressBar() =
        binding.let {
            it.progressBar.visibility = View.VISIBLE
            it.scroll.visibility = View.INVISIBLE
        }

    /**
     * used to hide the layouts while fetching results from api
     */
    fun hideLayouts() =
        binding.let {
            it.categorySpinner.visibility = View.INVISIBLE
            it.durationSpinner.visibility = View.INVISIBLE
            it.leaderboardList.visibility = View.INVISIBLE
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
}
