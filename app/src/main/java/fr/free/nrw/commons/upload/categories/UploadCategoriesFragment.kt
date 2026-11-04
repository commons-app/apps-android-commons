package fr.free.nrw.commons.upload.categories

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.databinding.UploadCategoriesFragmentBinding
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleKeyboardInsets
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE_CATEGORY
import javax.inject.Inject

/** Coordinates upload navigation while search and edit views own their UI behavior. */
class UploadCategoriesFragment(
    private val searchView: CategorySearchView = CategorySearchView(),
    private val editView: CategoryEditView = CategoryEditView(),
) : UploadBaseFragment(),
    CategoriesContract.View,
    CategoriesContract.SearchView by searchView,
    CategoriesContract.EditView by editView {
    init {
        searchView.fragment = this
        editView.fragment = this
        lifecycle.addObserver(editView)
    }

    @JvmField
    @Inject
    var presenter: CategoriesContract.UserActionListener? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    /**
     * Current media
     */
    internal var media: Media? = null

    /**
     * WikiText from the server
     */
    private var wikiText: String? = null
    internal var nearbyPlaceCategory: String? = null

    internal var binding: UploadCategoriesFragmentBinding? = null

    internal val onBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (media != null) {
                    binding?.etSearch?.clearFocus()
                    presenter?.clearPreviousSelection()
                    val mediaDetailFragment = checkNotNull(parentFragment as MediaDetailFragment?)
                    mediaDetailFragment.onResume()
                    goBackToPreviousScreen()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = UploadCategoriesFragmentBinding.inflate(inflater, container, false)
        binding!!.llContainerButtons.handleKeyboardInsets()
        return binding!!.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Categories")
            wikiText = bundle.getString("WikiText")
            nearbyPlaceCategory = bundle.getString(SELECTED_NEARBY_PLACE_CATEGORY)
        }
        init()
        presenter!!.getCategories().observe(
            viewLifecycleOwner,
        ) { categories: List<CategoryItem>? ->
            this.setCategories(
                categories,
            )
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun init() {
        if (binding == null) {
            return
        }
        if (media == null) {
            if (callback != null) {
                binding?.tvTitle?.text =
                    getString(
                        R.string.step_count,
                        callback?.getIndexInViewFlipper(this)?.plus(1) ?: 1,
                        callback?.totalNumberOfSteps ?: 1,
                        getString(R.string.categories_activity_title),
                    )
            }
        } else {
            binding!!.tvTitle.setText(R.string.edit_categories)
            binding!!.tvSubtitle.visibility = View.GONE
            binding!!.btnNext.setText(R.string.menu_save_categories)
            binding!!.btnPrevious.setText(R.string.menu_cancel_upload)
        }

        searchView.setTvSubTitle()
        binding?.let {
            it.tooltip.setOnClickListener {
                showAlertDialog(
                    requireActivity(),
                    getString(R.string.categories_activity_title),
                    getString(R.string.categories_tooltip),
                    getString(R.string.ok),
                    null,
                )
            }
        }
        if (media == null) {
            presenter?.onAttachView(this)
        } else {
            presenter?.onAttachViewWithMedia(this, media!!)
        }
        binding!!.btnNext.setOnClickListener { v: View? -> onNextButtonClicked() }
        binding!!.btnPrevious.setOnClickListener { v: View? -> onPreviousButtonClicked() }

        searchView.initRecyclerView()
        searchView.addTextChangeListenerToEtSearch()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.onDetachView()
        searchView.dispose()
    }

    override fun goToNextScreen() {
        callback?.let { it.onNextButtonClicked(it.getIndexInViewFlipper(this)) }
    }

    override fun showNoCategorySelected() {
        if (media == null) {
            showAlertDialog(
                requireActivity(),
                getString(R.string.no_categories_selected),
                getString(R.string.no_categories_selected_warning_desc),
                getString(R.string.continue_message),
                getString(R.string.cancel),
                { this.goToNextScreen() },
                null,
            )
        } else {
            Toast
                .makeText(
                    requireContext(),
                    getString(R.string.no_categories_selected),
                    Toast.LENGTH_SHORT,
                ).show()
            presenter!!.clearPreviousSelection()
            goBackToPreviousScreen()
        }
    }

    fun onNextButtonClicked() {
        if (media != null) {
            presenter?.updateCategories(media!!, wikiText!!)
        } else {
            presenter!!.verifyCategories()
        }
    }

    fun onPreviousButtonClicked() {
        if (media != null) {
            presenter!!.clearPreviousSelection()
            searchView.adapter!!.items = null
            val mediaDetailFragment = parentFragment as? MediaDetailFragment ?: return
            mediaDetailFragment.onResume()
            goBackToPreviousScreen()
        } else {
            callback?.let { it.onPreviousButtonClicked(it.getIndexInViewFlipper(this)) }
        }
    }

    override fun onBecameVisible() {
        super.onBecameVisible()
        if (binding == null) {
            return
        }
        presenter!!.selectCategories()
        val text = binding!!.etSearch.text
        if (text != null) {
            presenter!!.searchForCategories(text.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
