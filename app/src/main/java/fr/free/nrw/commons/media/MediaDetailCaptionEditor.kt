package fr.free.nrw.commons.media

import android.content.Intent
import android.os.Bundle
import android.view.View
import fr.free.nrw.commons.description.DescriptionEditActivity
import fr.free.nrw.commons.description.EditDescriptionConstants.LIST_OF_DESCRIPTION_AND_CAPTION
import fr.free.nrw.commons.description.EditDescriptionConstants.WIKITEXT
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.UploadMediaDetail
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Objects

/** Loads the descriptions and captions for editing. */
internal fun MediaDetailFragment.onDescriptionEditClicked() {
    binding.progressBarEdit.visibility = View.VISIBLE
    binding.descriptionEdit.visibility = View.GONE
    descriptionList
}

internal val MediaDetailFragment.descriptionList: Unit
    /**
     * Gets descriptions from wikitext
     */
    get() {
        viewDisposables.add(
            mediaDataExtractor
                .getCurrentWikiText(
                    Objects.requireNonNull(media?.filename!!),
                ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { s: String? -> extractCaptionDescription(s!!) },
                    { t: Throwable? -> Timber.e(t) },
                ),
        )
    }

/**
 * Gets captions and descriptions and merge them according to language code and arranges it in a
 * single list.
 * Send the list to DescriptionEditActivity
 * @param s wikitext
 */
internal fun MediaDetailFragment.extractCaptionDescription(s: String) {
    val descriptions: LinkedHashMap<String, String> = MediaDetailDescriptionParser.parse(s)
    val captions: LinkedHashMap<String, String> = captionsList

    val descriptionAndCaptions: ArrayList<UploadMediaDetail> = ArrayList()

    if (captions.size >= descriptions.size) {
        appendCaptionsFirst(captions, descriptions, descriptionAndCaptions)
    } else {
        appendDescriptionsFirst(captions, descriptions, descriptionAndCaptions)
    }
    val intent = Intent(requireContext(), DescriptionEditActivity::class.java)
    val bundle = Bundle()
    bundle.putParcelableArrayList(LIST_OF_DESCRIPTION_AND_CAPTION, descriptionAndCaptions)
    bundle.putString(WIKITEXT, s)
    bundle.putString(
        Prefs.DESCRIPTION_LANGUAGE,
        applicationKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, ""),
    )
    bundle.putParcelable("media", media)
    intent.putExtras(bundle)
    startActivity(intent)
}

private val MediaDetailFragment.captionsList: LinkedHashMap<String, String>
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

/** Adds captions in order and then appends descriptions without captions. */
private fun appendCaptionsFirst(
    captions: Map<String, String>,
    descriptions: Map<String, String>,
    descriptionAndCaptions: MutableList<UploadMediaDetail>,
) {
    for (mapElement: Map.Entry<*, *> in captions.entries) {
        val language: String = mapElement.key as String
        if (descriptions.containsKey(language)) {
            descriptionAndCaptions.add(
                UploadMediaDetail(
                    language,
                    Objects.requireNonNull(descriptions[language]!!),
                    (mapElement.value as String?)!!,
                ),
            )
        } else {
            descriptionAndCaptions.add(
                UploadMediaDetail(
                    language,
                    "",
                    (mapElement.value as String?)!!,
                ),
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
                    "",
                ),
            )
        }
    }
}

/** Adds descriptions in order and then appends captions without descriptions. */
private fun appendDescriptionsFirst(
    captions: Map<String, String>,
    descriptions: Map<String, String>,
    descriptionAndCaptions: MutableList<UploadMediaDetail>,
) {
    for (mapElement: Map.Entry<*, *> in descriptions.entries) {
        val language: String = mapElement.key as String
        if (captions.containsKey(language)) {
            descriptionAndCaptions.add(
                UploadMediaDetail(
                    language,
                    (mapElement.value as String?)!!,
                    Objects.requireNonNull(captions[language]!!),
                ),
            )
        } else {
            descriptionAndCaptions.add(
                UploadMediaDetail(
                    language,
                    (mapElement.value as String?)!!,
                    "",
                ),
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
                    Objects.requireNonNull(descriptions[language]!!),
                ),
            )
        }
    }
}
