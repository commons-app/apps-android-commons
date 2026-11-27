package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.customselector.helper.FolderUploadStatusChecker
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import fr.free.nrw.commons.databinding.FragmentCustomSelectorBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomPaddingInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Custom selector folder fragment.
 */
class FolderFragment : CommonsDaggerSupportFragment() {
    /**
     * ViewBinding
     */
    private var _binding: FragmentCustomSelectorBinding? = null
    val binding get() = _binding

    /**
     * View Model for images.
     */
    private var viewModel: CustomSelectorViewModel? = null

    /**
     * View Elements
     */
    private var selectorRV: RecyclerView? = null
    private var loader: ProgressBar? = null

    /**
     * View Model Factory.
     */
    var customSelectorViewModelFactory: CustomSelectorViewModelFactory? = null
        @Inject set

    var fileProcessor: FileProcessor? = null
        @Inject set

    var mediaClient: MediaClient? = null
        @Inject set

    /**
     * Finds the folders whose pictures have all been uploaded or marked as not for upload.
     */
    var folderUploadStatusChecker: FolderUploadStatusChecker? = null
        @Inject set

    /**
     * Running check of the handled folders.
     */
    private var handledFoldersJob: Job? = null

    /**
     * The pictures of a folder are shown on top of this fragment, so it is not resumed when the
     * user comes back from them. Checks the folders again once this fragment is shown again, as
     * pictures may have been uploaded or marked as not for upload in the meantime.
     */
    private val backStackListener =
        FragmentManager.OnBackStackChangedListener {
            if (parentFragmentManager.findFragmentById(id) === this) {
                updateHandledFolders()
            }
        }

    /**
     * Folder Adapter.
     */
    private lateinit var folderAdapter: FolderAdapter

    /**
     * Grid Layout Manager for recycler view.
     */
    private lateinit var gridLayoutManager: GridLayoutManager

    /**
     * Folder List.
     */
    private lateinit var folders: ArrayList<Folder>

    /**
     * Companion newInstance.
     */
    companion object {
        // todo change column count depending on the orientation of the device.
        private const val COLUMN_COUNT = 2

        fun newInstance(): FolderFragment = FolderFragment()
    }

    /**
     * OnCreate Fragment, get the view model.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel =
            ViewModelProvider(
                requireActivity(),
                customSelectorViewModelFactory!!,
            ).get(CustomSelectorViewModel::class.java)
    }

    /**
     * OnCreateView.
     * Inflate Layout, init adapter, init gridLayoutManager, setUp recycler view, observe the view model for result.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentCustomSelectorBinding.inflate(inflater, container, false)
        folderAdapter = FolderAdapter(requireActivity(), activity as FolderClickListener)
        gridLayoutManager = GridLayoutManager(context, COLUMN_COUNT)
        selectorRV = binding?.selectorRv
        loader = binding?.loader
        with(binding?.selectorRv) {
            this?.applyEdgeToEdgeBottomPaddingInsets()
            this?.layoutManager = gridLayoutManager
            this?.setHasFixedSize(true)
            this?.adapter = folderAdapter
        }
        viewModel?.result?.observe(viewLifecycleOwner) {
            handleResult(it)
        }
        parentFragmentManager.addOnBackStackChangedListener(backStackListener)
        return binding?.root
    }

    /**
     * Handle view model result.
     * Get folders from images.
     * Load adapter.
     */
    private fun handleResult(result: Result) {
        if (result.status is CallbackStatus.SUCCESS) {
            val images = result.images
            if (images.isEmpty()) {
                binding?.emptyText?.let {
                    it.visibility = View.VISIBLE
                }
            } else {
                binding?.emptyText?.let {
                    it.visibility = View.GONE
                }
            }
            folders = ImageHelper.folderListFromImages(result.images)
            folderAdapter.init(folders)
            folderAdapter.notifyDataSetChanged()
            updateHandledFolders()
            selectorRV?.let {
                it.visibility = View.VISIBLE
            }
        }
        loader?.let {
            it.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
        }
    }

    /**
     * onResume
     * notifyDataSetChanged, rebuild the holder views to account for deleted images, folders.
     */
    override fun onResume() {
        folderAdapter.notifyDataSetChanged()
        // Pictures may have been uploaded or marked as not for upload in the meantime
        updateHandledFolders()
        super.onResume()
    }

    /**
     * Greys out the folders whose pictures have all been uploaded or marked as not for upload.
     * Only the local upload status cache is checked, so no request is sent.
     */
    private fun updateHandledFolders() {
        val checker = folderUploadStatusChecker ?: return
        if (!::folders.isInitialized || _binding == null) {
            return
        }
        val currentFolders = folders.toList()
        handledFoldersJob?.cancel()
        handledFoldersJob =
            viewLifecycleOwner.lifecycleScope.launch {
                folderAdapter.setHandledFolders(checker.findHandledFolders(currentFolders))
            }
    }

    /**
     * onDestroyView
     * clearing view binding
     */
    override fun onDestroyView() {
        parentFragmentManager.removeOnBackStackChangedListener(backStackListener)
        handledFoldersJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
