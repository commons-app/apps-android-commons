package fr.free.nrw.commons.contributions

import android.app.Activity
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore

/**
 * Applies shared gallery and camera file-picker settings.
 *
 * @param activity activity configuring the picker
 * @param defaultKvStore preferences supplying the copy-to-gallery setting
 * @param allowMultipleUploads whether the gallery can select multiple images
 */
internal fun configureContributionFilePicker(
    activity: Activity,
    defaultKvStore: JsonKvStore,
    allowMultipleUploads: Boolean,
) {
    val copyToExternalStorage = defaultKvStore.getBoolean("useExternalStorage", true)
    FilePicker
        .configuration(activity)
        .setCopyTakenPhotosToPublicGalleryAppFolder(copyToExternalStorage)
        .setAllowMultiplePickInGallery(allowMultipleUploads)
}
