package fr.free.nrw.commons.upload

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.CommonsApplication
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
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment for displaying a list of failed uploads in Upload Progress Activity. This fragment provides
 * functionality for the user to retry or cancel failed uploads.
 */
class FailedUploadsFragment : CommonsDaggerSupportFragment(), PendingUploadsContract.View,
    FailedUploadsAdapter.Callback {

    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var sessionManager: SessionManager

    private var userName: String? = null

    lateinit var binding: FragmentFailedUploadsBinding

    private lateinit var adapter: FailedUploadsAdapter

    var contributionsList = ArrayList<Contribution>()

    private lateinit var uploadProgressActivity: UploadProgressActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Now that we are allowing this fragment to be started for
        // any userName- we expect it to be passed as an argument
        if (arguments != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
        }

        if (StringUtils.isEmpty(userName)) {
            userName = sessionManager!!.getUserName()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFailedUploadsBinding.inflate(layoutInflater)
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    fun initAdapter() {
        adapter = FailedUploadsAdapter(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    /**
     * Initializes the recycler view.
     */
    fun initRecyclerView() {
        binding.failedUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        binding.failedUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter!!.getFailedContributions()
        pendingUploadsPresenter!!.failedContributionList.observe(
            viewLifecycleOwner
        ) { list: PagedList<Contribution?> ->
            adapter.submitList(list)
            contributionsList = ArrayList()
            list.forEach {
                if (it != null) {
                    contributionsList.add(it)
                }
            }
            if (list.size == 0) {
                uploadProgressActivity.setErrorIconsVisibility(false)
                binding.nofailedTextView.visibility = View.VISIBLE
                binding.failedUplaodsLl.visibility = View.GONE
            } else {
                uploadProgressActivity.setErrorIconsVisibility(true)
                binding.nofailedTextView.visibility = View.GONE
                binding.failedUplaodsLl.visibility = View.VISIBLE
                binding.failedUploadsRecyclerView.setAdapter(adapter)
            }
        }
    }

    /**
     * Restarts all the failed uploads.
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
     * Restarts a specific upload.
     */
    override fun restartUpload(index: Int) {
        if (contributionsList != null) {
            pendingUploadsPresenter.restartUpload(
                contributionsList,
                index,
                this.requireContext().applicationContext
            )
        }
    }

    /**
     * Deletes a specific upload after getting a confirmation from the user using Dialog.
     */
    override fun deleteUpload(contribution: Contribution?) {
        DialogUtil.showAlertDialog(
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
     * Deletes all the uploads after getting a confirmation from the user using Dialog.
     */
    fun deleteUploads() {
        if (contributionsList != null) {
            DialogUtil.showAlertDialog(
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
                        listOf(Contribution.STATE_FAILED)
                    )
                },
                {}
            )
        }
    }
}