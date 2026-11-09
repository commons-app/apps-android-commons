package fr.free.nrw.commons.media

import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.DateUtil.getDateStringWithSkeletonPattern
import timber.log.Timber
import java.util.Date
import java.util.Locale

/** Updates the metadata fields and image for the current file. */
internal fun MediaDetailFragment.setTextFields(media: Media) {
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
 * Returns captions for media details
 *
 * @param media object of class media
 * @return caption as string
 */
internal fun MediaDetailFragment.prettyCaption(media: Media): String {
    for (caption: String in media.captions.values) {
        return if (caption == "") {
            getString(R.string.detail_caption_empty)
        } else {
            caption
        }
    }
    return getString(R.string.detail_caption_empty)
}

/** Formats the displayed description. */
internal fun MediaDetailFragment.prettyDescription(media: Media): String {
    var description: String? = chooseDescription(media)
    if (description!!.isNotEmpty()) {
        // Remove img tag that sometimes appears as a blue square in the app,
        // see https://github.com/commons-app/apps-android-commons/issues/4345
        description = description.replace("[<](/)?img[^>]*[>]".toRegex(), "")
    }
    return description.ifEmpty { getString(R.string.detail_description_empty) }
}

/** Chooses the description to display. */
internal fun MediaDetailFragment.chooseDescription(media: Media): String? {
    val descriptions: Map<String, String> = media.descriptions
    return descriptions[Locale.getDefault().language]
        ?: if (descriptions.isEmpty()) media.fallbackDescription else descriptions.values.first()
}

/** Formats the displayed discussion. */
internal fun MediaDetailFragment.prettyDiscussion(discussion: String): String =
    discussion.ifEmpty {
        getString(R.string.detail_discussion_empty)
    }

/** Formats the license label. */
internal fun MediaDetailFragment.prettyLicense(media: Media): String {
    val licenseKey: String? = media.license
    Timber.d("Media license is: %s", licenseKey)
    if (licenseKey == null || licenseKey == "") {
        return getString(R.string.detail_license_empty)
    }
    return licenseKey
}

/** Formats the upload date. */
internal fun MediaDetailFragment.prettyUploadedDate(media: Media): String {
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
internal fun MediaDetailFragment.prettyCoordinates(media: Media): String {
    if (media.coordinates == null) {
        return getString(R.string.media_detail_coordinates_empty)
    }
    return media.coordinates!!.getPrettyCoordinateString()
}
