package fr.free.nrw.commons.upload

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.databinding.FragmentFailedUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.ViewUtil
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

/**
 * Fragment for displaying a list of failed uploads in Upload Progress Activity. It provides
 * functionality for the user to retry or cancel failed uploads.
 */
class FailedUploadsFragment :
    CommonsDaggerSupportFragment(),
    PendingUploadsContract.View,
    FailedUploadsAdapter.Callback {

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var sessionManager: SessionManager

    private var userName: String? = null
    private lateinit var binding: FragmentFailedUploadsBinding
    private lateinit var adapter: FailedUploadsAdapter
    private val contributionsList = ArrayList<Contribution>()
    private lateinit var uploadProgressActivity: UploadProgressActivity
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Expecting userName as an argument; fallback to sessionManager's userName if not provided
        userName = arguments?.getString(ProfileActivity.KEY_USERNAME)
        if (StringUtils.isEmpty(userName)) {
            userName = sessionManager.userName
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFailedUploadsBinding.inflate(inflater, container, false)
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    private fun initAdapter() {
        adapter = FailedUploadsAdapter(this)
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
        binding.failedUploadsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.failedUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter.getFailedContributions()
        pendingUploadsPresenter.failedContributionList.observe(
            viewLifecycleOwner,
        ) { list: PagedList<Contribution?> ->
            adapter.submitList(list)
            contributionsList.clear()
            list.forEach { contribution ->
                contribution?.let { contributionsList.add(it) }
            }
            if (list.isEmpty()) {
                uploadProgressActivity.setErrorIconsVisibility(false)
                binding.nofailedTextView.visibility = View.VISIBLE
                binding.failedUplaodsLl.visibility = View.GONE
            } else {
                uploadProgressActivity.setErrorIconsVisibility(true)
                binding.nofailedTextView.visibility = View.GONE
                binding.failedUplaodsLl.visibility = View.VISIBLE
                // Setting the adapter again is redundant; it's already set above
            }
        }
    }

    /**
     * Restarts all the failed uploads.
     */
    fun restartUploads() {
        pendingUploadsPresenter.restartUploads(
            contributionsList,
            0,
            requireContext().applicationContext,
        )
    }

    /**
     * Restarts a specific upload.
     */
    override fun restartUpload(index: Int) {
        pendingUploadsPresenter.restartUpload(
            contributionsList,
            index,
            requireContext().applicationContext,
        )
    }

    /**
     * Deletes a specific upload after getting a confirmation from the user using Dialog.
     */
    override fun deleteUpload(contribution: Contribution?) {
        DialogUtil.showAlertDialog(
            requireActivity(),
            getString(R.string.cancelling_upload),
            getString(R.string.cancel_upload_dialog),
            getString(R.string.yes),
            getString(R.string.no),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                pendingUploadsPresenter.deleteUpload(
                    contribution,
                    requireContext().applicationContext,
                )
            },
            {}
        )
    }

    /**
     * Deletes all the uploads after getting a confirmation from the user using Dialog.
     */
    fun deleteUploads() {
        DialogUtil.showAlertDialog(
            requireActivity(),
            getString(R.string.cancelling_all_the_uploads),
            getString(R.string.are_you_sure_that_you_want_cancel_all_the_uploads),
            getString(R.string.yes),
            getString(R.string.no),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                uploadProgressActivity.hidePendingIcons()
                pendingUploadsPresenter.deleteUploads(
                    listOf(Contribution.STATE_FAILED),
                )
            },
            {}
        )
    }
}
