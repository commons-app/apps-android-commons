package fr.free.nrw.commons.upload.categories

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.databinding.UploadCategoriesFragmentBinding
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleKeyboardInsets
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE_CATEGORY
import io.reactivex.Notification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UploadCategoriesFragment : UploadBaseFragment(), CategoriesContract.View {
    @JvmField
    @Inject
    var presenter: CategoriesContract.UserActionListener? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null
    private var adapter: UploadCategoryAdapter? = null
    private var subscribe: Disposable? = null

    /**
     * Current media
     */
    private var media: Media? = null

    /**
     * Progress Dialog for showing background process
     */
    private var progressDialog: ProgressDialog? = null

    /**
     * WikiText from the server
     */
    private var wikiText: String? = null
    private var nearbyPlaceCategory: String? = null

    private var binding: UploadCategoriesFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = UploadCategoriesFragmentBinding.inflate(inflater, container, false)
        binding!!.llContainerButtons.handleKeyboardInsets()
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        if (bundle != null) {
            media = bundle.getParcelable("Existing_Categories")
            wikiText = bundle.getString("WikiText")
            nearbyPlaceCategory = bundle.getString(SELECTED_NEARBY_PLACE_CATEGORY)
        }
        init()
        presenter!!.getCategories().observe(
            viewLifecycleOwner
        ) { categories: List<CategoryItem>? ->
            this.setCategories(
                categories
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
                binding?.tvTitle?.text = getString(R.string.step_count,
                    callback?.getIndexInViewFlipper(this)?.plus(1) ?: 1,
                    callback?.totalNumberOfSteps ?: 1,
                    getString(R.string.categories_activity_title))
            }
        } else {
            binding!!.tvTitle.setText(R.string.edit_categories)
            binding!!.tvSubtitle.visibility = View.GONE
            binding!!.btnNext.setText(R.string.menu_save_categories)
            binding!!.btnPrevious.setText(R.string.menu_cancel_upload)
        }

        setTvSubTitle()
        binding?.let { it.tooltip.setOnClickListener {
            showAlertDialog(
                requireActivity(),
                getString(R.string.categories_activity_title),
                getString(R.string.categories_tooltip),
                getString(R.string.ok),
                null
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

        initRecyclerView()
        addTextChangeListenerToEtSearch()
    }

    private fun addTextChangeListenerToEtSearch() {
        if (binding == null) {
            return
        }
        subscribe = RxTextView.textChanges(binding!!.etSearch)
            .doOnEach { v: Notification<CharSequence?>? ->
                binding?.tilContainerSearch?.error =
                    null
            }
            .takeUntil(RxView.detaches(binding!!.etSearch))
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { filter: CharSequence -> searchForCategory(filter.toString()) },
                { t: Throwable? -> Timber.e(t) })
    }

    /**
     * Removes  the tv subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    private fun setTvSubTitle() {
        val activity: Activity? = activity
        if (activity is UploadActivity) {
            val isMultipleFileSelected = activity.isMultipleFilesSelected
            if (!isMultipleFileSelected) {
                binding!!.tvSubtitle.visibility = View.GONE
            }
        }
    }

    private fun searchForCategory(query: String) {
        presenter?.searchForCategories(query)
    }

    private fun initRecyclerView() {
    if (adapter == null) { adapter = UploadCategoryAdapter({ categoryItem: CategoryItem? ->
            presenter?.onCategoryItemClicked(categoryItem!!)
            Unit
        }, nearbyPlaceCategory)
        }
        binding?.rvCategories?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@UploadCategoriesFragment.adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.onDetachView()
        subscribe?.dispose()
    }

    override fun showProgress(shouldShow: Boolean) {
        binding?.pbCategories?.setVisibility(if (shouldShow) View.VISIBLE else View.GONE)
    }

    override fun showError(error: String?) {
        binding?.tilContainerSearch?.error = error
    }

    override fun showError(stringResourceId: Int) {
        binding?.tilContainerSearch?.error = getString(stringResourceId)
    }

    override fun showErrorDialog(message: String) {
        AlertDialog
            .Builder(requireContext())
            .setMessage(getString(R.string.error_loading_categories) + "\n" + message)
            .setCancelable(false)
            .setNegativeButton(R.string.ok){_,_ -> }
            .show()
    }

    override fun setCategories(categories: List<CategoryItem>?) {
        if (adapter == null) {
            Timber.e("Adapter is null in setCategories")
            return
        }

        if (categories == null) {
            adapter!!.clear()
        } else {
            adapter!!.items = categories
        }
        adapter!!.notifyDataSetChanged()

        binding?.let {
            it.rvCategories.post {
                it.rvCategories.smoothScrollToPosition(0)
                it.rvCategories.post {
                    it.rvCategories.smoothScrollToPosition(
                    0
                )
                }
            }
        } ?: Timber.e("Binding is null in setCategories")
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
                null
            )
        } else {
            Toast.makeText(
                requireContext(), getString(R.string.no_categories_selected),
                Toast.LENGTH_SHORT
            ).show()
            presenter!!.clearPreviousSelection()
            goBackToPreviousScreen()
        }
    }

    /**
     * Gets existing categories from media
     */
    override fun getExistingCategories(): List<String>? {
        return media?.categories
    }

    /**
     * Returns required context
     */
    override fun getFragmentContext(): Context {
        return requireContext()
    }

    /**
     * Returns to previous fragment
     */
    override fun goBackToPreviousScreen() {
        fragmentManager?.popBackStack()
    }

    /**
     * Shows the progress dialog
     */
    override fun showProgressDialog() {
        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage(getString(R.string.please_wait))
        }.also {
            it.show()
        }
    }

    /**
     * Hides the progress dialog
     */
    override fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    /**
     * Refreshes the categories
     */
    override fun refreshCategories() {
        (parentFragment as MediaDetailFragment?)?.updateCategories()
    }

    /**
     *
     */
    override fun navigateToLoginScreen() {
        val username = sessionManager!!.userName
        val logoutListener = CommonsApplication.BaseLogoutListener(
            requireActivity(),
            requireActivity().getString(R.string.invalid_login_message),
            username
        )

        instance.clearApplicationData(
            requireActivity(), logoutListener
        )
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
            adapter!!.items = null
            val mediaDetailFragment = parentFragment as? MediaDetailFragment?: return
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

    /**
     * Hides the action bar while opening editing fragment
     */
    override fun onResume() {
        super.onResume()

        if (media != null) {
            binding?.etSearch?.setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent? ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    binding!!.etSearch.clearFocus()
                    presenter!!.clearPreviousSelection()
                    val mediaDetailFragment =
                        checkNotNull(parentFragment as MediaDetailFragment?)
                    mediaDetailFragment.onResume()
                    goBackToPreviousScreen()
                    return@setOnKeyListener true
                }
                false
            }

            requireView().isFocusableInTouchMode = true
            requireView().requestFocus()
            requireView().setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent ->
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    presenter!!.clearPreviousSelection()
                    val mediaDetailFragment =
                        checkNotNull(parentFragment as MediaDetailFragment?)
                    mediaDetailFragment.onResume()
                    goBackToPreviousScreen()
                    return@setOnKeyListener true
                }
                false
            }

            (requireActivity() as AppCompatActivity).supportActionBar?.hide()


            if (parentFragment?.parentFragment?.parentFragment is ContributionsFragment) {
                ((parentFragment?.parentFragment?.parentFragment) as ContributionsFragment).binding?.cardViewNearby?.visibility = View.GONE
            }
        }
    }

    /**
     * Shows the action bar while closing editing fragment
     */
    override fun onStop() {
        super.onStop()
        if (media != null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
