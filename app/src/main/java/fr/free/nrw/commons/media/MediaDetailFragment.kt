package fr.free.nrw.commons.media

import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.viewModels
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.ThanksClient
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.getUserName
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.contributions.ContributionsRepository
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.contributions.hideTabs
import fr.free.nrw.commons.coordinates.CoordinateEditHelper
import fr.free.nrw.commons.databinding.FragmentMediaDetailBinding
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.delete.ReasonBuilder
import fr.free.nrw.commons.description.DescriptionEditHelper
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.review.ReviewHelper
import fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources
import fr.free.nrw.commons.utils.ViewUtilWrapper
import fr.free.nrw.commons.utils.setUnderlinedText
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/** Hosts the Media details view and its lifecycle. */
class MediaDetailFragment :
    CommonsDaggerSupportFragment(),
    CategoryEditHelper.Callback {
    private var editable: Boolean = false
    internal var isCategoryImage: Boolean = false
    private var detailProvider: MediaDetailProvider? = null
    internal var index: Int = 0
    internal var isDeleted: Boolean = false
    internal var fileWasDeletedFromCommons: Boolean = false
    private var isWikipediaButtonDisplayed: Boolean = false
    internal val callback: Callback? = null

    @Inject
    lateinit var mediaDetailViewModelFactory: MediaDetailViewModel.MediaDetailViewModelProviderFactory

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var mediaDataExtractor: MediaDataExtractor

    @Inject
    lateinit var contributionsRepository: ContributionsRepository

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    @field:Named("media_playback")
    lateinit var playbackHttpClient: OkHttpClient

    @Inject
    lateinit var reasonBuilder: ReasonBuilder

    @Inject
    lateinit var deleteHelper: DeleteHelper

    @Inject
    lateinit var reviewHelper: ReviewHelper

    @Inject
    lateinit var categoryEditHelper: CategoryEditHelper

    @Inject
    lateinit var coordinateEditHelper: CoordinateEditHelper

    @Inject
    lateinit var descriptionEditHelper: DescriptionEditHelper

    @Inject
    lateinit var viewUtil: ViewUtilWrapper

    @Inject
    lateinit var categoryClient: CategoryClient

    @Inject
    lateinit var thanksClient: ThanksClient

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    internal val viewModel: MediaDetailViewModel by viewModels<MediaDetailViewModel> { mediaDetailViewModelFactory }

    internal val onBackPressedCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (mediaFullscreen) {
                    setMediaFullscreen(false)
                } else {
                    binding.dummyCaptionDescriptionContainer.visibility = View.GONE
                    isEnabled = false
                }
            }
        }

    private var initialListTop: Int = 0

    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: FragmentMediaDetailBinding? = null
    internal val binding get() = _binding!!
    internal val currentBinding get() = _binding
    internal val viewDisposables get() = compositeDisposable

    internal var descriptionHtmlCode: String? = null

    internal val categoryNames: ArrayList<String> = ArrayList()
    private val fragmentHandler = Handler(Looper.getMainLooper())

    /**
     * Depicts is a feature part of Structured data.
     * Multiple Depictions can be added for an image just like categories.
     * However unlike categories depictions is multi-lingual
     * Ex: key: en value: monument
     */
    internal var imageInfoCache: ImageInfo? = null
    private var oldWidthOfImageView: Int = 0
    private var newWidthOfImageView: Int = 0
    private var heightVerifyingBoolean: Boolean = true // helps in maintaining aspect ratio

    // Had to make this class variable, to implement various onClicks, which access the media,
    // also I fell why make separate variables when one can serve the purpose
    internal var media: Media? = null

    internal val mediaDetailPlayer by lazy {
        MediaDetailPlayer(requireContext(), playbackHttpClient, ::handleMediaUnavailable)
    }
    internal var mediaRequestInProgress: Boolean = false
    internal var mediaFullscreen: Boolean = false

    internal lateinit var reasonList: ArrayList<String>
    internal lateinit var reasonListEnglishMappings: ArrayList<String>

    /**
     * Height stores the height of the frame layout as soon as it is initialised
     * and updates itself on configuration changes.
     * Used to adjust aspect ratio of image when length of the image is too large.
     */
    internal var frameLayoutHeight: Int = 0

    /**
     * Minimum height of the metadata, in pixels.
     * Images with a very narrow aspect ratio will be reduced so that the metadata information
     * panel always has at least this height.
     */
    internal val minimumHeightOfMetadata: Int = 200

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
        outState.putBoolean("editable", editable)
        outState.putBoolean("isCategoryImage", isCategoryImage)
        outState.putBoolean("isWikipediaButtonDisplayed", isWikipediaButtonDisplayed)
        outState.putBoolean("fileWasDeletedFromCommons", fileWasDeletedFromCommons)

        scrollPosition
        outState.putInt("listTop", initialListTop)
    }

    private val scrollPosition: Unit
        get() {
            initialListTop = _binding?.mediaDetailScrollView?.scrollY ?: initialListTop
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (parentFragment != null &&
            parentFragment is MediaDetailPagerFragment
        ) {
            detailProvider =
                (parentFragment as MediaDetailPagerFragment).mediaDetailProvider
        }
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable")
            isCategoryImage = savedInstanceState.getBoolean("isCategoryImage")
            isWikipediaButtonDisplayed = savedInstanceState.getBoolean("isWikipediaButtonDisplayed")
            fileWasDeletedFromCommons = savedInstanceState.getBoolean("fileWasDeletedFromCommons")
            index = savedInstanceState.getInt("index")
            initialListTop = savedInstanceState.getInt("listTop")
        } else {
            editable = requireArguments().getBoolean("editable")
            isCategoryImage = requireArguments().getBoolean("isCategoryImage")
            isWikipediaButtonDisplayed = requireArguments().getBoolean("isWikipediaButtonDisplayed")
            index = requireArguments().getInt("index")
            initialListTop = 0
        }

        reasonList = ArrayList()
        reasonList.add(getString(R.string.deletion_reason_uploaded_by_mistake))
        reasonList.add(getString(R.string.deletion_reason_publicly_visible))
        reasonList.add(getString(R.string.deletion_reason_not_interesting))
        reasonList.add(getString(R.string.deletion_reason_no_longer_want_public))
        reasonList.add(getString(R.string.deletion_reason_bad_for_my_privacy))

        // Add corresponding mappings in english locale so that we can upload it in deletion request
        reasonListEnglishMappings = ArrayList()
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH,
            ).getString(R.string.deletion_reason_uploaded_by_mistake),
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH,
            ).getString(R.string.deletion_reason_publicly_visible),
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH,
            ).getString(R.string.deletion_reason_not_interesting),
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH,
            ).getString(R.string.deletion_reason_no_longer_want_public),
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH,
            ).getString(R.string.deletion_reason_bad_for_my_privacy),
        )

        _binding = FragmentMediaDetailBinding.inflate(inflater, container, false)

        binding.seeMore.setUnderlinedText(R.string.nominated_see_more)

        if (!sessionManager.isUserLoggedIn) {
            binding.categoryEditButton.visibility = View.GONE
            binding.descriptionEdit.visibility = View.GONE
            binding.depictionsEditButton.visibility = View.GONE
        } else {
            binding.categoryEditButton.visibility = View.VISIBLE
            binding.descriptionEdit.visibility = View.VISIBLE
            binding.depictionsEditButton.visibility = View.VISIBLE
        }

        if (applicationKvStore.getBoolean("login_skipped")) {
            binding.nominateDeletion.visibility = View.GONE
            binding.coordinateEdit.visibility = View.GONE
        }

        // set onCLick listeners
        binding.mediaDetailLicense.setOnClickListener { onMediaDetailLicenceClicked() }
        binding.mediaDetailCoordinates.setOnClickListener { onMediaDetailCoordinatesClicked() }
        binding.sendThanks.setOnClickListener { sendThanksToAuthor() }
        binding.dummyCaptionDescriptionContainer.setOnClickListener { showCaptionAndDescription() }
        binding.mediaDetailMediaContainer.setOnClickListener { onMediaClicked() }
        binding.mediaDetailImageViewSpacer.setOnClickListener { onMediaClicked() }
        binding.categoryEditButton.setOnClickListener { onCategoryEditButtonClicked() }
        binding.depictionsEditButton.setOnClickListener { onDepictionsEditButtonClicked() }
        binding.seeMore.setOnClickListener { onSeeMoreClicked() }
        binding.mediaDetailAuthor.setOnClickListener { onAuthorViewClicked() }
        binding.nominateDeletion.setOnClickListener { onDeleteButtonClicked() }
        binding.descriptionEdit.setOnClickListener { onDescriptionEditClicked() }
        binding.coordinateEdit.setOnClickListener { onUpdateCoordinatesClicked() }
        binding.copyWikicode.setOnClickListener { onCopyWikicodeClicked() }

        binding.fileUsagesComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(
                    colorScheme =
                        if (isSystemInDarkTheme()) {
                            darkColorScheme(
                                primary = colorResource(R.color.primaryDarkColor),
                                surface = colorResource(R.color.main_background_dark),
                                background = colorResource(R.color.main_background_dark),
                            )
                        } else {
                            lightColorScheme(
                                primary = colorResource(R.color.primaryColor),
                                surface = colorResource(R.color.main_background_light),
                                background = colorResource(R.color.main_background_light),
                            )
                        },
                ) {
                    val commonsContainerState by viewModel.commonsContainerState.collectAsState()
                    val globalContainerState by viewModel.globalContainerState.collectAsState()

                    Surface {
                        Column {
                            Text(
                                text = stringResource(R.string.file_usages_container_heading),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                            FileUsagesContainer(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                commonsContainerState = commonsContainerState,
                                globalContainerState = globalContainerState,
                            )
                        }
                    }
                }
            }
        }

        /**
         * Gets the height of the frame layout as soon as the view is ready and updates aspect ratio
         * of the picture.
         */
        fragmentHandler.post {
            if (_binding == null) return@post

            val width = binding.mediaDetailScrollView.width
            if (width > 0) {
                frameLayoutHeight = binding.mediaDetailFrameLayout.measuredHeight
                updateAspectRatio(width)
            } else {
                fragmentHandler.postDelayed({
                    if (_binding == null) return@postDelayed
                    updateAspectRatio(binding.root.width)
                }, 1)
            }
        }

        return _binding?.root!!
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        val contributionsFragment: ContributionsFragment? = this.getContributionsFragmentParent()
        if (contributionsFragment?.binding != null) {
            contributionsFragment.binding!!.cardViewNearby.visibility = View.GONE
        }

        // detail provider is null when fragment is shown in review activity
        media =
            if (detailProvider != null) {
                detailProvider!!.getMediaAtPosition(index)
            } else {
                requireArguments().getParcelable("media")
            }

        if (media != null &&
            applicationKvStore.getBoolean(
                String.format(
                    NOMINATING_FOR_DELETION_MEDIA,
                    media!!.imageUrl,
                ),
                false,
            )
        ) {
            enableProgressBar()
        }

        if (getUserName(requireContext()) != null &&
            media != null &&
            getUserName(
                requireContext(),
            ) == media!!.author
        ) {
            binding.sendThanks.visibility = View.GONE
        } else {
            binding.sendThanks.visibility = View.VISIBLE
        }

        binding.mediaDetailScrollView.doOnNextLayout { view ->
            if (_binding == null) return@doOnNextLayout
            oldWidthOfImageView = view.width
            if (media != null) {
                displayMediaDetails()
                fetchFileUsages(media?.filename!!)
            }
        }

        binding.progressBarEdit.visibility = View.GONE
        binding.descriptionEdit.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (_binding == null) return
        binding.mediaDetailScrollView.doOnNextLayout { view ->
            if (_binding == null) return@doOnNextLayout
            frameLayoutHeight = binding.mediaDetailFrameLayout.measuredHeight
            updateAspectRatio(view.width)
            if (view.width != oldWidthOfImageView && newWidthOfImageView == 0) {
                newWidthOfImageView = view.width
                updateAspectRatio(newWidthOfImageView)
            }
        }

        // Ensuring correct aspect ratio for landscape mode
        if (heightVerifyingBoolean) {
            updateAspectRatio(newWidthOfImageView)
            heightVerifyingBoolean = false
        } else {
            updateAspectRatio(oldWidthOfImageView)
            heightVerifyingBoolean = true
        }
    }

    internal val aspectRatioListener: ControllerListener<ImageInfo?> =
        object : BaseControllerListener<ImageInfo?>() {
            override fun onIntermediateImageSet(
                id: String,
                imageInfo: ImageInfo?,
            ) {
                imageInfoCache = imageInfo
                if (_binding == null) return
                updateAspectRatio(binding.mediaDetailScrollView.width)
            }

            override fun onFinalImageSet(
                id: String,
                imageInfo: ImageInfo?,
                animatable: Animatable?,
            ) {
                imageInfoCache = imageInfo
                if (_binding == null) return
                updateAspectRatio(binding.mediaDetailScrollView.width)
                binding.mediaDetailImageProgressBar.visibility = View.GONE
            }

            override fun onFailure(
                id: String,
                throwable: Throwable,
            ) {
                _binding?.mediaDetailImageProgressBar?.visibility = View.GONE
                Timber.e(throwable, "Failed to load media detail image")
                revealMediaDetailWithoutImage()
            }
        }

    /**
     * Pauses media playback.
     *
     * Called when the user navigates away from this media.
     */
    fun pauseMedia() {
        // The pager can call this method on a fragment that has already been
        // detached, and creating the player (lazy) needs an attached fragment,
        // so skip the pause as there's nothing to pause anyway.
        if (isAdded) {
            mediaDetailPlayer.pause()
        }
    }

    override fun onStop() {
        mediaDetailPlayer.pause()
        super.onStop()
    }

    override fun onDestroyView() {
        fragmentHandler.removeCallbacksAndMessages(null)

        setMediaFullscreen(false)
        compositeDisposable.clear()
        cleanupMediaPlayer()
        _binding?.root?.clearFocus()
        _binding?.root?.setOnKeyListener(null)
        binding.mediaDetailImageView.controller = null
        _binding = null

        super.onDestroyView()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
        configureMediaPlayerView(binding.mediaDetailPlayerView)
        configureMediaPlayerView(binding.mediaDetailFullscreenPlayerView)
        if (activity is MainActivity) {
            // explicitly hides the tabs when the media details screen is opened.
            (activity as MainActivity).hideTabs()
        }
    }

    /** Refreshes categories after the category editor updates the file. */
    fun updateCategories() {
        refreshCategoryList()
    }

    override fun updateCategoryDisplay(categories: List<String>?): Boolean {
        if (categories == null) {
            return false
        } else {
            rebuildCatList(categories)
            return true
        }
    }

    interface Callback {
        fun nominatingForDeletion(index: Int)
    }

    companion object {
        const val DEFAULT_IMAGE_BACKGROUND_COLOR: Int = 0

        @JvmStatic
        fun forMedia(
            index: Int,
            editable: Boolean,
            isCategoryImage: Boolean,
            isWikipediaButtonDisplayed: Boolean,
        ): MediaDetailFragment {
            val mf = MediaDetailFragment()
            val state = Bundle()
            state.putBoolean("editable", editable)
            state.putBoolean("isCategoryImage", isCategoryImage)
            state.putInt("index", index)
            state.putInt("listIndex", 0)
            state.putInt("listTop", 0)
            state.putBoolean("isWikipediaButtonDisplayed", isWikipediaButtonDisplayed)
            mf.arguments = state

            return mf
        }

        const val NOMINATING_FOR_DELETION_MEDIA: String = "Nominating for deletion %s"
    }
}

@Composable
fun FileUsagesContainer(
    commonsContainerState: MediaDetailViewModel.FileUsagesContainerState,
    globalContainerState: MediaDetailViewModel.FileUsagesContainerState,
    modifier: Modifier = Modifier,
) {
    var isCommonsListExpanded by rememberSaveable { mutableStateOf(true) }
    var isOtherWikisListExpanded by rememberSaveable { mutableStateOf(true) }

    val uriHandle = LocalUriHandler.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.usages_on_commons_heading),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = { isCommonsListExpanded = !isCommonsListExpanded }) {
                Icon(
                    imageVector =
                        if (isCommonsListExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                    contentDescription = null,
                )
            }
        }

        if (isCommonsListExpanded) {
            when (commonsContainerState) {
                MediaDetailViewModel.FileUsagesContainerState.Loading -> {
                    LinearProgressIndicator()
                }
                is MediaDetailViewModel.FileUsagesContainerState.Success -> {
                    val data = commonsContainerState.data
                    if (data.isNullOrEmpty()) {
                        ListItem(headlineContent = {
                            Text(
                                text = stringResource(R.string.no_usages_found),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        })
                    } else {
                        data.forEach { usage ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        text = stringResource(R.string.bullet_point),
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        modifier =
                                            Modifier.clickable {
                                                usage.link?.let { uriHandle.openUri(it) }
                                            },
                                        text = usage.title,
                                        style =
                                            MaterialTheme.typography.titleSmall.copy(
                                                color = Color(0xFF5A6AEC),
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                    )
                                },
                            )
                        }
                    }
                }
                is MediaDetailViewModel.FileUsagesContainerState.Error -> {
                    ListItem(headlineContent = {
                        Text(
                            text = commonsContainerState.errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    })
                }
                MediaDetailViewModel.FileUsagesContainerState.Initial -> Unit
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.usages_on_other_wikis_heading),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
            IconButton(onClick = { isOtherWikisListExpanded = !isOtherWikisListExpanded }) {
                Icon(
                    imageVector =
                        if (isOtherWikisListExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                    contentDescription = null,
                )
            }
        }

        if (isOtherWikisListExpanded) {
            when (globalContainerState) {
                MediaDetailViewModel.FileUsagesContainerState.Loading -> {
                    LinearProgressIndicator()
                }
                is MediaDetailViewModel.FileUsagesContainerState.Success -> {
                    val data = globalContainerState.data
                    if (data.isNullOrEmpty()) {
                        ListItem(headlineContent = {
                            Text(
                                text = stringResource(R.string.no_usages_found),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        })
                    } else {
                        data.forEach { usage ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        text = stringResource(R.string.bullet_point),
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        modifier =
                                            Modifier.clickable {
                                                usage.link?.let { uriHandle.openUri(it) }
                                            },
                                        text = usage.title,
                                        style =
                                            MaterialTheme.typography.titleSmall.copy(
                                                color = Color(0xFF5A6AEC),
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                    )
                                },
                            )
                        }
                    }
                }
                is MediaDetailViewModel.FileUsagesContainerState.Error -> {
                    ListItem(headlineContent = {
                        Text(
                            text = globalContainerState.errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    })
                }
                MediaDetailViewModel.FileUsagesContainerState.Initial -> Unit
            }
        }
    }
}
