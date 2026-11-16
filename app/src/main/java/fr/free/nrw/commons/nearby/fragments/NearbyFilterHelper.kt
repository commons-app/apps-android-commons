package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.PlaceTypeLabelsClient
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.LayoutUtils.getScreenWidth
import fr.free.nrw.commons.utils.LayoutUtils.setLayoutHeightAlignedToWidth
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val FILTER_LIST_HEIGHT_TO_WIDTH_RATIO = 1.25
private const val FILTER_LIST_WIDTH_FRACTION = 0.75
private const val FILTER_SEARCH_DEBOUNCE_MS = 500L

/**
 * Manages place type filtering, search view query debouncing, tri-state checkboxes,
 * and advanced SPARQL query toggles for NearbyParentFragment.
 */
class NearbyFilterHelper : NearbyParentFragmentContract.Filter {
    /**
     * Interface defining callbacks for fragment-level actions triggered by filter interactions.
     */
    interface NearbyFilterActions {
        /** Collapses or hides the bottom sheet when the filter list appears. */
        fun hideBottomSheet()

        /** Returns true if dark mode is active. */
        fun isDarkTheme(): Boolean

        /** Toggles the fragment options menu visibility when showing advanced queries. */
        fun setHasOptionsMenu(hasMenu: Boolean)
    }

    /**
     * Encapsulates external clients and presenters required by [NearbyFilterHelper].
     */
    data class NearbyFilterDependencies(
        val placeTypeLabelsClientProvider: () -> PlaceTypeLabelsClient?,
        val compositeDisposableProvider: () -> CompositeDisposable?,
        val presenterProvider: () -> NearbyParentFragmentContract.UserActions?,
    )

    @VisibleForTesting
    var nearbyFilterSearchRecyclerViewAdapter: NearbyFilterSearchRecyclerViewAdapter? = null

    @VisibleForTesting
    var searchQueryDisposable: Disposable? = null

    @VisibleForTesting
    var advancedQueryFragmentVisible: Boolean = false

    @VisibleForTesting
    var bindingProvider: (() -> FragmentNearbyParentBinding?)? = null

    @VisibleForTesting
    var contextProvider: (() -> Context?)? = null

    @VisibleForTesting
    var activityProvider: (() -> Activity?)? = null

    @VisibleForTesting
    var dependencies: NearbyFilterDependencies? = null

    @VisibleForTesting
    var actions: NearbyFilterActions? = null

    /**
     * Initializes filter views, search view listeners, RecyclerView adapters, and callbacks.
     */
    fun init(
        bindingProvider: () -> FragmentNearbyParentBinding?,
        contextProvider: () -> Context?,
        activityProvider: () -> Activity?,
        dependencies: NearbyFilterDependencies,
        actions: NearbyFilterActions,
    ) {
        this.bindingProvider = bindingProvider
        this.contextProvider = contextProvider
        this.activityProvider = activityProvider
        this.dependencies = dependencies
        this.actions = actions

        val binding = bindingProvider()
        val context = contextProvider()
        val activity = activityProvider()
        if (binding == null || context == null || activity == null) {
            return
        }

        setupFilterViews(binding, actions, dependencies.presenterProvider)
        setupRecyclerView(this, binding, context, activity)
        setupSearchViewDebounce(this, binding)
    }

    override fun isNearbyFilterListVisible(): Boolean =
        bindingProvider
            ?.invoke()
            ?.nearbyFilterList
            ?.root
            ?.visibility == View.VISIBLE

    override fun hideNearbyFilterList() {
        val binding = bindingProvider?.invoke() ?: return
        binding.nearbyFilterList.root.visibility = View.GONE
        binding.nearbyFilter.searchViewLayout.searchView
            .clearFocus()
    }

    override fun setRecyclerViewAdapterAllSelected() {
        val adapter = nearbyFilterSearchRecyclerViewAdapter
        if (adapter != null && NearbyController.currentLocation != null) {
            adapter.setRecyclerViewAdapterAllSelected()
            updateFilterBadge(bindingProvider?.invoke(), adapter)
        }
    }

    override fun setRecyclerViewAdapterItemsGreyedOut() {
        val adapter = nearbyFilterSearchRecyclerViewAdapter
        if (adapter != null && NearbyController.currentLocation != null) {
            adapter.setRecyclerViewAdapterItemsGreyedOut()
            updateFilterBadge(bindingProvider?.invoke(), adapter)
        }
    }

    override fun setCheckBoxAction() {
        val binding = bindingProvider?.invoke() ?: return
        binding.nearbyFilterList.checkboxTriStates.addAction()
        binding.nearbyFilterList.checkboxTriStates.state = CheckBoxTriStates.UNKNOWN
    }

    override fun setCheckBoxState(state: Int) {
        val binding = bindingProvider?.invoke() ?: return
        binding.nearbyFilterList.checkboxTriStates.state = state
    }

    override fun setFilterState() {
        if (NearbyController.currentLocation != null) {
            val adapter = nearbyFilterSearchRecyclerViewAdapter ?: return
            val binding = bindingProvider?.invoke() ?: return
            dependencies?.presenterProvider?.invoke()?.filterByMarkerType(
                adapter.selectedLabels,
                binding.nearbyFilterList.checkboxTriStates.state,
                true,
                false,
            )
        }
    }

    override fun isAdvancedQueryFragmentVisible(): Boolean = advancedQueryFragmentVisible

    override fun showHideAdvancedQueryFragment(shouldShow: Boolean) {
        actions?.setHasOptionsMenu(!shouldShow)
        bindingProvider?.invoke()?.flContainerNearbyChildren?.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
        advancedQueryFragmentVisible = shouldShow
    }
}

/**
 * Drops a custom Nearby query and loads places for the current map center again.
 * The advanced query editor stays open so the restored query text remains visible.
 */
fun NearbyFilterHelper.resetAdvancedQuery(presenterOverride: NearbyParentFragmentContract.UserActions? = null) {
    val presenter = presenterOverride ?: dependencies?.presenterProvider?.invoke() ?: return
    presenter.setAdvancedQuery("")
    presenter.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
}

private fun setupFilterViews(
    binding: FragmentNearbyParentBinding,
    actions: NearbyFilterHelper.NearbyFilterActions,
    presenterProvider: (() -> NearbyParentFragmentContract.UserActions?)?,
) {
    binding.nearbyFilterList.root.visibility = View.GONE
    actions.hideBottomSheet()
    binding.nearbyFilter.searchViewLayout.searchView.apply {
        setIconifiedByDefault(false)
        isIconified = false
        setQuery("", false)
        clearFocus()
        imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
    }
    binding.nearbyFilter.searchViewLayout.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
        setLayoutHeightAlignedToWidth(
            FILTER_LIST_HEIGHT_TO_WIDTH_RATIO,
            binding.nearbyFilterList.root,
        )
        if (hasFocus) {
            binding.nearbyFilterList.root.visibility = View.VISIBLE
            presenterProvider?.invoke()?.searchViewGainedFocus()
        } else {
            binding.nearbyFilterList.root.visibility = View.GONE
        }
    }
    binding.nearbyFilterList.checkboxTriStates.setCallback { o, state, b, b1 ->
        presenterProvider?.invoke()?.filterByMarkerType(o, state, b, b1)
    }
}

private fun setupRecyclerView(
    helper: NearbyFilterHelper,
    binding: FragmentNearbyParentBinding,
    context: Context,
    activity: Activity,
) {
    binding.nearbyFilterList.searchListView.setHasFixedSize(true)
    binding.nearbyFilterList.searchListView.addItemDecoration(
        DividerItemDecoration(context, DividerItemDecoration.VERTICAL),
    )
    val linearLayoutManager = LinearLayoutManager(activity)
    linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
    binding.nearbyFilterList.searchListView.layoutManager = linearLayoutManager

    val adapter =
        NearbyFilterSearchRecyclerViewAdapter(
            context,
            ArrayList(Label.valuesAsList()),
            binding.nearbyFilterList.searchListView,
        )
    adapter.setCallback(
        object : NearbyFilterSearchRecyclerViewAdapter.Callback {
            override fun setCheckboxUnknown() {
                helper.dependencies
                    ?.presenterProvider
                    ?.invoke()
                    ?.setCheckboxUnknown()
            }

            override fun filterByMarkerType(
                selectedLabels: ArrayList<Label>,
                i: Int,
                b: Boolean,
                b1: Boolean,
            ) {
                helper.dependencies
                    ?.presenterProvider
                    ?.invoke()
                    ?.filterByMarkerType(selectedLabels, i, b, b1)
                updateFilterBadge(binding, adapter)
            }

            override fun isDarkTheme(): Boolean = helper.actions?.isDarkTheme() ?: false
        },
    )
    helper.nearbyFilterSearchRecyclerViewAdapter = adapter
    restoreStoredFilterSelection(adapter, binding)
    loadLocalizedPlaceTypeNames(
        helper.dependencies?.placeTypeLabelsClientProvider?.invoke(),
        helper.dependencies?.compositeDisposableProvider?.invoke(),
        adapter,
    )
    binding.nearbyFilterList.root.layoutParams.width =
        getScreenWidth(activity, FILTER_LIST_WIDTH_FRACTION).toInt()
    binding.nearbyFilterList.searchListView.adapter = adapter
    setLayoutHeightAlignedToWidth(FILTER_LIST_HEIGHT_TO_WIDTH_RATIO, binding.nearbyFilterList.root)
}

private fun setupSearchViewDebounce(
    helper: NearbyFilterHelper,
    binding: FragmentNearbyParentBinding,
) {
    helper.searchQueryDisposable?.dispose()
    helper.searchQueryDisposable =
        binding.nearbyFilter.searchViewLayout.searchView
            .queryTextChanges()
            .debounce(FILTER_SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { query: CharSequence ->
                helper.nearbyFilterSearchRecyclerViewAdapter?.filter?.filter(query.toString())
            }
}

/**
 * Updates the filter count badge based on currently selected labels.
 */
internal fun updateFilterBadge(
    binding: FragmentNearbyParentBinding?,
    adapter: NearbyFilterSearchRecyclerViewAdapter?,
) {
    val count = adapter?.selectedLabels?.size ?: 0
    val total = Label.values().size
    binding?.nearbyFilter?.searchViewLayout?.filterCount?.apply {
        // all or none selected = effectively no filter
        visibility = if (count in 1 until total) View.VISIBLE else View.GONE
        text = count.toString()
    }
}

/**
 * Restores stored place type filter selections from NearbyFilterState.
 */
internal fun restoreStoredFilterSelection(
    adapter: NearbyFilterSearchRecyclerViewAdapter?,
    binding: FragmentNearbyParentBinding?,
) {
    if (adapter == null) return
    val savedLabels = ArrayList(NearbyFilterState.getInstance().selectedLabels)
    adapter.selectedLabels.clear()
    val savedSet = savedLabels.toSet()
    Label.valuesAsList().forEach { label ->
        val isSelected = savedSet.contains(label)
        label.setSelected(isSelected)
        if (isSelected) {
            adapter.selectedLabels.add(label)
        }
    }
    NearbyFilterState.setSelectedLabels(ArrayList(adapter.selectedLabels))
    adapter.notifyDataSetChanged()
    updateFilterBadge(binding, adapter)
}

/**
 * Asynchronously loads localized place type names from Wikidata.
 */
internal fun loadLocalizedPlaceTypeNames(
    client: PlaceTypeLabelsClient?,
    compositeDisposable: CompositeDisposable?,
    adapter: NearbyFilterSearchRecyclerViewAdapter?,
) {
    if (client == null || compositeDisposable == null) return
    compositeDisposable.add(
        client
            .getLabels(Locale.getDefault().language)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ names ->
                Label.setLocalizedNames(names)
                adapter?.notifyDataSetChanged()
            }, { throwable ->
                Timber.e(throwable, "Failed to load localized place type names")
            }),
    )
}

/**
 * Applies theme-based styling (dark/light) to the nearby filter list views.
 */
fun NearbyFilterHelper.applyTheme(isDarkTheme: Boolean) {
    val binding = bindingProvider?.invoke() ?: return
    val context = contextProvider?.invoke() ?: return
    val textColor =
        if (isDarkTheme) {
            context.resources.getColor(R.color.white)
        } else {
            context.resources.getColor(R.color.contributionListDarkBackground)
        }
    val backgroundColor =
        if (isDarkTheme) {
            context.resources.getColor(R.color.contributionListDarkBackground)
        } else {
            context.resources.getColor(R.color.white)
        }
    binding.nearbyFilterList.checkboxTriStates.setTextColor(textColor)
    binding.nearbyFilterList.root.setBackgroundColor(backgroundColor)
}

/**
 * Disposes of search query subscriptions on view destruction to avoid memory leaks.
 */
fun NearbyFilterHelper.onDestroyView() {
    searchQueryDisposable?.dispose()
    searchQueryDisposable = null
}
