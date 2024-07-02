package fr.free.nrw.commons.upload

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.databinding.FragmentPendingUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.NetworkUtils
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
    PendingUploadsAdapter.Callback{
    private var param1: String? = null
    private var param2: String? = null
    private val ARG_PARAM1 = "param1"
    private val ARG_PARAM2 = "param2"
    private val MAX_RETRIES = 10

    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var sessionManager: SessionManager

    private var userName: String? = null

    private lateinit var binding: FragmentPendingUploadsBinding

    private lateinit var uploadProgressActivity: UploadProgressActivity

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
        initRecyclerView()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun initRecyclerView() {
        binding.pendingUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
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
            var failedUploads = 0
            list.forEach {
                if (it != null){
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
                    if (it.state == Contribution.STATE_FAILED){
                        failedUploads++
                    }
                }
            }
            if (contributionsSize == 0) {
                binding.nopendingTextView.visibility = View.VISIBLE
                binding.pendingUplaodsLl.visibility = View.GONE
                uploadProgressActivity.hidePendingIcons()
            } else {
                if (totalUploads == 0){
                    totalUploads = contributionsSize
                    binding.progressBarPending.max = totalUploads
                }
                binding.nopendingTextView.visibility = View.GONE
                binding.pendingUplaodsLl.visibility = View.VISIBLE

                val sortedContributionsList: List<Contribution> = if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    contributionsList.sortedByDescending { it.dateModifiedInMillis() }
                } else {
                    contributionsList.sortedBy { it.dateModifiedInMillis() }.reversed()
                }

                val newContributionList: MutableList<Contribution> = sortedContributionsList.toMutableList()
                val listOfRemoved: MutableList<Contribution> = mutableListOf()
                val last = sortedContributionsList.last()
                for (i in sortedContributionsList.indices) {
                    val current = sortedContributionsList[i]
                    if (current.transferred == 0L && (current.dateModifiedInMillis() / 100) > (last.dateModifiedInMillis() / 100)){
                        listOfRemoved.add(current)
                    }
                }
                newContributionList.removeAll(listOfRemoved)
                newContributionList.addAll(listOfRemoved)
                val adapter = PendingUploadsAdapter(newContributionList, this)
                binding.pendingUploadsRecyclerView.setAdapter(adapter)
                binding.progressTextView.setText((totalUploads-contributionsSize).toString() + "/" + totalUploads + " uploaded")
                binding.progressBarPending.progress = totalUploads-contributionsSize
                if (pausedOrQueuedUploads == contributionsSize) {
                    uploadProgressActivity.setPausedIcon(true)
                }else{
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
                pendingUploadsPresenter.deleteUpload(contribution, this.requireContext().applicationContext)
                CommonsApplication.cancelledUploads.add(contribution!!.pageId)
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
        if (contributionsList != null){
            pendingUploadsPresenter.restartUploads(contributionsList, 0 , this.requireContext().applicationContext)
        }
    }

    fun pauseUploads() {
        if (contributionsList != null){
            pendingUploadsPresenter.pauseUploads(contributionsList, 0, this.requireContext().applicationContext)
        }
    }

    fun deleteUploads(){
        if (contributionsList != null){
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
                    pendingUploadsPresenter.deleteUploads(contributionsList, 0, this.requireContext().applicationContext)
                },
                {}
            )
        }
    }

    /**
     * Restarts the upload process for a contribution
     *
     * @param contribution
     */
    fun restartUpload(contribution: Contribution) {
        contribution.state = Contribution.STATE_QUEUED
        pendingUploadsPresenter.saveContribution(contribution, this.requireContext().applicationContext)
        Timber.d("Restarting for %s", contribution.toString())
    }

    /**
     * Retry upload when it is failed
     *
     * @param contribution contribution to be retried
     */
    fun retryUpload(contribution: Contribution) {
        if (NetworkUtils.isInternetConnectionEstablished(context)) {
            if (contribution.state == Contribution.STATE_PAUSED) {
                restartUpload(contribution)
            } else if (contribution.state == Contribution.STATE_FAILED) {
                val retries = contribution.retries
                // TODO: Improve UX. Additional details: https://github.com/commons-app/apps-android-commons/pull/5257#discussion_r1304662562
                /* Limit the number of retries for a failed upload
                   to handle cases like invalid filename as such uploads
                   will never be successful */
                if (retries < MAX_RETRIES) {
                    contribution.retries = retries + 1
                    Timber.d(
                        "Retried uploading %s %d times", contribution.media.filename,
                        retries + 1
                    )
                    restartUpload(contribution)
                } else {
                    // TODO: Show the exact reason for failure
                    Toast.makeText(
                        context,
                        R.string.retry_limit_reached, Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Timber.d("Skipping re-upload for non-failed %s", contribution.toString())
            }
        } else {
            ViewUtil.showLongToast(context, R.string.this_function_needs_network_connection)
        }
    }

    fun resetProgressBar() {
        totalUploads = 0
    }
}