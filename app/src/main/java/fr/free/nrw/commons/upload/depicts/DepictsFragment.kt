package fr.free.nrw.commons.upload.depicts

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.databinding.UploadDepictsFragmentBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE
import io.reactivex.Notification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment for showing depicted items list in Upload activity after media details
 */
class DepictsFragment : UploadBaseFragment(), DepictsContract.View {
    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var presenter: DepictsContract.UserActionListener

    @Inject
    lateinit var sessionManager: SessionManager

    private var adapter: UploadDepictsAdapter? = null
    private var subscribe: Disposable? = null
    private var media: Media? = null
    private var progressDialog: ProgressDialog? = null

    /**
     * Determines each encounter of edit depicts
     */
    private var count = 0
    private var nearbyPlace: Place? = null

    private var _binding: UploadDepictsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UploadDepictsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            media = it.getParcelable("Existing_Depicts")
            nearbyPlace = it.getParcelable(SELECTED_NEARBY_PLACE)
        }

        if (callback != null || media != null) {
            init()
            presenter.getDepictedItems().observe(viewLifecycleOwner, ::setDepictsList)
        }
    }

    /**
     * Initialize presenter and views
     */
    private fun init() {
        if (_binding == null) {
            return
        }

        if (media == null) {
            binding.depictsTitle.text =
                String.format(
                    getString(R.string.step_count), callback.getIndexInViewFlipper(
                        this
                    ) + 1,
                    callback.totalNumberOfSteps, getString(R.string.depicts_step_title)
                )
        } else {
            binding.depictsTitle.setText(R.string.edit_depictions)
            binding.depictsSubtitle.visibility = View.GONE
            binding.depictsNext.setText(R.string.menu_save_categories)
            binding.depictsPrevious.setText(R.string.menu_cancel_upload)
        }

        setDepictsSubTitle()
        binding.tooltip.setOnClickListener { v: View? ->
            showAlertDialog(
                requireActivity(),
                getString(R.string.depicts_step_title),
                getString(R.string.depicts_tooltip),
                getString(android.R.string.ok),
                null
            )
        }
        if (media == null) {
            presenter.onAttachView(this)
        } else {
            presenter.onAttachViewWithMedia(this, media!!)
        }
        initRecyclerView()
        addTextChangeListenerToSearchBox()

        binding.depictsNext.setOnClickListener { v: View? -> onNextButtonClicked() }
        binding.depictsPrevious.setOnClickListener { v: View? -> onPreviousButtonClicked() }
    }

    /**
     * Removes the depicts subtitle If the activity is the instance of [UploadActivity] and
     * if multiple files aren't selected.
     */
    private fun setDepictsSubTitle() {
        val activity: Activity? = activity
        if (activity is UploadActivity) {
            val isMultipleFileSelected = activity.isMultipleFilesSelected
            if (!isMultipleFileSelected) {
                binding.depictsSubtitle.visibility = View.GONE
            }
        }
    }

    /**
     * Initialise recyclerView and set adapter
     */
    private fun initRecyclerView() {
        adapter = if (media == null) {
            UploadDepictsAdapter({ categoryItem: DepictedItem? ->
                presenter.onDepictItemClicked(categoryItem!!)
            }, nearbyPlace)
        } else {
            UploadDepictsAdapter({ item: DepictedItem? ->
                presenter.onDepictItemClicked(item!!)
            }, nearbyPlace)
        }
        if (_binding == null) {
            return
        }
        binding.depictsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.depictsRecyclerView.adapter = adapter
    }

    override fun onBecameVisible() {
        super.onBecameVisible()
        // Select Place depiction as the fragment becomes visible to ensure that the most up to date
        // Place is used (i.e. if the user accepts a nearby place dialog)
        presenter.selectPlaceDepictions()
    }

    override fun goToNextScreen() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this))
    }

    override fun goToPreviousScreen() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this))
    }

    override fun noDepictionSelected() {
        if (media == null) {
            showAlertDialog(
                requireActivity(),
                getString(R.string.no_depictions_selected),
                getString(R.string.no_depictions_selected_warning_desc),
                getString(R.string.continue_message),
                getString(R.string.cancel),
                { goToNextScreen() },
                null
            )
        } else {
            Toast.makeText(
                requireContext(), getString(R.string.no_depictions_selected),
                Toast.LENGTH_SHORT
            ).show()
            presenter.clearPreviousSelection()
            updateDepicts()
            goBackToPreviousScreen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        media = null
        presenter.onDetachView()
        subscribe!!.dispose()
    }

    override fun showProgress(shouldShow: Boolean) {
        if (_binding == null) {
            return
        }
        binding.depictsSearchInProgress.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun showError(value: Boolean) {
        if (_binding == null) {
            return
        }
        if (value) {
            binding.depictsSearchContainer.error =
                getString(R.string.no_depiction_found)
        } else {
            binding.depictsSearchContainer.isErrorEnabled = false
        }
    }

    override fun setDepictsList(depictedItemList: List<DepictedItem>) {
        if (applicationKvStore.getBoolean("first_edit_depict")) {
            count = 1
            applicationKvStore.putBoolean("first_edit_depict", false)
            adapter!!.items = depictedItemList
        } else {
            if ((count == 0) && (!depictedItemList.isEmpty())) {
                adapter!!.items = null
                count = 1
            } else {
                adapter!!.items = depictedItemList
            }
        }

        if (_binding == null) {
            return
        }
        // Nested waiting for search result data to load into the depicted item
        // list and smoothly scroll to the top of the search result list.
        binding.depictsRecyclerView.post {
            binding.depictsRecyclerView.smoothScrollToPosition(0)
            binding.depictsRecyclerView.post {
                binding.depictsRecyclerView.smoothScrollToPosition(
                    0
                )
            }
        }
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
     * Gets existing depictions IDs from media
     */
    override fun getExistingDepictions(): List<String>? {
        return if ((media == null)) null else media!!.depictionIds
    }

    /**
     * Shows the progress dialog
     */
    override fun showProgressDialog() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog!!.setMessage(getString(R.string.please_wait))
        progressDialog!!.show()
    }

    /**
     * Hides the progress dialog
     */
    override fun dismissProgressDialog() {
        progressDialog?.dismiss()
    }

    /**
     * Update the depicts
     */
    override fun updateDepicts() {
        (parentFragment as MediaDetailFragment?)?.onResume()
    }

    /**
     * Navigates to the login Activity
     */
    override fun navigateToLoginScreen() {
        val username = sessionManager.userName
        val logoutListener = CommonsApplication.BaseLogoutListener(
            requireActivity(),
            requireActivity().getString(R.string.invalid_login_message),
            username
        )

        instance.clearApplicationData(
            requireActivity(), logoutListener
        )
    }

    /**
     * Determines the calling fragment by media nullability and act accordingly
     */
    fun onNextButtonClicked() {
        if (media != null) {
            presenter.updateDepictions(media!!)
        } else {
            presenter.verifyDepictions()
        }
    }

    /**
     * Determines the calling fragment by media nullability and act accordingly
     */
    fun onPreviousButtonClicked() {
        if (media != null) {
            presenter.clearPreviousSelection()
            updateDepicts()
            goBackToPreviousScreen()
        } else {
            callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this))
        }
    }

    /**
     * Text change listener for the edit text view of depicts
     */
    private fun addTextChangeListenerToSearchBox() {
        subscribe = RxTextView.textChanges(binding.depictsSearch)
            .doOnEach { v: Notification<CharSequence?>? ->
                binding.depictsSearchContainer.error =
                    null
            }
            .takeUntil(RxView.detaches(binding.depictsSearch))
            .debounce(500, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { filter: CharSequence -> searchForDepictions(filter.toString()) },
                { t: Throwable? -> Timber.e(t) })
    }

    /**
     * Search for depictions for the following query
     *
     * @param query query string
     */
    private fun searchForDepictions(query: String) {
        presenter.searchForDepictions(query)
    }


    /**
     * Hides the action bar while opening editing fragment
     */
    override fun onResume() {
        super.onResume()

        if (media != null) {
            binding.depictsSearch.setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent? ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    binding.depictsSearch.clearFocus()
                    presenter.clearPreviousSelection()
                    updateDepicts()
                    goBackToPreviousScreen()
                    return@setOnKeyListener true
                }
                false
            }

            requireView().isFocusableInTouchMode = true
            requireView().requestFocus()
            requireView().setOnKeyListener { v: View?, keyCode: Int, event: KeyEvent ->
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    presenter.clearPreviousSelection()
                    updateDepicts()
                    goBackToPreviousScreen()
                    return@setOnKeyListener true
                }
                false
            }

            (requireActivity() as AppCompatActivity).supportActionBar?.hide()

            if (parentFragment?.parentFragment?.parentFragment is ContributionsFragment) {
                ((parentFragment?.parentFragment?.parentFragment) as ContributionsFragment?)?.binding?.cardViewNearby?.setVisibility(View.GONE)
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
        _binding = null
    }
}
