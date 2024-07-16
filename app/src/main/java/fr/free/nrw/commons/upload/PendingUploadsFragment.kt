package fr.free.nrw.commons.upload

import android.content.Context
import android.os.AsyncTask
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.databinding.FragmentPendingUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject


/**
 * A simple [Fragment] subclass.
 * Use the [PendingUploadsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PendingUploadsFragment : CommonsDaggerSupportFragment(), PendingUploadsContract.View,
    PendingUploadsAdapter.Callback {
    private var param1: String? = null
    private var param2: String? = null
    private val ARG_PARAM1 = "param1"
    private val ARG_PARAM2 = "param2"

    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var sessionManager: SessionManager

    private var userName: String? = null

    private lateinit var binding: FragmentPendingUploadsBinding

    private lateinit var uploadProgressActivity: UploadProgressActivity

    private lateinit var adapter: PendingUploadsAdapter

    private var contributionsSize = 0
    var contributionsList = ArrayList<Contribution>()
    private var totalUploads = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        //Now that we are allowing this fragment to be started for
        // any userName- we expect it to be passed as an argument
        if (arguments != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
        }

        if (StringUtils.isEmpty(userName)) {
            userName = sessionManager!!.getUserName()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        binding = FragmentPendingUploadsBinding.inflate(inflater, container, false)
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    fun initAdapter() {
        adapter = PendingUploadsAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    fun initRecyclerView() {
        binding.pendingUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        binding.pendingUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter!!.setup(
            userName,
            sessionManager!!.userName == userName
        )
        pendingUploadsPresenter!!.totalContributionList.observe(
            viewLifecycleOwner
        ) { list: PagedList<Contribution?> ->
            contributionsSize = list.size
            contributionsList = ArrayList()
            var pausedOrQueuedUploads = 0
            list.forEach {
                if (it != null) {
                    if (it.state == Contribution.STATE_PAUSED
                        || it.state == Contribution.STATE_QUEUED
                        || it.state == Contribution.STATE_IN_PROGRESS
                    ) {
                        contributionsList.add(it)
                    }
                    if (it.state == Contribution.STATE_PAUSED
                        || it.state == Contribution.STATE_QUEUED
                    ) {
                        pausedOrQueuedUploads++
                    }
                }
            }
            if (contributionsSize == 0) {
                binding.nopendingTextView.visibility = View.VISIBLE
                binding.pendingUplaodsLl.visibility = View.GONE
                uploadProgressActivity.hidePendingIcons()
            } else {
                if (totalUploads == 0) {
                    totalUploads = contributionsSize
                    binding.progressBarPending.max = totalUploads
                }
                binding.nopendingTextView.visibility = View.GONE
                binding.pendingUplaodsLl.visibility = View.VISIBLE

                val sortedContributionsList: List<Contribution> =
                    if (VERSION.SDK_INT >= VERSION_CODES.N) {
                        contributionsList.sortedByDescending { it.dateModifiedInMillis() }
                    } else {
                        contributionsList.sortedBy { it.dateModifiedInMillis() }.reversed()
                    }

                val newContributionList: MutableList<Contribution> =
                    sortedContributionsList.toMutableList()
                val listOfRemoved: MutableList<Contribution> = mutableListOf()
                val last = sortedContributionsList.last()
                for (i in sortedContributionsList.indices) {
                    val current = sortedContributionsList[i]
                    if (current.transferred == 0L && (current.dateModifiedInMillis() / 100) > (last.dateModifiedInMillis() / 100)) {
                        listOfRemoved.add(current)
                    }
                }
                newContributionList.removeAll(listOfRemoved)
                newContributionList.addAll(listOfRemoved)

                // TODO: WORK ON THE SORTING ISSUE
                val dataSource = object : PositionalDataSource<Contribution>() {
                    override fun loadInitial(
                        params: LoadInitialParams,
                        callback: LoadInitialCallback<Contribution>
                    ) {
                        callback.onResult(newContributionList, 0, newContributionList.size)
                    }

                    override fun loadRange(
                        params: LoadRangeParams,
                        callback: LoadRangeCallback<Contribution>
                    ) {
                        val start = params.startPosition
                        val end = Math.min(start + params.loadSize, newContributionList.size)
                        callback.onResult(newContributionList.subList(start, end))
                    }
                }

                val pagedList = PagedList.Builder(dataSource, 10)
                    .setFetchExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    .setNotifyExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    .build()

                adapter.submitList(pagedList)

                binding.progressTextView.setText((totalUploads - contributionsSize).toString() + "/" + totalUploads + " uploaded")
                binding.progressBarPending.progress = totalUploads - contributionsSize
                if (pausedOrQueuedUploads == contributionsSize) {
                    uploadProgressActivity.setPausedIcon(true)
                } else {
                    uploadProgressActivity.setPausedIcon(false)
                }
            }
        }
    }

    override fun deleteUpload(contribution: Contribution?) {
        showAlertDialog(
            requireActivity(),
            String.format(
                Locale.getDefault(),
                getString(R.string.cancelling_upload)
            ),
            String.format(
                Locale.getDefault(),
                getString(R.string.cancel_upload_dialog)
            ),
            String.format(Locale.getDefault(), getString(R.string.yes)),
            String.format(Locale.getDefault(), getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                pendingUploadsPresenter.deleteUpload(
                    contribution,
                    this.requireContext().applicationContext
                )
                resetProgressBar()
            },
            {}
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PendingUploadsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    fun restartUploads() {
        if (contributionsList != null) {
            pendingUploadsPresenter.restartUploads(
                contributionsList,
                0,
                this.requireContext().applicationContext
            )
        }
    }

    fun pauseUploads() {
        if (contributionsList != null) {
            pendingUploadsPresenter.pauseUploads(
                contributionsList,
                0,
                this.requireContext().applicationContext
            )
        }
    }

    fun deleteUploads() {
        if (contributionsList != null) {
            showAlertDialog(
                requireActivity(),
                String.format(
                    Locale.getDefault(),
                    getString(R.string.cancelling_all_the_uploads)
                ),
                String.format(
                    Locale.getDefault(),
                    getString(R.string.are_you_sure_that_you_want_cancel_all_the_uploads)
                ),
                String.format(Locale.getDefault(), getString(R.string.yes)),
                String.format(Locale.getDefault(), getString(R.string.no)),
                {
                    ViewUtil.showShortToast(context, R.string.cancelling_upload)
                    uploadProgressActivity.hidePendingIcons()
                    pendingUploadsPresenter.deleteUploads(
                        contributionsList,
                        0,
                        this.requireContext().applicationContext
                    )
                },
                {}
            )
        }
    }

    fun resetProgressBar() {
        totalUploads = 0
    }
}