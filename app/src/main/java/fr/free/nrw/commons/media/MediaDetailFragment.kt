package fr.free.nrw.commons.media

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.viewModels
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.Companion.instance
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.ThanksClient
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.auth.getUserName
import fr.free.nrw.commons.category.CATEGORY_NEEDING_CATEGORIES
import fr.free.nrw.commons.category.CATEGORY_UNCATEGORISED
import fr.free.nrw.commons.category.CategoryClient
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.category.CategoryEditHelper
import fr.free.nrw.commons.contributions.ContributionsFragment
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.coordinates.CoordinateEditHelper
import fr.free.nrw.commons.databinding.FragmentMediaDetailBinding
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.delete.ReasonBuilder
import fr.free.nrw.commons.description.DescriptionEditActivity
import fr.free.nrw.commons.description.DescriptionEditHelper
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.language.AppLanguageLookUpTable
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.locationpicker.LocationPicker
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.review.ReviewHelper
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.utils.DateUtil.getDateStringWithSkeletonPattern
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformAction
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.ViewUtil
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.utils.ViewUtilWrapper
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.utils.setUnderlinedText
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage.Revision
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.lang.String.format
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Named

class MediaDetailFragment : CommonsDaggerSupportFragment(), CategoryEditHelper.Callback {
    private var editable: Boolean = false
    private var isCategoryImage: Boolean = false
    private var detailProvider: MediaDetailProvider? = null
    private var index: Int = 0
    private var isDeleted: Boolean = false
    private var isWikipediaButtonDisplayed: Boolean = false
    private val callback: Callback? = null

    @Inject
    lateinit var mediaDetailViewModelFactory: MediaDetailViewModel.MediaDetailViewModelProviderFactory

    @Inject
    lateinit var locationManager: LocationServiceManager


    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var mediaDataExtractor: MediaDataExtractor

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

    private val viewModel: MediaDetailViewModel by viewModels<MediaDetailViewModel> { mediaDetailViewModelFactory }

    private var initialListTop: Int = 0

    private var _binding: FragmentMediaDetailBinding? = null
    private val binding get() = _binding!!

    private var descriptionHtmlCode: String? = null


    private val categoryNames: ArrayList<String> = ArrayList()

    /**
     * Depicts is a feature part of Structured data.
     * Multiple Depictions can be added for an image just like categories.
     * However unlike categories depictions is multi-lingual
     * Ex: key: en value: monument
     */
    private var imageInfoCache: ImageInfo? = null
    private var oldWidthOfImageView: Int = 0
    private var newWidthOfImageView: Int = 0
    private var heightVerifyingBoolean: Boolean = true // helps in maintaining aspect ratio
    private var layoutListener: OnGlobalLayoutListener? = null // for layout stuff, only used once!

    //Had to make this class variable, to implement various onClicks, which access the media,
    // also I fell why make separate variables when one can serve the purpose
    private var media: Media? = null
    private lateinit var reasonList: ArrayList<String>
    private lateinit var reasonListEnglishMappings: ArrayList<String>

    /**
     * Height stores the height of the frame layout as soon as it is initialised
     * and updates itself on configuration changes.
     * Used to adjust aspect ratio of image when length of the image is too large.
     */
    private var frameLayoutHeight: Int = 0

    /**
     * Minimum height of the metadata, in pixels.
     * Images with a very narrow aspect ratio will be reduced so that the metadata information
     * panel always has at least this height.
     */
    private val minimumHeightOfMetadata: Int = 200

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
        outState.putBoolean("editable", editable)
        outState.putBoolean("isCategoryImage", isCategoryImage)
        outState.putBoolean("isWikipediaButtonDisplayed", isWikipediaButtonDisplayed)

        scrollPosition
        outState.putInt("listTop", initialListTop)
    }

    private val scrollPosition: Unit
        get() {
            initialListTop = binding.mediaDetailScrollView.scrollY
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (parentFragment != null
            && parentFragment is MediaDetailPagerFragment
        ) {
            detailProvider =
                (parentFragment as MediaDetailPagerFragment).mediaDetailProvider
        }
        if (savedInstanceState != null) {
            editable = savedInstanceState.getBoolean("editable")
            isCategoryImage = savedInstanceState.getBoolean("isCategoryImage")
            isWikipediaButtonDisplayed = savedInstanceState.getBoolean("isWikipediaButtonDisplayed")
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
                Locale.ENGLISH
            ).getString(R.string.deletion_reason_uploaded_by_mistake)
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH
            ).getString(R.string.deletion_reason_publicly_visible)
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH
            ).getString(R.string.deletion_reason_not_interesting)
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH
            ).getString(R.string.deletion_reason_no_longer_want_public)
        )
        reasonListEnglishMappings.add(
            getLocalizedResources(
                requireContext(),
                Locale.ENGLISH
            ).getString(R.string.deletion_reason_bad_for_my_privacy)
        )

        _binding = FragmentMediaDetailBinding.inflate(inflater, container, false)
        val view: View = binding.root

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

        handleBackEvent(view)

        //set onCLick listeners
        binding.mediaDetailLicense.setOnClickListener { onMediaDetailLicenceClicked() }
        binding.mediaDetailCoordinates.setOnClickListener { onMediaDetailCoordinatesClicked() }
        binding.sendThanks.setOnClickListener { sendThanksToAuthor() }
        binding.dummyCaptionDescriptionContainer.setOnClickListener { showCaptionAndDescription() }
        binding.mediaDetailImageView.setOnClickListener {
            launchZoomActivity(
                binding.mediaDetailImageView
            )
        }
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
                    colorScheme = if (isSystemInDarkTheme()) darkColorScheme(
                        primary = colorResource(R.color.primaryDarkColor),
                        surface = colorResource(R.color.main_background_dark),
                        background = colorResource(R.color.main_background_dark)
                    ) else lightColorScheme(
                        primary = colorResource(R.color.primaryColor),
                        surface = colorResource(R.color.main_background_light),
                        background = colorResource(R.color.main_background_light)
                    )
                ) {

                    val commonsContainerState by viewModel.commonsContainerState.collectAsState()
                    val globalContainerState by viewModel.globalContainerState.collectAsState()

                    Surface {
                        Column {
                            Text(
                                text = stringResource(R.string.file_usages_container_heading),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                            FileUsagesContainer(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                                commonsContainerState = commonsContainerState,
                                globalContainerState = globalContainerState
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
        view.post{
            val width = binding.mediaDetailScrollView.width
            if (width > 0) {
                frameLayoutHeight = binding.mediaDetailFrameLayout.measuredHeight
                updateAspectRatio(width)
            } else {
                view.postDelayed({ updateAspectRatio(binding.root.width) }, 1)
            }
        }

        return view
    }

    fun launchZoomActivity(view: View) {
        val hasPermission: Boolean = hasPermission(requireActivity(), PERMISSIONS_STORAGE)
        if (hasPermission) {
            launchZoomActivityAfterPermissionCheck(view)
        } else {
            checkPermissionsAndPerformAction(
                requireActivity(),
                {
                    launchZoomActivityAfterPermissionCheck(view)
                },
                R.string.storage_permission_title,
                R.string.read_storage_permission_rationale,
                *PERMISSIONS_STORAGE
            )
        }
    }

    private fun fetchFileUsages(fileName: String) {
        if (viewModel.commonsContainerState.value == MediaDetailViewModel.FileUsagesContainerState.Initial) {
            viewModel.loadFileUsagesCommons(fileName)
        }

        if (viewModel.globalContainerState.value == MediaDetailViewModel.FileUsagesContainerState.Initial) {
            viewModel.loadGlobalFileUsages(fileName)
        }
    }

    /**
     * launch zoom acitivity after permission check
     * @param view as ImageView
     */
    private fun launchZoomActivityAfterPermissionCheck(view: View) {
        if (media!!.imageUrl != null) {
            val ctx: Context = view.context
            val zoomableIntent = Intent(ctx, ZoomableActivity::class.java)
            zoomableIntent.setData(Uri.parse(media!!.imageUrl))
            zoomableIntent.putExtra(
                ZoomableActivity.ZoomableActivityConstants.ORIGIN, "MediaDetails"
            )

            val backgroundColor: Int = imageBackgroundColor
            if (backgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
                zoomableIntent.putExtra(
                    ZoomableActivity.ZoomableActivityConstants.PHOTO_BACKGROUND_COLOR,
                    backgroundColor
                )
            }

            ctx.startActivity(
                zoomableIntent
            )
        }
    }

    /**
     * Retrieves the ContributionsFragment that is potentially the parent, grandparent, etc
     * fragment of this fragment.
     *
     * @return The ContributionsFragment instance. If the ContributionsFragment instance could not
     * be found, null is returned.
     */
    private fun getContributionsFragmentParent(): ContributionsFragment? {
        var fragment: Fragment? = this

        while (fragment != null && fragment !is ContributionsFragment) {
            fragment = fragment.parentFragment
        }

        if (fragment == null) {
            return null
        }

        return fragment as ContributionsFragment
    }

    override fun onResume() {
        super.onResume()

        val contributionsFragment: ContributionsFragment? = this.getContributionsFragmentParent()
        if (contributionsFragment?.binding != null) {
            contributionsFragment.binding!!.cardViewNearby.visibility = View.GONE
        }

        // detail provider is null when fragment is shown in review activity
        media = if (detailProvider != null) {
            detailProvider!!.getMediaAtPosition(index)
        } else {
            requireArguments().getParcelable("media")
        }

        if (media != null && applicationKvStore.getBoolean(
                String.format(
                    NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl
                ), false
            )
        ) {
            enableProgressBar()
        }

        if (getUserName(requireContext()) != null && media != null && getUserName(
                requireContext()
            ) == media!!.author
        ) {
            binding.sendThanks.visibility = View.GONE
        } else {
            binding.sendThanks.visibility = View.VISIBLE
        }

        binding.mediaDetailScrollView.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (context == null) {
                        return
                    }
                    binding.mediaDetailScrollView.viewTreeObserver.removeOnGlobalLayoutListener(
                        this
                    )
                    oldWidthOfImageView = binding.mediaDetailScrollView.width
                    if (media != null) {
                        displayMediaDetails()
                        fetchFileUsages(media?.filename!!)
                    }
                }
            }
        )
        binding.progressBarEdit.visibility = View.GONE
        binding.descriptionEdit.visibility = View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.mediaDetailScrollView.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    /**
                     * We update the height of the frame layout as the configuration changes.
                     */
                    binding.mediaDetailFrameLayout.post {
                        frameLayoutHeight = binding.mediaDetailFrameLayout.measuredHeight
                        updateAspectRatio(binding.mediaDetailScrollView.width)
                    }
                    if (binding.mediaDetailScrollView.width != oldWidthOfImageView) {
                        if (newWidthOfImageView == 0) {
                            newWidthOfImageView = binding.mediaDetailScrollView.width
                            updateAspectRatio(newWidthOfImageView)
                        }
                        binding.mediaDetailScrollView.viewTreeObserver.removeOnGlobalLayoutListener(
                            this
                        )
                    }
                }
            }
        )
        // Ensuring correct aspect ratio for landscape mode
        if (heightVerifyingBoolean) {
            updateAspectRatio(newWidthOfImageView)
            heightVerifyingBoolean = false
        } else {
            updateAspectRatio(oldWidthOfImageView)
            heightVerifyingBoolean = true
        }
    }

    private fun displayMediaDetails() {
        setTextFields(media!!)
        compositeDisposable.addAll(
            mediaDataExtractor.refresh(media!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { media: Media -> onMediaRefreshed(media) },
                    { t: Throwable? -> Timber.e(t) }),
            mediaDataExtractor.getCurrentWikiText(
                Objects.requireNonNull(media?.filename!!)
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { s: String? -> updateCategoryList(s!!) },
                    { t: Throwable? -> Timber.e(t) }),
            mediaDataExtractor.checkDeletionRequestExists(media!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { deletionPageExists: Boolean -> onDeletionPageExists(deletionPageExists) },
                    { t: Throwable? -> Timber.e(t) }),
            mediaDataExtractor.fetchDiscussion(media!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { discussion: String -> onDiscussionLoaded(discussion) },
                    { t: Throwable? -> Timber.e(t) })
        )
    }

    private fun onMediaRefreshed(media: Media) {
        media.categories = this.media!!.categories
        this.media = media
        setTextFields(media)
        compositeDisposable.addAll(
            mediaDataExtractor.fetchDepictionIdsAndLabels(media)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { idAndCaptions: List<IdAndLabels> -> onDepictionsLoaded(idAndCaptions) },
                    { t: Throwable? -> Timber.e(t) })
        )
    }

    private fun onDiscussionLoaded(discussion: String) {
        binding.mediaDetailDisc.text = prettyDiscussion(discussion.trim { it <= ' ' })
    }

    private fun onDeletionPageExists(deletionPageExists: Boolean) {
        if (getUserName(requireContext()) == null && getUserName(requireContext()) != media!!.author) {
            binding.nominateDeletion.visibility = View.GONE
            binding.nominatedDeletionBanner.visibility = View.GONE
        } else if (deletionPageExists) {
            if (applicationKvStore.getBoolean(
                    String.format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl), false
                )
            ) {
                applicationKvStore.remove(
                    String.format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl)
                )
                binding.progressBarDeletion.visibility = View.GONE
            }
            binding.nominateDeletion.visibility = View.GONE

            binding.nominatedDeletionBanner.visibility = View.VISIBLE
        } else if (!isCategoryImage) {
            binding.nominateDeletion.visibility = View.VISIBLE
            binding.nominatedDeletionBanner.visibility = View.GONE
        }
    }

    private fun onDepictionsLoaded(idAndCaptions: List<IdAndLabels>) {
        binding.depictsLayout.visibility = View.VISIBLE
        binding.depictionsEditButton.visibility = View.VISIBLE
        buildDepictionList(idAndCaptions)
    }

    /**
     * By clicking on the edit depictions button, it will send user to depict fragment
     */
    fun onDepictionsEditButtonClicked() {
        binding.mediaDetailDepictionContainer.removeAllViews()
        binding.depictionsEditButton.visibility = View.GONE
        val depictsFragment: Fragment = DepictsFragment()
        val bundle = Bundle()
        bundle.putParcelable("Existing_Depicts", media)
        depictsFragment.arguments = bundle
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.mediaDetailFrameLayout, depictsFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    /**
     * The imageSpacer is Basically a transparent overlay for the SimpleDraweeView
     * which holds the image to be displayed( moreover this image is out of
     * the scroll view )
     *
     *
     * If the image is sufficiently large i.e. the image height extends the view height, we reduce
     * the height and change the width to maintain the aspect ratio, otherwise image takes up the
     * total possible width and height is adjusted accordingly.
     *
     * @param scrollWidth the current width of the scrollView
     */
    private fun updateAspectRatio(scrollWidth: Int) {
        if (imageInfoCache != null) {
            var finalHeight: Int = (scrollWidth * imageInfoCache!!.height) / imageInfoCache!!.width
            val params: ViewGroup.LayoutParams = binding.mediaDetailImageView.layoutParams
            val spacerParams: ViewGroup.LayoutParams =
                binding.mediaDetailImageViewSpacer.layoutParams
            params.width = scrollWidth
            if (finalHeight > frameLayoutHeight - minimumHeightOfMetadata) {
                // Adjust the height and width of image.

                val temp: Int = frameLayoutHeight - minimumHeightOfMetadata
                params.width = (scrollWidth * temp) / finalHeight
                finalHeight = temp
            }
            params.height = finalHeight
            spacerParams.height = finalHeight
            binding.mediaDetailImageView.layoutParams = params
            binding.mediaDetailImageViewSpacer.layoutParams = spacerParams
        }
    }

    private val aspectRatioListener: ControllerListener<ImageInfo?> =
        object : BaseControllerListener<ImageInfo?>() {
            override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) {
                imageInfoCache = imageInfo
                updateAspectRatio(binding.mediaDetailScrollView.width)
            }

            override fun onFinalImageSet(
                id: String,
                imageInfo: ImageInfo?,
                animatable: Animatable?
            ) {
                imageInfoCache = imageInfo
                updateAspectRatio(binding.mediaDetailScrollView.width)
            }
        }

    /**
     * Uses two image sources.
     * - low resolution thumbnail is shown initially
     * - when the high resolution image is available, it replaces the low resolution image
     */
    private fun setupImageView() {
        val imageBackgroundColor: Int = imageBackgroundColor
        if (imageBackgroundColor != DEFAULT_IMAGE_BACKGROUND_COLOR) {
            binding.mediaDetailImageView.setBackgroundColor(imageBackgroundColor)
        }

        binding.mediaDetailImageView.hierarchy.setPlaceholderImage(R.drawable.image_placeholder)
        binding.mediaDetailImageView.hierarchy.setFailureImage(R.drawable.image_placeholder)

        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setLowResImageRequest(ImageRequest.fromUri(if (media != null) media!!.thumbUrl else null))
            .setRetainImageOnFailure(true)
            .setImageRequest(ImageRequest.fromUri(if (media != null) media!!.imageUrl else null))
            .setControllerListener(aspectRatioListener)
            .setOldController(binding.mediaDetailImageView.controller)
            .build()
        binding.mediaDetailImageView.controller = controller
    }

    private fun updateToDoWarning() {
        var toDoMessage = ""
        var toDoNeeded = false
        var categoriesPresent: Boolean =
            if (media!!.categories == null) false else (media!!.categories!!.isNotEmpty())

        // Check if the presented category is about need of category
        if (categoriesPresent) {
            for (category: String in media!!.categories!!) {
                if (category.lowercase().contains(CATEGORY_NEEDING_CATEGORIES) ||
                    category.lowercase().contains(CATEGORY_UNCATEGORISED)
                ) {
                    categoriesPresent = false
                }
                break
            }
        }
        if (!categoriesPresent) {
            toDoNeeded = true
            toDoMessage += getString(R.string.missing_category)
        }
        if (isWikipediaButtonDisplayed) {
            toDoNeeded = true
            toDoMessage += if ((toDoMessage.isEmpty())) "" else "\n" + getString(R.string.missing_article)
        }

        if (toDoNeeded) {
            toDoMessage = getString(R.string.todo_improve) + "\n" + toDoMessage
            binding.toDoLayout.visibility = View.VISIBLE
            binding.toDoReason.text = toDoMessage
        } else {
            binding.toDoLayout.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        if (layoutListener != null && view != null) {
            requireView().viewTreeObserver.removeGlobalOnLayoutListener(layoutListener) // old Android was on crack. CRACK IS WHACK
            layoutListener = null
        }

        compositeDisposable.clear()

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is MainActivity) {
            //explicitly hides the tabs when the media details screen is opened.
            (activity as MainActivity).hideTabs()
        }
    }

    private fun setTextFields(media: Media) {
        setupImageView()
        binding.mediaDetailTitle.text = media.displayTitle
        binding.mediaDetailDesc.setHtmlText(prettyDescription(media))
        binding.mediaDetailLicense.text = prettyLicense(media)
        binding.mediaDetailCoordinates.text = prettyCoordinates(media)
        binding.mediaDetailuploadeddate.text = prettyUploadedDate(media)
        if (prettyCaption(media) == requireContext().getString(R.string.detail_caption_empty)) {
            binding.captionLayout.visibility = View.GONE
        } else {
            binding.mediaDetailCaption.text = prettyCaption(media)
        }

        categoryNames.clear()
        categoryNames.addAll(media.categories!!)

        // Show author or uploader information for licensing compliance
        val authorName = media.getAttributedAuthor()
        val uploaderName = media.user
        
        when {
            !authorName.isNullOrEmpty() -> {
                // Show author if available
                binding.mediaDetailAuthorLabel.text = getString(R.string.media_detail_author)
                binding.mediaDetailAuthor.text = authorName
                binding.authorLinearLayout.visibility = View.VISIBLE
            }
            !uploaderName.isNullOrEmpty() -> {
                // Show uploader as fallback
                binding.mediaDetailAuthorLabel.text = getString(R.string.media_detail_uploader)
                binding.mediaDetailAuthor.text = uploaderName
                binding.authorLinearLayout.visibility = View.VISIBLE
            }
            else -> {
                // Hide if neither author nor uploader is available
                binding.authorLinearLayout.visibility = View.GONE
            }
        }
    }

    /**
     * Gets new categories from the WikiText and updates it on the UI
     *
     * @param s WikiText
     */
    private fun updateCategoryList(s: String) {
        val allCategories: MutableList<String> = ArrayList()
        var i: Int = s.indexOf("[[Category:")
        while (i != -1) {
            val category: String = s.substring(i + 11, s.indexOf("]]", i))
            allCategories.add(category)
            i = s.indexOf("]]", i)
            i = s.indexOf("[[Category:", i)
        }
        media!!.categories = allCategories
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(getString(R.string.detail_panel_cats_none))
        }
        if (sessionManager.isUserLoggedIn) {
            binding.categoryEditButton.visibility = View.VISIBLE
        }
        rebuildCatList(allCategories)
    }

    /**
     * Updates the categories
     */
    fun updateCategories() {
        val allCategories: MutableList<String> = ArrayList(
            media?.addedCategories!!
        )
        media!!.categories = allCategories
        if (allCategories.isEmpty()) {
            // Stick in a filler element.
            allCategories.add(getString(R.string.detail_panel_cats_none))
        }

        rebuildCatList(allCategories)
    }

    /**
     * Populates media details fragment with depiction list
     * @param idAndCaptions
     */
    private fun buildDepictionList(idAndCaptions: List<IdAndLabels>) {
        binding.mediaDetailDepictionContainer.removeAllViews()

        // Create a mutable list from the original list
        val mutableIdAndCaptions = idAndCaptions.toMutableList()

        if (mutableIdAndCaptions.isEmpty()) {
            // Create a placeholder IdAndLabels object and add it to the list
            mutableIdAndCaptions.add(
                IdAndLabels(
                    id = media?.pageId ?: "", // Use an empty string if media?.pageId is null
                    labels = mapOf(Locale.getDefault().language to getString(R.string.detail_panel_cats_none)) // Create a Map with the language as the key and the message as the value
                )
            )
        }

        val locale: String = Locale.getDefault().language
        for (idAndCaption in mutableIdAndCaptions) {
            binding.mediaDetailDepictionContainer.addView(
                buildDepictLabel(
                    getDepictionCaption(idAndCaption, locale),
                    idAndCaption.id,
                    binding.mediaDetailDepictionContainer
                )
            )
        }
    }


    private fun getDepictionCaption(idAndCaption: IdAndLabels, locale: String): String? {
        // Check if the Depiction Caption is available in user's locale
        // if not then check for english, else show any available.
        if (idAndCaption.labels[locale] != null) {
            return idAndCaption.labels[locale]
        }
        if (idAndCaption.labels["en"] != null) {
            return idAndCaption.labels["en"]
        }
        return idAndCaption.labels.values.iterator().next()
    }

    private fun onMediaDetailLicenceClicked() {
        val url: String? = media!!.licenseUrl
        if (!StringUtils.isBlank(url) && activity != null) {
            handleWebUrl(requireContext(), Uri.parse(url))
        } else {
            viewUtil.showShortToast(requireActivity(), getString(R.string.null_url))
        }
    }

    private fun onMediaDetailCoordinatesClicked() {
        if (media!!.coordinates != null && activity != null) {
            handleGeoCoordinates(requireContext(), media!!.coordinates!!)
        }
    }

    private fun onCopyWikicodeClicked() {
        val data: String =
            "[[" + media!!.filename + "|thumb|" + media!!.fallbackDescription + "]]"
        requireContext().copyToClipboard("wikiCode", data)
        Timber.d("Generated wikidata copy code: %s", data)

        Toast.makeText(requireContext(), getString(R.string.wikicode_copied), Toast.LENGTH_SHORT)
            .show()
    }

    /**
     * Sends thanks to author if the author is not the user
     */
    private fun sendThanksToAuthor() {
        val fileName: String? = media!!.filename
        if (TextUtils.isEmpty(fileName)) {
            Toast.makeText(
                context, getString(R.string.error_sending_thanks),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        compositeDisposable.add(
            reviewHelper.getFirstRevisionOfFile(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { revision: Revision? ->
                    sendThanks(
                        requireContext(), revision
                    )
                }
        )
    }

    /**
     * Api call for sending thanks to the author when the author is not the user
     * and display toast depending on the result
     * @param context context
     * @param firstRevision the revision id of the image
     */
    @SuppressLint("CheckResult", "StringFormatInvalid")
    fun sendThanks(context: Context, firstRevision: Revision?) {
        showShortToast(
            context,
            context.getString(R.string.send_thank_toast, media!!.displayTitle)
        )

        if (firstRevision == null) {
            return
        }

        Observable.defer {
            thanksClient.thank(
                firstRevision.revisionId()
            )
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Boolean ->
                    displayThanksToast(
                        requireContext(), result
                    )
                },
                { throwable: Throwable? ->
                    if (throwable is InvalidLoginTokenException) {
                        val username: String? = sessionManager.userName
                        val logoutListener: CommonsApplication.BaseLogoutListener =
                            CommonsApplication.BaseLogoutListener(
                                requireActivity(),
                                requireActivity().getString(R.string.invalid_login_message),
                                username
                            )

                        instance.clearApplicationData(
                            requireActivity(), logoutListener
                        )
                    } else {
                        Timber.e(throwable)
                    }
                })
    }

    /**
     * Method to display toast when api call to thank the author is completed
     * @param context context
     * @param result true if success, false otherwise
     */
    @SuppressLint("StringFormatInvalid")
    private fun displayThanksToast(context: Context, result: Boolean) {
        val message: String = if (result) {
            context.getString(
                R.string.send_thank_success_message,
                media!!.user
            )
        } else {
            context.getString(
                R.string.send_thank_failure_message,
                media!!.user
            )
        }

        showShortToast(context, message)
    }

    fun onCategoryEditButtonClicked() {
        binding.progressBarEditCategory.visibility = View.VISIBLE
        binding.categoryEditButton.visibility = View.GONE
        wikiText
    }

    private val wikiText: Unit
        /**
         * Gets WikiText from the server and send it to catgory editor
         */
        get() {
            compositeDisposable.add(
                mediaDataExtractor.getCurrentWikiText(
                    Objects.requireNonNull(media?.filename!!)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: String? -> gotoCategoryEditor(s!!) },
                        { t: Throwable? -> Timber.e(t) })
            )
        }

    /**
     * Opens the category editor
     *
     * @param s WikiText
     */
    private fun gotoCategoryEditor(s: String) {
        binding.categoryEditButton.visibility = View.VISIBLE
        binding.progressBarEditCategory.visibility = View.GONE
        val categoriesFragment: Fragment = UploadCategoriesFragment()
        val bundle = Bundle()
        bundle.putParcelable("Existing_Categories", media)
        bundle.putString("WikiText", s)
        categoriesFragment.arguments = bundle
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.mediaDetailFrameLayout, categoriesFragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun onUpdateCoordinatesClicked() {
        goToLocationPickerActivity()
    }

    /**
     * Start location picker activity with a request code and get the coordinates from the activity.
     */
    private fun goToLocationPickerActivity() {
        /*
        If location is not provided in media this coordinates will act as a placeholder in
        location picker activity
         */
        var defaultLatitude = 37.773972
        var defaultLongitude: Double = -122.431297
        if (media!!.coordinates != null) {
            defaultLatitude = media!!.coordinates!!.latitude
            defaultLongitude = media!!.coordinates!!.longitude
        } else {
            if (locationManager.getLastLocation() != null) {
                defaultLatitude = locationManager.getLastLocation()!!.latitude
                defaultLongitude = locationManager.getLastLocation()!!.longitude
            } else {
                val lastLocation: Array<String>? = applicationKvStore.getString(
                    UploadMediaDetailFragment.LAST_LOCATION,
                    ("$defaultLatitude,$defaultLongitude")
                )?.split(",".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()

                if (lastLocation != null) {
                    defaultLatitude = lastLocation[0].toDouble()
                    defaultLongitude = lastLocation[1].toDouble()
                }
            }
        }


        startActivity(
            LocationPicker.IntentBuilder()
                .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, 16.0))
                .activityKey("MediaActivity")
                .media(media!!)
                .build(requireActivity())
        )
    }

    fun onDescriptionEditClicked() {
        binding.progressBarEdit.visibility = View.VISIBLE
        binding.descriptionEdit.visibility = View.GONE
        descriptionList
    }

    private val descriptionList: Unit
        /**
         * Gets descriptions from wikitext
         */
        get() {
            compositeDisposable.add(
                mediaDataExtractor.getCurrentWikiText(
                    Objects.requireNonNull(media?.filename!!)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: String? -> extractCaptionDescription(s!!) },
                        { t: Throwable? -> Timber.e(t) })
            )
        }

    /**
     * Gets captions and descriptions and merge them according to language code and arranges it in a
     * single list.
     * Send the list to DescriptionEditActivity
     * @param s wikitext
     */
    private fun extractCaptionDescription(s: String) {
        val descriptions: LinkedHashMap<String, String> = getDescriptions(s)
        val captions: LinkedHashMap<String, String> = captionsList

        val descriptionAndCaptions: ArrayList<UploadMediaDetail> = ArrayList()

        if (captions.size >= descriptions.size) {
            for (mapElement: Map.Entry<*, *> in captions.entries) {
                val language: String = mapElement.key as String
                if (descriptions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language,
                            Objects.requireNonNull(descriptions[language]!!),
                            (mapElement.value as String?)!!
                        )
                    )
                } else {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language, "",
                            (mapElement.value as String?)!!
                        )
                    )
                }
            }
            for (mapElement: Map.Entry<*, *> in descriptions.entries) {
                val language: String = mapElement.key as String
                if (!captions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language,
                            Objects.requireNonNull(descriptions[language]!!),
                            ""
                        )
                    )
                }
            }
        } else {
            for (mapElement: Map.Entry<*, *> in descriptions.entries) {
                val language: String = mapElement.key as String
                if (captions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language, (mapElement.value as String?)!!,
                            Objects.requireNonNull(captions[language]!!)
                        )
                    )
                } else {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language, (mapElement.value as String?)!!,
                            ""
                        )
                    )
                }
            }
            for (mapElement: Map.Entry<*, *> in captions.entries) {
                val language: String = mapElement.key as String
                if (!descriptions.containsKey(language)) {
                    descriptionAndCaptions.add(
                        UploadMediaDetail(
                            language,
                            "",
                            Objects.requireNonNull(descriptions[language]!!)
                        )
                    )
                }
            }
        }
        val intent = Intent(requireContext(), DescriptionEditActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, descriptionAndCaptions)
        bundle.putString(WIKITEXT, s)
        bundle.putString(
            Prefs.DESCRIPTION_LANGUAGE,
            applicationKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, "")
        )
        bundle.putParcelable("media", media)
        intent.putExtras(bundle)
        startActivity(intent)
    }

    /**
     * Filters descriptions from current wikiText and arranges it in LinkedHashmap according to the
     * language code
     * @param s wikitext
     * @return LinkedHashMap<LanguageCode></LanguageCode>,Description>
     */
    private fun getDescriptions(s: String): LinkedHashMap<String, String> {
        val pattern: Pattern = Pattern.compile("[dD]escription *=(.*?)\n *\\|", Pattern.DOTALL)
        val matcher: Matcher = pattern.matcher(s)
        var description: String? = null
        if (matcher.find()) {
            description = matcher.group()
        }
        if (description == null) {
            return LinkedHashMap()
        }

        val descriptionList: LinkedHashMap<String, String> = LinkedHashMap()

        var count = 0 // number of "{{"
        var startCode = 0
        var endCode = 0
        var startDescription = 0
        var endDescription: Int
        val allLanguageCodes: HashSet<String> = HashSet(
            mutableListOf(
                "en",
                "es",
                "de",
                "ja",
                "fr",
                "ru",
                "pt",
                "it",
                "zh-hans",
                "zh-hant",
                "ar",
                "ko",
                "id",
                "pl",
                "nl",
                "fa",
                "hi",
                "th",
                "vi",
                "sv",
                "uk",
                "cs",
                "simple",
                "hu",
                "ro",
                "fi",
                "el",
                "he",
                "nb",
                "da",
                "sr",
                "hr",
                "ms",
                "bg",
                "ca",
                "tr",
                "sk",
                "sh",
                "bn",
                "tl",
                "mr",
                "ta",
                "kk",
                "lt",
                "az",
                "bs",
                "sl",
                "sq",
                "arz",
                "zh-yue",
                "ka",
                "te",
                "et",
                "lv",
                "ml",
                "hy",
                "uz",
                "kn",
                "af",
                "nn",
                "mk",
                "gl",
                "sw",
                "eu",
                "ur",
                "ky",
                "gu",
                "bh",
                "sco",
                "ast",
                "is",
                "mn",
                "be",
                "an",
                "km",
                "si",
                "ceb",
                "jv",
                "eo",
                "als",
                "ig",
                "su",
                "be-x-old",
                "la",
                "my",
                "cy",
                "ne",
                "bar",
                "azb",
                "mzn",
                "as",
                "am",
                "so",
                "pa",
                "map-bms",
                "scn",
                "tg",
                "ckb",
                "ga",
                "lb",
                "war",
                "zh-min-nan",
                "nds",
                "fy",
                "vec",
                "pnb",
                "zh-classical",
                "lmo",
                "tt",
                "io",
                "ia",
                "br",
                "hif",
                "mg",
                "wuu",
                "gan",
                "ang",
                "or",
                "oc",
                "yi",
                "ps",
                "tk",
                "ba",
                "sah",
                "fo",
                "nap",
                "vls",
                "sa",
                "ce",
                "qu",
                "ku",
                "min",
                "bcl",
                "ilo",
                "ht",
                "li",
                "wa",
                "vo",
                "nds-nl",
                "pam",
                "new",
                "mai",
                "sn",
                "pms",
                "eml",
                "yo",
                "ha",
                "gn",
                "frr",
                "gd",
                "hsb",
                "cv",
                "lo",
                "os",
                "se",
                "cdo",
                "sd",
                "ksh",
                "bat-smg",
                "bo",
                "nah",
                "xmf",
                "ace",
                "roa-tara",
                "hak",
                "bjn",
                "gv",
                "mt",
                "pfl",
                "szl",
                "bpy",
                "rue",
                "co",
                "diq",
                "sc",
                "rw",
                "vep",
                "lij",
                "kw",
                "fur",
                "pcd",
                "lad",
                "tpi",
                "ext",
                "csb",
                "rm",
                "kab",
                "gom",
                "udm",
                "mhr",
                "glk",
                "za",
                "pdc",
                "om",
                "iu",
                "nv",
                "mi",
                "nrm",
                "tcy",
                "frp",
                "myv",
                "kbp",
                "dsb",
                "zu",
                "ln",
                "mwl",
                "fiu-vro",
                "tum",
                "tet",
                "tn",
                "pnt",
                "stq",
                "nov",
                "ny",
                "xh",
                "crh",
                "lfn",
                "st",
                "pap",
                "ay",
                "zea",
                "bxr",
                "kl",
                "sm",
                "ak",
                "ve",
                "pag",
                "nso",
                "kaa",
                "lez",
                "gag",
                "kv",
                "bm",
                "to",
                "lbe",
                "krc",
                "jam",
                "ss",
                "roa-rup",
                "dv",
                "ie",
                "av",
                "cbk-zam",
                "chy",
                "inh",
                "ug",
                "ch",
                "arc",
                "pih",
                "mrj",
                "kg",
                "rmy",
                "dty",
                "na",
                "ts",
                "xal",
                "wo",
                "fj",
                "tyv",
                "olo",
                "ltg",
                "ff",
                "jbo",
                "haw",
                "ki",
                "chr",
                "sg",
                "atj",
                "sat",
                "ady",
                "ty",
                "lrc",
                "ti",
                "din",
                "gor",
                "lg",
                "rn",
                "bi",
                "cu",
                "kbd",
                "pi",
                "cr",
                "koi",
                "ik",
                "mdf",
                "bug",
                "ee",
                "shn",
                "tw",
                "dz",
                "srn",
                "ks",
                "test",
                "en-x-piglatin",
                "ab"
            )
        )
        var i = 0
        while (i < description.length - 1) {
            if (description.startsWith("{{", i)) {
                if (count == 0) {
                    startCode = i
                    endCode = description.indexOf("|", i)
                    startDescription = endCode + 1
                    if (description.startsWith("1=", endCode + 1)) {
                        startDescription += 2
                        i += 2
                    }
                }
                i++
                count++
            } else if (description.startsWith("}}", i)) {
                count--
                if (count == 0) {
                    endDescription = i
                    val languageCode: String = description.substring(startCode + 2, endCode)
                    val languageDescription: String =
                        description.substring(startDescription, endDescription)
                    if (allLanguageCodes.contains(languageCode)) {
                        descriptionList[languageCode] = languageDescription
                    }
                }
                i++
            }
            i++
        }
        return descriptionList
    }

    private val captionsList: LinkedHashMap<String, String>
        /**
         * Gets list of caption and arranges it in a LinkedHashmap according to the language code
         * @return LinkedHashMap<LanguageCode></LanguageCode>,Caption>
         */
        get() {
            val captionList: LinkedHashMap<String, String> =
                LinkedHashMap()
            val captions: Map<String, String> = media!!.captions
            for (map: Map.Entry<String, String> in captions.entries) {
                val language: String = map.key
                val languageCaption: String = map.value
                captionList[language] = languageCaption
            }
            return captionList
        }

    /**
     * Adds caption to the map and updates captions
     * @param mediaDetail UploadMediaDetail
     * @param updatedCaptions updated captionds
     */
    private fun updateCaptions(
        mediaDetail: UploadMediaDetail,
        updatedCaptions: MutableMap<String, String>
    ) {
        updatedCaptions[mediaDetail.languageCode!!] = mediaDetail.captionText
        media!!.captions = updatedCaptions
    }

    @SuppressLint("StringFormatInvalid")
    fun onDeleteButtonClicked() {
        if (getUserName(requireContext()) != null && getUserName(requireContext()) == media!!.author) {
            val languageAdapter: ArrayAdapter<String> = ArrayAdapter(
                requireActivity(),
                R.layout.simple_spinner_dropdown_list, reasonList
            )
            val spinner = Spinner(activity)
            spinner.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            spinner.adapter = languageAdapter
            spinner.gravity = 17

            val dialog: AlertDialog? = showAlertDialog(
                requireActivity(),
                getString(R.string.nominate_delete),
                null,
                getString(R.string.about_translate_proceed),
                getString(R.string.about_translate_cancel),
                { onDeleteClicked(spinner) },
                {},
                spinner
            )
            if (isDeleted) {
                dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            }
        } else if (getUserName(requireContext()) != null) {
            val input = EditText(activity)
            input.requestFocus()
            val d: AlertDialog? = showAlertDialog(
                requireActivity(),
                null,
                getString(R.string.dialog_box_text_nomination, media!!.displayTitle),
                getString(R.string.ok),
                getString(R.string.cancel),
                {
                    val reason: String = input.text.toString()
                    onDeleteClickedDialogText(reason)
                },
                {},
                input
            )
            input.addTextChangedListener(object : TextWatcher {
                fun handleText() {
                    val okButton: Button = d!!.getButton(AlertDialog.BUTTON_POSITIVE)
                    if (input.text.isEmpty() || isDeleted) {
                        okButton.isEnabled = false
                    } else {
                        okButton.isEnabled = true
                    }
                }

                override fun afterTextChanged(arg0: Editable) {
                    handleText()
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }
            })
            d!!.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        }
    }

    @SuppressLint("CheckResult")
    private fun onDeleteClicked(spinner: Spinner) {
        applicationKvStore.putBoolean(
            String.format(
                NOMINATING_FOR_DELETION_MEDIA,
                media!!.imageUrl
            ), true
        )
        enableProgressBar()
        val reason: String = reasonListEnglishMappings[spinner.selectedItemPosition]
        val finalReason: String = reason
        val resultSingle: Single<Boolean> = reasonBuilder.getReason(media, reason)
            .flatMap {
                deleteHelper.makeDeletion(
                    context, media, finalReason
                )
            }
        resultSingle
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::handleDeletionResult, this::handleDeletionError);
    }

    /**
     * Disables Progress Bar and Update delete button text.
     */
    private fun disableProgressBar() {
        activity?.run {
            runOnUiThread(Runnable {
                binding.progressBarDeletion.visibility = View.GONE
            })
        } ?: return // Prevent NullPointerException when fragment is not attached to activity
    }

    private fun handleDeletionResult(success: Boolean) {
        if (success) {
            binding.nominateDeletion.text = getString(R.string.nominated_for_deletion_btn)
            ViewUtil.showLongSnackbar(requireView(), getString(R.string.nominated_for_deletion))
            disableProgressBar()
            checkAndClearDeletionFlag()
        } else {
            disableProgressBar()
        }
    }

    private fun handleDeletionError(throwable: Throwable) {
        throwable.printStackTrace()
        disableProgressBar()
        checkAndClearDeletionFlag()
    }

    private fun checkAndClearDeletionFlag() {
        if (applicationKvStore
            .getBoolean(format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl), false)
        ) {
            applicationKvStore.remove(format(NOMINATING_FOR_DELETION_MEDIA, media!!.imageUrl))
            callback!!.nominatingForDeletion(index)
        }
    }

    @SuppressLint("CheckResult")
    private fun onDeleteClickedDialogText(reason: String) {
        applicationKvStore.putBoolean(
            String.format(
                NOMINATING_FOR_DELETION_MEDIA,
                media!!.imageUrl
            ), true
        )
        enableProgressBar()
        val resultSingletext: Single<Boolean> = reasonBuilder.getReason(media, reason)
            .flatMap { _ ->
                deleteHelper.makeDeletion(
                    context, media, reason
                )
            }
        resultSingletext
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::handleDeletionResult, this::handleDeletionError);
    }

    private fun onSeeMoreClicked() {
        if (binding.nominatedDeletionBanner.visibility == View.VISIBLE && activity != null) {
            handleWebUrl(requireContext(), Uri.parse(media!!.pageTitle.mobileUri))
        }
    }

    private fun onAuthorViewClicked() {
        if (media == null || media!!.user == null) {
            return
        }
        if (sessionManager.userName == null) {
            val userProfileLink: String = BuildConfig.COMMONS_URL + "/wiki/User:" + media!!.user
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(userProfileLink))
            startActivity(browserIntent)
            return
        }
        ProfileActivity.startYourself(
            requireActivity(),  // Ensure this is a non-null Activity context
            media?.user ?: "",  // Provide a fallback value if media?.user is null
            sessionManager.userName != media?.user  // This can remain as is, null check will apply
        )

    }

    /**
     * Enable Progress Bar and Update delete button text.
     */
    private fun enableProgressBar() {
        binding.progressBarDeletion.visibility = View.VISIBLE
        binding.nominateDeletion.text = requireContext().getString(R.string.nominate_deletion)
        isDeleted = true
    }

    private fun rebuildCatList(categories: List<String>) {
        binding.mediaDetailCategoryContainer.removeAllViews()
        for (category: String in categories) {
            binding.mediaDetailCategoryContainer.addView(
                buildCatLabel(
                    sanitise(category),
                    binding.mediaDetailCategoryContainer
                )
            )
        }
    }

    //As per issue #1826(see https://github.com/commons-app/apps-android-commons/issues/1826),
    // some categories come suffixed with strings prefixed with |. As per the discussion
    //that was meant for alphabetical sorting of the categories and can be safely removed.
    private fun sanitise(category: String): String {
        val indexOfPipe: Int = category.indexOf('|')
        if (indexOfPipe != -1) {
            //Removed everything after '|'
            return category.substring(0, indexOfPipe)
        }
        return category
    }

    /**
     * Add view to depictions obtained also tapping on depictions should open the url
     */
    private fun buildDepictLabel(
        depictionName: String?,
        entityId: String,
        depictionContainer: LinearLayout
    ): View {
        val item: View = LayoutInflater.from(context)
            .inflate(R.layout.detail_category_item, depictionContainer, false)
        val textView: TextView = item.findViewById(R.id.mediaDetailCategoryItemText)
        textView.text = depictionName
        item.setOnClickListener {
            val intent = Intent(
                context,
                WikidataItemDetailsActivity::class.java
            )
            intent.putExtra("wikidataItemName", depictionName)
            intent.putExtra("entityId", entityId)
            intent.putExtra("fragment", "MediaDetailFragment")
            requireContext().startActivity(intent)
        }
        return item
    }

    private fun buildCatLabel(catName: String, categoryContainer: ViewGroup): View {
        val item: View = LayoutInflater.from(context)
            .inflate(R.layout.detail_category_item, categoryContainer, false)
        val textView: TextView = item.findViewById(R.id.mediaDetailCategoryItemText)

        textView.text = catName
        if (getString(R.string.detail_panel_cats_none) != catName) {
            textView.setOnClickListener {
                // Open Category Details page
                val intent = Intent(context, CategoryDetailsActivity::class.java)
                intent.putExtra("categoryName", catName)
                requireContext().startActivity(intent)
            }
        }
        return item
    }

    /**
     * Returns captions for media details
     *
     * @param media object of class media
     * @return caption as string
     */
    private fun prettyCaption(media: Media): String {
        for (caption: String in media.captions.values) {
            return if (caption == "") {
                getString(R.string.detail_caption_empty)
            } else {
                caption
            }
        }
        return getString(R.string.detail_caption_empty)
    }

    private fun prettyDescription(media: Media): String {
        var description: String? = chooseDescription(media)
        if (description!!.isNotEmpty()) {
            // Remove img tag that sometimes appears as a blue square in the app,
            // see https://github.com/commons-app/apps-android-commons/issues/4345
            description = description.replace("[<](/)?img[^>]*[>]".toRegex(), "")
        }
        return description.ifEmpty { getString(R.string.detail_description_empty) }
    }

    private fun chooseDescription(media: Media): String? {
        val descriptions: Map<String, String> = media.descriptions
        val multilingualDesc: String? = descriptions[Locale.getDefault().language]
        if (multilingualDesc != null) {
            return multilingualDesc
        }
        for (description: String in descriptions.values) {
            return description
        }
        return media.fallbackDescription
    }

    private fun prettyDiscussion(discussion: String): String {
        return discussion.ifEmpty { getString(R.string.detail_discussion_empty) }
    }

    private fun prettyLicense(media: Media): String {
        val licenseKey: String? = media.license
        Timber.d("Media license is: %s", licenseKey)
        if (licenseKey == null || licenseKey == "") {
            return getString(R.string.detail_license_empty)
        }
        return licenseKey
    }

    private fun prettyUploadedDate(media: Media): String {
        val date: Date? = media.dateUploaded
        if (date?.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available"
        }
        return getDateStringWithSkeletonPattern(date, "dd MMM yyyy")
    }

    /**
     * Returns the coordinates nicely formatted.
     *
     * @return Coordinates as text.
     */
    private fun prettyCoordinates(media: Media): String {
        if (media.coordinates == null) {
            return getString(R.string.media_detail_coordinates_empty)
        }
        return media.coordinates!!.getPrettyCoordinateString()
    }

    override fun updateCategoryDisplay(categories: List<String>?): Boolean {
        if (categories == null) {
            return false
        } else {
            rebuildCatList(categories)
            return true
        }
    }

    fun showCaptionAndDescription() {
        if (binding.dummyCaptionDescriptionContainer.visibility == View.GONE) {
            binding.dummyCaptionDescriptionContainer.visibility = View.VISIBLE
            setUpCaptionAndDescriptionLayout()
        } else {
            binding.dummyCaptionDescriptionContainer.visibility = View.GONE
        }
    }

    /**
     * setUp Caption And Description Layout
     */
    private fun setUpCaptionAndDescriptionLayout() {
        val captions: List<Caption> = captions

        if (descriptionHtmlCode == null) {
            binding.showCaptionsBinding.pbCircular.visibility = View.VISIBLE
        }

        description
        val adapter = CaptionListViewAdapter(captions)
        binding.showCaptionsBinding.captionListview.adapter = adapter
    }

    private val captions: List<Caption>
        /**
         * Generate the caption with language
         */
        get() {
            val captionList: MutableList<Caption> =
                ArrayList()
            val captions: Map<String, String> = media!!.captions
            val appLanguageLookUpTable =
                AppLanguageLookUpTable(requireContext())
            for (map: Map.Entry<String, String> in captions.entries) {
                val language: String? = appLanguageLookUpTable.getLocalizedName(map.key)
                val languageCaption: String = map.value
                captionList.add(Caption(language, languageCaption))
            }

            if (captionList.size == 0) {
                captionList.add(Caption("", "No Caption"))
            }
            return captionList
        }

    private val description: Unit
        get() {
            compositeDisposable.add(
                mediaDataExtractor.getHtmlOfPage(
                    Objects.requireNonNull(media?.filename!!)
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { s: String -> extractDescription(s) },
                        { t: Throwable? -> Timber.e(t) })
            )
        }

    /**
     * extract the description from html of imagepage
     */
    private fun extractDescription(s: String) {
        val descriptionClassName = "<td class=\"description\">"
        val start: Int = s.indexOf(descriptionClassName) + descriptionClassName.length
        val end: Int = s.indexOf("</td>", start)
        descriptionHtmlCode = ""
        for (i in start until end) {
            descriptionHtmlCode += s.toCharArray()[i]
        }

        binding.showCaptionsBinding.descriptionWebview
            .loadDataWithBaseURL(null, descriptionHtmlCode!!, "text/html", "utf-8", null)
        binding.showCaptionsBinding.pbCircular.visibility = View.GONE
    }

    /**
     * Handle back event when fragment when showCaptionAndDescriptionContainer is visible
     */
    private fun handleBackEvent(view: View) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View, keycode: Int, keyEvent: KeyEvent): Boolean {
                if (keycode == KeyEvent.KEYCODE_BACK) {
                    if (binding.dummyCaptionDescriptionContainer.visibility == View.VISIBLE) {
                        binding.dummyCaptionDescriptionContainer.visibility =
                            View.GONE
                        return true
                    }
                }
                return false
            }
        })
    }


    interface Callback {
        fun nominatingForDeletion(index: Int)
    }

    /**
     * Called when the image background color is changed.
     * You should pass a useable color, not a resource id.
     * @param color
     */
    fun onImageBackgroundChanged(color: Int) {
        val currentColor: Int = imageBackgroundColor
        if (currentColor == color) {
            return
        }

        binding.mediaDetailImageView.setBackgroundColor(color)
        imageBackgroundColorPref.edit().putInt(IMAGE_BACKGROUND_COLOR, color).apply()
    }

    private val imageBackgroundColorPref: SharedPreferences
        get() = requireContext().getSharedPreferences(
            IMAGE_BACKGROUND_COLOR + media!!.pageId,
            Context.MODE_PRIVATE
        )

    private val imageBackgroundColor: Int
        get() {
            val imageBackgroundColorPref: SharedPreferences =
                imageBackgroundColorPref
            return imageBackgroundColorPref.getInt(
                IMAGE_BACKGROUND_COLOR,
                DEFAULT_IMAGE_BACKGROUND_COLOR
            )
        }

    companion object {
        private const val IMAGE_BACKGROUND_COLOR: String = "image_background_color"
        const val DEFAULT_IMAGE_BACKGROUND_COLOR: Int = 0

        @JvmStatic
        fun forMedia(
            index: Int,
            editable: Boolean,
            isCategoryImage: Boolean,
            isWikipediaButtonDisplayed: Boolean
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
    modifier: Modifier = Modifier,
    commonsContainerState: MediaDetailViewModel.FileUsagesContainerState,
    globalContainerState: MediaDetailViewModel.FileUsagesContainerState,
) {
    var isCommonsListExpanded by rememberSaveable { mutableStateOf(true) }
    var isOtherWikisListExpanded by rememberSaveable { mutableStateOf(true) }

    val uriHandle = LocalUriHandler.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.usages_on_commons_heading),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { isCommonsListExpanded = !isCommonsListExpanded }) {
                Icon(
                    imageVector = if (isCommonsListExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
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
                                style = MaterialTheme.typography.titleSmall
                            )
                        })
                    } else {
                        data.forEach { usage ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        text = stringResource(R.string.bullet_point),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        modifier = Modifier.clickable {
                                            usage.link?.let { uriHandle.openUri(it) }
                                        },
                                        text = usage.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color(0xFF5A6AEC),
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                is MediaDetailViewModel.FileUsagesContainerState.Error -> {
                    ListItem(headlineContent = {
                        Text(
                            text = commonsContainerState.errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.titleSmall
                        )
                    })
                }
                MediaDetailViewModel.FileUsagesContainerState.Initial -> {}
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.usages_on_other_wikis_heading),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall
            )
            IconButton(onClick = { isOtherWikisListExpanded = !isOtherWikisListExpanded }) {
                Icon(
                    imageVector = if (isOtherWikisListExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
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
                                style = MaterialTheme.typography.titleSmall
                            )
                        })
                    } else {
                        data.forEach { usage ->
                            ListItem(
                                leadingContent = {
                                    Text(
                                        text = stringResource(R.string.bullet_point),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                headlineContent = {
                                    Text(
                                        modifier = Modifier.clickable {
                                            usage.link?.let { uriHandle.openUri(it) }
                                        },
                                        text = usage.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color(0xFF5A6AEC),
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                is MediaDetailViewModel.FileUsagesContainerState.Error -> {
                    ListItem(headlineContent = {
                        Text(
                            text = globalContainerState.errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.titleSmall
                        )
                    })
                }
                MediaDetailViewModel.FileUsagesContainerState.Initial -> {}
            }
        }
    }
}