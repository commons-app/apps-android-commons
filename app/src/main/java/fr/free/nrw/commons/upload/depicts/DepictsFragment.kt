package fr.free.nrw.commons.upload.depicts

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleKeyboardInsets
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
 *
 * The view contract is implemented by [DepictsProgressViewHandler],
 * [DepictsNavigationViewHandler] and [DepictsDataViewHandler] through delegation. The fragment
 * keeps a public no-argument constructor because [fragmentRef] has a default value.
 *
 * @property fragmentRef gives the delegated handlers access to this fragment
 */
class DepictsFragment(
    private val fragmentRef: DepictsFragmentRef = DepictsFragmentRef(),
) : UploadBaseFragment(),
    DepictsContract.View,
    DepictsContract.ProgressView by DepictsProgressViewHandler(fragmentRef),
    DepictsContract.NavigationView by DepictsNavigationViewHandler(fragmentRef),
    DepictsContract.DataView by DepictsDataViewHandler(fragmentRef) {
    init {
        fragmentRef.fragment = this
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 500L
    }

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var presenter: DepictsContract.UserActionListener

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var locationManager: LocationServiceManager

    internal var adapter: UploadDepictsAdapter? = null
    private var subscribe: Disposable? = null
    internal var media: Media? = null
    internal var progressDialog: ProgressDialog? = null

    /**
     * Determines each encounter of edit depicts
     */
    internal var count = 0
    private var nearbyPlace: Place? = null

    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: UploadDepictsFragmentBinding? = null
    internal val binding get() = _binding!!
    internal val hasBinding get() = _binding != null

    private val mapPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data
                    ?.getParcelableExtra<Place>(NearbyParentFragment.PICKER_RESULT_PLACE)
                    ?.let { presenter.onPlaceSelectedFromMap(it) }
            }
        }

    private val onBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (media != null) {
                    binding.depictsSearch.clearFocus()
                    presenter.clearPreviousSelection()
                    updateDepicts()
                    goBackToPreviousScreen()
                }
            }
        }

    private val mapPickerHandler = DepictsMapPickerHandler()
    private val searchViewHandler = DepictsSearchViewHandler()
    private val uiHandler = DepictsUIHandler()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = UploadDepictsFragmentBinding.inflate(inflater, container, false)
        _binding!!.navigationButtonsContainer.handleKeyboardInsets()
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            media = it.getParcelable("Existing_Depicts")
            nearbyPlace = it.getParcelable(SELECTED_NEARBY_PLACE)
        }

        if (callback != null || media != null) {
            uiHandler.init()
            presenter.getDepictedItems().observe(viewLifecycleOwner, ::setDepictsList)
        }
    }

    override fun onBecameVisible() {
        super.onBecameVisible()
        // Select Place depiction as the fragment becomes visible to ensure that the most up to date
        // Place is used (i.e. if the user accepts a nearby place dialog)
        presenter.selectPlaceDepictions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        media = null
        presenter.onDetachView()
        subscribe?.dispose()
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
            callback!!.onPreviousButtonClicked(callback!!.getIndexInViewFlipper(this))
        }
    }

    /**
     * Hides the action bar while opening editing fragment
     */
    override fun onResume() {
        super.onResume()

        if (media != null) {
            onBackPressedCallback.isEnabled = true

            (requireActivity() as AppCompatActivity).supportActionBar?.hide()

            if (parentFragment?.parentFragment?.parentFragment is ContributionsFragment) {
                ((parentFragment?.parentFragment?.parentFragment) as ContributionsFragment?)
                    ?.binding
                    ?.cardViewNearby
                    ?.setVisibility(
                        View.GONE,
                    )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        onBackPressedCallback.isEnabled = false
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

    /**
     * Handles map picker support operations.
     */
    private inner class DepictsMapPickerHandler {
        /**
         * Launches Depicts picker activity to select Depicts from map
         * bundles the latitude and longitude (if present)
         */
        fun launchDepictsPicker() {
            hideKeyboard()

            val allPhotoCoordinates = getPhotoCoordinates()

            if (allPhotoCoordinates.isEmpty()) {
                Toast
                    .makeText(
                        requireContext(),
                        R.string.depictions_map_no_photo_location,
                        Toast.LENGTH_LONG,
                    ).show()
            }

            Intent(requireContext(), DepictsPickerActivity::class.java).apply {
                putParcelableArrayListExtra(
                    NearbyParentFragment.ARG_PHOTO_LATLNGS,
                    allPhotoCoordinates,
                )
                mapPickerLauncher.launch(this)
            }
        }

        /**
         * Hides the soft keyboard of the host fragment's view.
         */
        private fun hideKeyboard() {
            val imm =
                view?.context?.let {
                    ContextCompat.getSystemService(
                        it,
                        InputMethodManager::class.java,
                    )
                }
            imm?.hideSoftInputFromWindow(view?.windowToken, 0)
        }

        /**
         * Collects the photo coordinates that the map picker should show.
         */
        private fun getPhotoCoordinates(): ArrayList<LatLng> {
            val allPhotoCoordinates = ArrayList<LatLng>()
            if (media == null && callback != null) {
                val coords =
                    presenter.getUploads().mapNotNull { item ->
                        item.gpsCoords?.takeIf { it.imageCoordsExists }?.latLng
                    }
                allPhotoCoordinates.addAll(coords)
            } else {
                media?.coordinates?.let { allPhotoCoordinates.add(it) }
            }
            return allPhotoCoordinates
        }
    }

    /**
     * Handles the search box of the depicts screen.
     */
    private inner class DepictsSearchViewHandler {
        /**
         * Text change listener for the edit text view of depicts
         */
        fun addTextChangeListenerToSearchBox() {
            subscribe =
                RxTextView
                    .textChanges(binding.depictsSearch)
                    .doOnEach { v: Notification<CharSequence?>? ->
                        binding.depictsSearchContainer.error =
                            null
                    }.takeUntil(RxView.detaches(binding.depictsSearch))
                    .debounce(SEARCH_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { filter: CharSequence -> searchForDepictions(filter.toString()) },
                        { t: Throwable? -> Timber.e(t) },
                    )
        }

        /**
         * Search for depictions for the following query
         *
         * @param query query string
         */
        private fun searchForDepictions(query: String) {
            presenter.searchForDepictions(query)
        }
    }

    /**
     * Handles the setup of the views of the depicts screen.
     */
    private inner class DepictsUIHandler {
        /**
         * Initialize presenter and views
         */
        fun init() {
            if (_binding == null) {
                return
            }

            if (media == null) {
                binding.depictsTitle.text =
                    String.format(
                        getString(R.string.step_count),
                        callback!!.getIndexInViewFlipper(this@DepictsFragment) + 1,
                        callback!!.totalNumberOfSteps,
                        getString(R.string.depicts_step_title),
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
                    getString(R.string.ok),
                    null,
                )
            }
            if (media == null) {
                presenter.onAttachView(this@DepictsFragment)
            } else {
                presenter.onAttachViewWithMedia(this@DepictsFragment, media!!)
            }
            initRecyclerView()
            searchViewHandler.addTextChangeListenerToSearchBox()

            binding.chooseFromMap.setOnClickListener { v: View? ->
                mapPickerHandler.launchDepictsPicker()
            }
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
            adapter =
                if (media == null) {
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
    }
}

/**
 * Gives the handlers that implement the view contract on behalf of [DepictsFragment] access to
 * the fragment. It is set when the fragment is constructed.
 */
class DepictsFragmentRef {
    /**
     * The fragment that the handlers work for
     */
    lateinit var fragment: DepictsFragment
}

/**
 * Handles the progress and error part of the view contract for [DepictsFragment].
 *
 * @property ref gives access to the fragment
 */
class DepictsProgressViewHandler(
    private val ref: DepictsFragmentRef,
) : DepictsContract.ProgressView {
    /**
     * Shows or hides the search progress indicator
     */
    override fun showProgress(shouldShow: Boolean) {
        val fragment = ref.fragment
        if (!fragment.hasBinding) {
            return
        }
        fragment.binding.depictsSearchInProgress.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    /**
     * Shows or hides the error of the search box
     */
    override fun showError(value: Boolean) {
        val fragment = ref.fragment
        if (!fragment.hasBinding) {
            return
        }
        if (value) {
            fragment.binding.depictsSearchContainer.error =
                fragment.getString(R.string.no_depiction_found)
        } else {
            fragment.binding.depictsSearchContainer.isErrorEnabled = false
        }
    }

    /**
     * Shows the progress dialog
     */
    override fun showProgressDialog() {
        val fragment = ref.fragment
        fragment.progressDialog = ProgressDialog(fragment.requireContext())
        fragment.progressDialog!!.setMessage(fragment.getString(R.string.please_wait))
        fragment.progressDialog!!.setCancelable(false)
        fragment.progressDialog!!.show()
    }

    /**
     * Hides the progress dialog
     */
    override fun dismissProgressDialog() {
        ref.fragment.progressDialog?.dismiss()
    }
}

/**
 * Handles the navigation part of the view contract for [DepictsFragment].
 *
 * @property ref gives access to the fragment
 */
class DepictsNavigationViewHandler(
    private val ref: DepictsFragmentRef,
) : DepictsContract.NavigationView {
    /**
     * Goes to the next step of the upload flow
     */
    override fun goToNextScreen() {
        val fragment = ref.fragment
        fragment.callback!!.onNextButtonClicked(fragment.callback!!.getIndexInViewFlipper(fragment))
    }

    /**
     * Goes to the previous step of the upload flow
     */
    override fun goToPreviousScreen() {
        val fragment = ref.fragment
        fragment.callback!!.onPreviousButtonClicked(
            fragment.callback!!.getIndexInViewFlipper(fragment),
        )
    }

    /**
     * Warns the user or leaves the screen when no depiction is selected
     */
    override fun noDepictionSelected() {
        val fragment = ref.fragment
        if (fragment.media == null) {
            showAlertDialog(
                fragment.requireActivity(),
                fragment.getString(R.string.no_depictions_selected),
                fragment.getString(R.string.no_depictions_selected_warning_desc),
                fragment.getString(R.string.continue_message),
                fragment.getString(R.string.cancel),
                { goToNextScreen() },
                null,
            )
        } else {
            Toast
                .makeText(
                    fragment.requireContext(),
                    fragment.getString(R.string.no_depictions_selected),
                    Toast.LENGTH_SHORT,
                ).show()
            fragment.presenter.clearPreviousSelection()
            fragment.updateDepicts()
            goBackToPreviousScreen()
        }
    }

    /**
     * Returns to previous fragment
     */
    override fun goBackToPreviousScreen() {
        ref.fragment.fragmentManager?.popBackStack()
    }

    /**
     * Navigates to the login Activity
     */
    override fun navigateToLoginScreen() {
        val fragment = ref.fragment
        val username = fragment.sessionManager.userName
        val logoutListener =
            CommonsApplication.BaseLogoutListener(
                fragment.requireActivity(),
                fragment.requireActivity().getString(R.string.invalid_login_message),
                username,
            )

        instance.clearApplicationData(
            fragment.requireActivity(),
            logoutListener,
        )
    }
}

/**
 * Handles the data part of the view contract for [DepictsFragment].
 *
 * @property ref gives access to the fragment
 */
class DepictsDataViewHandler(
    private val ref: DepictsFragmentRef,
) : DepictsContract.DataView {
    /**
     * Shows the given depicted items in the list
     */
    override fun setDepictsList(depictedItemList: List<DepictedItem>) {
        val fragment = ref.fragment
        if (fragment.applicationKvStore.getBoolean("first_edit_depict")) {
            fragment.count = 1
            fragment.applicationKvStore.putBoolean("first_edit_depict", false)
            fragment.adapter!!.items = depictedItemList
        } else {
            if ((fragment.count == 0) && (!depictedItemList.isEmpty())) {
                fragment.adapter!!.items = null
                fragment.count = 1
            } else {
                fragment.adapter!!.items = depictedItemList
            }
        }

        if (!fragment.hasBinding) {
            return
        }
        // Nested waiting for search result data to load into the depicted item
        // list and smoothly scroll to the top of the search result list.
        fragment.binding.depictsRecyclerView.post {
            fragment.binding.depictsRecyclerView.smoothScrollToPosition(0)
            fragment.binding.depictsRecyclerView.post {
                fragment.binding.depictsRecyclerView.smoothScrollToPosition(
                    0,
                )
            }
        }
    }

    /**
     * Returns required context
     */
    override fun getFragmentContext(): Context = ref.fragment.requireContext()

    /**
     * Gets existing depictions IDs from media
     */
    override fun getExistingDepictions(): List<String>? =
        if ((ref.fragment.media == null)) null else ref.fragment.media!!.depictionIds

    /**
     * Update the depicts
     */
    override fun updateDepicts() {
        (ref.fragment.parentFragment as MediaDetailFragment?)?.onResume()
    }

    /**
     * Shows the result of creating a Wikidata item for a Wikipedia article.
     */
    override fun showDepictionCreationMessage(messageRes: Int) {
        Toast
            .makeText(ref.fragment.requireContext(), ref.fragment.getString(messageRes), Toast.LENGTH_SHORT)
            .show()
    }
}
