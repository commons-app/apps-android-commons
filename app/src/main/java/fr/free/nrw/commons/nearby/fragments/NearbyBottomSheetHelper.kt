package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.nearby.BottomSheetAdapter
import fr.free.nrw.commons.nearby.NearbyUtil
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.WikidataFeedback
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.model.BottomSheetItem
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl

private const val PORTRAIT_SPAN_COUNT = 3
private const val LANDSCAPE_SPAN_COUNT = 6
private const val BOTTOM_SHEET_HEIGHT_DIVISOR = 16
private const val BOTTOM_SHEET_HEIGHT_MULTIPLIER = 9
private const val COMMONS_FILE_REDIRECT_URL = "https://commons.wikimedia.org/wiki/Special:Redirect/file/"
private const val THUMBNAIL_WIDTH_QUERY = "?width=25"
private const val PLACE_CLIPBOARD_LABEL = "place"

/**
 * Manages the list and details bottom sheets, user interactions, and place details presentation.
 */
class NearbyBottomSheetHelper : NearbyParentFragmentContract.BottomSheet {
    /**
     * Interface defining callbacks for fragment-level actions triggered by bottom sheet events.
     */
    interface NearbyBottomSheetActions {
        /** Invoked when a place marker requires camera centering. */
        fun onCameraCenterRequested(place: Place)

        /** Invoked when a place thumbnail is clicked to display full media details. */
        fun onMediaClicked(imageName: String)

        /** Invoked when the bookmark button is clicked to toggle persistence. */
        fun onBookmarkToggled(place: Place)

        /** Invoked when the details bottom sheet state changes (e.g. collapsed, expanded, hidden). */
        fun onSheetStateChanged(newState: Int)

        /** Returns true if the fragment is in depicts picker mode. */
        fun isDepictsPickerMode(): Boolean

        /** Invoked when an upload FAB (camera, gallery, custom gallery) is clicked for a place. */
        fun onUploadActionInitiated(
            place: Place,
            uploadType: UploadType,
        )

        /** Supplies the lifecycle coroutine scope for async DAO operations. */
        fun getCoroutineScope(): LifecycleCoroutineScope?

        /** Supplies the PlaceAdapter for the nearby list sheet. */
        fun getPlaceAdapter(): PlaceAdapter?
    }

    /**
     * Enumeration of upload trigger sources available on the place details sheet.
     */
    enum class UploadType {
        CAMERA,
        GALLERY,
        CUSTOM_GALLERY,
    }

    /** Backing behavior for places list bottom sheet. */
    @VisibleForTesting
    var bottomSheetListBehavior: BottomSheetBehavior<*>? = null

    /** Backing behavior for place details bottom sheet. */
    @VisibleForTesting
    var bottomSheetDetailsBehavior: BottomSheetBehavior<*>? = null

    /** Currently selected place displayed in the details sheet. */
    @VisibleForTesting
    var selectedPlace: Place? = null

    /** List of action items displayed in the details sheet recycler view. */
    @VisibleForTesting
    var dataList: MutableList<BottomSheetItem>? = null

    /** Adapter managing action items in the details sheet recycler view. */
    @VisibleForTesting
    var bottomSheetAdapter: BottomSheetAdapter? = null

    private var bindingProvider: (() -> FragmentNearbyParentBinding?)? = null
    private var contextProvider: (() -> Context?)? = null
    private var activityProvider: (() -> Activity?)? = null
    internal var bookmarkDaoProvider: (() -> BookmarkLocationsDao?)? = null
    private var actions: NearbyBottomSheetActions? = null

    /**
     * Initializes helper dependencies, behaviors, and interaction callbacks.
     *
     * @param bindingProvider Provider supplying the FragmentNearbyParentBinding
     * @param contextProvider Provider supplying Context
     * @param activityProvider Provider supplying Activity
     * @param bookmarkDaoProvider Provider supplying BookmarkLocationsDao
     * @param actions Callback interface for fragment-level delegations
     */
    fun init(
        bindingProvider: () -> FragmentNearbyParentBinding?,
        contextProvider: () -> Context?,
        activityProvider: () -> Activity?,
        bookmarkDaoProvider: () -> BookmarkLocationsDao?,
        actions: NearbyBottomSheetActions,
    ) {
        this.bindingProvider = bindingProvider
        this.contextProvider = contextProvider
        this.activityProvider = activityProvider
        this.bookmarkDaoProvider = bookmarkDaoProvider
        this.actions = actions

        val binding = bindingProvider() ?: return
        val activity = activityProvider()
        val context = contextProvider()

        configureBottomSheetBehaviors(binding, this, actions.isDepictsPickerMode())
        if (activity != null && context != null) {
            setupBottomSheetCallbacks(binding, this, activity, context, actions)
        }
    }

    /**
     * Checks whether the list bottom sheet is currently in expanded state.
     *
     * @return true if expanded, false otherwise
     */
    override fun isListBottomSheetExpanded(): Boolean =
        bottomSheetListBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    /**
     * Checks whether the details bottom sheet is currently visible (collapsed or expanded).
     *
     * @return true if visible, false otherwise
     */
    override fun isDetailsBottomSheetVisible(): Boolean =
        bottomSheetDetailsBehavior?.state != BottomSheetBehavior.STATE_HIDDEN

    /**
     * Hides the list bottom sheet completely.
     */
    override fun hideBottomSheet() {
        bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Hides the place details bottom sheet completely.
     */
    override fun hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Collapses the details sheet if expanded, or hides it if already collapsed.
     */
    override fun setBottomSheetDetailsSmaller() {
        if (bottomSheetDetailsBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /**
     * Updates the places list adapter and toggles the empty results message.
     *
     * @param placeList The list of nearby places to display
     */
    override fun updateListFragment(placeList: List<Place>) {
        val adapter = actions?.getPlaceAdapter() ?: return
        adapter.clear()
        adapter.items = placeList.filter { it.name.isNotEmpty() }
        val binding = bindingProvider?.invoke() ?: return
        binding.bottomSheetNearby.noResultsMessage.visibility =
            if (placeList.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    override fun listOptionMenuItemClicked() {
        bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        val listState = bottomSheetListBehavior?.state
        if (listState == BottomSheetBehavior.STATE_COLLAPSED ||
            listState == BottomSheetBehavior.STATE_HIDDEN
        ) {
            bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else if (listState == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    fun passInfoToSheet(place: Place) {
        selectedPlace = place
        val context = contextProvider?.invoke()
        val binding = bindingProvider?.invoke()
        val currentActions = actions
        if (context == null || binding == null || currentActions == null) {
            return
        }

        if (currentActions.isDepictsPickerMode()) {
            activityProvider?.invoke()?.let { activity ->
                bindDepictConfirmButton(binding, place, context, activity)
            }
        } else {
            bindActionItems(binding, place, context, this, currentActions)
        }
        bindPlaceThumbnail(binding, place, currentActions)
        bindPlaceTexts(binding, place)
        bindPlaceUploadFabs(binding, place, currentActions)
    }

    /**
     * Click and long-click listener for action buttons displayed in the details sheet recycler view.
     */
    val itemClickListener: BottomSheetAdapter.ItemClickListener =
        object : BottomSheetAdapter.ItemClickListener {
            override fun onBottomSheetItemClick(
                view: View?,
                position: Int,
            ) {
                val item = dataList?.getOrNull(position)
                val place = selectedPlace
                val context = contextProvider?.invoke()
                val currentActions = actions
                val hasValidData = item != null && place != null
                val hasValidDependencies = context != null && currentActions != null
                if (!hasValidData || !hasValidDependencies) {
                    return
                }
                dispatchItemClick(item, place, context, currentActions)
            }

            private fun dispatchItemClick(
                item: BottomSheetItem,
                place: Place,
                context: Context,
                currentActions: NearbyBottomSheetActions,
            ) {
                when (item.imageResourceId) {
                    R.drawable.ic_round_star_border_24px -> handleBookmarkClick(place, currentActions)
                    R.drawable.ic_round_star_filled_24px -> {
                        handleBookmarkClick(place, currentActions)
                        updateBookmarkButtonImage(place, currentActions)
                    }
                    R.drawable.ic_directions_black_24dp -> handleGeoCoordinates(context, place.location)
                    R.drawable.ic_feedback_black_24dp -> launchFeedback(context, place)
                    else -> dispatchSiteLink(context, place, item.imageResourceId)
                }
            }

            private fun handleBookmarkClick(
                place: Place,
                currentActions: NearbyBottomSheetActions,
            ) {
                currentActions.onBookmarkToggled(place)
                bottomSheetAdapter?.toggleBookmarkIcon()
            }

            private fun updateBookmarkButtonImage(
                place: Place,
                currentActions: NearbyBottomSheetActions,
            ) {
                val bookmarkDao = bookmarkDaoProvider?.invoke()
                val adapter = bottomSheetAdapter
                if (bookmarkDao != null && adapter != null) {
                    NearbyUtil.getBookmarkLocationExists(
                        bookmarkDao,
                        place.name,
                        currentActions.getCoroutineScope(),
                        adapter,
                    )
                }
            }

            private fun launchFeedback(
                context: Context,
                place: Place,
            ) {
                val intent =
                    Intent(context, WikidataFeedback::class.java).apply {
                        putExtra("lat", place.location.latitude)
                        putExtra("lng", place.location.longitude)
                        putExtra("place", place.name)
                        putExtra("qid", place.wikiDataEntityId)
                    }
                context.startActivity(intent)
            }

            private fun dispatchSiteLink(
                context: Context,
                place: Place,
                resId: Int,
            ) {
                val link =
                    when (resId) {
                        R.drawable.ic_wikidata_logo_24dp -> place.siteLinks?.wikidataLink
                        R.drawable.ic_wikipedia_logo_24dp -> place.siteLinks?.wikipediaLink
                        R.drawable.ic_commons_icon_vector -> place.siteLinks?.commonsLink
                        else -> null
                    }
                link?.let { handleWebUrl(context, it) }
            }

            override fun onBottomSheetItemLongClick(
                view: View?,
                position: Int,
            ) {
                val item = dataList?.getOrNull(position)
                val context = contextProvider?.invoke()
                if (item == null || context == null) {
                    return
                }
                val message =
                    when (item.imageResourceId) {
                        R.drawable.ic_round_star_border_24px,
                        R.drawable.ic_round_star_filled_24px,
                        -> context.getString(R.string.menu_bookmark)
                        R.drawable.ic_directions_black_24dp -> context.getString(R.string.nearby_directions)
                        R.drawable.ic_wikidata_logo_24dp -> context.getString(R.string.nearby_wikidata)
                        R.drawable.ic_feedback_black_24dp -> context.getString(R.string.nearby_report_problem)
                        R.drawable.ic_wikipedia_logo_24dp -> context.getString(R.string.nearby_wikipedia)
                        R.drawable.ic_commons_icon_vector -> context.getString(R.string.nearby_commons)
                        else -> "Long click"
                    }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
}

/**
 * a) Creates bottom sheet behaviours from bottom sheets, sets initial states and visibility b)
 * Gets the touch event on the map to perform following actions: if fab is open then close fab.
 * if bottom sheet details are expanded then collapse bottom sheet details. if bottom sheet
 * details are collapsed then hide the bottom sheet details. if listBottomSheet is open then
 * hide the list bottom sheet.
 */
private fun configureBottomSheetBehaviors(
    binding: FragmentNearbyParentBinding,
    helper: NearbyBottomSheetHelper,
    isDepictsPickerMode: Boolean,
) {
    helper.bottomSheetListBehavior =
        BottomSheetBehavior.from<View>(binding.bottomSheetNearby.bottomSheet)
    helper.bottomSheetDetailsBehavior =
        BottomSheetBehavior.from(binding.bottomSheetDetails.root)
    helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    binding.bottomSheetDetails.root.visibility = View.VISIBLE
    binding.bottomSheetNearby.root.visibility =
        if (isDepictsPickerMode) View.GONE else View.VISIBLE
    helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
}

/**
 * Defines how bottom sheets will act on click
 */
private fun setupBottomSheetCallbacks(
    binding: FragmentNearbyParentBinding,
    helper: NearbyBottomSheetHelper,
    activity: Activity,
    context: Context,
    actions: NearbyBottomSheetHelper.NearbyBottomSheetActions,
) {
    helper.bottomSheetDetailsBehavior?.setBottomSheetCallback(
        object : BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                actions.onSheetStateChanged(newState)
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) = Unit
        },
    )

    binding.bottomSheetDetails.root.setOnClickListener {
        val detailsBehavior = helper.bottomSheetDetailsBehavior ?: return@setOnClickListener
        if (detailsBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            detailsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else if (detailsBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            detailsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    val displayHeight = activity.windowManager.defaultDisplay.height
    binding.bottomSheetNearby.bottomSheet.layoutParams.height =
        displayHeight / BOTTOM_SHEET_HEIGHT_DIVISOR * BOTTOM_SHEET_HEIGHT_MULTIPLIER
    helper.bottomSheetListBehavior =
        BottomSheetBehavior.from<View>(binding.bottomSheetNearby.bottomSheet)
    helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

    helper.bottomSheetListBehavior?.setBottomSheetCallback(
        object : BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int,
            ) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    helper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float,
            ) = Unit
        },
    )

    if (actions.isDepictsPickerMode()) {
        helper.bottomSheetListBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
    }

    setupTitleActions(binding, helper, context)
}

/**
 * Configures click and long-click listeners on the details sheet title bar.
 */
private fun setupTitleActions(
    binding: FragmentNearbyParentBinding,
    helper: NearbyBottomSheetHelper,
    context: Context,
) {
    binding.bottomSheetDetails.title.setOnLongClickListener {
        val titleText =
            binding.bottomSheetDetails.title.text
                ?.toString() ?: ""
        context.copyToClipboard(PLACE_CLIPBOARD_LABEL, titleText)
        Toast.makeText(context, R.string.text_copy, Toast.LENGTH_SHORT).show()
        true
    }

    binding.bottomSheetDetails.title.setOnClickListener {
        helper.hideBottomSheet()
        val detailsBehavior = helper.bottomSheetDetailsBehavior ?: return@setOnClickListener
        if (detailsBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            detailsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            detailsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }
}

/**
 * Determines the number of spans (columns) in the RecyclerView based on device orientation
 * and adapter item count.
 *
 * @return The number of spans to be used in the RecyclerView.
 */
private fun bindActionItems(
    binding: FragmentNearbyParentBinding,
    place: Place,
    context: Context,
    helper: NearbyBottomSheetHelper,
    actions: NearbyBottomSheetHelper.NearbyBottomSheetActions,
) {
    val items = buildBottomSheetItems(place, context)
    helper.dataList = items
    val orientation = context.resources.configuration.orientation
    val spanCount =
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            PORTRAIT_SPAN_COUNT
        } else {
            if (items.size > 0) items.size else LANDSCAPE_SPAN_COUNT
        }
    binding.bottomSheetDetails.bottomSheetRecyclerView.layoutManager =
        GridLayoutManager(context, spanCount)
    val adapter = BottomSheetAdapter(context, items)
    adapter.setClickListener(helper.itemClickListener)
    helper.bottomSheetAdapter = adapter
    binding.bottomSheetDetails.bottomSheetRecyclerView.adapter = adapter

    val bookmarkDao = helper.bookmarkDaoProvider?.invoke()
    if (bookmarkDao != null) {
        NearbyUtil.getBookmarkLocationExists(
            bookmarkDao,
            place.name,
            actions.getCoroutineScope(),
            adapter,
        )
    }
}

/** Constructs the list of action items available for a place. */
private fun buildBottomSheetItems(
    place: Place,
    context: Context,
): ArrayList<BottomSheetItem> {
    val items = ArrayList<BottomSheetItem>()
    items.add(
        BottomSheetItem(
            R.drawable.ic_round_star_border_24px,
            context.getString(R.string.nearby_bookmark),
        ),
    )
    items.add(
        BottomSheetItem(
            R.drawable.ic_directions_black_24dp,
            context.getString(R.string.nearby_directions),
        ),
    )
    if (place.hasWikidataLink()) {
        items.add(
            BottomSheetItem(
                R.drawable.ic_wikidata_logo_24dp,
                context.getString(R.string.nearby_wikidata),
            ),
        )
    }
    items.add(
        BottomSheetItem(
            R.drawable.ic_feedback_black_24dp,
            context.getString(R.string.nearby_report_problem_short),
        ),
    )
    if (place.hasWikipediaLink()) {
        items.add(
            BottomSheetItem(
                R.drawable.ic_wikipedia_logo_24dp,
                context.getString(R.string.nearby_wikipedia),
            ),
        )
    }
    if (place.hasCommonsLink()) {
        items.add(
            BottomSheetItem(
                R.drawable.ic_commons_icon_vector,
                context.getString(R.string.nearby_commons),
            ),
        )
    }
    return items
}

/** Configures the depictions picker mode confirm button and peek height. */
private fun bindDepictConfirmButton(
    binding: FragmentNearbyParentBinding,
    place: Place,
    context: Context,
    activity: Activity,
) {
    // Hide the existing actions recyclerview and show the confirm button
    binding.bottomSheetDetails.bottomSheetRecyclerView.visibility = View.GONE
    binding.bottomSheetDetails.depictionsMapConfirm.visibility = View.VISIBLE
    // increase the peek height to make the confirm button visible
    val peekHeight =
        (
            context.resources.getDimension(R.dimen.overflow_button_dimen) +
                context.resources.getDimension(R.dimen.big_height)
        ).toInt()
    val behavior = BottomSheetBehavior.from(binding.bottomSheetDetails.root)
    behavior.peekHeight = peekHeight
    binding.bottomSheetDetails.depictionsMapConfirm.setOnClickListener {
        val intent = Intent()
        intent.putExtra(NearbyParentFragment.PICKER_RESULT_PLACE, place)
        activity.setResult(Activity.RESULT_OK, intent)
        activity.finish()
    }
}

/** Binds place thumbnail with Glide, placeholder animation, and error fallback. */
private fun bindPlaceThumbnail(
    binding: FragmentNearbyParentBinding,
    place: Place,
    actions: NearbyBottomSheetHelper.NearbyBottomSheetActions,
) {
    val iconView = binding.bottomSheetDetails.icon
    val imageName = place.pic?.substringAfterLast("/")?.takeIf { it.isNotEmpty() }
    if (imageName != null) {
        loadThumbnailWithGlide(iconView, imageName, place.label.icon)
        iconView.setOnClickListener {
            actions.onMediaClicked(imageName)
        }
    } else {
        iconView.setImageResource(place.label.icon)
        iconView.setOnClickListener(null)
    }
}

/** Executes Glide thumbnail loading with animated loading icon and error handler. */
private fun loadThumbnailWithGlide(
    iconView: ImageView,
    imageName: String,
    fallbackIconRes: Int,
) {
    Glide.with(iconView.context).clear(iconView)
    val loadingDrawable = ContextCompat.getDrawable(iconView.context, R.drawable.loading_icon)
    val animation = AnimationUtils.loadAnimation(iconView.context, R.anim.rotate)

    Glide
        .with(iconView.context)
        .load("$COMMONS_FILE_REDIRECT_URL$imageName$THUMBNAIL_WIDTH_QUERY")
        .placeholder(loadingDrawable)
        .error(fallbackIconRes)
        .listener(
            object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean,
                ): Boolean {
                    iconView.clearAnimation()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    iconView.clearAnimation()
                    return false
                }
            },
        ).into(iconView)

    if (iconView.drawable != null && iconView.drawable?.constantState == loadingDrawable?.constantState) {
        iconView.startAnimation(animation)
    } else {
        iconView.clearAnimation()
    }
}

/** Binds place name, distance, and cleaned short description. */
private fun bindPlaceTexts(
    binding: FragmentNearbyParentBinding,
    place: Place,
) {
    binding.bottomSheetDetails.title.text = place.name
    binding.bottomSheetDetails.category.text = place.distance

    val longDescription = place.longDescription
    if (longDescription == null) {
        binding.bottomSheetDetails.description.text = ""
        return
    }
    // Remove label since it is double information
    val rawDescription = longDescription.replace(place.name + " (", "")
    // Set the short description after we remove place name from long description
    val descriptionText =
        if (rawDescription == longDescription) {
            rawDescription
        } else {
            rawDescription.replaceFirst(".$".toRegex(), "")
        }
    binding.bottomSheetDetails.description.text = descriptionText
}

/** Attaches upload click listeners for camera, gallery, and custom gallery FABs. */
private fun bindPlaceUploadFabs(
    binding: FragmentNearbyParentBinding,
    place: Place,
    actions: NearbyBottomSheetHelper.NearbyBottomSheetActions,
) {
    binding.fabCamera.setOnClickListener {
        if (binding.fabCamera.isShown) {
            actions.onUploadActionInitiated(place, NearbyBottomSheetHelper.UploadType.CAMERA)
        }
    }
    binding.fabGallery.setOnClickListener {
        if (binding.fabGallery.isShown) {
            actions.onUploadActionInitiated(place, NearbyBottomSheetHelper.UploadType.GALLERY)
        }
    }
    binding.fabCustomGallery.setOnClickListener {
        if (binding.fabCustomGallery.isShown) {
            actions.onUploadActionInitiated(place, NearbyBottomSheetHelper.UploadType.CUSTOM_GALLERY)
        }
    }
}
