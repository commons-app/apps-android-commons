package fr.free.nrw.commons.upload

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_IN_PROGRESS
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_PAUSED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.databinding.FragmentPendingUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment for showing pending uploads in Upload Progress Activity. This fragment provides
 * functionality for the user to pause uploads.
 */
class PendingUploadsFragment :
    CommonsDaggerSupportFragment(),
    PendingUploadsContract.View,
    PendingUploadsAdapter.Callback {
    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    private lateinit var binding: FragmentPendingUploadsBinding

    private lateinit var uploadProgressActivity: UploadProgressActivity

    private lateinit var adapter: PendingUploadsAdapter

    private var contributionsSize = 0

    private var contributionsList = mutableListOf<Contribution>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreate(savedInstanceState)
        binding = FragmentPendingUploadsBinding.inflate(inflater, container, false)
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    fun initAdapter() {
        adapter = PendingUploadsAdapter(this)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    /**
     * Initializes the recycler view.
     */
    private fun initRecyclerView() {
        binding.pendingUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        binding.pendingUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter.setup()
        pendingUploadsPresenter.totalContributionList
            .observe(viewLifecycleOwner) { list: PagedList<Contribution> ->
            contributionsSize = list.size
            contributionsList = mutableListOf()
            var pausedOrQueuedUploads = 0
            list.forEach {
                if (it != null) {
                    if (it.state == STATE_PAUSED ||
                        it.state == STATE_QUEUED ||
                        it.state == STATE_IN_PROGRESS
                    ) {
                        contributionsList.add(it)
                    }
                    if (it.state == STATE_PAUSED || it.state == STATE_QUEUED) {
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
                binding.progressTextView.setText("$contributionsSize uploads left")
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
        val activity = requireActivity()
        val locale = Locale.getDefault()
        showAlertDialog(
            activity,
            String.format(locale, activity.getString(R.string.cancelling_upload)),
            String.format(locale, activity.getString(R.string.cancel_upload_dialog)),
            String.format(locale, activity.getString(R.string.yes)),
            String.format(locale, activity.getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                pendingUploadsPresenter.deleteUpload(
                    contribution, requireContext().applicationContext,
                )
            },
            {},
        )
    }

    /**
     * Restarts all the paused uploads.
     */
    fun restartUploads() = pendingUploadsPresenter.restartUploads(
        contributionsList, 0, requireContext().applicationContext
    )

    /**
     * Pauses all the ongoing uploads.
     */
    fun pauseUploads() = pendingUploadsPresenter.pauseUploads()

    /**
     * Cancels all the uploads after getting a confirmation from the user using Dialog.
     */
    fun deleteUploads() {
        val activity = requireActivity()
        val locale = Locale.getDefault()
        showAlertDialog(
            activity,
            String.format(locale, activity.getString(R.string.cancelling_all_the_uploads)),
            String.format(locale, activity.getString(R.string.are_you_sure_that_you_want_cancel_all_the_uploads)),
            String.format(locale, activity.getString(R.string.yes)),
            String.format(locale, activity.getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                uploadProgressActivity.hidePendingIcons()
                pendingUploadsPresenter.deleteUploads(
                    listOf(
                        STATE_QUEUED,
                        STATE_IN_PROGRESS,
                        STATE_PAUSED,
                    ),
                )
            },
            {},
        )
    }
}
