package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.view.View
import androidx.activity.result.ActivityResult
import fr.free.nrw.commons.databinding.FragmentUploadMediaDetailFragmentBinding
import fr.free.nrw.commons.edit.EditActivity
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import timber.log.Timber

/** Routes presenter updates to the image, metadata, location, and dialog views. */
internal class UploadMediaDetailView(
    val dialogs: UploadMediaDetailDialogs = UploadMediaDetailDialogs(),
    val locationController: UploadMediaLocationController = UploadMediaLocationController(),
) : UploadMediaDetailsContract.View,
    UploadMediaDetailsContract.Dialogs by dialogs,
    UploadMediaDetailsContract.Location by locationController {
    lateinit var fragment: UploadMediaDetailFragment
    lateinit var bindingProvider: () -> FragmentUploadMediaDetailFragmentBinding?
    lateinit var adapterProvider: () -> UploadMediaDetailAdapter
    lateinit var storeProvider: () -> BasicKvStore?
    lateinit var editLauncher: (Intent) -> Unit
    lateinit var locationLauncher: (Intent) -> Unit
    val metadata = UploadMediaMetadataController(this)
    private val currentBinding get() = bindingProvider()
    private val binding get() = currentBinding!!

    init {
        dialogs.view = this
        locationController.view = this
    }

    /**
     * Tracks the URI of an edited image (after rotation/crop in EditActivity).
     * When set, this takes priority over the original uploadItem.mediaUri so that
     * the async onImageProcessed callback doesn't overwrite the preview with the
     * original (pre-edit) image.
     */
    var editedImageUri: Uri? = null

    /** Displays the edited image in preference to an asynchronous original preview. */
    override fun onImageProcessed(uploadItem: UploadItem) {
        if (currentBinding == null) {
            return
        }
        // If the user has already edited the image, show the edited version
        // instead of the original. This prevents the async preProcessImage
        // callback from overwriting the edited preview.
        val uriToShow = editedImageUri ?: uploadItem.mediaUri
        binding.backgroundImage.setImageURI(uriToShow)
    }

    /** Updates progress through the upload activity. */
    override fun showProgress(shouldShow: Boolean) {
        if (fragment.fragmentCallback == null) {
            return
        }
        fragment.fragmentCallback!!.showProgress(shouldShow)
    }

    /** Advances after the current image has passed validation. */
    override fun onImageValidationSuccess() {
        if (fragment.fragmentCallback == null) {
            return
        }
        fragment.fragmentCallback!!.onNextButtonClicked(fragment.indexOfFragment)
    }

    /**
     * Launches the image editing activity to edit the specified UploadItem.
     *
     * @param uploadItem The UploadItem to be edited.
     *
     * This method is called to start the image editing activity for a specific UploadItem.
     * It creates an intent to launch the EditActivity, and passes the image file path as an
     * extra in the intent. The activity is started using resultLauncher that handles the
     * result in respective callback.
     */
    override fun showEditActivity(uploadItem: UploadItem) {
        val intent = Intent(fragment.context, EditActivity::class.java)
        // used the mediaUri from the uploadItem.
        // now, if the image is edited, uploadItem.mediaUri points to the new rotated file.
        // if it is not edited, it points to the original.
        val currentPath = uploadItem.mediaUri?.path ?: fragment.uploadableFile?.getFilePath().toString()
        intent.putExtra("image", currentPath)
        editLauncher(intent)
    }

    /** Displays an upload message. */
    override fun showMessage(
        message: String,
        colorResourceId: Int,
    ) = showLongToast(fragment.requireContext(), message)

    /** Displays an upload message. */
    override fun showMessage(
        stringResourceId: Int,
        colorResourceId: Int,
    ) = showLongToast(fragment.requireContext(), stringResourceId)

    /** Applies captions and descriptions supplied by the presenter. */
    override fun updateMediaDetails(uploadMediaDetails: List<UploadMediaDetail>) =
        metadata.updateMediaDetails(uploadMediaDetails)

    /** Passes a nearby place to the metadata controller. */
    override fun onNearbyPlaceFound(
        uploadItem: UploadItem,
        place: Place?,
    ) = metadata.onNearbyPlaceFound(uploadItem, place)

    /** Applies the speech recognition result to the description. */
    fun onVoiceInput(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultData =
                result.data!!.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS,
                )
            adapterProvider().handleSpeechResult(resultData!![0])
        } else {
            Timber.e("Error %s", result.resultCode)
        }
    }

    /**
     * Returns the upload item shown by this fragment. It is looked up each time instead of being
     * kept in a field, because the fragment can be recreated while the editor or the location
     * picker is open, and their result is then delivered to the new fragment.
     */
    internal fun currentUploadItem(): UploadItem? =
        fragment.fragmentCallback?.getIndexInViewFlipper(fragment)?.let { fragment.presenter.getUploadItem(it) }
}
