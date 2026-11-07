package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.chrisbanes.photoview.PhotoView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentUploadMediaDetailFragmentBinding
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter.Companion.presenterCallback
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.handleKeyboardInsets
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.Objects
import javax.inject.Inject
import javax.inject.Named

/** Owns the lifecycle and setup of one upload media details step. */
class UploadMediaDetailFragment private constructor(
    private val mediaView: UploadMediaDetailView,
) : UploadBaseFragment(),
    UploadMediaDetailsContract.View by mediaView,
    UploadMediaDetailAdapter.EventListener by mediaView.metadata {
    /** Creates a fragment with its own upload view state. */
    constructor() : this(UploadMediaDetailView())

    init {
        mediaView.fragment = this
        mediaView.bindingProvider = { _binding }
        mediaView.adapterProvider = { uploadMediaDetailAdapter }
        mediaView.storeProvider = { basicKvStore }
        mediaView.editLauncher = { startForEditActivityResult.launch(it) }
        mediaView.locationLauncher = { startForResult.launch(it) }
    }

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var startForEditActivityResult: ActivityResultLauncher<Intent>
    private lateinit var voiceInputResultLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var presenter: UploadMediaDetailsContract.UserActionListener

    @Inject
    @field:Named("default_preferences")
    lateinit var defaultKvStore: JsonKvStore

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    /**
     * True when user removes location from the current image
     */
    var hasUserRemovedLocation = false

    /**
     * inAppPictureLocation: use location recorded while using the in-app camera if device camera
     * does not record it in the EXIF
     */
    var inAppPictureLocation: LatLng? = null

    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: FragmentUploadMediaDetailFragmentBinding? = null
    private val binding: FragmentUploadMediaDetailFragmentBinding get() = _binding!!

    private var basicKvStore: BasicKvStore? = null
    internal var uploadableFile: UploadableFile? = null
    internal var place: Place? = null
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter
    var indexOfFragment = 0
    var isExpanded = true
    var fragmentCallback: UploadMediaDetailFragmentCallback? = null
        set(value) {
            field = value
            UploadMediaPresenter.presenterCallback = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null && uploadableFile == null) {
            uploadableFile = savedInstanceState.getParcelable(UPLOADABLE_FILE)
        }
        if (savedInstanceState != null) {
            mediaView.editedImageUri = savedInstanceState.getParcelable(EDITED_IMAGE_URI)
        }
        // Register the ActivityResultLauncher for LocationPickerActivity
        startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                mediaView.locationController.onCameraPosition(result)
            }
        startForEditActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onEditActivityResult)
        voiceInputResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                mediaView.metadata::onVoiceInput,
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUploadMediaDetailFragmentBinding.inflate(inflater, container, false)
        _binding!!.mediaDetailCardView.handleKeyboardInsets()
        // intialise the adapter early to prevent uninitialized access
        uploadMediaDetailAdapter =
            UploadMediaDetailAdapter(
                this,
                defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, "")!!,
                recentLanguagesDao,
                voiceInputResultLauncher,
            )
        uploadMediaDetailAdapter.callback =
            UploadMediaDetailAdapter.Callback { titleStringID: Int, messageStringId: Int ->
                mediaView.dialogs.showInfoAlert(titleStringID, messageStringId)
            }
        uploadMediaDetailAdapter.eventListener = mediaView.metadata
        binding.rvDescriptions.layoutManager = LinearLayoutManager(context)
        binding.rvDescriptions.adapter = uploadMediaDetailAdapter
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        basicKvStore = BasicKvStore(requireActivity(), "CurrentUploadImageQualities")

        // restore adapter items from savedInstanceState if available
        if (savedInstanceState != null) {
            val savedItems = savedInstanceState.getParcelableArrayList<UploadMediaDetail>(UPLOAD_MEDIA_DETAILS)
            Timber.d("Restoring state: savedItems size = %s", savedItems?.size ?: "null")
            if (savedItems != null && savedItems.isNotEmpty()) {
                uploadMediaDetailAdapter.items = savedItems
                // only call setUploadMediaDetails if indexOfFragment is valid
                if (fragmentCallback != null) {
                    indexOfFragment = fragmentCallback!!.getIndexInViewFlipper(this)
                    if (indexOfFragment >= 0) {
                        presenter.setUploadMediaDetails(uploadMediaDetailAdapter.items, indexOfFragment)
                        Timber.d("Restored and set upload media details for index %d", indexOfFragment)
                    } else {
                        Timber.w("Invalid indexOfFragment %d, skipping setUploadMediaDetails", indexOfFragment)
                    }
                } else {
                    Timber.w("fragmentCallback is null, skipping setUploadMediaDetails")
                }
            } else {
                // initialize with a default UploadMediaDetail if saved state is empty or null
                uploadMediaDetailAdapter.items = mutableListOf(UploadMediaDetail())
                Timber.d("Initialized default UploadMediaDetail due to empty or null savedItems")
            }
        } else {
            // intitialise with a default UploadMediaDetail for fresh fragment
            if (uploadMediaDetailAdapter.items.isEmpty()) {
                uploadMediaDetailAdapter.items = mutableListOf(UploadMediaDetail())
                Timber.d("Initialized default UploadMediaDetail for new fragment")
            }
        }

        if (fragmentCallback != null) {
            indexOfFragment = fragmentCallback!!.getIndexInViewFlipper(this)
            Timber.d("Fragment callback present, indexOfFragment = %d", indexOfFragment)
            initializeFragment()
        } else {
            Timber.w("Fragment callback is null, skipping initializeFragment")
        }

        try {
            if (indexOfFragment >= 0 &&
                !presenter.getImageQuality(indexOfFragment, inAppPictureLocation, requireActivity())
            ) {
                Timber.d("Image quality check failed, redirecting to MainActivity")
                startActivityWithFlags(
                    requireActivity(),
                    MainActivity::class.java,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during image quality check")
        }
    }

    internal fun initializeFragment() {
        if (_binding == null) {
            return
        }
        binding.tvTitle.text =
            getString(
                R.string.step_count,
                (indexOfFragment + 1),
                fragmentCallback!!.totalNumberOfSteps,
                getString(R.string.media_detail_step_title),
            )
        binding.tooltip.setOnClickListener {
            mediaView.dialogs.showInfoAlert(
                R.string.media_detail_step_title,
                R.string.media_details_tooltip,
            )
        }
        presenter.onAttachView(mediaView)
        presenter.setupBasicKvStoreFactory { BasicKvStore(requireActivity(), it) }

        presenter.receiveImage(uploadableFile, place, inAppPictureLocation)

        if (binding.backgroundImage is PhotoView) {
            (binding.backgroundImage as PhotoView).setMaximumScale(10.0f)
            Timber.d("PhotoView max scale set to 10.0f for deeper zoom.")
        }

        with(binding) {
            if (indexOfFragment == 0) {
                btnPrevious.isEnabled = false
                btnPrevious.alpha = 0.5f
            } else {
                btnPrevious.isEnabled = true
                btnPrevious.alpha = 1.0f
            }

            // If the image EXIF data contains the location, show the map icon with a green tick
            if (inAppPictureLocation != null || (uploadableFile != null && uploadableFile!!.hasLocation())) {
                val mapTick =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_available_20dp)
                locationImageView.setImageDrawable(mapTick)
                locationTextView.setText(R.string.edit_location)
            } else {
                // Otherwise, show the map icon with a red question mark
                val mapQuestionMark =
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_map_not_available_20dp,
                    )
                locationImageView.setImageDrawable(mapQuestionMark)
                locationTextView.setText(R.string.add_location)
            }

            // If this is the last media, we have nothing to copy, lets not show the button
            btnCopySubsequentMedia.visibility =
                if (indexOfFragment == fragmentCallback!!.totalNumberOfSteps - 4) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            // Jpegtran only supports lossless JPEG rotation, so we disable editing for other formats
            val filePath = uploadableFile?.getFilePath()?.toString() ?: ""
            val isJpeg =
                filePath.endsWith(".jpeg", ignoreCase = true) ||
                    filePath.endsWith(".jpg", ignoreCase = true)
            llEditImage.visibility = View.VISIBLE
            llEditImage.alpha = if (isJpeg) 1.0f else 0.5f
            // update the click listener to handle the both cases
            llEditImage.setOnClickListener {
                if (isJpeg) {
                    presenter.onEditButtonClicked(indexOfFragment)
                } else {
                    // dialog for non jpg formats
                    showAlertDialog(
                        requireActivity(),
                        getString(R.string.edit_image),
                        getString(R.string.edit_unsupported_format_explanation),
                        getString(R.string.ok),
                        null,
                    )
                }
            }

            btnNext.setOnClickListener {
                presenter.displayLocDialog(
                    indexOfFragment,
                    inAppPictureLocation,
                    hasUserRemovedLocation,
                )
            }
            btnPrevious.setOnClickListener { fragmentCallback?.onPreviousButtonClicked(indexOfFragment) }
            llContainerTitle.setOnClickListener { mediaView.metadata.expandCollapseLlMediaDetail(!isExpanded) }
            llLocationStatus.setOnClickListener { presenter.onMapIconClicked(indexOfFragment) }
            btnCopySubsequentMedia.setOnClickListener { mediaView.metadata.onButtonCopyTitleDescToSubsequentMedia() }
        }

        mediaView.metadata.attachImageViewScaleChangeListener()
    }

    private fun onEditActivityResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data!!.getStringExtra("editedImageFilePath")

            if (Objects.equals(path, "Error")) {
                Timber.e("Error in editing image")
                return
            }
            try {
                val editedFile = File(path!!)
                val editedUri = Uri.fromFile(editedFile)

                // Store the edited URI so that any future onImageProcessed callback
                // (e.g. from an async receiveImage after activity recreation) won't
                // overwrite the preview with the original image.
                mediaView.editedImageUri = editedUri

                if (_binding != null) {
                    binding.backgroundImage.setImageURI(editedUri)
                }
                mediaView.editableUploadItem!!.setContentAndMediaUri(editedUri)
                // Update uploadableFile so re-opening the edit screen loads
                // the already-edited image instead of the original
                uploadableFile = UploadableFile(editedUri, editedFile)
                fragmentCallback!!.changeThumbnail(
                    indexOfFragment,
                    path,
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    /** Preserves the upload UI's exception handling around file and dialog operations. */
    internal fun handleUploadExceptions(
        action: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            action()
        } catch (e: Exception) {
            onError(e)
        }
    }

    /** Refreshes metadata when this upload step becomes visible. */
    override fun onBecameVisible() {
        super.onBecameVisible()
        mediaView.metadata.onBecameVisible()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDetachView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (uploadableFile != null) {
            outState.putParcelable(UPLOADABLE_FILE, uploadableFile)
        }
        if (mediaView.editedImageUri != null) {
            outState.putParcelable(EDITED_IMAGE_URI, mediaView.editedImageUri)
        }
        outState.putParcelableArrayList(
            UPLOAD_MEDIA_DETAILS,
            ArrayList(uploadMediaDetailAdapter.items),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    interface UploadMediaDetailFragmentCallback : Callback {
        fun deletePictureAtIndex(index: Int)

        fun changeThumbnail(
            index: Int,
            uri: String,
        )
    }

    companion object {
        /**
         * A key for applicationKvStore. By this key we can retrieve the location of last UploadItem ex.
         * 12.3433,54.78897 from applicationKvStore.
         */
        const val LAST_LOCATION: String = "last_location_while_uploading"
        const val LAST_ZOOM: String = "last_zoom_level_while_uploading"
        const val UPLOADABLE_FILE: String = "uploadable_file"
        const val UPLOAD_MEDIA_DETAILS: String = "upload_media_detail_adapter"
        private const val EDITED_IMAGE_URI: String = "edited_image_uri"
    }
}
