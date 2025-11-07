package fr.free.nrw.commons.profile.leaderboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.MergeAdapter
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentLeaderboardBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LoadingStatus.LOADED
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LoadingStatus.LOADING
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.START_OFFSET
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Objects
import javax.inject.Inject

/**
 * This class extends the CommonsDaggerSupportFragment and creates leaderboard fragment
 */
@AndroidEntryPoint
class LeaderboardFragment : CommonsDaggerSupportFragment() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private var viewModel: LeaderboardListViewModel? = null
    private var duration: String? = null
    private var category: String? = null
    private val limit: Int = PAGE_SIZE
    private val offset: Int = START_OFFSET
    private var userRank = 0
    private var scrollToRank = false
    private var userName: String? = null
    private var binding: FragmentLeaderboardBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { userName = it.getString(ProfileActivity.KEY_USERNAME) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false)

        hideLayouts()

        // Leaderboard currently unimplemented in Beta flavor. Skip all API calls and disable menu
        if (isBetaFlavour) {
            binding!!.progressBar.visibility = View.GONE
            binding!!.scroll.visibility = View.GONE
            return binding!!.root
        }

        binding!!.progressBar.visibility = View.VISIBLE
        setSpinners()

        /*
         * This array is for the duration filter, we have three filters weekly, yearly and all-time
         * each filter have a key and value pair, the value represents the param of the API
         */
        val durationValues = requireContext().resources
            .getStringArray(R.array.leaderboard_duration_values)
        duration = durationValues[0]

        /*
         * This array is for the category filter, we have three filters upload, used and nearby
         * each filter have a key and value pair, the value represents the param of the API
         */
        val categoryValues = requireContext().resources
            .getStringArray(R.array.leaderboard_category_values)
        category = categoryValues[0]

        setLeaderboard(duration, category, limit, offset)

        with(binding!!) {
            durationSpinner.onItemSelectedListener = SelectionListener {
                duration = durationValues[durationSpinner.selectedItemPosition]
                refreshLeaderboard()
            }

            categorySpinner.onItemSelectedListener = SelectionListener {
                category = categoryValues[categorySpinner.selectedItemPosition]
                refreshLeaderboard()
            }

            scroll.setOnClickListener { scrollToUserRank() }

            return root
        }
    }

    override fun setMenuVisibility(visible: Boolean) {
        super.setMenuVisibility(visible)

        // Whenever this fragment is revealed in a menu,
        // notify Beta users the page data is unavailable
        if (isBetaFlavour && visible) {
            val ctx: Context? = if (context != null) {
                context
            } else if (view != null && requireView().context != null) {
                requireView().context
            } else {
                null
            }

            ctx?.let {
                Toast.makeText(it, R.string.leaderboard_unavailable_beta, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Refreshes the leaderboard list
     */
    private fun refreshLeaderboard() {
        scrollToRank = false
        viewModel?.let {
            it.refresh(duration, category, limit, offset)
            setLeaderboard(duration, category, limit, offset)
        }
    }

    /**
     * Performs Auto Scroll to the User's Rank
     * We use userRank+1 to load one extra user and prevent overlapping of my rank button
     * If you are viewing the leaderboard below userRank, it scrolls to the user rank at the top
     */
    private fun scrollToUserRank() {
        if (userRank == 0) {
            Toast.makeText(context, R.string.no_achievements_yet, Toast.LENGTH_SHORT).show()
        } else {
            if (binding == null) {
                return
            }
            val itemCount = binding?.leaderboardList?.adapter?.itemCount ?: 0
            if (itemCount > userRank + 1) {
                binding!!.leaderboardList.smoothScrollToPosition(userRank + 1)
            } else {
                viewModel?.let {
                    it.refresh(duration, category, userRank + 1, 0)
                    setLeaderboard(duration, category, userRank + 1, 0)
                    scrollToRank = true
                }
            }
        }
    }

    /**
     * Set the spinners for the leaderboard filters
     */
    private fun setSpinners() {
        val categoryAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.leaderboard_categories, android.R.layout.simple_spinner_item
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding!!.categorySpinner.adapter = categoryAdapter

        val durationAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.leaderboard_durations, android.R.layout.simple_spinner_item
        )
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding!!.durationSpinner.adapter = durationAdapter
    }

    /**
     * To call the API to get results
     * which then sets the views using setLeaderboardUser method
     */
    private fun setLeaderboard(duration: String?, category: String?, limit: Int, offset: Int) {
        if (checkAccount()) {
            try {
                compositeDisposable.add(
                    okHttpJsonApiClient.getLeaderboard(
                        Objects.requireNonNull(userName),
                        duration, category, null, null
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { response: LeaderboardResponse? ->
                            if (response != null && response.status == 200) {
                                userRank = response.rank!!
                                setViews(response, duration, category, limit, offset)
                            }
                        },
                        { t: Throwable? ->
                            Timber.e(t, "Fetching leaderboard statistics failed")
                            onError()
                        }
                    ))
            } catch (e: Exception) {
                Timber.d(e, "success")
            }
        }
    }

    /**
     * Set the views
     * @param response Leaderboard Response Object
     */
    private fun setViews(
        response: LeaderboardResponse,
        duration: String?,
        category: String?,
        limit: Int,
        offset: Int
    ) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(
            LeaderboardListViewModel::class.java
        )
        viewModel!!.setParams(duration, category, limit, offset)
        val leaderboardListAdapter = LeaderboardListAdapter()
        val userDetailAdapter = UserDetailAdapter(response)
        val mergeAdapter = MergeAdapter(userDetailAdapter, leaderboardListAdapter)
        val linearLayoutManager = LinearLayoutManager(context)
        binding!!.leaderboardList.layoutManager = linearLayoutManager
        binding!!.leaderboardList.adapter = mergeAdapter
        viewModel!!.listLiveData.observe(viewLifecycleOwner, leaderboardListAdapter::submitList)

        viewModel!!.progressLoadStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                LOADING -> {
                    showProgressBar()
                }
                LOADED -> {
                    hideProgressBar()
                    if (scrollToRank) {
                        binding!!.leaderboardList.smoothScrollToPosition(userRank + 1)
                    }
                }
            }
        }
    }

    /**
     * to hide progressbar
     */
    private fun hideProgressBar() = binding?.let {
        it.progressBar.visibility = View.GONE
        it.categorySpinner.visibility = View.VISIBLE
        it.durationSpinner.visibility = View.VISIBLE
        it.scroll.visibility = View.VISIBLE
        it.leaderboardList.visibility = View.VISIBLE
    }

    /**
     * to show progressbar
     */
    private fun showProgressBar() = binding?.let {
        it.progressBar.visibility = View.VISIBLE
        it.scroll.visibility = View.INVISIBLE
    }

    /**
     * used to hide the layouts while fetching results from api
     */
    private fun hideLayouts()  = binding?.let {
        it.categorySpinner.visibility = View.INVISIBLE
        it.durationSpinner.visibility = View.INVISIBLE
        it.leaderboardList.visibility = View.INVISIBLE
    }

    /**
     * check to ensure that user is logged in
     */
    private fun checkAccount() = if (sessionManager.currentAccount == null) {
        Timber.d("Current account is null")
        showLongToast(requireActivity(), resources.getString(R.string.user_not_logged_in))
        sessionManager.forceLogin(requireActivity())
        false
    } else {
        true
    }

    /**
     * Shows a generic error toast when error occurs while loading leaderboard
     */
    private fun onError() {
        showLongToast(requireActivity(), resources.getString(R.string.error_occurred))
        binding?.let { it.progressBar.visibility = View.GONE }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        binding = null
    }

    private class SelectionListener(private val handler: () -> Unit): AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) =
            handler()

        override fun onNothingSelected(p0: AdapterView<*>?) = Unit
    }
}
