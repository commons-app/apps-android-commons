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
import fr.free.nrw.commons.CommonsApplication
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
 * Fragment for showing pending uploads in Upload Progress Activity. This fragment provides
 * functionality for the user to pause uploads.
 */
class PendingUploadsFragment : CommonsDaggerSupportFragment(), PendingUploadsContract.View,
    PendingUploadsAdapter.Callback {

    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    private lateinit var binding: FragmentPendingUploadsBinding

    private lateinit var uploadProgressActivity: UploadProgressActivity

    private lateinit var adapter: PendingUploadsAdapter

    private var contributionsSize = 0
    var contributionsList = ArrayList<Contribution>()

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

    /**
     * Initializes the recycler view.
     */
    fun initRecyclerView() {
        binding.pendingUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        binding.pendingUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter!!.setup()
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
                binding.nopendingTextView.visibility = View.GONE
                binding.pendingUplaodsLl.visibility = View.VISIBLE
                adapter.submitList(list)
                binding.progressTextView.setText(contributionsSize.toString() + " uploads left")
                if ((pausedOrQueuedUploads == contributionsSize) || CommonsApplication.isPaused) {
                    uploadProgressActivity.setPausedIcon(true)
                } else {
                    uploadProgressActivity.setPausedIcon(false)
                }
            }
        }
    }

    /**
     * Cancels a specific upload after getting a confirmation from the user using Dialog.
     */
    override fun deleteUpload(contribution: Contribution?) {
        showAlertDialog(
            requireActivity(),
            String.format(
                Locale.getDefault(),
                requireActivity().getString(R.string.cancelling_upload)
            ),
            String.format(
                Locale.getDefault(),
                requireActivity().getString(R.string.cancel_upload_dialog)
            ),
            String.format(Locale.getDefault(), requireActivity().getString(R.string.yes)),
            String.format(Locale.getDefault(), requireActivity().getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                pendingUploadsPresenter.deleteUpload(
                    contribution,
                    this.requireContext().applicationContext
                )
            },
            {}
        )
    }

    /**
     * Restarts all the paused uploads.
     */
    fun restartUploads() {
        if (contributionsList != null) {
            pendingUploadsPresenter.restartUploads(
                contributionsList,
                0,
                this.requireContext().applicationContext
            )
        }
    }

    /**
     * Pauses all the ongoing uploads.
     */
    fun pauseUploads() {
        pendingUploadsPresenter.pauseUploads()
    }

    /**
     * Cancels all the uploads after getting a confirmation from the user using Dialog.
     */
    fun deleteUploads() {
        showAlertDialog(
            requireActivity(),
            String.format(
                Locale.getDefault(),
                requireActivity().getString(R.string.cancelling_all_the_uploads)
            ),
            String.format(
                Locale.getDefault(),
                requireActivity().getString(R.string.are_you_sure_that_you_want_cancel_all_the_uploads)
            ),
            String.format(Locale.getDefault(), requireActivity().getString(R.string.yes)),
            String.format(Locale.getDefault(), requireActivity().getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                uploadProgressActivity.hidePendingIcons()
                pendingUploadsPresenter.deleteUploads(
                    listOf(
                        Contribution.STATE_QUEUED,
                        Contribution.STATE_IN_PROGRESS,
                        Contribution.STATE_PAUSED
                    )
                )
            },
            {}
        )

    }
}