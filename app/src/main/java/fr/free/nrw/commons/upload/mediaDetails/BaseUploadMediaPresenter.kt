package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.SimilarImageInterface
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail

/**
 * Base presenter class for upload media presentation, handling navigation,
 * media detail updates, similar image checks, and delegating image quality validation.
 */
abstract class BaseUploadMediaPresenter(
    protected val repository: UploadRepository,
) : UploadMediaDetailsContract.UserActionListener,
    SimilarImageInterface {
    protected var view: UploadMediaDetailsContract.View = UploadMediaPresenter.DUMMY

    protected abstract val qualityHelper: UploadMediaQualityHelper

    override fun setupBasicKvStoreFactory(factory: (String) -> BasicKvStore) {
        qualityHelper.setupBasicKvStoreFactory(factory)
    }

    override fun getImageQuality(
        uploadItemIndex: Int,
        inAppPictureLocation: LatLng?,
        activity: Activity,
    ): Boolean = qualityHelper.getImageQuality(uploadItemIndex, inAppPictureLocation, activity)

    override fun checkImageQuality(
        uploadItem: UploadItem,
        index: Int,
    ) {
        qualityHelper.checkImageQuality(uploadItem, index)
    }

    override fun updateImageQualitiesJSON(
        size: Int,
        index: Int,
    ) {
        qualityHelper.updateImageQualitiesJSON(size, index)
    }

    /**
     * Copies the caption and description of the current item to the subsequent media
     */
    override fun copyTitleAndDescriptionToSubsequentMedia(indexInViewFlipper: Int) {
        for (i in indexInViewFlipper + 1 until repository.getCount()) {
            val subsequentUploadItem = repository.getUploads()[i]
            subsequentUploadItem.uploadMediaDetails =
                deepCopy(
                    repository.getUploads()[indexInViewFlipper].uploadMediaDetails,
                ).toMutableList()
        }
    }

    /**
     * Fetches and set the caption and description of the item
     */
    override fun fetchTitleAndDescription(indexInViewFlipper: Int) =
        view.updateMediaDetails(repository.getUploads()[indexInViewFlipper].uploadMediaDetails)

    private fun deepCopy(uploadMediaDetails: List<UploadMediaDetail>) =
        uploadMediaDetails.map(UploadMediaDetail::javaCopy)

    override fun onMapIconClicked(indexInViewFlipper: Int) =
        view.showExternalMap(repository.getUploads()[indexInViewFlipper])

    override fun onEditButtonClicked(indexInViewFlipper: Int) =
        view.showEditActivity(repository.getUploads()[indexInViewFlipper])

    /**
     * notifies the user that a similar image exists
     */
    override fun showSimilarImageFragment(
        originalFilePath: String?,
        possibleFilePath: String?,
        similarImageCoordinates: ImageCoordinates?,
    ) = view.showSimilarImageFragment(originalFilePath, possibleFilePath, similarImageCoordinates)
}
