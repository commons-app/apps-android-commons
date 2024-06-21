package fr.free.nrw.commons.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.databinding.FragmentFailedUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

/**
 * A simple [Fragment] subclass.
 * Use the [FailedUploadsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class FailedUploadsFragment : CommonsDaggerSupportFragment(),PendingUploadsContract.View {
    // TODO: Rename and change types of parameters
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

    lateinit var binding: FragmentFailedUploadsBinding

    var contributionsList = ArrayList<Contribution>()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFailedUploadsBinding.inflate(layoutInflater)
        pendingUploadsPresenter.onAttachView(this)
        initRecyclerView()
        return binding.root
    }

    fun initRecyclerView() {
        binding.failedUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        pendingUploadsPresenter!!.setup(
            userName,
            sessionManager!!.userName == userName
        )
        pendingUploadsPresenter!!.totalContributionList.observe(
            viewLifecycleOwner
        ) { list: PagedList<Contribution?> ->
            contributionsList = ArrayList()
            list.forEach {
                if (it != null){
                    if (it.state == Contribution.STATE_FAILED) {
                        contributionsList.add(it)
                    }
                }
            }
            if (contributionsList.size == 0) {
                binding.nofailedTextView.visibility = View.VISIBLE
                binding.failedUplaodsLl.visibility = View.GONE
            } else {
                binding.nofailedTextView.visibility = View.GONE
                binding.failedUplaodsLl.visibility = View.VISIBLE
                val adapter = FailedUploadsAdapter(contributionsList)
                binding.failedUploadsRecyclerView.setAdapter(adapter)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FailedUploadsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FailedUploadsFragment().apply {
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
}